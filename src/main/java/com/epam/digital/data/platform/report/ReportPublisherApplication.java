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

package com.epam.digital.data.platform.report;

import static com.epam.digital.data.platform.report.service.RoleService.ADMIN_ROLE_NAME;
import static com.epam.digital.data.platform.report.service.RoleService.AUDITOR_ROLE_NAME;
import static com.epam.digital.data.platform.report.util.IOUtils.getChildDirectories;
import static com.epam.digital.data.platform.report.util.IOUtils.getDirectoryFiles;

import com.epam.digital.data.platform.report.config.properties.AppProperties;
import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.DataSource;
import com.epam.digital.data.platform.report.pipeline.AbstractPipeline;
import com.epam.digital.data.platform.report.service.CleanupService;
import com.epam.digital.data.platform.report.service.DataSourceService;
import com.epam.digital.data.platform.report.service.ExcerptService;
import com.epam.digital.data.platform.report.service.RoleService;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Lazy;

@SpringBootApplication
@EnableFeignClients
public class ReportPublisherApplication implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportPublisherApplication.class);
  private static final String LOG_PROCESSING_ROLE_DIR = "Processing directory with reports for role '{}'";

  private final AppProperties appProperties;
  private final RedashProperties redashProperties;
  private final DataSourceService dataSourceService;
  private final List<AbstractPipeline> pipelines;
  private final RoleService roleService;

  private static final String CLEANUP_OPTION = "cleanup";

  private ExcerptService excerptPdfService;
  private ExcerptService excerptDocxService;
  private ExcerptService excerptCsvService;
  private CleanupService cleanupService;


  public ReportPublisherApplication(
      AppProperties appProperties,
      RedashProperties redashProperties,
      DataSourceService dataSourceService,
      List<AbstractPipeline> pipelines,
      RoleService roleService) {
    this.appProperties = appProperties;
    this.redashProperties = redashProperties;
    this.dataSourceService = dataSourceService;
    this.pipelines = new ArrayList<>(Objects.requireNonNullElse(pipelines, Collections.emptyList()));
    this.roleService = roleService;
  }

  public static void main(String[] args) {
    SpringApplication.run(ReportPublisherApplication.class, args);
  }

  @Override
  public void run(ApplicationArguments args) {
    if (args.containsOption(ADMIN_ROLE_NAME)) {
      roleService.createAdminRegistry();
    }

    if (args.containsOption(AUDITOR_ROLE_NAME)) {
      roleService.createAuditorGroups();
    }

    if (args.containsOption("roles")) {
      handleRoles();
    }

    if (args.containsOption("reports")) {
      handleReports();
    }

    if (args.containsOption("excerpts")) {
      handlePdfExcerpts();
    }

    if (args.containsOption("excerpts-docx")) {
      handleExcerptsDocx();
    }

    if (args.containsOption("excerpts-csv")) {
      handleExcerptsCsv();
    }

    if (args.containsOption(CLEANUP_OPTION)) {
      cleanupService.cleanup();
    }
  }

  private void handleReports() {
    handleReportsForLangSpecificRedash();
    handleReportsForDefaultRedash();
  }

  private void handleReportsForLangSpecificRedash() {
    redashProperties
        .getLang()
        .forEach(
            (lang, properties) -> {
              LOGGER.info("Creating reports for language '{}'", lang);
              var dataSources = dataSourceService.getDataSources(lang);

              var reportsDirectory = FileUtils.getFile(appProperties.getReportsDirectoryName());
              for (File roleDir : getChildDirectories(reportsDirectory)) {
                LOGGER.info(LOG_PROCESSING_ROLE_DIR, roleDir.getName());

                var reportsRoleDirectory = FileUtils.getFile(reportsDirectory, roleDir.getName());
                var childDirectoriesNameToContent =
                    getChildDirectories(reportsRoleDirectory).stream()
                        .collect(Collectors.toMap(File::getName, Function.identity()));
                getDataSource(dataSources, roleDir.getName())
                    .ifPresent(
                        dataSourceId -> {
                          var context = createContext(dataSourceId, lang);
                          var files = getDirectoryFiles(childDirectoriesNameToContent.get(lang));

                          processPipelines(context, files);
                        });
              }
            });
  }

  private void handleReportsForDefaultRedash() {
    if (redashProperties.getDefaultInstance() == null) {
      LOGGER.info("Default instance of redash is not configured");
      return;
    }
    var dataSources = dataSourceService.getDataSources(null);

    LOGGER.info("Creating reports for default instance of redash");
    var reportsDirectory = FileUtils.getFile(appProperties.getReportsDirectoryName());
    for (File roleDir : getChildDirectories(reportsDirectory)) {
      LOGGER.info(LOG_PROCESSING_ROLE_DIR, roleDir.getName());

      var reportsRoleDirectory = FileUtils.getFile(reportsDirectory, roleDir.getName());
      for (File langReportDir : getChildDirectories(reportsRoleDirectory)) {
        getDataSource(dataSources, roleDir.getName())
            .ifPresent(
                dataSourceId -> {
                  var context = createContext(dataSourceId, null);
                  var files = getDirectoryFiles(langReportDir);

                  processPipelines(context, files);
                });
      }
    }
  }

  private void handleRoles() {
    var directory = FileUtils.getFile(appProperties.getRolesDirectoryName());
    var files = getDirectoryFiles(directory);

    processPipelines(null, files);
  }

  private void handlePdfExcerpts() {
    var excerptsDirectory = FileUtils.getFile(appProperties.getExcerptsDirectoryName());
    var pdfTemplates = getChildDirectories(excerptsDirectory);
    for (File templateDir : pdfTemplates) {
      LOGGER.info("Processing directory '{}'", templateDir.getName());
      excerptPdfService.processExcerpt(templateDir);
    }
    excerptPdfService.cleanNonExistingExcerptTemplates(pdfTemplates);
  }

  private void handleExcerptsDocx() {
    handleSingleFileExcerpts(excerptDocxService, appProperties.getExcerptsDocxDirectoryName());
  }

  private void handleExcerptsCsv() {
    handleSingleFileExcerpts(excerptCsvService, appProperties.getExcerptsCsvDirectoryName());
  }

  private void handleSingleFileExcerpts(ExcerptService excerptService, String directoryName) {
    var directory = FileUtils.getFile(directoryName);
    if (!directory.exists()) {
      LOGGER.error("Directory '{}' does not exist", directory);
      return;
    }
    var templates = getDirectoryFiles(directory);
    for (File templateFile : templates) {
      LOGGER.info("Processing file '{}'", templateFile.getName());
      excerptService.processExcerpt(templateFile);
    }
    excerptService.cleanNonExistingExcerptTemplates(templates);
  }

  private void processPipelines(Context context, List<File> files) {
    for (var pipeline : pipelines) {
      if (pipeline.isApplicable(files)) {
        pipeline.process(files, context);
      }
    }
  }

  private Context createContext(Integer dataSourceId, String language) {
    var context = new Context();
    context.setDataSourceId(dataSourceId);
    context.setLanguage(language);
    return context;
  }

  private Optional<Integer> getDataSource(List<DataSource> dataSources, String name) {
    return getDataSourceId(
        dataSources,
        dataSource ->
            dataSource
                .getName()
                .equalsIgnoreCase(dataSourceService.buildDataSourceNameForRole(name)));
  }

  private Optional<Integer> getDataSourceId(List<DataSource> dataSources,
      Predicate<? super DataSource> rule) {
    return dataSources.stream()
        .filter(rule)
        .map(DataSource::getId)
        .findFirst();
  }

  @Autowired
  @Lazy
  public void setExcerptPdfService(ExcerptService excerptPdfService) {
    this.excerptPdfService = excerptPdfService;
  }

  @Autowired
  @Lazy
  public void setExcerptDocxService(ExcerptService excerptDocxService) {
    this.excerptDocxService = excerptDocxService;
  }

  @Autowired
  @Lazy
  public void setExcerptCsvService(ExcerptService excerptCsvService) {
    this.excerptCsvService = excerptCsvService;
  }

  @Autowired
  @Lazy
  public void setCleanupService(CleanupService cleanupService) {
    this.cleanupService = cleanupService;
  }
}
