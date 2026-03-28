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

import static java.util.stream.Collectors.toList;

import com.epam.digital.data.platform.report.pipeline.AbstractPipeline;
import com.epam.digital.data.platform.report.pipeline.Archiver;
import com.epam.digital.data.platform.report.pipeline.PipelineOrder;
import com.epam.digital.data.platform.report.pipeline.Publisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Dashboard;

@Order(PipelineOrder.DASHBOARD_PIPELINE)
@Component
public class DashboardPipeline extends AbstractPipeline {

    private final Archiver archiver;
    private final Publisher publisher;

    private final Logger log = LoggerFactory.getLogger(DashboardPipeline.class);

    private static final Predicate<? super File> DASHBOARD_FILTER =
        file -> !file.isDirectory() && file.getName().endsWith(".json");

    public DashboardPipeline(ObjectMapper objectMapper, Archiver archiver, Publisher publisher) {
        super(objectMapper);
        this.archiver = archiver;
        this.publisher = publisher;
    }

    public void process(List<File> files, Context context) {
        archiver.archive(context);
        for (Dashboard dashboard : getDashboards(files)) {
            log.info("Publishing dashboard '{}'", dashboard.getName());
            publisher.publish(dashboard, context);
        }
    }

    public boolean isApplicable(List<File> files) {
        return files.isEmpty() || files.stream().anyMatch(DASHBOARD_FILTER) || files.stream().anyMatch(File::isDirectory);
    }

    private List<Dashboard> getDashboards(List<File> files) {
        return files.stream()
            .filter(DASHBOARD_FILTER)
            .map(file -> readValue(file, Dashboard.class))
            .collect(toList());
    }
}
