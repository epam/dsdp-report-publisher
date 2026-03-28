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

import static com.epam.digital.data.platform.report.service.RoleService.ADMIN_REGISTRY_NAME;
import static com.epam.digital.data.platform.report.service.RoleService.ADMIN_ROLE_NAME;
import static com.epam.digital.data.platform.report.service.RoleService.AUDITOR_REGISTRY_NAME;
import static com.epam.digital.data.platform.report.service.RoleService.AUDITOR_ROLE_NAME;
import static com.epam.digital.data.platform.report.util.ResponseHandler.handleResponse;

import com.epam.digital.data.platform.report.client.DataSourceClient;
import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.exception.NoDataSourceFoundException;
import com.epam.digital.data.platform.report.model.DataSource;
import com.epam.digital.data.platform.report.model.Group;
import com.epam.digital.data.platform.report.model.Role;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DataSourceService {

  private static final String DATASOURCE_NAME_TEMPLATE = "%s_%s";

  private final DataSourceClient client;
  private final RedashProperties redashProperties;

  private final String dbHost;
  private final String dbName;

  public DataSourceService(
      DataSourceClient client,
      RedashProperties redashProperties,
      @Value("${DB_URL}") String dbHost,
      @Value("${DB_NAME}") String dbName) {
    this.client = client;
    this.redashProperties = redashProperties;
    this.dbHost = dbHost;
    this.dbName = dbName;
  }

  public DataSource getDataSource(String language, Group group) {
    var redashLanguageProperties =
        redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    return handleResponse(
            client.getDataSources(
                redashLanguageProperties.getUrl(), redashLanguageProperties.getApiKey()))
        .stream()
        .filter(
            dataSource -> {
              var existingDsName = dataSource.getName();
              var searchedDsName = buildDataSourceNameForRole(group.getName());
              return existingDsName.equalsIgnoreCase(searchedDsName);
            })
        .findFirst()
        .orElseThrow(
            () ->
                new NoDataSourceFoundException(
                    String.format("No datasource found for group: %s", group.getName())));
  }

  public List<DataSource> getDataSources(String language) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    return handleResponse(
        client.getDataSources(
            redashLanguageProperties.getUrl(), redashLanguageProperties.getApiKey()));
  }

  public DataSource createDataSource(String language, DataSource dataSource) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    return handleResponse(
        client.createDataSource(
            redashLanguageProperties.getUrl(), dataSource, redashLanguageProperties.getApiKey()));
  }

  public DataSource updateDataSource(String language, int dataSourceId, DataSource dataSource) {
    var redashLanguageProperties =
        redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    return handleResponse(
        client.updateDataSource(
            redashLanguageProperties.getUrl(), dataSourceId, dataSource, redashLanguageProperties.getApiKey()));
  }

  public void deleteDataSource(String language, DataSource dataSource) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    client.deleteDataSource(
        redashLanguageProperties.getUrl(),
        dataSource.getId(),
        redashLanguageProperties.getApiKey());
  }

  public DataSource buildDataSource(Role role) {
    var map = new HashMap<String, String>();
    map.put("host", dbHost);
    map.put("dbname", dbName);
    map.put("user", "analytics_" + role.getName());
    map.put("password", role.getPassword());

    var dataSource = new DataSource();
    dataSource.setName(buildDataSourceNameForRole(role.getName()));
    dataSource.setQueueName("queries");
    dataSource.setType("pg");
    dataSource.setSyntax("sql");
    dataSource.setOptions(map);

    return dataSource;
  }

  public String buildDataSourceNameForRole(String roleName) {
    return switch (roleName) {
      case AUDITOR_ROLE_NAME -> String.format(DATASOURCE_NAME_TEMPLATE, AUDITOR_REGISTRY_NAME, AUDITOR_ROLE_NAME);
      case ADMIN_ROLE_NAME -> ADMIN_REGISTRY_NAME;
      default -> String.format(DATASOURCE_NAME_TEMPLATE, dbName, roleName);
    };
  }
}
