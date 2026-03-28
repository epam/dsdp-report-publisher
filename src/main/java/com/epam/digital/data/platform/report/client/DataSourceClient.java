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
import com.epam.digital.data.platform.report.model.DataSource;

import java.net.URI;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "datasource", url = "http://mock", configuration = FeignConfig.class)
public interface DataSourceClient {

  @GetMapping("/api/data_sources")
  ResponseEntity<List<DataSource>> getDataSources(
      URI baseUrl, @RequestHeader(name = "Authorization") String authorizationHeader);

  @PostMapping("/api/data_sources")
  ResponseEntity<DataSource> createDataSource(
      URI baseUrl,
      @RequestBody DataSource dataSource,
      @RequestHeader(name = "Authorization") String authorizationHeader);

  @PostMapping("/api/data_sources/{id}")
  ResponseEntity<DataSource> updateDataSource(
          URI baseUrl,
          @PathVariable("id") int id,
          @RequestBody DataSource dataSource,
          @RequestHeader(name = "Authorization") String authorizationHeader);

  @DeleteMapping("/api/data_sources/{id}")
  ResponseEntity<Void> deleteDataSource(
      URI baseUrl,
      @PathVariable("id") int id,
      @RequestHeader(name = "Authorization") String authorizationHeader);
}
