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
import static java.util.stream.Collectors.toSet;

import com.epam.digital.data.platform.report.client.QueryClient;
import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Page;
import com.epam.digital.data.platform.report.model.Query;
import com.epam.digital.data.platform.report.model.Visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class QueryService {
  public static final String MISSING_PARAMETER_ON_QUERY_PREPOPULATION_ERROR_PREFIX =
      "Missing parameter value for:";
  private static final String QUERY_ID_KEY = "queryId";

  private final QueryClient queryClient;
  private final VisualizationService visualizationService;
  private final RedashProperties redashProperties;

  public Set<Query> save(Map<Query, List<Visualization>> queries, Context context) {
    var saved = saveQueries(queries, context);
    visualizationService.save(context.getLanguage(), saved);
    return saved.keySet();
  }

  public void publish(String language, Set<Query> queries) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    Query queryStub = new Query();
    queries.forEach(
        query ->
            handleResponse(
                queryClient.updateQuery(
                    redashLanguageProperties.getUrl(),
                    query.getId(),
                    queryStub,
                    redashLanguageProperties.getApiKey())));
  }

  public void execute(String language, Set<Query> queries) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    queries.forEach(
        query -> {
          try {
            queryClient.executeQuery(
                redashLanguageProperties.getUrl(),
                query.getId(),
                redashLanguageProperties.getApiKey());
            log.info("Executed query {}", query.getName());
          } catch (FeignException e) {
            if (!e.getMessage().contains(MISSING_PARAMETER_ON_QUERY_PREPOPULATION_ERROR_PREFIX)) {
              throw e;
            }
            log.warn("Couldn't execute query, error: {}", e.getMessage());
          }
        });
  }

  public void archive(String language, List<Query> queries) {
    var redashLanguageProperties = redashProperties.getLang().getOrDefault(language, redashProperties.getDefaultInstance());
    var redashUrl = redashLanguageProperties.getUrl();
    var redashApiKey = redashLanguageProperties.getApiKey();
    for (String name : getQueryNames(queries)) {
      log.info("Processing query '{}'", name);
      int page = 0;
      List<Query> foundQueries = new ArrayList<>();
      Page<Query> response;
      do {
        ++page;
        response = handleResponse(queryClient.getQueries(redashUrl, name, page, redashApiKey));
        foundQueries.addAll(response.getResults());
      } while (page * response.getPageSize() < response.getCount());

      log.info(
          "Found {} queries: {}",
          foundQueries.size(),
          foundQueries.stream()
              .map(q -> "(id = " + q.getId() + ", name = '" + q.getName() + "')")
              .collect(Collectors.toList()));

      foundQueries.stream()
          .filter(query -> query.getName().equals(name))
          .map(
              query -> {
                log.info("Archiving query: id = {}, name = '{}'", query.getId(), name);
                return query.getId();
              })
          .forEach(q -> handleResponse(queryClient.archiveQuery(redashUrl, q, redashApiKey)));
    }
  }

  private Set<String> getQueryNames(List<Query> queries) {
    return queries.stream().map(Query::getName).collect(toSet());
  }

  private Map<Query, List<Visualization>> saveQueries(
      Map<Query, List<Visualization>> queryToVisualizations, Context context) {
    var redashLanguageProperties =
        redashProperties
            .getLang()
            .getOrDefault(context.getLanguage(), redashProperties.getDefaultInstance());
    Map<Query, List<Visualization>> created = new HashMap<>();

    for (Map.Entry<Query, List<Visualization>> entry : queryToVisualizations.entrySet()) {
      Query query = entry.getKey();
      log.info("Publishing query '{}'", query.getName());
      prepareQuery(query, context);
      Query published =
          handleResponse(
              queryClient.postQuery(
                  redashLanguageProperties.getUrl(), query, redashLanguageProperties.getApiKey()));
      log.info("Query was published with id = {}", published.getId());
      created.put(published, entry.getValue());
    }

    return created;
  }

  private void prepareQuery(Query query, Context context) {
    modifyOptions(query, context);
    query.setDataSourceId(context.getDataSourceId());
  }

  private void modifyOptions(Query query, Context context) {
    var parameters = (List<Map<String, Object>>) query.getOptions().get("parameters");

    if (parameters.isEmpty()) {
      return;
    }

    for (var parameter : parameters) {
      if ("query".equals(parameter.get("type"))) {
        Integer old = (Integer) parameter.get(QUERY_ID_KEY);
        parameter.replace(QUERY_ID_KEY, context.getMappedIds().get(old));
      }
      parameter.remove("parentQueryId");
    }
  }
}
