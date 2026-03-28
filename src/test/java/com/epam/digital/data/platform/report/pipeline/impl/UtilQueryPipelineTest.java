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

import com.epam.digital.data.platform.report.config.TestConfig;
import com.epam.digital.data.platform.report.exception.QueryPublishingException;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Query;
import com.epam.digital.data.platform.report.service.QueryService;
import feign.FeignException;
import net.bytebuddy.utility.RandomString;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.Map;

import static com.epam.digital.data.platform.report.util.TestUtils.context;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {UtilQueryPipeline.class})
@Import(TestConfig.class)
class UtilQueryPipelineTest {

  @MockBean
  private QueryService queryService;
  @Autowired
  private UtilQueryPipeline utilQueryPipeline;

  @Test
  void shouldSuccessfullyArchiveAndPublishDashboardAndExecuteQueries() throws IOException {
    var dir = ResourceUtils.getFile("classpath:pipeline/officer/queries");
    var files = of(dir);

    utilQueryPipeline.process(files, context());

    verify(queryService).archive(eq("en"), any());
    verify(queryService).save(any(), any());
    verify(queryService).publish(eq("en"), any());
    verify(queryService).execute(eq("en"), any());
  }

  @Test
  void shouldThrowExceptionIfItsNotMissingParameter() throws IOException {
    var dir = ResourceUtils.getFile("classpath:pipeline/officer/queries");
    var files = of(dir);

    var exception = Mockito.mock(FeignException.class);
    var exMessage = RandomString.make();
    Mockito.when(exception.getMessage()).thenReturn(exMessage);
    Mockito.doThrow(exception).when(queryService).execute(eq("en"), any());
    Assertions.assertThatThrownBy(() -> utilQueryPipeline.process(files, context()))
            .isInstanceOf(FeignException.class)
            .hasMessage(exMessage);

    verify(queryService).archive(eq("en"), any());
    verify(queryService).save(any(), any());
    verify(queryService).execute(eq("en"), any());
  }

  @Nested
  class PublishingNestedQueriesFromLowerLevelToHigher {

    @Test
    void shouldPublishQueriesFromFirstLevel() throws IOException {
      // First-level queries not depends on any other queries. They should be published first
      var dir = ResourceUtils.getFile("classpath:pipeline/sub-query/queries");
      var files = of(dir);
      Context context = new Context();

      var firstLevel = ArgumentCaptor.forClass(Map.class);

      try {
        utilQueryPipeline.process(files, context);
      } catch (QueryPublishingException e) {
        assertThat(e.getMessage()).isEqualTo(
            "Queries '[48, 49, 50]' cannot be published because they have a circular "
                + "dependency or depend on other queries that are not in the file with queries");
      }
      verify(queryService).save(firstLevel.capture(), any());
      var queryIds = getQueryIds(firstLevel);

      assertThat(queryIds).containsExactlyInAnyOrder(35, 43, 46, 47);
    }

    @Test
    void shouldPublishQueriesFromFirstAndSecondLevel() throws IOException {
      // Second-level queries depends on queries from first level. 
      // They should be published after first-level queries
      var dir = ResourceUtils.getFile("classpath:pipeline/sub-query/queries");
      var files = of(dir);
      Context context = new Context();
      context.addMappedIds(Map.of(35, 135, 43, 143, 46, 146, 47, 147));

      var secondLevel = ArgumentCaptor.forClass(Map.class);

      try {
        utilQueryPipeline.process(files, context);
      } catch (QueryPublishingException e) {
        assertThat(e.getMessage()).isEqualTo(
            "Queries '[49, 50]' cannot be published because they have a circular "
                + "dependency or depend on other queries that are not in the file with queries");
      }
      verify(queryService).save(secondLevel.capture(), any());
      var queryIds = getQueryIds(secondLevel);

      assertThat(queryIds).containsExactlyInAnyOrder(35, 43, 46, 47, 48);
    }

    @Test
    void shouldPublishQueriesFromFirstSecondAndThirdLevel() throws IOException {
      // Third-level queries depends on queries from second level
      var dir = ResourceUtils.getFile("classpath:pipeline/sub-query/queries");
      var files = of(dir);
      Context context = new Context();
      context.addMappedIds(Map.of(35, 135, 43, 143, 46, 146, 47, 147, 48, 148));

      var secondLevel = ArgumentCaptor.forClass(Map.class);

      try {
        utilQueryPipeline.process(files, context);
      } catch (QueryPublishingException e) {
        assertThat(e.getMessage()).isEqualTo(
            "Queries '[50]' cannot be published because they have a circular " 
                + "dependency or depend on other queries that are not in the file with queries");
      }
      verify(queryService).save(secondLevel.capture(), any());
      var queryIds = getQueryIds(secondLevel);

      assertThat(queryIds).containsExactlyInAnyOrder(35, 43, 46, 47, 48, 49);
    }

    @Test
    void shouldPublishQueriesFirstSecondThirdAndFourthLevel() throws IOException {
      var dir = ResourceUtils.getFile("classpath:pipeline/sub-query/queries");
      var files = of(dir);
      Context context = new Context();
      context.addMappedIds(Map.of(35, 135, 43, 143, 46, 146, 47, 147, 48, 148, 49, 149));

      var secondLevel = ArgumentCaptor.forClass(Map.class);

      utilQueryPipeline.process(files, context);

      verify(queryService).save(secondLevel.capture(), any());
      var queryIds = getQueryIds(secondLevel);

      assertThat(queryIds).containsExactlyInAnyOrder(35, 43, 46, 47, 48, 49, 50);
    }

    
    private int[] getQueryIds(ArgumentCaptor<Map> queries) {
      return queries.getValue().keySet().stream()
          .map(q -> ((Query) q).getId())
          .mapToInt(x -> (int) x).toArray();
    }
  }
}
