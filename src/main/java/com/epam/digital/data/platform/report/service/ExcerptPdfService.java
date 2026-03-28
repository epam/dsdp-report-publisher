/*
 * Copyright 2022 EPAM Systems.
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

import static com.epam.digital.data.platform.report.util.DBUtils.saveToDataBase;
import com.epam.digital.data.platform.report.exception.ExcerptBuildingException;
import com.epam.digital.data.platform.report.model.ExcerptTemplate;
import com.epam.digital.data.platform.report.repository.ExcerptTemplateRepository;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
public class ExcerptPdfService implements ExcerptService {

  static final String TEMPLATE_TYPE = "pdf";

  private final ExcerptTemplateRepository repository;
  private final Logger log = LoggerFactory.getLogger(ExcerptPdfService.class);

  public ExcerptPdfService(ExcerptTemplateRepository repository) {
    this.repository = repository;
  }

  @Transactional
  @Override
  public void processExcerpt(File dir) {
    var indexFile = Path.of(dir.getPath(), "index.html.ftl").toFile();
    try {
      var htmlString = FileUtils.readFileToString(indexFile, StandardCharsets.UTF_8);
      var document = Jsoup.parse(htmlString);
      document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

      embedImagesToHtml(document, dir);
      embedStyleToHtml(document, dir);

      var template = new ExcerptTemplate();
      template.setTemplate(document.toString());
      template.setTemplateName(dir.getName());
      template.setTemplateType(TEMPLATE_TYPE);
      template.setChecksum(DigestUtils.sha256Hex(document.toString()));

      saveToDataBase(repository, template);
    } catch (Exception e) {
      throw new ExcerptBuildingException("Failed to build template", e);
    }
  }

  @Transactional
  @Override
  public void cleanNonExistingExcerptTemplates(List<File> existingTemplates) {
    var existingTemplateNames = existingTemplates.stream()
            .map(File::getName)
            .toList();
    if (existingTemplateNames.isEmpty()) {
      repository.deleteAllByTemplateType(TEMPLATE_TYPE);
    }
    repository.deleteByTemplateTypeAndTemplateNameNotIn(TEMPLATE_TYPE, existingTemplateNames);
  }

  private void embedImagesToHtml(Document htmlDocument, File dir) {
    for (Element image : htmlDocument.select("img")) {
      var imageFile = Path.of(dir.getPath(), image.attr("src")).toFile();
      try {
        var bytes = FileUtils.readFileToByteArray(imageFile);
        var base64encodedImage = Base64.getEncoder().encodeToString(bytes);
        image.attr("src", "data:image/jpeg;base64," + base64encodedImage);
      } catch (Exception e) {
        throw new ExcerptBuildingException(
            String.format("Failed to embed picture \"%s\" into template",
                image.attr("src")), e);
      }
    }
  }

  private void embedStyleToHtml(Document document, File dir) {
    var styleString = getStyleAsString(document, dir);

    document.head().select("style").remove();
    styleString += "\n* { font-family: Roboto; }\n";
    document.head().append("<style>" + styleString + "</style>");
  }

  private String getStyleAsString(Document document, File dir) {
    Elements links = document.select("link[href]");

    var styleString = new StringBuilder();
    for (Element link : links) {
      appendStyleFromLink(link, dir, styleString);
    }

    return styleString.toString();
  }

  private void appendStyleFromLink(Element link, File dir, StringBuilder styleString) {
    String styleNameHref = link.attr("href");

    if (StringUtils.isBlank(styleNameHref)) {
      return;
    }

    var styleFile = Path.of(dir.getPath(), "css", FilenameUtils.getName(styleNameHref))
      .toFile();

    if (styleFile.exists()) {
      try {
        styleString.append(FileUtils.readFileToString(styleFile, StandardCharsets.UTF_8))
          .append("\n");
        link.remove();
      } catch (Exception e) {
        throw new ExcerptBuildingException("Failed to embed styles into template", e);
      }
    } else {
      log.error("Failed to embed styles into template, file '{}' doesn't exist.",
        styleNameHref);
    }
  }
}
