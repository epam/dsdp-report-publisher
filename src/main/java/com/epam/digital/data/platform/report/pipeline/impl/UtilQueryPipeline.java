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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.epam.digital.data.platform.report.exception.QueryNotFoundException;
import com.epam.digital.data.platform.report.exception.QueryPublishingException;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Query;
import com.epam.digital.data.platform.report.model.Visualization;
import com.epam.digital.data.platform.report.pipeline.AbstractPipeline;
import com.epam.digital.data.platform.report.pipeline.PipelineOrder;
import com.epam.digital.data.platform.report.service.QueryService;
import com.epam.digital.data.platform.report.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(PipelineOrder.UTIL_QUERY_PIPELINE)
@Component
public class UtilQueryPipeline extends AbstractPipeline {
  private static final Logger LOGGER = LoggerFactory.getLogger(UtilQueryPipeline.class);
  private static final String QUERIES_DIR_NAME = "queries";
  private static final String PARAMETER_KEY = "parameters";
  private static final String QUERY_ID_KEY = "queryId";

  private final QueryService queryService;

  private final Predicate<? super File> QUERY_FILTER =
      file -> file.isDirectory() && QUERIES_DIR_NAME.equals(file.getName());

  public UtilQueryPipeline(ObjectMapper objectMapper, QueryService queryService) {
    super(objectMapper);
    this.queryService = queryService;
  }

  public void process(List<File> files, Context context) {
    LOGGER.info("Processing parameter queries for dataSource with id {}",
        context.getDataSourceId());
    var utilQueries = getUtilQueries(files);
    var buffer = new ArrayList<>(utilQueries);

    queryService.archive(context.getLanguage(), utilQueries);

    while (!buffer.isEmpty()) {
      var queriesForPublishing = getQueriesThatCanBePublished(buffer, context);
      if (queriesForPublishing.isEmpty()) {
        var notPublished = buffer.stream().map(Query::getId).collect(Collectors.toSet());
        throw new QueryPublishingException("Queries '" + notPublished + "' cannot be published "
            + "because they have a circular dependency or depend on other queries that "
            + "are not in the file with queries");
      }
      var saved = queryService.save(visualizationsByQuery(queriesForPublishing), context);
      queryService.publish(context.getLanguage(), saved);
      queryService.execute(context.getLanguage(), saved);

      context.addMappedIds(mapToNewIds(saved, utilQueries));
      buffer.removeAll(queriesForPublishing);
    }
  }

  private List<Query> getQueriesThatCanBePublished(List<Query> queries, Context context) {
    List<Query> result = new ArrayList<>();
    for (var query : queries) {
      var params = (List<Map<String, Object>>) query.getOptions().get(PARAMETER_KEY);

      if (params.isEmpty()) {
        result.add(query);
        continue;
      }
      Set<Integer> subQueryIds = new HashSet<>();
      for (var parameter : params) {
        var subQueryId = parameter.get(QUERY_ID_KEY);
        if (subQueryId != null) {
          subQueryIds.add((Integer) subQueryId);
        }
      }
      if (context.getMappedIds().keySet().containsAll(subQueryIds)) {
        result.add(query);
      }
    }
    return result;
  }

  public boolean isApplicable(List<File> files) {
    return files.stream().anyMatch(QUERY_FILTER);
  }

  private List<Query> getUtilQueries(List<File> files) {
    var queries = getQueries(files);
    var utilIds = getUtilQueryIds(queries);

    List<Query> utilQueries = queries.stream()
        .filter(query -> utilIds.contains(query.getId()))
        .collect(toList());
    
    if(utilQueries.size() != utilIds.size()) {
      var allQueryIds = queries.stream().map(Query::getId).collect(Collectors.toSet());
      utilIds.removeAll(allQueryIds);
      throw new QueryNotFoundException("Queries with id " + utilIds + " not found");
    }
    return utilQueries;
  }

  private Set<Integer> getUtilQueryIds(List<Query> queries) {
    var utilIds = new HashSet<Integer>();

    for (var query : queries) {
      var params = (List<Map<String, Object>>) query.getOptions().get(PARAMETER_KEY);

      for (var parameter : params) {
        var utilId = parameter.get(QUERY_ID_KEY);
        if (utilId != null) {
          utilIds.add((Integer) utilId);
        }
      }
    }
    return utilIds;
  }

  private Map<Integer, Integer> mapToNewIds(Set<Query> queries, List<Query> oldQueries) {
    var mappedIds = new HashMap<Integer, Integer>();

    var old = oldQueries.stream()
        .collect(Collectors.toMap(Query::getQuery, Query::getId));

    queries.forEach(query -> {
      var oldId = old.get(query.getQuery());
      var newId = query.getId();
      mappedIds.put(oldId, newId);
    });

    return mappedIds;
  }

  private Map<Query, List<Visualization>> visualizationsByQuery(List<Query> queries) {
    var queryToVisualizations = new HashMap<Query, List<Visualization>>();
    queries.forEach(query -> queryToVisualizations.put(query, query.getVisualizations()));
    return queryToVisualizations;
  }

  private List<Query> getQueries(List<File> files) {
    return files.stream()
        .filter(QUERY_FILTER)
        .map(IOUtils::getFileList)
        .flatMap(Arrays::stream)
        .filter(file -> file.getName().endsWith(".json"))
        .map(file -> readPage(file, Query.class))
        .findFirst()
        .orElse(emptyList());
  }
}
