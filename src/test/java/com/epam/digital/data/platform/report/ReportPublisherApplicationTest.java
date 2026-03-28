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

import static com.epam.digital.data.platform.report.util.TestUtils.dataSources;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.config.properties.AppProperties;
import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.pipeline.AbstractPipeline;
import com.epam.digital.data.platform.report.service.DataSourceService;
import com.epam.digital.data.platform.report.service.ExcerptCsvService;
import com.epam.digital.data.platform.report.service.ExcerptDocxService;
import com.epam.digital.data.platform.report.service.ExcerptPdfService;
import com.epam.digital.data.platform.report.service.CleanupService;
import com.epam.digital.data.platform.report.service.RoleService;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ResourceUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportPublisherApplicationTest {

  @Mock
  private ApplicationArguments args;
  @Mock
  private DataSourceService dataSourceService;
  @Mock
  private AbstractPipeline skippedPipeline;
  @Mock
  private AbstractPipeline appliedPipeline;
  @Mock
  private ExcerptPdfService excerptPdfService;
  @Mock
  private ExcerptDocxService excerptDocxService;
  @Mock
  private ExcerptCsvService excerptCsvService;
  @Mock
  private RoleService roleService;
  @Mock
  private CleanupService cleanupService;

  private AppProperties appProperties;
  private String reportsDirectoryName;
  private String excerptsDirectoryName;
  private String excerptsDocxDirectoryName;
  private String excerptsCsvDirectoryName;

  private ReportPublisherApplication reportPublisherApplication;

  @BeforeEach
  void init() throws FileNotFoundException {
    reportsDirectoryName = ResourceUtils.getFile("classpath:pipeline").getAbsolutePath();
    excerptsDirectoryName = ResourceUtils.getFile("classpath:excerpts").getAbsolutePath();
    excerptsDocxDirectoryName = ResourceUtils.getFile("classpath:excerpts-docx").getAbsolutePath();
    excerptsCsvDirectoryName = ResourceUtils.getFile("classpath:excerpts-csv").getAbsolutePath();

    var pipelines = new ArrayList<AbstractPipeline>();
    pipelines.add(skippedPipeline);
    pipelines.add(appliedPipeline);

    appProperties = new AppProperties();
    appProperties.setReportsDirectoryName(reportsDirectoryName);

    var redashProperties = new RedashProperties();
    redashProperties.setLang(
        Map.of(
            "en",
            new RedashProperties.InstanceProperties(URI.create("http://mock-en"), "apiKeyEn"),
            "uk",
            new RedashProperties.InstanceProperties(
                URI.create("http://mock-uk"), "apiKeyUk")));

    reportPublisherApplication = new ReportPublisherApplication(
        appProperties, redashProperties, dataSourceService, pipelines, roleService);
  }

  @Test
  void shouldApplyPipelineForLocalizedRedashInstances() {
    when(args.containsOption("reports")).thenReturn(true);
    when(skippedPipeline.isApplicable(any())).thenReturn(false);
    when(appliedPipeline.isApplicable(any())).thenReturn(true);
    when(dataSourceService.getDataSources("en")).thenReturn(dataSources("registry"));
    when(dataSourceService.getDataSources("uk")).thenReturn(dataSources("registry"));
    when(dataSourceService.buildDataSourceNameForRole(any())).thenCallRealMethod();

    reportPublisherApplication.run(args);

    verify(skippedPipeline, times(2)).isApplicable(any());
    verify(skippedPipeline, never()).process(any(), any());
    verify(appliedPipeline, times(2)).isApplicable(any());
    verify(appliedPipeline, times(2)).process(any(), any());
  }

  @Test
  void shouldApplyPipelineForLocalizedRedashInstancesForNonExistingLangFolders() {
    var redashProperties = new RedashProperties();
    redashProperties.setLang(
            Map.of(
                    "en",
                    new RedashProperties.InstanceProperties(URI.create("http://mock-en"), "apiKeyEn"),
                    "fr",
                    new RedashProperties.InstanceProperties(
                            URI.create("http://mock-fr"), "apiKeyFr")));
    ReflectionTestUtils.setField(reportPublisherApplication, "redashProperties", redashProperties);

    when(args.containsOption("reports")).thenReturn(true);
    when(skippedPipeline.isApplicable(any())).thenReturn(false);
    when(appliedPipeline.isApplicable(any())).thenReturn(true);
    when(dataSourceService.getDataSources("en")).thenReturn(dataSources("registry"));
    when(dataSourceService.getDataSources("fr")).thenReturn(dataSources("registry"));
    when(dataSourceService.buildDataSourceNameForRole(any())).thenCallRealMethod();

    reportPublisherApplication.run(args);

    verify(skippedPipeline, times(2)).isApplicable(any());
    verify(skippedPipeline, never()).process(any(), any());
    verify(appliedPipeline, times(2)).isApplicable(any());
    verify(appliedPipeline, times(2)).process(any(), any());
  }

  @Test
  void shouldApplyPipelineForDefaultRedashInstances() {
    var redashProperties = new RedashProperties();
    redashProperties.setDefaultInstance(new RedashProperties.InstanceProperties(URI.create("http://mock"), "apiKey"));

    var pipelines = new ArrayList<AbstractPipeline>();
    pipelines.add(skippedPipeline);
    pipelines.add(appliedPipeline);

    reportPublisherApplication = new ReportPublisherApplication(
            appProperties, redashProperties, dataSourceService, pipelines, roleService);

    when(args.containsOption("reports")).thenReturn(true);
    when(skippedPipeline.isApplicable(any())).thenReturn(false);
    when(appliedPipeline.isApplicable(any())).thenReturn(true);
    when(dataSourceService.getDataSources(null)).thenReturn(dataSources("registry"));
    when(dataSourceService.buildDataSourceNameForRole(any())).thenCallRealMethod();

    reportPublisherApplication.run(args);

    verify(skippedPipeline, times(2)).isApplicable(any());
    verify(skippedPipeline, never()).process(any(), any());
    verify(appliedPipeline, times(2)).isApplicable(any());
    ArgumentCaptor<List<File>> processedFilesCaptor = ArgumentCaptor.forClass(List.class);
    verify(appliedPipeline, times(2)).process(processedFilesCaptor.capture(), any());
    assertThat(processedFilesCaptor.getAllValues().get(0)).hasSize(2);
  }

  @Test
  void shouldCallExcerptPdfServiceForEachFolder() {
    when(args.containsOption("excerpts")).thenReturn(true);
    appProperties.setExcerptsDirectoryName(excerptsDirectoryName);
    reportPublisherApplication.setExcerptPdfService(excerptPdfService);

    reportPublisherApplication.run(args);

    verify(excerptPdfService, times(4)).processExcerpt(any());
    verify(excerptPdfService).cleanNonExistingExcerptTemplates(any());
  }

  @Test
  void shouldReturnEmptyListOfFilesWhenExcerptFolderAbsent() {
    when(args.containsOption("excerpts")).thenReturn(true);
    appProperties.setExcerptsDirectoryName(excerptsDirectoryName + "a");
    reportPublisherApplication.setExcerptPdfService(excerptPdfService);

    reportPublisherApplication.run(args);

    verify(excerptPdfService, never()).processExcerpt(any());
  }

  @Test
  void shouldCallExcerptDocxServiceOnce() {
    when(args.containsOption("excerpts-docx")).thenReturn(true);
    reportPublisherApplication.setExcerptDocxService(excerptDocxService);
    appProperties.setExcerptsDocxDirectoryName(excerptsDocxDirectoryName);

    reportPublisherApplication.run(args);

    verify(excerptDocxService, times(1)).processExcerpt(any());
    verify(excerptDocxService).cleanNonExistingExcerptTemplates(any());
  }

  @Test
  void shouldNotProcessIfIncorrectDocxFolder() {
    when(args.containsOption("excerpts-docx")).thenReturn(true);
    appProperties.setExcerptsDocxDirectoryName(excerptsDocxDirectoryName + "a");

    reportPublisherApplication.run(args);

    verify(excerptDocxService, never()).processExcerpt(any());
  }

  @Test
  void shouldCallExcerptCsvServiceOnce() {
    when(args.containsOption("excerpts-csv")).thenReturn(true);
    reportPublisherApplication.setExcerptCsvService(excerptCsvService);
    appProperties.setExcerptsCsvDirectoryName(excerptsCsvDirectoryName);

    reportPublisherApplication.run(args);

    verify(excerptCsvService, times(1)).processExcerpt(any());
    verify(excerptCsvService).cleanNonExistingExcerptTemplates(any());
  }

  @Test
  void shouldNotProcessIfIncorrectCsvFolder() {
    when(args.containsOption("excerpts-csv")).thenReturn(true);
    appProperties.setExcerptsCsvDirectoryName(excerptsCsvDirectoryName + "a");

    reportPublisherApplication.run(args);

    verify(excerptDocxService, never()).processExcerpt(any());
  }

  @Test
  void createAdminServiceGroupWhenAdminOptionGiven() {
    when(args.containsOption("admin")).thenReturn(true);

    reportPublisherApplication.run(args);

    verify(roleService).createAdminRegistry();
  }

  @Test
  void createAuditorServiceGroupWhenAuditorOptionGiven() {
    when(args.containsOption("auditor")).thenReturn(true);

    reportPublisherApplication.run(args);

    verify(roleService).createAuditorGroups();
  }

  @Test
  void shouldCallCleanupServiceWhenCleanupOptionGiven() {
    when(args.containsOption("cleanup")).thenReturn(true);
    reportPublisherApplication.setCleanupService(cleanupService);

    reportPublisherApplication.run(args);

    verify(cleanupService).cleanup();
  }

  @Test
  void shouldNotCallCleanupServiceWhenCleanupOptionAbsent() {
    reportPublisherApplication.setCleanupService(cleanupService);

    reportPublisherApplication.run(args);

    verify(cleanupService, never()).cleanup();
  }
}

