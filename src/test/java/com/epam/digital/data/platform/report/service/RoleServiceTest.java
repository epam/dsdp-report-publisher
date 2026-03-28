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

import static com.epam.digital.data.platform.report.util.TestUtils.redashProperties;
import static com.epam.digital.data.platform.report.util.TestUtils.redashPropertiesForAdmin;
import static com.epam.digital.data.platform.report.util.TestUtils.dataSource;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.model.DataSource;
import com.epam.digital.data.platform.report.model.Group;
import com.epam.digital.data.platform.report.model.Role;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

  @Mock
  GroupService groupService;

  @Mock
  DataSourceService dataSourceService;

  private static final String ADMIN_PASS = "admPass";
  private static final String AUDITOR_PASS = "audPass";

  RoleService instance;

  @BeforeEach
  void init() {
    instance =
        new RoleService(
            ADMIN_PASS,
            AUDITOR_PASS,
            groupService,
            dataSourceService,
            redashProperties());
  }

  @Test
  void shouldSkipAuditorGroupCreationIfAuditorAlreadyExists() {
    var group = new Group();
    group.setName("auditor");

    when(groupService.getGroup(eq("en"), any())).thenReturn(Optional.of(group));

    instance.createAuditorGroups();

    verify(dataSourceService, never()).buildDataSource(any());
    verify(dataSourceService, never()).createDataSource(eq("en"), any());
    verify(groupService, never()).createGroup(eq("en"), any());
    verify(groupService, never()).associate(eq("en"), any(), any());
  }

  @Test
  void shouldCreateAuditorGroup() {
    when(groupService.getGroup(eq("en"), any())).thenReturn(Optional.empty());

    instance.createAuditorGroups();

    verify(dataSourceService).buildDataSource(any());
    verify(dataSourceService).createDataSource(eq("en"), any());
    verify(groupService).createGroup(eq("en"), any());
    verify(groupService).associate(eq("en"), any(), any());
  }

  @Test
  void shouldSkipAdminDataSourceCreationIfAdminAlreadyExists() {
    instance =
        new RoleService(
            ADMIN_PASS, AUDITOR_PASS, groupService, dataSourceService, redashPropertiesForAdmin());
    var redashAdminGroup = new Group("redash-admin");
    when(groupService.getGroup(isNull(), any())).thenReturn(Optional.of(redashAdminGroup));
    var registryDs = dataSource("registry", 1);
    var officerRegistryDs = dataSource("officer registry", 2);
    when(dataSourceService.getDataSources(null)).thenReturn(List.of(registryDs, officerRegistryDs));

    instance.createAdminRegistry();

    verify(dataSourceService, never()).buildDataSource(any());
    verify(dataSourceService, never()).createDataSource(isNull(), any());
    verify(groupService).associate(null, registryDs, redashAdminGroup);
  }

  @Test
  void shouldCreateAdminDataSource() {
    instance =
            new RoleService(
                    ADMIN_PASS, AUDITOR_PASS, groupService, dataSourceService, redashPropertiesForAdmin());
    var redashAdminGroup = new Group("redash-admin");
    when(groupService.getGroup(isNull(), any())).thenReturn(Optional.of(redashAdminGroup));
    when(dataSourceService.getDataSources(null)).thenReturn(emptyList());
    when(dataSourceService.buildDataSource(any())).thenReturn(new DataSource());
    var createdDs = new DataSource();
    when(dataSourceService.createDataSource(isNull(), any())).thenReturn(createdDs);

    instance.createAdminRegistry();

    verify(dataSourceService).buildDataSource(any());
    verify(dataSourceService).createDataSource(isNull(), any());
    verify(groupService).associate(null, createdDs, redashAdminGroup);
  }

  @Test
  void shouldCreateRole() {
    when(dataSourceService.buildDataSource(any())).thenReturn(new DataSource());
    var createdGroup = new Group("officer");
    when(groupService.createGroup(eq("en"), any())).thenReturn(createdGroup);

    var role = new Role();
    role.setName("officer");

    instance.create("en", role);

    verify(dataSourceService).buildDataSource(role);
    verify(dataSourceService).createDataSource(eq("en"), any());
    verify(groupService).createGroup(eq("en"), any());
    verify(groupService).associate(eq("en"), any(), eq(createdGroup));
  }

  @Test
  void shouldUpdateRole() {
    var existingDataSource = new DataSource();
    existingDataSource.setId(1);
    when(dataSourceService.getDataSource(eq("en"), any())).thenReturn(existingDataSource);

    var role = new Role();
    role.setName("officer");

    var newDataSource = new DataSource();
    newDataSource.setName("new_ds");
    when(dataSourceService.buildDataSource(role)).thenReturn(newDataSource);

    instance.update("en", role);

    verify(dataSourceService).buildDataSource(role);
    verify(dataSourceService).updateDataSource("en", 1, newDataSource);
  }

  @Test
  void shouldDeleteGroup() {
    var group = new Group();
    group.setName("officer");

    var dataSource = new DataSource();
    dataSource.setName("officer registry");

    when(dataSourceService.getDataSource("en", group)).thenReturn(dataSource);

    instance.delete("en", group);

    verify(dataSourceService).getDataSource("en", group);
    verify(dataSourceService).deleteDataSource("en", dataSource);
    verify(groupService).deleteGroup("en", group);
  }
}
