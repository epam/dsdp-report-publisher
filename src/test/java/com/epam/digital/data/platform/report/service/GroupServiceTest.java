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
import static com.epam.digital.data.platform.report.util.TestUtils.groups;
import static com.epam.digital.data.platform.report.util.TestUtils.redashProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.client.GroupClient;
import com.epam.digital.data.platform.report.model.DataSource;
import com.epam.digital.data.platform.report.model.DataSourceAssociation;
import com.epam.digital.data.platform.report.model.Group;
import com.epam.digital.data.platform.report.model.Role;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

  private final String GROUP_NAME = "officer";

  @Mock
  GroupClient client;

  GroupService instance;

  @BeforeEach
  void init() {
    instance = new GroupService(client, redashProperties());
  }

  @Test
  void shouldGetGroup() {
    var role = new Role();
    role.setName(GROUP_NAME);

    when(client.getGroups(REDASH_EN_URL, REDASH_EN_API_KEY)).thenReturn(listResponse());

    var result = instance.getGroup("en", role);

    assertThat(result.isPresent()).isTrue();
    assertThat(result.get().getName()).isEqualTo(GROUP_NAME);
  }

  @Test
  void shouldNotGetGroupIfNothingMatches() {
    var role = new Role();
    role.setName("citizen");

    when(client.getGroups(REDASH_EN_URL, REDASH_EN_API_KEY)).thenReturn(listResponse());

    var result = instance.getGroup("en", role);

    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void shouldGetGroups() {
    when(client.getGroups(REDASH_EN_URL, REDASH_EN_API_KEY)).thenReturn(listResponse());

    instance.getGroups("en");

    verify(client).getGroups(REDASH_EN_URL, REDASH_EN_API_KEY);
  }

  @Test
  void shouldCreateGroup() {
    var group = new Group();
    group.setName(GROUP_NAME);

    when(client.createGroup(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY))).thenReturn(entityResponse());

    instance.createGroup("en", group);

    verify(client).createGroup(REDASH_EN_URL, group, REDASH_EN_API_KEY);
  }

  @Test
  void shouldDeleteGroup() {
    var group = new Group();
    group.setName(GROUP_NAME);
    group.setId(1);

    instance.deleteGroup("en", group);

    verify(client).deleteGroup(REDASH_EN_URL, 1, REDASH_EN_API_KEY);
  }

  @Test
  void shouldAssociate() {
    var group = new Group();
    group.setName(GROUP_NAME);
    group.setId(1);

    var dataSource = new DataSource();
    dataSource.setId(1);

    var association = new DataSourceAssociation();
    association.setDataSourceId(1);

    instance.associate("en", dataSource, group);

    verify(client).associateGroupWithDataSource(REDASH_EN_URL, 1, association, REDASH_EN_API_KEY);
  }

  private ResponseEntity<List<Group>> listResponse() {
    return new ResponseEntity<>(groups(GROUP_NAME), HttpStatus.OK);
  }

  private ResponseEntity<Group> entityResponse() {
    var group = new Group();
    group.setName(GROUP_NAME);
    group.setId(1);

    return new ResponseEntity<>(group, HttpStatus.OK);
  }
}
