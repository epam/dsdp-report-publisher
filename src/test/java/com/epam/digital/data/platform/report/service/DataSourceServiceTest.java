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

import static com.epam.digital.data.platform.report.util.TestUtils.REDASH_EN_API_KEY;
import static com.epam.digital.data.platform.report.util.TestUtils.REDASH_EN_URL;
import static com.epam.digital.data.platform.report.util.TestUtils.dataSources;
import static com.epam.digital.data.platform.report.util.TestUtils.redashProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.client.DataSourceClient;
import com.epam.digital.data.platform.report.exception.NoDataSourceFoundException;
import com.epam.digital.data.platform.report.model.DataSource;
import com.epam.digital.data.platform.report.model.Group;
import com.epam.digital.data.platform.report.model.Role;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class DataSourceServiceTest {

  @Mock
  private DataSourceClient client;

  private final String DB_HOST = "HOST";
  private final String DB_NAME = "dbname";
  private static final String TEST_PASSWORD = UUID.randomUUID().toString();

  private DataSourceService instance;

  @BeforeEach
  void init() {
    instance = new DataSourceService(client, redashProperties(), DB_HOST, DB_NAME);
  }

  @Test
  void getDataSourceHappyPath() {
    var group = new Group("officer");
    when(client.getDataSources(REDASH_EN_URL, REDASH_EN_API_KEY)).thenReturn(listResponse("dbname_officer"));

    var result = instance.getDataSource("en", group);

    assertThat(result.getName()).isEqualTo("dbname_officer");
  }

  @Test
  void getDataSourceThrowExceptionWhenNoDataSourceFound() {
    var group = new Group("citizen");
    when(client.getDataSources(REDASH_EN_URL, REDASH_EN_API_KEY)).thenReturn(listResponse("dbname_officer"));

    assertThatThrownBy(() -> instance.getDataSource("en", group))
        .isInstanceOf(NoDataSourceFoundException.class)
        .hasMessage("No datasource found for group: citizen");
  }

  @Test
  void getDataSourceThrowExceptionWhenDataSourceNamesContainsGroupButNotEquals() {
    var group = new Group("birth-officer");
    when(client.getDataSources(REDASH_EN_URL, REDASH_EN_API_KEY)).thenReturn(listResponse("dbname_officer"));

    assertThatThrownBy(() -> instance.getDataSource("en", group))
            .isInstanceOf(NoDataSourceFoundException.class)
            .hasMessage("No datasource found for group: birth-officer");
  }

  @Test
  void shouldDeleteDataSource() {
    var dataSource = new DataSource();
    dataSource.setId(1);

    instance.deleteDataSource("en", dataSource);

    verify(client).deleteDataSource(REDASH_EN_URL, 1, REDASH_EN_API_KEY);
  }

  @Test
  void shouldCreateDataSource() {
    var dataSource = new DataSource();
    dataSource.setId(1);

    when(client.createDataSource(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY))).thenReturn(entityResponse());

    instance.createDataSource("en", dataSource);

    verify(client).createDataSource(REDASH_EN_URL, dataSource, REDASH_EN_API_KEY);
  }

  @Test
  void shouldUpdateDataSource() {
    var dataSource = new DataSource();
    dataSource.setId(1);
    dataSource.setName("dashboard");

    when(client.updateDataSource(REDASH_EN_URL, 2, dataSource, REDASH_EN_API_KEY)).thenReturn(entityResponse());

    instance.updateDataSource("en", 2, dataSource);

    verify(client).updateDataSource(REDASH_EN_URL, 2, dataSource, REDASH_EN_API_KEY);
  }

  @Test
  void getDataSources() {
    when(client.getDataSources(REDASH_EN_URL, REDASH_EN_API_KEY)).thenReturn(listResponse());

    instance.getDataSources("en");

    verify(client).getDataSources(REDASH_EN_URL, REDASH_EN_API_KEY);
  }

  @Test
  void buildDataSource() {
    var role = new Role();
    role.setName("officer");
    role.setPassword(TEST_PASSWORD);

    var result = instance.buildDataSource(role);
    var options = result.getOptions();

    assertThat(options.get("host")).isEqualTo(DB_HOST);
    assertThat(options.get("dbname")).isEqualTo(DB_NAME);
    assertThat(options.get("user")).isEqualTo("analytics_officer");
    assertThat(options.get("password")).isEqualTo(TEST_PASSWORD);
    assertThat(result.getName()).isEqualTo(DB_NAME + "_" + role.getName());
  }

  @Test
  void expectCorrectDataSourceBuiltForAuditor() {
    var role = new Role();
    role.setName("auditor");
    role.setPassword(TEST_PASSWORD);

    var result = instance.buildDataSource(role);
    var options = result.getOptions();

    assertThat(options.get("host")).isEqualTo(DB_HOST);
    assertThat(options.get("dbname")).isEqualTo(DB_NAME);
    assertThat(options.get("user")).isEqualTo("analytics_auditor");
    assertThat(options.get("password")).isEqualTo(TEST_PASSWORD);
    assertThat(result.getName()).isEqualTo("audit_auditor");
  }

  private ResponseEntity<List<DataSource>> listResponse(String... dsNames) {
    return new ResponseEntity<>(dataSources(dsNames), HttpStatus.OK);
  }

  private ResponseEntity<DataSource> entityResponse() {
    var dataSource = new DataSource();
    dataSource.setName("registry");
    dataSource.setId(1);

    return new ResponseEntity<>(dataSource, HttpStatus.OK);
  }
}
