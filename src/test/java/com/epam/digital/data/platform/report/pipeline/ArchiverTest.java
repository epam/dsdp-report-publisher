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
import static com.epam.digital.data.platform.report.util.TestUtils.mockVoidResponse;
import static com.epam.digital.data.platform.report.util.TestUtils.redashProperties;
import static java.util.List.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.client.DashboardClient;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Dashboard;
import com.epam.digital.data.platform.report.model.Page;
import com.epam.digital.data.platform.report.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class ArchiverTest {

    @Mock
    DashboardClient dashboardClient;

    @Mock
    QueryService queryService;

    Archiver instance;

    @BeforeEach
    void init() {
        instance = new Archiver(dashboardClient, queryService, redashProperties());
    }

    @Test
    void shouldSuccessfullyArchiveDashboardAndQueries() {
        ResponseEntity<Page<Dashboard>> dashboardResponse = mockPageDashboardResponse(1,"stub", 4);
        ResponseEntity<Page<Dashboard>> dashboardResponse2 = mockPageDashboardResponse(2, "stub2", 4);

        when(dashboardClient.findAllDashboards(REDASH_EN_URL, 1, REDASH_EN_API_KEY)).thenReturn(dashboardResponse);
        when(dashboardClient.findAllDashboards(REDASH_EN_URL, 2, REDASH_EN_API_KEY)).thenReturn(dashboardResponse2);
        when(dashboardClient.findDashboardById(REDASH_EN_URL, 1, REDASH_EN_API_KEY)).thenReturn(mockDashboardResponse(1, "stub", 4));
        when(dashboardClient.findDashboardById(REDASH_EN_URL, 2, REDASH_EN_API_KEY)).thenReturn(mockDashboardResponse(2, "stub2", 4));
        when(dashboardClient.archiveDashboard(eq(REDASH_EN_URL), anyInt(), eq(REDASH_EN_API_KEY))).thenReturn(mockVoidResponse());

        var context = new Context();
        context.setLanguage("en");
        context.setDataSourceId(4);

        instance.archive(context);

        verify(dashboardClient).findAllDashboards(REDASH_EN_URL, 1, REDASH_EN_API_KEY);
        verify(dashboardClient).findAllDashboards(REDASH_EN_URL, 2, REDASH_EN_API_KEY);
        verify(dashboardClient).findDashboardById(REDASH_EN_URL, 1, REDASH_EN_API_KEY);
        verify(dashboardClient).findDashboardById(REDASH_EN_URL, 2, REDASH_EN_API_KEY);
        verify(dashboardClient).archiveDashboard(REDASH_EN_URL, 1, REDASH_EN_API_KEY);
        verify(dashboardClient).archiveDashboard(REDASH_EN_URL, 2, REDASH_EN_API_KEY);
        verify(queryService, times(2)).archive(eq("en"), any());
    }

    @Test
    void shouldNotArchiveDashboardAndQueriesFromWrongDatasource() {
        ResponseEntity<Page<Dashboard>> dashboardResponse = mockPageDashboardResponse(1,"stub", 4);
        ResponseEntity<Page<Dashboard>> dashboardResponse2 = mockPageDashboardResponse(2, "stub2", 4);

        when(dashboardClient.findAllDashboards(REDASH_EN_URL, 1, REDASH_EN_API_KEY)).thenReturn(dashboardResponse);
        when(dashboardClient.findAllDashboards(REDASH_EN_URL, 2, REDASH_EN_API_KEY)).thenReturn(dashboardResponse2);
        when(dashboardClient.findDashboardById(REDASH_EN_URL, 1, REDASH_EN_API_KEY)).thenReturn(mockDashboardResponse(1, "stub", 4));
        when(dashboardClient.findDashboardById(REDASH_EN_URL, 2, REDASH_EN_API_KEY)).thenReturn(mockDashboardResponse(2, "stub2", 4));

        var context = new Context();
        context.setLanguage("en");
        context.setDataSourceId(5);

        instance.archive(context);

        verify(dashboardClient).findAllDashboards(REDASH_EN_URL, 1, REDASH_EN_API_KEY);
        verify(dashboardClient).findAllDashboards(REDASH_EN_URL, 2, REDASH_EN_API_KEY);
        verify(dashboardClient).findDashboardById(REDASH_EN_URL, 1, REDASH_EN_API_KEY);
        verify(dashboardClient).findDashboardById(REDASH_EN_URL, 2, REDASH_EN_API_KEY);
        verify(dashboardClient, never()).archiveDashboard(eq(REDASH_EN_URL), anyInt(), eq(REDASH_EN_API_KEY));
        verify(queryService, never()).archive(eq("en"), any());
    }

    private ResponseEntity<Dashboard> mockDashboardResponse(int id, String dashboardName, int dataSourceId) {
        return ResponseEntity.status(200)
                .body(dashboard(id, dashboardName, dataSourceId));
    }

    private ResponseEntity<Page<Dashboard>> mockPageDashboardResponse(int counter,
                                                                      String dashboardName, int dataSourceId) {
        Page<Dashboard> page = new Page<>();
        page.setPage(counter);
        page.setPageSize(1);
        page.setCount(2);
        page.setResults(of(dashboard(counter, dashboardName, dataSourceId)));

        return ResponseEntity.status(200)
                .body(page);
    }
}
