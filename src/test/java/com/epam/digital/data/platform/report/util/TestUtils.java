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

package com.epam.digital.data.platform.report.util;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Dashboard;
import com.epam.digital.data.platform.report.model.DataSource;
import com.epam.digital.data.platform.report.model.Group;
import com.epam.digital.data.platform.report.model.Query;
import com.epam.digital.data.platform.report.model.Snippet;
import com.epam.digital.data.platform.report.model.Visualization;
import com.epam.digital.data.platform.report.model.Widget;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.http.ResponseEntity;

public class TestUtils {

  private static final String VISUALIZATION_TABLE_TYPE = "TABLE";

  public static final URI REDASH_EN_URL = URI.create("http://mock");
  public static final String REDASH_EN_API_KEY = "apiKey";

  public static Query query(String name) {
    return query(name, 1, 1);
  }

  public static Set<Query> queries(String... names) {
    return IntStream.range(0, names.length)
        .mapToObj(index -> query(names[index], index, 1))
        .collect(toSet());
  }

  public static Visualization visualization(String type) {
    return visualization(type, 1);
  }

  public static Dashboard dashboard(int id, String name) {
    var dashboard = dashboard(name, id);
    var widget = widget("text");
    var visualization = visualization(VISUALIZATION_TABLE_TYPE);
    var query = query("query name");

    widget.setVisualization(visualization);
    dashboard.setWidgets(Set.of(widget));
    visualization.setQuery(query);

    return dashboard;
  }

  public static Dashboard dashboard(int id, String name, int datasourceId) {
    var dashboard = dashboard(name, id);
    var widget = widget("text");
    var visualization = visualization(VISUALIZATION_TABLE_TYPE);
    var query = query("query name", 1, datasourceId);

    widget.setVisualization(visualization);
    dashboard.setWidgets(Set.of(widget));
    visualization.setQuery(query);

    return dashboard;
  }

  public static Snippet snippet(String name) {
    var snippet = new Snippet();

    snippet.setSnippet(name);

    return snippet;
  }

  public static List<DataSource> dataSources(String... names) {
    return IntStream.range(0, names.length)
        .mapToObj(index -> dataSource(names[index], index))
        .collect(toList());
  }

  public static List<Group> groups(String... names) {
    return IntStream.range(0, names.length)
        .mapToObj(index -> group(names[index], index))
        .collect(toList());
  }

  public static Context context() {
    var context = new Context();
    var mappedIds = new HashMap<Integer, Integer>();

    mappedIds.put(0, 1);
    context.setDataSourceId(1);
    context.setLanguage("en");
    context.addMappedIds(mappedIds);

    return context;
  }

  public static RedashProperties redashProperties() {
    var redashProperties = new RedashProperties();
    redashProperties.setLang(
        Map.of(
            Locale.ENGLISH.getLanguage(),
            new RedashProperties.InstanceProperties(REDASH_EN_URL, REDASH_EN_API_KEY)));

    return redashProperties;
  }

  public static RedashProperties redashPropertiesForAdmin() {
    var redashProperties = new RedashProperties();
    redashProperties.setDefaultInstance(new RedashProperties.InstanceProperties(REDASH_EN_URL, REDASH_EN_API_KEY));
    return redashProperties;
  }

  private static Query query(String name, int id, int datasourceId) {
    var query = new Query();

    query.setId(id);
    query.setName(name);
    query.setDataSourceId(datasourceId);
    query.setOptions(options());

    return query;
  }

  private static Map<String, Object> options() {
    var options = new HashMap<String, Object>();

    options.put("parameters", List.of(parameters()));

    return options;
  }

  private static Map<String, Object> parameters() {
    var parameters = new HashMap<String, Object>();

    parameters.put("type", "query");
    parameters.put("queryId", 2);
    parameters.put("parentQueryId", 1);

    return parameters;
  }

  private static Visualization visualization(String type, int id) {
    var visualization = new Visualization();

    visualization.setId(id);
    visualization.setType(type);

    return visualization;
  }

  private static Widget widget(String text) {
    var widget = new Widget();

    widget.setText(text);

    return widget;
  }

  private static Dashboard dashboard(String name, int id) {
    var dashboard = new Dashboard();

    dashboard.setId(id);
    dashboard.setName(name);
    dashboard.setSlug(name);

    return dashboard;
  }

  public static DataSource dataSource(String name, int id) {
    var dataSource = new DataSource();

    dataSource.setName(name);
    dataSource.setId(id);

    return dataSource;
  }

  private static Group group(String name, int id) {
    var group = new Group();

    group.setName(name);
    group.setId(id);

    return group;
  }

  public static ResponseEntity<Void> mockVoidResponse() {
    return ResponseEntity.status(200).build();
  }
}
