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

import com.epam.digital.data.platform.report.client.VisualizationClient;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.epam.digital.data.platform.report.model.Query;
import com.epam.digital.data.platform.report.model.Visualization;

@RequiredArgsConstructor
@Service
public class VisualizationService {

  private static final String TABLE_TYPE = "TABLE";

  private final VisualizationClient visualizationClient;
  private final RedashProperties redashProperties;

  public void save(String language, Map<Query, List<Visualization>> queryToVisualizations) {
    for (Entry<Query, List<Visualization>> pair : queryToVisualizations.entrySet()) {
      if (pair.getValue() != null) {
        updateDefaultVisualization(language, pair.getKey(), pair.getValue());
        createNewVisualizations(language, pair.getKey(), pair.getValue());
      }
    }
  }

  private void updateDefaultVisualization(
      String language, Query query, List<Visualization> visualizations) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    findTableVisualization(visualizations)
        .ifPresent(
            visualization -> {
              visualization.setQueryId(query.getId());

              var created =
                  handleResponse(
                      visualizationClient.updateVisualization(
                          redashLanguageProperties.getUrl(),
                          getDefaultVisualizationId(query),
                          visualization,
                          redashLanguageProperties.getApiKey()));
              visualization.setId(created.getId());

              visualizations.remove(visualization);
            });
  }

  private void createNewVisualizations(
      String language, Query query, List<Visualization> visualizations) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    for (Visualization visualization : visualizations) {
      visualization.setQueryId(query.getId());
      Visualization created =
          handleResponse(
              visualizationClient.createVisualization(
                  redashLanguageProperties.getUrl(),
                  visualization,
                  redashLanguageProperties.getApiKey()));
      visualization.setId(created.getId());
    }
  }

  private Optional<Visualization> findTableVisualization(List<Visualization> visualizations) {
    return visualizations.stream()
        .filter(visualization -> TABLE_TYPE.equals(visualization.getType()))
        .findFirst();
  }

  private Integer getDefaultVisualizationId(Query query) {
    return query.getVisualizations().get(0).getId();
  }
}
