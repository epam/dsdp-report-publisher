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

import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.exception.NoGroupFoundException;
import com.epam.digital.data.platform.report.model.DataSource;
import com.epam.digital.data.platform.report.model.Group;
import com.epam.digital.data.platform.report.model.Role;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoleService {

  public static final String AUDITOR_ROLE_NAME = "auditor";
  public static final String ADMIN_ROLE_NAME = "admin";
  public static final String ADMIN_REGISTRY_NAME = "registry";
  public static final String AUDITOR_REGISTRY_NAME = "audit";
  public static final String REDASH_ADMIN_ROLE_NAME = "redash-admin";

  private final Logger log = LoggerFactory.getLogger(RoleService.class);

  private final GroupService groupService;
  private final DataSourceService dataSourceService;
  private final RedashProperties redashProperties;

  private final Map<String, String> rolePasswordMap = new HashMap<>();

  public RoleService(
      @Value("${PWD_ADMIN}") String adminPassword,
      @Value("${PWD_AUDITOR}") String auditorPassword,
      GroupService groupService,
      DataSourceService dataSourceService,
      RedashProperties redashProperties) {
    this.groupService = groupService;
    this.dataSourceService = dataSourceService;
    this.redashProperties = redashProperties;

    this.rolePasswordMap.put(ADMIN_ROLE_NAME, adminPassword);
    this.rolePasswordMap.put(AUDITOR_ROLE_NAME, auditorPassword);
  }

  public void createAuditorGroups() {
    redashProperties.getLang().keySet().forEach(this::createAuditorGroupForLang);
    Optional.ofNullable(redashProperties.getDefaultInstance())
        .ifPresent(props -> createAuditorGroupForLang(null));
  }

  private void createAuditorGroupForLang(String lang) {
    if (!isGroupCreated(lang, AUDITOR_ROLE_NAME)) {
      log.info("Creating auditor group and datasource for language {}", lang);

      var group = new Group(AUDITOR_ROLE_NAME);
      var role = new Role(AUDITOR_ROLE_NAME);

      role.setPassword(rolePasswordMap.get(AUDITOR_ROLE_NAME));
      var dataSource = dataSourceService.buildDataSource(role);

      var createdGroup = groupService.createGroup(lang, group);
      var createdDataSource = dataSourceService.createDataSource(lang, dataSource);
      groupService.associate(lang, createdDataSource, createdGroup);
    }
  }

  public void createAdminRegistry() {
    redashProperties.getLang().keySet().forEach(this::createAdminRegistryForLang);
    Optional.ofNullable(redashProperties.getDefaultInstance())
        .ifPresent(props -> createAdminRegistryForLang(null));
  }

  private void createAdminRegistryForLang(String lang) {
    var redashAdminGroup = groupService.getGroup(lang, new Role(REDASH_ADMIN_ROLE_NAME))
            .orElseThrow(() -> new NoGroupFoundException(REDASH_ADMIN_ROLE_NAME));
    DataSource adminDataSource;
    var existingAdminDataSourceOpt = getAdminDataSource(lang);
    if (existingAdminDataSourceOpt.isEmpty()) {
      log.info("Creating admin datasource for language {}", lang);

      var role = new Role(ADMIN_ROLE_NAME);

      role.setPassword(rolePasswordMap.get(ADMIN_ROLE_NAME));
      var dataSource = dataSourceService.buildDataSource(role);
      dataSource.setName(ADMIN_REGISTRY_NAME);

      adminDataSource = dataSourceService.createDataSource(lang, dataSource);
    } else {
      adminDataSource = existingAdminDataSourceOpt.get();
    }
    groupService.associate(lang, adminDataSource, redashAdminGroup);
  }

  public void create(String language, Role role) {
    log.info("Creating role {} for language {}", role.getName(), language);

    var group = new Group(role.getName());

    var dataSource = dataSourceService.buildDataSource(role);

    createAndAssociate(language, group, dataSource);
  }

  public void update(String language, Role role) {
    log.info("Updating role {} for language {}", role.getName(), language);

    var group = new Group(role.getName());
    var existingDataSource = dataSourceService.getDataSource(language, group);
    log.info("Retrieved existing datasource with id {} for role {}", existingDataSource.getId(), role.getName());
    var updatedDataSource = dataSourceService.buildDataSource(role);
    log.info("Updating data source {} with new password {}", updatedDataSource.getName(), role.getPassword());
    dataSourceService.updateDataSource(language, existingDataSource.getId(), updatedDataSource);
  }

  public void delete(String language, Group group) {
    log.info("Deleting group {} for language {}", group.getName(), language);

    var dataSource = dataSourceService.getDataSource(language, group);
    dataSourceService.deleteDataSource(language, dataSource);
    groupService.deleteGroup(language, group);
  }

  private void createAndAssociate(String language, Group group, DataSource dataSource) {
    log.info("Creating group {} for language {}", group.getName(), language);
    var createdGroup = groupService.createGroup(language, group);
    log.info("Creating data source {} for language {}", dataSource.getName(), language);
    var createdDataSource = dataSourceService.createDataSource(language, dataSource);
    log.info("Associating group {} with data source {} for language {}", group.getName(), dataSource.getName(), language);
    groupService.associate(language, createdDataSource, createdGroup);
  }

  private Optional<DataSource> getAdminDataSource(String language) {
    return dataSourceService.getDataSources(language).stream()
        .filter(dataSource -> dataSource.getName().equals(ADMIN_REGISTRY_NAME))
        .findFirst();
  }

  private boolean isGroupCreated(String language, String role) {
    return groupService.getGroup(language, new Role(role)).isPresent();
  }
}
