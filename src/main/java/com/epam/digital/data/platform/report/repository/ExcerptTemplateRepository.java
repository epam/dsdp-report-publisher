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

package com.epam.digital.data.platform.report.repository;

import com.epam.digital.data.platform.report.model.ExcerptTemplate;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface ExcerptTemplateRepository extends CrudRepository<ExcerptTemplate, UUID> {

  ExcerptTemplate findFirstByTemplateName(String templateName);

  List<ExcerptTemplate> findAllByTemplateType(String templateType);

  List<ExcerptTemplate> findAllByTemplateTypeAndTemplateNameNotIn(String templateType, List<String> templateNames);

  void deleteAllByTemplateType(String templateType);

  void deleteByTemplateTypeAndTemplateNameNotIn(String templateType, List<String> templateNames);
}
