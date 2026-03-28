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
import com.epam.digital.data.platform.report.exception.ExcerptBuildingException;
import com.epam.digital.data.platform.report.model.ExcerptTemplate;
import com.epam.digital.data.platform.report.repository.ExcerptTemplateRepository;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.digital.data.platform.report.util.ExcerptCreationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import static com.epam.digital.data.platform.report.util.DBUtils.saveToDataBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
public class ExcerptCsvService implements ExcerptService {

  static final String EXCERPT_CONTENT_TYPE = "application/octet-stream";
  static final String CSV = "csv";

  private final Logger log = LoggerFactory.getLogger(ExcerptCsvService.class);

  private final ExcerptTemplateRepository repository;
  private final CephService datafactoryCephService;
  private final String bucket;

  public ExcerptCsvService(ExcerptTemplateRepository repository,
      CephService datafactoryCephService,
      @Value("${datafactory-excerpt-ceph.bucket}") String bucket) {
    this.repository = repository;
    this.datafactoryCephService = datafactoryCephService;
    this.bucket = bucket;
  }

  @Override
  @Transactional
  public void processExcerpt(File file) {
    try {
      saveFileToCeph(file);
      saveToDataBase(repository, ExcerptCreationUtils.createTemplate(file, CSV));
    } catch (Exception e) {
      throw new ExcerptBuildingException("Failed to process csv file", e);
    }
  }

  @Override
  @Transactional
  public void cleanNonExistingExcerptTemplates(List<File> existingTemplates) {
    var existingTemplateNames =
        existingTemplates.stream().map(file -> FilenameUtils.getBaseName(file.getName())).toList();
    List<ExcerptTemplate> templatesToDelete;
    if (existingTemplateNames.isEmpty()) {
      templatesToDelete = repository.findAllByTemplateType(CSV);
    } else {
      templatesToDelete =
          repository.findAllByTemplateTypeAndTemplateNameNotIn(CSV, existingTemplateNames);
    }
    repository.deleteAll(templatesToDelete);
    var cephKeysToDelete =
        templatesToDelete.stream()
            .map(ExcerptTemplate::getTemplateName)
            .map(this::getExcerptCephKey)
            .collect(Collectors.toSet());
    datafactoryCephService.delete(bucket, cephKeysToDelete);
  }

  private void saveFileToCeph(File file) {
    try {
      var cephKey = getExcerptCephKey(FilenameUtils.getBaseName(file.getName()));
      byte[] bytes = FileUtils.readFileToByteArray(file);
      datafactoryCephService.put(
          bucket, cephKey, EXCERPT_CONTENT_TYPE, Collections.emptyMap(),
          new ByteArrayInputStream(bytes));
    } catch (IOException e) {
      log.error("Error when extract content from {} file", file.getName());
      throw new ExcerptBuildingException("Failed to extract content", e);
    }

  }

  private String getExcerptCephKey(String excerptName) {
    return CSV + ExcerptCreationUtils.SLASH_DELIMITER + excerptName;
  }
}
