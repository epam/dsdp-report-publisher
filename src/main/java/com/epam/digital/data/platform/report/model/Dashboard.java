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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Dashboard {

  @JsonProperty(access = Access.WRITE_ONLY)
  private Integer id;
  private String name;
  @JsonProperty(required = true)
  private String slug;
  private List<String> tags;
  private Set<Widget> widgets;
  private Object options;
  @JsonProperty("is_draft")
  private boolean isDraft;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<Widget> getWidgets() {
    return new HashSet<>(Objects.requireNonNullElse(widgets, Collections.emptySet()));
  }

  public void setWidgets(Set<Widget> widgets) {
    this.widgets = new HashSet<>(Objects.requireNonNullElse(widgets, Collections.emptySet()));
  }

  public List<String> getTags() {
    return new ArrayList<>(Objects.requireNonNullElse(tags, Collections.emptyList()));
  }

  public void setTags(List<String> tags) {
    this.tags = new ArrayList<>(Objects.requireNonNullElse(tags, Collections.emptyList()));
  }

  public boolean isDraft() {
    return isDraft;
  }

  public void setDraft(boolean draft) {
    isDraft = draft;
  }

  public Object getOptions() {
    return options;
  }

  public void setOptions(Object options) {
    this.options = options;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }


}
