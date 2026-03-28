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

package com.epam.digital.data.platform.report.pipeline;

import static com.epam.digital.data.platform.report.util.TestUtils.REDASH_EN_API_KEY;
import static com.epam.digital.data.platform.report.util.TestUtils.REDASH_EN_URL;
import static com.epam.digital.data.platform.report.util.TestUtils.dashboard;
import static com.epam.digital.data.platform.report.util.TestUtils.redashProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.client.DashboardClient;
import com.epam.digital.data.platform.report.model.Dashboard;
import com.epam.digital.data.platform.report.service.QueryService;
import com.epam.digital.data.platform.report.service.WidgetService;
import com.epam.digital.data.platform.report.util.TestUtils;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class PublisherTest {

    @Mock
    DashboardClient dashboardClient;

    @Mock
    WidgetService widgetService;

    @Mock
    QueryService queryService;

    Publisher instance;

    @BeforeEach
    void init() {
        instance = new Publisher(dashboardClient, widgetService, queryService, redashProperties());
    }

    @Test
    void shouldPublishDashboard() {
        var input = dashboard(1, "stub");
        var created = mockResponse();
        when(dashboardClient.createDashboard(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY))).thenReturn(created);
        when(dashboardClient.updateDashboard(eq(REDASH_EN_URL), anyInt(), any(), eq(REDASH_EN_API_KEY))).thenReturn(created);
        when(queryService.save(any(), any())).thenReturn(new HashSet<>());

        instance.publish(input, TestUtils.context());

        assertThat(input.getId()).isEqualTo(created.getBody().getId());
        verify(dashboardClient).createDashboard(REDASH_EN_URL, input, REDASH_EN_API_KEY);
        verify(queryService).save(any(), any());
        verify(queryService).publish(eq("en"), any());
        verify(widgetService).save(eq("en"), any());
        verify(dashboardClient).updateDashboard(eq(REDASH_EN_URL), anyInt(), any(), eq(REDASH_EN_API_KEY));
    }

    private ResponseEntity<Dashboard> mockResponse() {
        var created = new Dashboard();
        created.setId(999);
        return new ResponseEntity<>(created, HttpStatus.OK);
    }
}
