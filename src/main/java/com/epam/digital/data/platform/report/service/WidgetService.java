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

import static com.epam.digital.data.platform.report.util.ResponseHandler.handleResponse;

import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.epam.digital.data.platform.report.client.WidgetClient;
import com.epam.digital.data.platform.report.model.Dashboard;
import com.epam.digital.data.platform.report.model.Widget;

@RequiredArgsConstructor
@Service
public class WidgetService {

  private final WidgetClient widgetClient;
  private final RedashProperties redashProperties;

  public void save(String language, Dashboard dashboard) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    for (Widget widget : dashboard.getWidgets()) {
      var visualization = widget.getVisualization();
      if (visualization != null) {
        widget.setVisualizationId(visualization.getId());
      }
      widget.setDashboardId(dashboard.getId());
      handleResponse(
          widgetClient.createWidget(
              redashLanguageProperties.getUrl(), widget, redashLanguageProperties.getApiKey()));
    }
  }
}
