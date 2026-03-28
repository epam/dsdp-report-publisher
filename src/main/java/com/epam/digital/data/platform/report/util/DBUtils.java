package com.epam.digital.data.platform.report.util;

import com.epam.digital.data.platform.report.model.ExcerptTemplate;
import com.epam.digital.data.platform.report.repository.ExcerptTemplateRepository;

public final class DBUtils {

  private DBUtils() {
  }

  public static void saveToDataBase(ExcerptTemplateRepository repository,
      ExcerptTemplate newTemplate) {
    var saved = repository.findFirstByTemplateName(newTemplate.getTemplateName());
    if (saved == null) {
      repository.save(newTemplate);
      return;
    }

    if (saved.getChecksum().equals(newTemplate.getChecksum())) {
      return;
    }

    saved.update(newTemplate);
    repository.save(saved);
  }

}
