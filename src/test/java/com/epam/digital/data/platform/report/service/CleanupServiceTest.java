/*
 * Copyright 2021 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.report.model.ExcerptTemplate;
import com.epam.digital.data.platform.report.repository.ExcerptTemplateRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CleanupServiceTest {

  private static final String BUCKET = "excerpt-templates";
  private static final String CSV = "csv";
  private static final String DOCX = "docx";
  private static final String PDF = "pdf";

  @Mock
  private ExcerptTemplateRepository repository;
  @Mock
  private CephService cephService;

  private CleanupService service;

  @BeforeEach
  void init() {
    service = new CleanupService(repository, cephService, BUCKET);
  }

  // --- CSV ---

  @Test
  void shouldDeleteCsvTemplatesFromS3AndDb() {
    var template = excerptTemplate("report1", CSV);
    when(repository.findAllByTemplateType(CSV)).thenReturn(List.of(template));

    service.cleanup();

    verify(cephService).delete(BUCKET, Set.of("csv/report1"));
    verify(repository).deleteAll(List.of(template));
  }

  @Test
  void shouldDeleteMultipleCsvTemplates() {
    var t1 = excerptTemplate("report1", CSV);
    var t2 = excerptTemplate("report2", CSV);
    when(repository.findAllByTemplateType(CSV)).thenReturn(List.of(t1, t2));

    service.cleanup();

    verify(cephService).delete(BUCKET, Set.of("csv/report1", "csv/report2"));
    verify(repository).deleteAll(List.of(t1, t2));
  }

  @Test
  void shouldSkipCsvCleanupWhenNoCsvTemplatesExist() {
    when(repository.findAllByTemplateType(CSV)).thenReturn(Collections.emptyList());
    when(repository.findAllByTemplateType(DOCX)).thenReturn(Collections.emptyList());
    when(repository.findAllByTemplateType(PDF)).thenReturn(Collections.emptyList());

    service.cleanup();

    verify(cephService, never()).delete(any(), any());
    verify(repository, never()).deleteAll(any());
  }

  // --- DOCX ---

  @Test
  void shouldDeleteDocxTemplatesFromS3AndDb() {
    var template = excerptTemplate("contract", DOCX);
    when(repository.findAllByTemplateType(CSV)).thenReturn(Collections.emptyList());
    when(repository.findAllByTemplateType(DOCX)).thenReturn(List.of(template));
    when(repository.findAllByTemplateType(PDF)).thenReturn(Collections.emptyList());
    when(cephService.getKeys(BUCKET, "docx/contract/"))
        .thenReturn(Set.of("docx/contract/word/document.xml"));

    service.cleanup();

    verify(cephService).getKeys(BUCKET, "docx/contract/");
    verify(cephService).delete(BUCKET, Set.of("docx/contract/word/document.xml"));
    verify(repository).deleteAll(List.of(template));
  }

  @Test
  void shouldDeleteMultipleDocxTemplates() {
    var t1 = excerptTemplate("contract", DOCX);
    var t2 = excerptTemplate("invoice", DOCX);
    when(repository.findAllByTemplateType(CSV)).thenReturn(Collections.emptyList());
    when(repository.findAllByTemplateType(DOCX)).thenReturn(List.of(t1, t2));
    when(repository.findAllByTemplateType(PDF)).thenReturn(Collections.emptyList());
    when(cephService.getKeys(BUCKET, "docx/contract/"))
        .thenReturn(Set.of("docx/contract/word/document.xml"));
    when(cephService.getKeys(BUCKET, "docx/invoice/"))
        .thenReturn(Set.of("docx/invoice/word/document.xml", "docx/invoice/[Content_Types].xml"));

    service.cleanup();

    verify(cephService).delete(BUCKET, Set.of("docx/contract/word/document.xml"));
    verify(cephService).delete(BUCKET,
        Set.of("docx/invoice/word/document.xml", "docx/invoice/[Content_Types].xml"));
    verify(repository).deleteAll(List.of(t1, t2));
  }

  @Test
  void shouldSkipDocxCleanupWhenNoDocxTemplatesExist() {
    when(repository.findAllByTemplateType(CSV)).thenReturn(Collections.emptyList());
    when(repository.findAllByTemplateType(DOCX)).thenReturn(Collections.emptyList());
    when(repository.findAllByTemplateType(PDF)).thenReturn(Collections.emptyList());

    service.cleanup();

    verify(cephService, never()).getKeys(BUCKET, "docx/");
  }

  // --- PDF ---

  @Test
  void shouldDeletePdfTemplatesFromDbOnly() {
    var template = excerptTemplate("extract", PDF);
    when(repository.findAllByTemplateType(CSV)).thenReturn(Collections.emptyList());
    when(repository.findAllByTemplateType(DOCX)).thenReturn(Collections.emptyList());
    when(repository.findAllByTemplateType(PDF)).thenReturn(List.of(template));

    service.cleanup();

    verify(repository).deleteAll(List.of(template));
    verify(cephService, never()).delete(BUCKET, Set.of("pdf/extract"));
  }

  @Test
  void shouldSkipPdfCleanupWhenNoPdfTemplatesExist() {
    when(repository.findAllByTemplateType(CSV)).thenReturn(Collections.emptyList());
    when(repository.findAllByTemplateType(DOCX)).thenReturn(Collections.emptyList());
    when(repository.findAllByTemplateType(PDF)).thenReturn(Collections.emptyList());

    service.cleanup();

    verify(repository, never()).deleteAll(any());
  }

  // --- cleanup() orchestration ---

  @Test
  void shouldCleanupAllTypesInSingleCall() {
    var csv = excerptTemplate("csvReport", CSV);
    var docx = excerptTemplate("docxReport", DOCX);
    var pdf = excerptTemplate("pdfReport", PDF);

    when(repository.findAllByTemplateType(CSV)).thenReturn(List.of(csv));
    when(repository.findAllByTemplateType(DOCX)).thenReturn(List.of(docx));
    when(repository.findAllByTemplateType(PDF)).thenReturn(List.of(pdf));
    when(cephService.getKeys(BUCKET, "docx/docxReport/"))
        .thenReturn(Set.of("docx/docxReport/word/document.xml"));

    service.cleanup();

    verify(cephService).delete(BUCKET, Set.of("csv/csvReport"));
    verify(repository).deleteAll(List.of(csv));

    verify(cephService).delete(BUCKET, Set.of("docx/docxReport/word/document.xml"));
    verify(repository).deleteAll(List.of(docx));

    verify(repository).deleteAll(List.of(pdf));
  }

  // --- getSupportedTypes ---

  @Test
  void shouldReturnAllSupportedTypes() {
    assertThat(service.getSupportedTypes()).containsExactly(CSV, DOCX, PDF);
  }

  // --- helpers ---

  private ExcerptTemplate excerptTemplate(String name, String type) {
    var t = new ExcerptTemplate();
    t.setTemplateName(name);
    t.setTemplateType(type);
    return t;
  }
}
