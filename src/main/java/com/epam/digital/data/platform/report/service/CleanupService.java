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

import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.report.repository.ExcerptTemplateRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
public class CleanupService {

  private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

  private static final String CSV = "csv";
  private static final String DOCX = "docx";
  private static final String PDF = "pdf";

  private final ExcerptTemplateRepository repository;
  private final CephService datafactoryCephService;
  private final String bucket;

  public CleanupService(
      ExcerptTemplateRepository repository,
      CephService datafactoryCephService,
      @Value("${datafactory-excerpt-ceph.bucket}") String bucket) {
    this.repository = repository;
    this.datafactoryCephService = datafactoryCephService;
    this.bucket = bucket;
  }

  @Transactional
  public void cleanup() {
    log.info("Starting full cleanup of excerpt templates from DB and S3");
    cleanupCsvTemplates();
    cleanupDocxTemplates();
    cleanupPdfTemplates();
    log.info("Cleanup complete");
  }

  private void cleanupCsvTemplates() {
    var templates = repository.findAllByTemplateType(CSV);
    if (templates.isEmpty()) {
      log.info("No CSV templates found, skipping");
      return;
    }
    log.info("Deleting {} CSV template(s) from S3 and DB", templates.size());
    Set<String> keys = templates.stream()
        .map(t -> CSV + "/" + t.getTemplateName())
        .collect(Collectors.toSet());
    datafactoryCephService.delete(bucket, keys);
    repository.deleteAll(templates);
  }

  private void cleanupDocxTemplates() {
    var templates = repository.findAllByTemplateType(DOCX);
    if (templates.isEmpty()) {
      log.info("No DOCX templates found, skipping");
      return;
    }
    log.info("Deleting {} DOCX template(s) from S3 and DB", templates.size());
    templates.forEach(t -> {
      String prefix = DOCX + "/" + t.getTemplateName() + "/";
      Set<String> keys = datafactoryCephService.getKeys(bucket, prefix);
      datafactoryCephService.delete(bucket, keys);
    });
    repository.deleteAll(templates);
  }

  private void cleanupPdfTemplates() {
    var templates = repository.findAllByTemplateType(PDF);
    if (templates.isEmpty()) {
      log.info("No PDF templates found, skipping");
      return;
    }
    log.info("Deleting {} PDF template(s) from DB", templates.size());
    repository.deleteAll(templates);
  }

  List<String> getSupportedTypes() {
    return List.of(CSV, DOCX, PDF);
  }
}
