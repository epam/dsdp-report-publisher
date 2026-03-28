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

import static com.epam.digital.data.platform.report.service.QueryService.MISSING_PARAMETER_ON_QUERY_PREPOPULATION_ERROR_PREFIX;
import static com.epam.digital.data.platform.report.util.TestUtils.REDASH_EN_API_KEY;
import static com.epam.digital.data.platform.report.util.TestUtils.REDASH_EN_URL;
import static com.epam.digital.data.platform.report.util.TestUtils.queries;
import static com.epam.digital.data.platform.report.util.TestUtils.query;
import static com.epam.digital.data.platform.report.util.TestUtils.redashProperties;
import static com.epam.digital.data.platform.report.util.TestUtils.visualization;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.client.QueryClient;
import com.epam.digital.data.platform.report.model.Page;
import com.epam.digital.data.platform.report.model.Query;
import com.epam.digital.data.platform.report.model.Visualization;
import com.epam.digital.data.platform.report.util.TestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import feign.FeignException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class QueryServiceTest {

    @Mock
    private QueryClient queryClient;
    @Mock
    private VisualizationService visualizationService;

    private QueryService instance;

    @BeforeEach
    void init() {
        instance = new QueryService(queryClient, visualizationService, redashProperties());
    }

    @Test
    void shouldSaveQueries() {
        when(queryClient.postQuery(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY))).thenReturn(mockQueryResponse());

        instance.save(mockQueryWithOneVisualization(visualization("TABLE")), TestUtils.context());

        verify(queryClient).postQuery(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY));
        verify(visualizationService).save(eq("en"), any());
    }

    @Test
    void shouldPublishQueries() {
        when(queryClient.updateQuery(eq(REDASH_EN_URL), anyInt(), any(), eq(REDASH_EN_API_KEY))).thenReturn(mockResponse(Query.class));

        instance.publish("en", queries("first", "second"));

        verify(queryClient, times(2)).updateQuery(eq(REDASH_EN_URL), anyInt(), any(), eq(REDASH_EN_API_KEY));
    }

    @Test
    void shouldExecuteQueries() {
        when(queryClient.executeQuery(eq(REDASH_EN_URL), anyInt(), eq(REDASH_EN_API_KEY))).thenReturn(mockResponse(String.class));

        instance.execute("en", queries("first", "second"));

        verify(queryClient, times(2)).executeQuery(eq(REDASH_EN_URL), anyInt(), eq(REDASH_EN_API_KEY));
    }

    @Test
    void shouldSkipErrorsRelatedToMissingParams() {
        var exception = Mockito.mock(FeignException.class);
        Mockito.when(exception.getMessage()).thenReturn(MISSING_PARAMETER_ON_QUERY_PREPOPULATION_ERROR_PREFIX);
        Mockito.doThrow(exception).when(queryClient).executeQuery(eq(REDASH_EN_URL), anyInt(), eq(REDASH_EN_API_KEY));
        Assertions.assertThatCode(() -> instance.execute("en", queries("first", "second"))).doesNotThrowAnyException();
    }

    @Test
    void shouldArchiveQueries() {
        when(queryClient.archiveQuery(eq(REDASH_EN_URL), anyInt(), eq(REDASH_EN_API_KEY))).thenReturn(mockResponse(Void.class));
        when(queryClient.getQueries(eq(REDASH_EN_URL), any(), anyInt(), eq(REDASH_EN_API_KEY))).thenReturn(mockPageResponse());

        instance.archive("en", new ArrayList<>(queries("first", "second")));

        verify(queryClient, times(2)).getQueries(eq(REDASH_EN_URL), any(), anyInt(), eq(REDASH_EN_API_KEY));
        verify(queryClient, times(2)).archiveQuery(eq(REDASH_EN_URL), anyInt(), eq(REDASH_EN_API_KEY));
    }

    @Test
    void shouldCollectQueriesFromThreePages() {
        when(queryClient.archiveQuery(eq(REDASH_EN_URL), anyInt(), eq(REDASH_EN_API_KEY))).thenReturn(mockResponse(Void.class));
        when(queryClient.getQueries(REDASH_EN_URL, "first", 1, REDASH_EN_API_KEY)).thenReturn(mockPageResponse("first-abc"));
        when(queryClient.getQueries(REDASH_EN_URL, "first", 2, REDASH_EN_API_KEY)).thenReturn(mockPageResponse("first"));
        when(queryClient.getQueries(REDASH_EN_URL, "first", 3, REDASH_EN_API_KEY)).thenReturn(mockPageResponse("first"));

        instance.archive("en", new ArrayList<>(queries("first")));

        verify(queryClient, times(3)).getQueries(eq(REDASH_EN_URL), any(), anyInt(), eq(REDASH_EN_API_KEY));
        verify(queryClient, times(2)).archiveQuery(eq(REDASH_EN_URL), anyInt(), eq(REDASH_EN_API_KEY));
    }

    private <T> ResponseEntity<T> mockResponse(Class<T> clazz) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<Query> mockQueryResponse() {
        return new ResponseEntity<>(query("query name"), HttpStatus.OK);
    }

    private ResponseEntity<Page<Query>> mockPageResponse() {
        var page = new Page<Query>();
        page.setCount(2);
        page.setPageSize(25);
        var query1 = new Query();
        query1.setId(1);
        query1.setName("first");
        var query2 = new Query();
        query2.setId(1);
        query2.setName("second");
        page.setResults(List.of(query1, query2));
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    private ResponseEntity<Page<Query>> mockPageResponse(String name) {
        var page = new Page<Query>();
        page.setCount(3);
        page.setPageSize(1);
        var query = new Query();
        query.setId(1);
        query.setName(name);
        page.setResults(List.of(query));
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    private Map<Query, List<Visualization>> mockQueryWithOneVisualization(Visualization visualization) {
        var query = query("query name");
        var visualizations = new ArrayList<Visualization>();

        visualizations.add(visualization);
        query.setVisualizations(visualizations);

        return Map.of(query, visualizations);
    }
}
