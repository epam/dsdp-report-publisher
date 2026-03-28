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

package com.epam.digital.data.platform.report.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOUtils {

  private static final Logger log = LoggerFactory.getLogger(IOUtils.class);

  private IOUtils() {}

  public static List<File> getChildDirectories(File root) {
    if (!FileUtils.getFile(root).exists()) {
      log.error("Directory '{}' does not exist", root);
      return Collections.emptyList();
    }
    return Arrays.stream(getFileList(FileUtils.getFile(root))).filter(File::isDirectory).toList();
  }

  public static List<File> getDirectoryFiles(File dir) {
    return Arrays.stream(getFileList(dir)).toList();
  }

  public static File[] getFileList(File path) {
    return Optional.ofNullable(path)
        .map(p -> p.listFiles(file -> !file.isHidden()))
        .orElse(new File[0]);
  }

  public static String getFileChecksum(File file) {
    StringBuilder sb = new StringBuilder();
    try (var fis = new FileInputStream(file)) {
      sb.append(DigestUtils.sha256Hex(fis));
    } catch (IOException e) {
      log.error("Error when calculate file {} checksum", file.getName(), e);
    }
    return sb.toString();
  }
}
