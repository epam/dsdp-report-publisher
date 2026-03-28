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

package com.epam.digital.data.platform.report.client;

import com.epam.digital.data.platform.report.config.FeignConfig;
import com.epam.digital.data.platform.report.model.Page;
import com.epam.digital.data.platform.report.model.Query;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;

@FeignClient(name = "query", url = "http://mock", configuration = FeignConfig.class)
public interface QueryClient {

  @PostMapping("/api/queries")
  ResponseEntity<Query> postQuery(
      URI baseUrl,
      @RequestBody Query query,
      @RequestHeader(name = "Authorization") String authorizationHeader);

  @PostMapping("/api/queries/{id}")
  ResponseEntity<Query> updateQuery(
      URI baseUrl,
      @PathVariable("id") int id,
      @RequestBody Query query,
      @RequestHeader(name = "Authorization") String authorizationHeader);

  @PostMapping("/api/queries/{id}/refresh")
  ResponseEntity<String> executeQuery(
      URI baseUrl,
      @PathVariable("id") int id,
      @RequestHeader(name = "Authorization") String authorizationHeader);

  @GetMapping("/api/queries")
  ResponseEntity<Page<Query>> getQueries(
      URI baseUrl,
      @RequestParam("q") String name,
      @RequestParam("page") int page,
      @RequestHeader(name = "Authorization") String authorizationHeader);

  @DeleteMapping("/api/queries/{id}")
  ResponseEntity<Void> archiveQuery(
      URI baseUrl,
      @PathVariable("id") int id,
      @RequestHeader(name = "Authorization") String authorizationHeader);
}
