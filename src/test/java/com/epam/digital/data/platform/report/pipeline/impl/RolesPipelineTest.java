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

package com.epam.digital.data.platform.report.pipeline.impl;

import static com.epam.digital.data.platform.report.util.TestUtils.context;
import static com.epam.digital.data.platform.report.util.TestUtils.redashProperties;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.model.Group;
import com.epam.digital.data.platform.report.model.Role;
import com.epam.digital.data.platform.report.service.GroupService;
import com.epam.digital.data.platform.report.service.RoleService;
import com.epam.digital.data.platform.report.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;

@ExtendWith(MockitoExtension.class)
public class RolesPipelineTest {

  @Mock
  GroupService groupService;
  @Mock
  RoleService roleService;
  @Mock
  UserService userService;

  ObjectMapper mapper;

  RolesPipeline instance;

  private static List<File> rolesFile;

  @BeforeAll
  static void setup() throws FileNotFoundException {
    var file = ResourceUtils.getFile("classpath:roles/officer.yml");
    rolesFile = of(file);
  }

  @BeforeEach
  void init() {
    mapper = new ObjectMapper(new YAMLFactory());
    instance = new RolesPipeline(mapper, groupService, roleService, userService, redashProperties());
  }

  @Test
  void shouldSuccessfullyCreateRoleInSingleRegion() {
    when(groupService.getGroups("en")).thenReturn(emptyList());

    instance.process(rolesFile, context());

    verify(groupService).getGroups("en");
    verify(roleService).create(eq("en"), any());
    verify(roleService, never()).delete(eq("en"), any());

    var roleCaptor = ArgumentCaptor.forClass(Role.class);
    verify(userService).createUser(roleCaptor.capture());
    assertThat(roleCaptor.getValue().getName()).isEqualTo("officer");
    assertThat(roleCaptor.getValue().getPassword()).isNotEmpty();
  }

  @Test
  void shouldSuccessfullyCreateRoleIfNoGroupExists() {
    when(groupService.getGroups("en")).thenReturn(emptyList());
    when(userService.isUserExists("officer")).thenReturn(true);

    instance.process(rolesFile, context());

    verify(groupService).getGroups("en");
    verify(roleService).create(eq("en"), any());
    verify(roleService, never()).delete(eq("en"), any());

    var roleCaptor = ArgumentCaptor.forClass(Role.class);
    verify(userService).updateUser(roleCaptor.capture());
    assertThat(roleCaptor.getValue().getName()).isEqualTo("officer");
    assertThat(roleCaptor.getValue().getPassword()).isNotEmpty();
  }

  @Test
  void shouldSuccessfullyDeleteRole() {
    when(groupService.getGroups("en")).thenReturn(groups("citizen", "officer"));

    instance.process(rolesFile, context());

    verify(groupService).getGroups("en");
    verify(roleService, never()).create(eq("en"), any());
    verify(roleService).delete(eq("en"), any());
    verify(userService).deleteUser("citizen");
  }

  @Test
  void shouldSuccessfullyUpdateExistingRole() {
    when(groupService.getGroups("en")).thenReturn(groups("officer"));
    when(userService.isUserExists("officer")).thenReturn(true);

    instance.process(rolesFile, context());

    verify(groupService).getGroups("en");
    verify(roleService, never()).create(eq("en"), any());
    verify(roleService, never()).delete(eq("en"), any());
    verify(roleService).update(eq("en"), any());
    verify(userService).updateUser(any());
    verify(userService, never()).deleteUser(any());
  }

  @Test
  void shouldSuccessfullyUpdateInRedashAndCreateInDb() {
    when(groupService.getGroups("en")).thenReturn(groups("officer"));
    when(userService.isUserExists("officer")).thenReturn(false);

    instance.process(rolesFile, context());

    verify(groupService).getGroups("en");
    verify(roleService).update(eq("en"), any());
    verify(roleService, never()).delete(eq("en"), any());
    verify(userService).createUser(any());
    verify(userService, never()).deleteUser(any());
  }

  @Test
  void shouldNotBeApplicableToNotYamlFile() throws FileNotFoundException {
    var file = ResourceUtils.getFile("classpath:roles/officer.json");
    var files = of(file);

    var result = instance.isApplicable(files);

    assertThat(result).isFalse();
  }

  @Test
  void shouldNotBeApplicableToNotOfficerYaml() throws FileNotFoundException {
    var file = ResourceUtils.getFile("classpath:roles/citizen.yaml");
    var files = of(file);

    var result = instance.isApplicable(files);

    assertThat(result).isFalse();
  }

  private List<Group> groups(String... names) {
    var groups = new ArrayList<Group>();

    for (var name : names) {
      var group = new Group();
      group.setName(name);
      groups.add(group);
    }

    return groups;
  }
}
