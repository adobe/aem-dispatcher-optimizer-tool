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

package com.adobe.aem.dot.common.reporter;

import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.common.analyzer.CountedRuleViolation;
import com.adobe.aem.dot.common.analyzer.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Render a List of Violation objects as a CSV document.
 */
public class CSVReporter implements ViolationReporter {

  private final List<String[]> dataLines = new ArrayList<>();
  private final Logger logger = LoggerFactory.getLogger(CSVReporter.class);

  @Override
  public String generateViolationReport(List<Violation> violationList) {
    if (violationList == null) {
      throw new IllegalArgumentException("CSV generateViolationReport received a null violationList.");
    }
    dataLines.clear();

    logger.trace("Begin: Generating report in CSV format.");

    addHeaders();
    for (Violation violation : violationList) {
      int count = (violation instanceof CountedRuleViolation) ?
                          ((CountedRuleViolation) violation).getRuleViolationCount() :
                          -1;
      addViolation(violation, count);
    }

    logger.debug("End: Finished generating report in CSV format. rowCount={}.", dataLines.size());

    return dataLines
                   .stream()
                   .map(this::convertToCSV)
                   .collect(Collectors.joining(System.lineSeparator()));
  }

  private String convertToCSV(String[] data) {
    return Stream.of(data)
            .map(this::escapeSpecialCharacters)
            .collect(Collectors.joining(","));
  }

  private String escapeSpecialCharacters(String data) {
    if (data == null) {
      // Replace null entries with an empty string
      data = "";
    }
    String escapedData = data.replaceAll("\\R", " ");
    if (data.contains(",") || data.contains("\"") || data.contains("'")) {
      // Replace quotes with double quotes
      data = data.replace("\"", "\"\"");
      // Wrap data in quotes
      escapedData = "\"" + data + "\"";
    }
    return escapedData;
  }

  private void addHeaders() {
    dataLines.add(headers);
  }

  private void addViolation(Violation violation, int count)  {
    AnalyzerRule rule = violation.getAnalyzerRule();
    String fileLocation = violation.getConfigurationSource() == null ? "<unknown file location>" :
                                  violation.getConfigurationSource().getFileName();
    String lineNumber = String.valueOf(violation.getConfigurationSource() == null ? "0" :
                                               violation.getConfigurationSource().getLineNumber());
    if (count > 1) {
      fileLocation += " (" + count + " occurrences)";
    }

    dataLines.add(new String[] {
            // File Location
            fileLocation,
            // Line Number
            lineNumber,
            // Issue
            rule.getDescription(),
            // Type
            rule.getType(),
            // Severity
            String.valueOf(rule.getSeverity()),
            // Effort
            rule.getEffort(),
            // Rule
            rule.getId(),
            // Tags
            rule.getTags() == null ? "" : String.join(",", rule.getTags()),
            // Documentation
            rule.getDocumentationURL()
    });
  }
}
