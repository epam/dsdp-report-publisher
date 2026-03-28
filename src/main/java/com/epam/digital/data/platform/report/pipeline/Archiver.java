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

package com.epam.digital.data.platform.report.pipeline;

import static java.util.stream.Collectors.toList;
import static com.epam.digital.data.platform.report.util.ResponseHandler.handleResponse;

import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Page;
import com.epam.digital.data.platform.report.service.QueryService;

import java.util.Collection;
import java.util.List;

import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.epam.digital.data.platform.report.client.DashboardClient;
import com.epam.digital.data.platform.report.model.Dashboard;
import com.epam.digital.data.platform.report.model.Query;
import com.epam.digital.data.platform.report.model.Visualization;
import com.epam.digital.data.platform.report.model.Widget;

@Slf4j
@RequiredArgsConstructor
@Component
public class Archiver {

  private final DashboardClient dashboardClient;
  private final QueryService queryService;
  private final RedashProperties redashProperties;

  public void archive(Context context) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(context.getLanguage(), redashProperties.getDefaultInstance());
    var redashUrl = redashLanguageProperties.getUrl();
    var redashApiKey = redashLanguageProperties.getApiKey();
    int page = 0;
    Page<Dashboard> dashboardsPageResponse;
    do {
      page++;
      dashboardsPageResponse =
          handleResponse(dashboardClient.findAllDashboards(redashUrl, page, redashApiKey));
      dashboardsPageResponse
          .getResults()
          .forEach(
              dashboard -> {
                // findAllDashboards returns empty info about dashboard widgets,
                // to get it we make an additional call
                var actualDashboard =
                    handleResponse(
                        dashboardClient.findDashboardById(
                            redashUrl, dashboard.getId(), redashApiKey));
                var queries = getQueries(actualDashboard);

                Integer dataSourceId = context.getDataSourceId();
                var isDashboardFromCurrentDatasource =
                    queries.stream().map(Query::getDataSourceId).allMatch(dataSourceId::equals);
                if (isDashboardFromCurrentDatasource) {
                  log.info(
                      "Archiving dashboard: id = {}, name = '{}'",
                      dashboard.getId(),
                      dashboard.getName());
                  handleResponse(
                      dashboardClient.archiveDashboard(redashUrl, dashboard.getId(), redashApiKey));
                  queryService.archive(context.getLanguage(), queries);
                }
              });
    } while (page * dashboardsPageResponse.getPageSize() < dashboardsPageResponse.getCount());
  }

  private List<Query> getQueries(Dashboard dashboard) {
    return Optional.ofNullable(dashboard.getWidgets()).stream()
        .flatMap(Collection::stream)
        .map(Widget::getVisualization)
        .filter(Objects::nonNull)
        .map(Visualization::getQuery)
        .collect(toList());
  }
}
