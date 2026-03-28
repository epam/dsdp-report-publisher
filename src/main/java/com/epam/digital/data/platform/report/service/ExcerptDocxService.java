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
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.epam.digital.data.platform.report.util.ExcerptCreationUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import static com.epam.digital.data.platform.report.util.DBUtils.saveToDataBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
public class ExcerptDocxService implements ExcerptService {

  static final String EXCERPT_CONTENT_TYPE = "application/octet-stream";
  static final String DOCX = "docx";

  private final Logger log = LoggerFactory.getLogger(ExcerptDocxService.class);

  private final ExcerptTemplateRepository repository;
  private final CephService datafactoryCephService;
  private final String bucket;

  public ExcerptDocxService(ExcerptTemplateRepository repository,
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
      extractContentAndSaveToCeph(file);
      saveToDataBase(repository, ExcerptCreationUtils.createTemplate(file, DOCX));
    } catch (Exception e) {
      throw new ExcerptBuildingException("Failed to process docx file", e);
    }
  }

  @Override
  @Transactional
  public void cleanNonExistingExcerptTemplates(List<File> existingTemplates) {
    var existingTemplateNames =
            existingTemplates.stream().map(file -> FilenameUtils.getBaseName(file.getName())).toList();
    List<ExcerptTemplate> templatesToDelete;
    if (existingTemplateNames.isEmpty()) {
      templatesToDelete = repository.findAllByTemplateType(DOCX);
    } else {
      templatesToDelete =
              repository.findAllByTemplateTypeAndTemplateNameNotIn(DOCX, existingTemplateNames);
    }
    repository.deleteAll(templatesToDelete);
    var cephKeyPrefixesToDelete = templatesToDelete.stream()
            .map(ExcerptTemplate::getTemplateName)
            .map(this::getExcerptCephKeyPrefix)
            .collect(Collectors.toSet());
    cephKeyPrefixesToDelete.forEach(cephKeyPrefix -> {
      var keysToDelete = datafactoryCephService.getKeys(bucket, cephKeyPrefix);
      datafactoryCephService.delete(bucket, keysToDelete);
    });
  }

  private void extractContentAndSaveToCeph(File file) {
    try (var fis = Files.newInputStream(file.toPath());
        var zipIn = new ZipInputStream(fis);
        ZipFile zipFile = new ZipFile(file)) {
      ZipEntry entry = zipIn.getNextEntry();
      while (entry != null) {
        var cephKey = getExcerptCephKeyPrefix(FilenameUtils.getBaseName(file.getName())) + entry.getName();
        var bytes = IOUtils.toByteArray(zipFile.getInputStream(entry));
        saveFileToCeph(cephKey, bytes);

        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
    } catch (IOException e) {
      log.error("Error when extract content from {} file", file.getName());
      throw new ExcerptBuildingException("Failed to extract content", e);
    }
  }

  private void saveFileToCeph(String cephKey, byte[] bytes) {
    datafactoryCephService.put(
        bucket, cephKey, EXCERPT_CONTENT_TYPE, Collections.emptyMap(),
        new ByteArrayInputStream(bytes));
  }

  private String getExcerptCephKeyPrefix(String excerptName) {
    return DOCX + ExcerptCreationUtils.SLASH_DELIMITER + excerptName + ExcerptCreationUtils.SLASH_DELIMITER;
  }
}
