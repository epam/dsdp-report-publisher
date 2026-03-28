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

import static com.epam.digital.data.platform.report.util.TestUtils.REDASH_EN_API_KEY;
import static com.epam.digital.data.platform.report.util.TestUtils.REDASH_EN_URL;
import static com.epam.digital.data.platform.report.util.TestUtils.query;
import static com.epam.digital.data.platform.report.util.TestUtils.redashProperties;
import static com.epam.digital.data.platform.report.util.TestUtils.visualization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.client.VisualizationClient;
import com.epam.digital.data.platform.report.model.Query;
import com.epam.digital.data.platform.report.model.Visualization;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class VisualizationServiceTest {

    @Mock
    private VisualizationClient visualizationClient;
    private VisualizationService visualizationService;

    @BeforeEach
    void init() {
        visualizationService = new VisualizationService(visualizationClient, redashProperties());
    }
    @Test
    void shouldUpdateDefaultVisualization() {
        when(visualizationClient.updateVisualization(eq(REDASH_EN_URL), anyInt(), any(), eq(REDASH_EN_API_KEY))).thenReturn(mockResponse());
        var input = mockQueryWithOneVisualization(visualization("TABLE"));

        visualizationService.save("en", input);

        var updatedVisualization = input.values().stream()
            .flatMap(Collection::stream)
            .toList();

        Assertions.assertThat(updatedVisualization.isEmpty()).isEqualTo(true);
        verify(visualizationClient).updateVisualization(eq(REDASH_EN_URL), anyInt(), any(), eq(REDASH_EN_API_KEY));
        verify(visualizationClient, times(0)).createVisualization(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY));
    }

    @Test
    void shouldUpdateDefaultVisualizationAndCreateNewOne() {
        when(visualizationClient.updateVisualization(eq(REDASH_EN_URL), anyInt(), any(), eq(REDASH_EN_API_KEY))).thenReturn(mockResponse());
        when(visualizationClient.createVisualization(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY))).thenReturn(mockResponse());
        var input = mockQueryWithTwoVisualizations();

        visualizationService.save("en", input);

        var updatedVisualization = input.values().stream()
            .flatMap(Collection::stream)
            .toList();

        Assertions.assertThat(updatedVisualization.size()).isEqualTo(1);
        Assertions.assertThat(updatedVisualization.get(0).getId()).isEqualTo(2);
        verify(visualizationClient).updateVisualization(eq(REDASH_EN_URL), anyInt(), any(), eq(REDASH_EN_API_KEY));
        verify(visualizationClient).createVisualization(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY));
    }

    @Test
    void shouldNotUpdateDefaultVisualizationIfNoTableVisualizations() {
        when(visualizationClient.createVisualization(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY))).thenReturn(mockResponse());
        var input = mockQueryWithOneVisualization(visualization("OTHER"));

        visualizationService.save("en", input);

        var updatedVisualization = input.values().stream()
            .flatMap(Collection::stream)
            .toList();

        Assertions.assertThat(updatedVisualization.size()).isEqualTo(1);
        Assertions.assertThat(updatedVisualization.get(0).getId()).isEqualTo(2);
        verify(visualizationClient, times(0)).updateVisualization(any(), anyInt(), any(), anyString());
        verify(visualizationClient).createVisualization(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY));
    }

    private Map<Query, List<Visualization>> mockQueryWithOneVisualization(Visualization visualization) {
        var query = query("first");
        var visualizations = new ArrayList<Visualization>();

        visualizations.add(visualization);
        query.setVisualizations(visualizations);

        return Map.of(query, visualizations);
    }

    private Map<Query, List<Visualization>> mockQueryWithTwoVisualizations() {
        var query = query("first");
        var first = visualization("TABLE");
        var second = new Visualization();
        var visualizations = new ArrayList<Visualization>();

        visualizations.add(first);
        visualizations.add(second);
        query.setVisualizations(visualizations);
        second.setId(3);

        return Map.of(query, visualizations);
    }

    private ResponseEntity<Visualization> mockResponse() {
        var created = new Visualization();
        created.setId(2);
        return new ResponseEntity<>(created, HttpStatus.OK);
    }
}
