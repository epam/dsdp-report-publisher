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

import com.epam.digital.data.platform.report.client.SnippetClient;
import com.epam.digital.data.platform.report.config.properties.RedashProperties;
import com.epam.digital.data.platform.report.model.Context;
import com.epam.digital.data.platform.report.model.Snippet;

import java.net.URI;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SnippetService {

  private final SnippetClient client;
  private final RedashProperties redashProperties;

  public void publish(List<Snippet> snippets, Context context) {
    log.info("Publishing snippets");
    var redashLanguageProperties =
        redashProperties
            .getLang()
            .getOrDefault(context.getLanguage(), redashProperties.getDefaultInstance());
    snippets.forEach(
        snippet -> {
          URI url = redashLanguageProperties.getUrl();
          String apiKey = redashLanguageProperties.getApiKey();
          client.postSnippet(url, snippet, apiKey);
        });
  }
}
