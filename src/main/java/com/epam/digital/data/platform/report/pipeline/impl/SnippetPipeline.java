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

import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Snippet;
import com.epam.digital.data.platform.report.pipeline.AbstractPipeline;
import com.epam.digital.data.platform.report.pipeline.PipelineOrder;
import com.epam.digital.data.platform.report.service.SnippetService;
import com.epam.digital.data.platform.report.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(PipelineOrder.SNIPPET_PIPELINE)
@Component
public class SnippetPipeline extends AbstractPipeline {

  private static final String SNIPPETS_DIR_NAME = "snippets";

  private final SnippetService service;

  private final Predicate<? super File> SNIPPET_FILTER =
      file -> file.isDirectory() && SNIPPETS_DIR_NAME.equals(file.getName());

  public SnippetPipeline(ObjectMapper objectMapper, SnippetService service) {
    super(objectMapper);
    this.service = service;
  }

  @Override
  public void process(List<File> files, Context context) {
    var snippets = getSnippets(files);

    service.publish(snippets, context);
  }

  @Override
  public boolean isApplicable(List<File> files) {
    return files.stream().anyMatch(SNIPPET_FILTER);
  }

  private List<Snippet> getSnippets(List<File> files) {
    return files.stream()
        .filter(SNIPPET_FILTER)
        .map(IOUtils::getFileList)
        .flatMap(Arrays::stream)
        .filter(file -> file.getName().endsWith(".json"))
        .map(file -> readList(file, Snippet.class))
        .findFirst()
        .orElse(emptyList());
  }
}
