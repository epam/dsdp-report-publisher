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

import static com.epam.digital.data.platform.report.service.RoleService.ADMIN_ROLE_NAME;
import static com.epam.digital.data.platform.report.service.RoleService.AUDITOR_ROLE_NAME;
import static org.apache.commons.io.FilenameUtils.isExtension;

import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Group;
import com.epam.digital.data.platform.report.model.Role;
import com.epam.digital.data.platform.report.model.RolesList;
import com.epam.digital.data.platform.report.pipeline.AbstractPipeline;
import com.epam.digital.data.platform.report.pipeline.PipelineOrder;
import com.epam.digital.data.platform.report.service.GroupService;
import com.epam.digital.data.platform.report.service.RoleService;
import com.epam.digital.data.platform.report.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Order(PipelineOrder.ROLES_PIPELINE)
@Component
public class RolesPipeline extends AbstractPipeline {

  private static final Predicate<? super File> OFFICER_ROLE_FILTER =
      file -> !file.isDirectory() && file.getName().contains("officer")
          && isExtension(file.getName(), "yaml", "yml");

  private static final List<String> DEFAULT_GROUPS = List.of(ADMIN_ROLE_NAME, "default",
      AUDITOR_ROLE_NAME);
  private static final int PASSWORD_LENGTH = 16;
  private static final String CHARSET_ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final SecureRandom secureRandom = new SecureRandom();

  private final GroupService groupService;
  private final RoleService roleService;
  private final UserService userService;
  private final RedashProperties redashProperties;

  public RolesPipeline(
      @Qualifier("yamlMapper") ObjectMapper objectMapper,
      GroupService groupService,
      RoleService roleService,
      UserService userService,
      RedashProperties redashProperties) {
    super(objectMapper);
    this.groupService = groupService;
    this.roleService = roleService;
    this.userService = userService;
    this.redashProperties = redashProperties;
  }

  @Override
  public void process(List<File> files, Context context) {
    var roles = getRoles(files);

    Set<String> usersToRemoveFromDb = new HashSet<>();
    Map<String, Role> usersToAddToDb = new HashMap<>();
    Map<String, Role> usersToUpdateInDb = new HashMap<>();
    redashProperties
        .getLang()
        .keySet()
        .forEach(
            lang -> {
              log.info("Processing roles for lang: {}", lang);
              var existingGroups = groupService.getGroups(lang);
              log.info(
                  "Existing groups for lang {}: {}",
                  lang,
                  existingGroups.stream().map(Group::getName).toList());
              var groupsToDelete = getGroupsToDelete(existingGroups, roles);
              groupsToDelete.forEach(group -> roleService.delete(lang, group));
              usersToRemoveFromDb.addAll(groupsToDelete.stream().map(Group::getName).toList());

              var rolesToCreate = getRolesToCreate(existingGroups, roles);
              var rolesToUpdate = getRolesToUpdate(existingGroups, roles);
              log.info(
                  "Redash resources exists for roles : {}, new redash resources will be created for roles: {}",
                  rolesToUpdate.stream().map(Role::getName).toList(),
                  rolesToCreate.stream().map(Role::getName).toList());
              rolesToUpdate.forEach(
                  role -> {
                    roleService.update(lang, role);
                    addToCreateOrUpdateInDb(usersToAddToDb, usersToUpdateInDb, role);
                  });
              rolesToCreate.forEach(
                  role -> {
                    roleService.create(lang, role);
                    addToCreateOrUpdateInDb(usersToAddToDb, usersToUpdateInDb, role);
                  });
            });

    log.info("Removing roles {}", usersToRemoveFromDb);
    usersToRemoveFromDb.forEach(userService::deleteUser);
    log.info("Adding roles {}", usersToAddToDb.keySet());
    usersToAddToDb.forEach((key, value) -> userService.createUser(value));
    log.info("Updating roles {}", usersToUpdateInDb.keySet());
    usersToUpdateInDb.forEach((key, value) -> userService.updateUser(value));
  }

  private List<Role> getRolesToCreate(List<Group> existingGroups, List<Role> roles) {
    var existingNames = existingGroups.stream().map(Group::getName).toList();

    return roles.stream().filter(role -> !existingNames.contains(role.getName())).toList();
  }

  private List<Role> getRolesToUpdate(List<Group> existingGroups, List<Role> roles) {
    var existingNames = existingGroups.stream().map(Group::getName).toList();

    return roles.stream().filter(role -> existingNames.contains(role.getName())).toList();
  }

  private List<Group> getGroupsToDelete(List<Group> existingGroups, List<Role> roles) {
    var roleNames = roles.stream().map(Role::getName).toList();

    return existingGroups.stream()
        .filter(group -> !DEFAULT_GROUPS.contains(group.getName()))
        .filter(group -> !roleNames.contains(group.getName()))
        .toList();
  }

  @Override
  public boolean isApplicable(List<File> files) {
    return files.stream().anyMatch(OFFICER_ROLE_FILTER);
  }

  private List<Role> getRoles(List<File> files) {
    var rolesFromRegistryYaml =
        files.stream()
            .filter(OFFICER_ROLE_FILTER)
            .map(file -> readValue(file, RolesList.class))
            .flatMap(rolesList -> rolesList.getRoles().stream())
            .toList();
    return rolesFromRegistryYaml.stream()
        .peek(role -> role.setPassword(generateSecurePassword(PASSWORD_LENGTH)))
        .toList();
  }

  private void addToCreateOrUpdateInDb(
      Map<String, Role> usersToAddToDb, Map<String, Role> usersToUpdateInDb, Role role) {
    if (userService.isUserExists(role.getName())) {
      usersToUpdateInDb.put(role.getName(), role);
    } else {
      usersToAddToDb.put(role.getName(), role);
    }
  }

  private static String generateSecurePassword(int length) {
    StringBuilder password = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      int index = secureRandom.nextInt(CHARSET_ALPHA_NUMERIC .length());
      password.append(CHARSET_ALPHA_NUMERIC .charAt(index));
    }
    return password.toString();
  }
}
