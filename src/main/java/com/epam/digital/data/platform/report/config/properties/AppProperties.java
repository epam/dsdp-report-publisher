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

package com.epam.digital.data.platform.report.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private String reportsDirectoryName;
  private String excerptsDirectoryName;
  private String excerptsDocxDirectoryName;
  private String excerptsCsvDirectoryName;
  private String rolesDirectoryName;

  public String getReportsDirectoryName() {
    return reportsDirectoryName;
  }

  public void setReportsDirectoryName(String reportsDirectoryName) {
    this.reportsDirectoryName = reportsDirectoryName;
  }

  public String getExcerptsDirectoryName() {
    return excerptsDirectoryName;
  }

  public void setExcerptsDirectoryName(String excerptsDirectoryName) {
    this.excerptsDirectoryName = excerptsDirectoryName;
  }

  public String getExcerptsDocxDirectoryName() {
    return excerptsDocxDirectoryName;
  }

  public void setExcerptsDocxDirectoryName(String excerptsDocxDirectoryName) {
    this.excerptsDocxDirectoryName = excerptsDocxDirectoryName;
  }

  public String getExcerptsCsvDirectoryName() {
    return excerptsCsvDirectoryName;
  }

  public void setExcerptsCsvDirectoryName(String excerptsCsvDirectoryName) {
    this.excerptsCsvDirectoryName = excerptsCsvDirectoryName;
  }

  public String getRolesDirectoryName() {
    return rolesDirectoryName;
  }

  public void setRolesDirectoryName(String rolesDirectoryName) {
    this.rolesDirectoryName = rolesDirectoryName;
  }
}
