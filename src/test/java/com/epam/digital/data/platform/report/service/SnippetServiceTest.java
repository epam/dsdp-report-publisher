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

import static com.epam.digital.data.platform.report.util.TestUtils.snippet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.report.client.SnippetClient;

import java.net.URI;
import java.util.List;

import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.model.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class SnippetServiceTest {

  private static final URI REDASH_ADMIN_URL = URI.create("http://admin");
  private static final String REDASH_ADMIN_API_KEY = "adminApiKey";

  @Mock
  SnippetClient client;

  SnippetService instance;

  @BeforeEach
  void init() {
    var redashProperties = new RedashProperties();
    redashProperties.setDefaultInstance(
        new RedashProperties.InstanceProperties(REDASH_ADMIN_URL, REDASH_ADMIN_API_KEY));

    instance = new SnippetService(client, redashProperties);
  }

  @Test
  void shouldPublishSnippets() {
    var snippet = snippet("stub");
    when(client.postSnippet(REDASH_ADMIN_URL, snippet, REDASH_ADMIN_API_KEY)).thenReturn(mockVoidResponse());

    var context = new Context();
    context.setDataSourceId(1);

    instance.publish(List.of(snippet), context);

    verify(client).postSnippet(REDASH_ADMIN_URL, snippet, REDASH_ADMIN_API_KEY);
  }

  private ResponseEntity<Void> mockVoidResponse() {
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
