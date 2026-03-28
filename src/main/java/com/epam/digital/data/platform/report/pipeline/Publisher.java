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

import static com.epam.digital.data.platform.report.util.ResponseHandler.handleResponse;

import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.service.QueryService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.epam.digital.data.platform.report.client.DashboardClient;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Dashboard;
import com.epam.digital.data.platform.report.model.Query;
import com.epam.digital.data.platform.report.model.Visualization;
import com.epam.digital.data.platform.report.model.Widget;
import com.epam.digital.data.platform.report.service.WidgetService;

@Slf4j
@RequiredArgsConstructor
@Component
public class Publisher {

  private final DashboardClient dashboardClient;
  private final WidgetService widgetService;
  private final QueryService queryService;
  private final RedashProperties redashProperties;

  public void publish(Dashboard dashboard, Context context) {
    Dashboard created = createDashboard(context.getLanguage(), dashboard);
    dashboard.setId(created.getId());
    log.info("Created new dashboard '{}' with id = {}", created.getName(), created.getId());

    processQueries(visualizationsByQuery(dashboard), context);

    widgetService.save(context.getLanguage(), dashboard);
    publish(context.getLanguage(), dashboard);
  }

  private void publish(String language, Dashboard dashboard) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    Dashboard dashboardStub = new Dashboard();
    handleResponse(
        dashboardClient.updateDashboard(
            redashLanguageProperties.getUrl(),
            dashboard.getId(),
            dashboardStub,
            redashLanguageProperties.getApiKey()));
  }

  private Dashboard createDashboard(String language, Dashboard dashboard) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    return handleResponse(
        dashboardClient.createDashboard(
            redashLanguageProperties.getUrl(), dashboard, redashLanguageProperties.getApiKey()));
  }

  private Set<Query> processQueries(Map<Query, List<Visualization>> queries, Context context) {
    var saved = queryService.save(queries, context);
    queryService.publish(context.getLanguage(), saved);
    queryService.execute(context.getLanguage(), saved);
    return saved;
  }

  private Map<Query, List<Visualization>> visualizationsByQuery(Dashboard dashboard) {
    return dashboard.getWidgets().stream()
        .map(Widget::getVisualization)
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(Visualization::getQuery));
  }
}
