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

package com.epam.digital.data.platform.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataSource {

  @JsonProperty(access = Access.WRITE_ONLY)
  private int id;
  private String name;
  private String queueName;
  private String syntax;
  private String type;
  private Map<String, String> options;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public String getSyntax() {
    return syntax;
  }

  public void setSyntax(String syntax) {
    this.syntax = syntax;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, String> getOptions() {
    return new HashMap<>(Objects.requireNonNullElse(options, Collections.emptyMap()));
  }

  public void setOptions(Map<String, String> options) {
    this.options = new HashMap<>(Objects.requireNonNullElse(options, Collections.emptyMap()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataSource that = (DataSource) o;
    return id == that.id && Objects.equals(name, that.name) && Objects
        .equals(queueName, that.queueName) && Objects.equals(syntax, that.syntax)
        && Objects.equals(type, that.type) && Objects
        .equals(options, that.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, queueName, syntax, type, options);
  }
}
