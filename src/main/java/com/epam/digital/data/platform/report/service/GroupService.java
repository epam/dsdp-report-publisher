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

import com.epam.digital.data.platform.report.client.GroupClient;
import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.model.DataSource;
import com.epam.digital.data.platform.report.model.DataSourceAssociation;
import com.epam.digital.data.platform.report.model.Group;
import com.epam.digital.data.platform.report.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.epam.digital.data.platform.report.util.ResponseHandler.handleResponse;

@RequiredArgsConstructor
@Service
public class GroupService {

  private final GroupClient client;
  private final RedashProperties redashProperties;

  public Optional<Group> getGroup(String language, Role role) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    return handleResponse(
            client.getGroups(
                redashLanguageProperties.getUrl(), redashLanguageProperties.getApiKey()))
        .stream()
        .filter(group -> group.getName().equals(role.getName()))
        .findFirst();
  }

  public List<Group> getGroups(String language) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    return handleResponse(
        client.getGroups(redashLanguageProperties.getUrl(), redashLanguageProperties.getApiKey()));
  }

  public Group createGroup(String language, Group group) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    return handleResponse(
        client.createGroup(
            redashLanguageProperties.getUrl(), group, redashLanguageProperties.getApiKey()));
  }

  public void deleteGroup(String language, Group group) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    client.deleteGroup(
        redashLanguageProperties.getUrl(), group.getId(), redashLanguageProperties.getApiKey());
  }

  public void associate(String language, DataSource dataSource, Group... groups) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    for (var group : groups) {
      var association = new DataSourceAssociation();
      association.setDataSourceId(dataSource.getId());
      client.associateGroupWithDataSource(
          redashLanguageProperties.getUrl(),
          group.getId(),
          association,
          redashLanguageProperties.getApiKey());
    }
  }
}
