/*
 *    Copyright 2021 Adobe. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.adobe.aem.dot.common.analyzer.rules;

import com.adobe.aem.dot.common.util.PathUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JSONRuleReader {
  private final Logger logger = LoggerFactory.getLogger(JSONRuleReader.class);

  /**
   * Ingest the rules to use to evaluate the configuration.  The core-rules are always read.  All JSON files in
   * the provided folder will be read and applied in alphabetical order.
   * @param folder Folder holding additional rule files.
   * @return A merged rule list.
   * @throws IOException It might throw this exception.
   */
  AnalyzerRuleList readInternalRulesAndRulesFromFiles(String folder) throws IOException {
    AnalyzerRuleList combinedRuleList = readInternalRules();
    if (StringUtils.isNotEmpty(folder)) {
      File ruleFolder = FileUtils.getFile(folder);
      if (ruleFolder.exists()) {
        List<String> jsonFilesInFolder = getJsonFilesInFolder(folder);

        // Sort by file name.
        Collections.sort(jsonFilesInFolder);

        for (String path : jsonFilesInFolder) {
          int lastSlash = PathUtil.getLastSeparatorIndex(path);
          if (lastSlash >= 0) {
            String origin = path.substring(lastSlash + 1);  // Filename as an indicator of the source of the new rules.
            try {
              logger.trace("Reading in rules.  File=\"{}\".", path);
              AnalyzerRuleList ruleList = readRulesFromFile(path);
              combinedRuleList.addRules(ruleList, origin);
            } catch (IOException ioEx) {
              logger.error("Skipping rule file.  File=\"{}\".  Reason=\"{}\"", path, ioEx.getLocalizedMessage(), ioEx);
            }
          }
        }
      } else {
        logger.info("Folder=\"{}\" does not exist. No additional rule files were loaded.", folder);
      }
    }

    return combinedRuleList;
  }

  /**
   * Ingest the rules that will be used to evaluate the configuration.  The core-rules are always read.  Additionally, an
   * InputStream can be provided which will be used to extend the core-rules.
   * @param externalRules InputStream containing rules to be combined with the core-rules.
   * @return A merged rule list.
   * @throws IOException if IO issues are encountered.
   */
  AnalyzerRuleList readInternalRulesAndRulesFromInputStream(InputStream externalRules) throws IOException {
    AnalyzerRuleList combinedRuleList = readInternalRules();
    if (externalRules != null) {
      logger.trace("Reading in rules from provided InputStream.");
      AnalyzerRuleList ruleList = parseRulesFromInputStream(externalRules);
      combinedRuleList.addRules(ruleList, "InputStream");
    }

    return combinedRuleList;
  }

  private AnalyzerRuleList readRulesFromFile(String pathToRulesFile) throws IOException {
    InputStream fileInputStream = new FileInputStream(pathToRulesFile);
    return parseRulesFromInputStream(fileInputStream);
  }

  AnalyzerRuleList readInternalRules() throws IOException {
    ClassLoader classloader = getClass().getClassLoader();
    InputStream resourceInputStream = classloader.getResourceAsStream("core-rules.json");
    return parseRulesFromInputStream(resourceInputStream);
  }

  private AnalyzerRuleList parseRulesFromInputStream(InputStream input) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    // Check for empty file.
    if (input != null && input.available() > 0) {
      try {
        return objectMapper.readValue(input, AnalyzerRuleList.class);
      } catch (IOException ioEx) {
        logger.warn("Error reading rule file.  Reason=\"{}\"", ioEx.getLocalizedMessage(), ioEx);
      }
    }
    return new AnalyzerRuleList();
  }

  /**
   * Determine and return a list of the JSON files in a folder.  Do not look in subfolders.
   *
   * @param folderPath The path to use when looking for JSON files.
   * @return A list of JSON files.
   * @throws IOException It does.
   */
  private List<String> getJsonFilesInFolder(String folderPath) throws IOException {
    return Files.find(Paths.get(folderPath), 1,
            (p, a) -> p.toString().toLowerCase().endsWith(".json"))
            .map(Path::toString)
            .collect(Collectors.toList());
  }
}
