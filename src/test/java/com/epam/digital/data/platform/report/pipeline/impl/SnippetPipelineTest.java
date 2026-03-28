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

import static java.util.List.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.epam.digital.data.platform.report.config.TestConfig;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.service.SnippetService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.util.ResourceUtils;

@SpringBootTest(classes = {SnippetPipeline.class})
@Import(TestConfig.class)
public class SnippetPipelineTest {

  @MockBean
  private SnippetService service;
  @Autowired
  private SnippetPipeline pipeline;

  @Test
  void shouldSuccessfullyPublishSnippets() throws IOException {
    var dir = ResourceUtils.getFile("classpath:pipeline/admin/en/snippets");
    var files = of(dir);

    var context = new Context();
    context.setDataSourceId(1);

    pipeline.process(files, context);

    verify(service).publish(any(), eq(context));
  }
}
