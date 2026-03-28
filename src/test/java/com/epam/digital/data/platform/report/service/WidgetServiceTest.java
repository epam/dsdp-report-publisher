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
import static com.epam.digital.data.platform.report.util.TestUtils.dashboard;
import static com.epam.digital.data.platform.report.util.TestUtils.redashProperties;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.client.WidgetClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class WidgetServiceTest {

    @Mock
    private WidgetClient widgetClient;

    private WidgetService instance;

    @BeforeEach
    void init() {
        instance = new WidgetService(widgetClient, redashProperties());
    }

    @Test
    void shouldSaveWidgets() {
        when(widgetClient.createWidget(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY))).thenReturn(mockResponse());

        instance.save("en", dashboard(1, "stub"));

        verify(widgetClient).createWidget(eq(REDASH_EN_URL), any(), eq(REDASH_EN_API_KEY));
    }

    private <T> ResponseEntity<T> mockResponse() {
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
