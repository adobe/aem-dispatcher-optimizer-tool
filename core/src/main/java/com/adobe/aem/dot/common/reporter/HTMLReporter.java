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
import com.adobe.aem.dot.common.analyzer.ViolationVerbosity;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Render a List of Violation objects as a HTML document.
 */
public class HTMLReporter implements ViolationReporter {
  // Set the configuration path to have it included in the report, above the table.  Nothing will be
  // output, if it is left blank.
  @Setter
  private String configurationPath;
  @Setter
  private ViolationVerbosity verbosity = null;

  private final List<String> dataLines = new ArrayList<>();
  private final Logger logger = LoggerFactory.getLogger(HTMLReporter.class);

  @Override
  public String generateViolationReport(List<Violation> violationList) {
    if (violationList == null) {
      throw new IllegalArgumentException("HTML generateViolationReport received a null violationList.");
    }
    dataLines.clear();

    logger.trace("Begin: Generating report in HTML format.");

    addHTMLStart();

    // Table has been started, add header and violation rows.
    addHeaderRow();
    addViolationRows(violationList);

    addHTMLEnd();

    logger.debug("End: Finished generating report in HTML format. rowCount={}.", dataLines.size());

    return dataLines
                   .stream()
                   .collect(Collectors.joining(System.lineSeparator()));
  }

  protected void addHTMLStart() {
    dataLines.add("<html>");
    dataLines.add("<head>");
    dataLines.add("  <style>table, th, td { border: 1px solid black; }</style>");
    dataLines.add("</head>");
    dataLines.add("<body>");
    if (StringUtils.isNotEmpty(this.configurationPath)) {
      dataLines.add("<p><b>Configuration Path</b>: " + this.configurationPath + "</p>");
    }
    if (this.verbosity != null) {
      dataLines.add("<p><b>Report Verbosity</b>: " + this.verbosity.toString() + "</p>");
    }
    dataLines.add("  <table cellspacing=\"0\" cellpadding=\"2px\">");
  }

  protected void addHTMLEnd() {
    dataLines.add("  </table>");
    dataLines.add("</body>");
    dataLines.add("</html>");
  }

  private void addHeaderRow() {
    String format = "      <th>{0}</th>";   // + System.lineSeparator();

    dataLines.add("    <tr>");

    for (String header: headers) {
      dataLines.add(MessageFormat.format(format, header));
    }

    dataLines.add("    </tr>");
  }

  private void addViolationRows(List<Violation> violationList) {
    for (Violation violation : violationList) {
      int count = (violation instanceof CountedRuleViolation) ?
                          ((CountedRuleViolation) violation).getRuleViolationCount() :
                          -1;
      addViolationRow(violation, count);
    }
  }

  private void addViolationRow(Violation violation, int count) {
    AnalyzerRule rule = violation.getAnalyzerRule();
    String fileLocation = violation.getConfigurationSource() == null ? "<unknown file location>" :
                                  violation.getConfigurationSource().getFileName();
    String lineNumber = String.valueOf(violation.getConfigurationSource() == null ? "0" :
                                               violation.getConfigurationSource().getLineNumber());
    if (count > 1) {
      fileLocation += " (" + count + " occurrences)";
    }

    String format = "      <td>{0}</td>";

    dataLines.add("    <tr>");
    dataLines.add(MessageFormat.format(format, fileLocation));
    dataLines.add(MessageFormat.format(format, lineNumber));
    dataLines.add(MessageFormat.format(format, processViolationProperty(rule.getDescription())));
    dataLines.add(MessageFormat.format(format, processViolationProperty(rule.getType())));
    dataLines.add(MessageFormat.format(format, String.valueOf(rule.getSeverity())));
    dataLines.add(MessageFormat.format(format, processViolationProperty(rule.getEffort())));
    dataLines.add(MessageFormat.format(format, processViolationProperty(rule.getId())));
    dataLines.add(MessageFormat.format(format, processViolationProperty(rule.getTags() == null ? "" : String.join(",", rule.getTags()))));

    if (StringUtils.isNotEmpty(rule.getDocumentationURL())) {
      String documentationLink =
              MessageFormat.format("<a href=\"{0}\">{1}</a>",
                      rule.getDocumentationURL(), rule.getDocumentationURL());
      dataLines.add(MessageFormat.format(format,documentationLink));
    } else {
      dataLines.add(MessageFormat.format(format, ""));
    }
    dataLines.add("    </tr>");
  }

  // Any processing the of the property values.  Check for null.  Escape characters.  etc.
  private String processViolationProperty(String value) {
    if (value == null) {
      return "";
    }
    return value;
  }
}
