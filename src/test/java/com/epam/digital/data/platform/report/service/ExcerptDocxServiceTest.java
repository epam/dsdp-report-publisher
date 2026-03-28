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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.report.exception.ExcerptBuildingException;
import com.epam.digital.data.platform.report.model.ExcerptTemplate;
import com.epam.digital.data.platform.report.repository.ExcerptTemplateRepository;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExcerptDocxServiceTest {

  @Mock
  private ExcerptTemplateRepository repository;
  @Mock
  private CephService cephService;
  @Mock
  private ExcerptTemplate mockTemplate;
  @Captor
  private ArgumentCaptor<ExcerptTemplate> templateCaptor;

  private ExcerptDocxService service;

  private static final String bucket = "excerpt-templates";
  private static final String templateName = "SomeExcerpt";
  private static final String template = "docx/SomeExcerpt";
  private static final String expectedChecksum = "b3b8b6c44639ba8a52e36aecb5d84b805555b2fe2f7ddbae3041fc16e40112e3";
  private static File correctReportFile;

  @BeforeAll
  static void setup() throws URISyntaxException {
    correctReportFile = getFile("/excerpts-docx/SomeExcerpt.docx");
  }

  @BeforeEach
  void init() {
    service = new ExcerptDocxService(repository, cephService, bucket);
  }

  @Test
  void correctSaveToDataBase() {
    service.processExcerpt(correctReportFile);

    verify(repository).save(templateCaptor.capture());
    var captorTemplate = templateCaptor.getValue().getTemplate();
    var captorChecksum = templateCaptor.getValue().getChecksum();
    var captorType = templateCaptor.getValue().getTemplateType();
    var captorName = templateCaptor.getValue().getTemplateName();

    assertThat(captorTemplate).isEqualTo(template);
    assertThat(captorChecksum).isEqualTo(expectedChecksum);
    assertThat(captorType).isEqualTo(ExcerptDocxService.DOCX);
    assertThat(captorName).isEqualTo(templateName);
  }

  @Test
  void shouldNotSaveNewTemplateIfAlreadyExistTemplateWithTheSameNameAndChecksum() {
    when(repository.findFirstByTemplateName(templateName)).thenReturn(mockTemplate);
    when(mockTemplate.getChecksum()).thenReturn(expectedChecksum);

    service.processExcerpt(correctReportFile);

    verify(repository, never()).save(any());
  }

  @Test
  void shouldUpdateSavedTemplateIfOldChecksumNotEqualToNewChecksum() {
    when(repository.findFirstByTemplateName(templateName)).thenReturn(mockTemplate);
    when(mockTemplate.getChecksum()).thenReturn("0");

    service.processExcerpt(correctReportFile);

    verify(mockTemplate).update(templateCaptor.capture());
    var checksum = templateCaptor.getValue().getChecksum();
    var captorTemplate = templateCaptor.getValue().getTemplate();

    assertThat(checksum).isEqualTo(expectedChecksum);
    assertThat(captorTemplate).isEqualTo(template);

    verify(repository).save(mockTemplate);
  }

  @Test
  void verifyCountOfCephServiceCall() {
    when(repository.findFirstByTemplateName(templateName)).thenReturn(mockTemplate);
    when(mockTemplate.getChecksum()).thenReturn(expectedChecksum);

    service.processExcerpt(correctReportFile);

    var actualContentCapture = ArgumentCaptor.forClass(ByteArrayInputStream.class);
    verify(cephService, times(11))
        .put(
            eq(bucket),
            anyString(),
            eq(ExcerptDocxService.EXCERPT_CONTENT_TYPE),
            eq(Collections.emptyMap()),
            actualContentCapture.capture());
  }

  @Test
  public void shouldThrowExceptionWhenSomethingWentWrongWithFileProcess() {
    when(repository.findFirstByTemplateName(templateName)).thenThrow(NullPointerException.class);

    assertThrows(ExcerptBuildingException.class, () -> service.processExcerpt(correctReportFile));
  }

  @Test
  void shouldCleanAllDocxTemplatesIfNoExistingTemplateFiles() {
    when(repository.findAllByTemplateType(ExcerptDocxService.DOCX)).thenReturn(Collections.singletonList(mockTemplate));
    when(mockTemplate.getTemplateName()).thenReturn(templateName);
    when(cephService.getKeys(bucket, "docx/SomeExcerpt/")).thenReturn(Collections.singleton("docx/SomeExcerpt/zipEntry1"));

    service.cleanNonExistingExcerptTemplates(Collections.emptyList());

    verify(repository).deleteAll(Collections.singletonList(mockTemplate));
    verify(cephService).delete(bucket, Collections.singleton("docx/SomeExcerpt/zipEntry1"));
  }

  @Test
  void shouldCleanCsvTemplatesNotInExistingTemplateFiles() {
    var existingTemplates = List.of(new File("ExistingExcerpt2"));
    when(repository.findAllByTemplateTypeAndTemplateNameNotIn(
            ExcerptDocxService.DOCX, List.of("ExistingExcerpt2")))
            .thenReturn(Collections.singletonList(mockTemplate));
    when(mockTemplate.getTemplateName()).thenReturn(templateName);
    when(cephService.getKeys(bucket, "docx/SomeExcerpt/")).thenReturn(Collections.singleton("docx/SomeExcerpt/zipEntry1"));

    service.cleanNonExistingExcerptTemplates(existingTemplates);

    verify(repository).deleteAll(Collections.singletonList(mockTemplate));
    verify(cephService).delete(bucket, Collections.singleton("docx/SomeExcerpt/zipEntry1"));
  }

  private static File getFile(String path) throws URISyntaxException {
    Objects.requireNonNull(path, "path must not be null");

    URL resourceUrl = ExcerptDocxServiceTest.class.getResource(path);
    if (resourceUrl == null) {
      throw new IllegalArgumentException("Resource not found in classpath: " + path);
    }

    return Path.of(resourceUrl.toURI()).normalize().toFile();
  }
}