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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static java.util.List.of;
import static com.epam.digital.data.platform.report.util.TestUtils.context;

import com.epam.digital.data.platform.report.pipeline.Archiver;
import com.epam.digital.data.platform.report.pipeline.Publisher;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.util.ResourceUtils;
import com.epam.digital.data.platform.report.config.TestConfig;

@SpringBootTest(classes = {DashboardPipeline.class})
@Import(TestConfig.class)
public class DashboardPipelineTest {

    @MockBean
    private Publisher publisher;
    @MockBean
    private Archiver archiver;
    @Autowired
    private DashboardPipeline dashboardPipeline;

    @Test
    void shouldSuccessfullyArchiveAndPublishDashboard() throws IOException {
        var dashboard = ResourceUtils.getFile("classpath:pipeline/admin/en/correct_dashboard.json");
        var files = of(dashboard);

        var context = context();

        dashboardPipeline.process(files, context);

        verify(archiver).archive(context);
        verify(publisher).publish(any(), any());
    }

    @Test
    void shouldNotPublishDashboardIfNoneFound() throws FileNotFoundException {
        var dashboard = ResourceUtils.getFile("classpath:pipeline");
        var files = of(dashboard);

        var context = context();
        dashboardPipeline.process(files, context);

        verify(archiver).archive(context);
        verify(publisher, times(0)).publish(any(), any());
    }
}
