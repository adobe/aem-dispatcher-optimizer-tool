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

import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.common.analyzer.CountedRuleViolation;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.analyzer.Violation;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class HTMLViolationReporterTest {

  private final List<String> emptyHTMLReport = Arrays.asList(
          "<html><head>  <style>table, th, td { border: 1px solid black; }",
          "</style></head><body>",
          "  <table cellspacing=\"0\" cellpadding=\"2px\">",
          "    <tr>",
          "      <th>File Location</th>",
          "      <th>Line Number</th>",
          "      <th>Issue</th>",
          "      <th>Type</th>",
          "      <th>Severity</th>",
          "      <th>Effort</th>",
          "      <th>Rule</th>",
          "      <th>Tags</th>",
          "      <th>Documentation</th>",
          "    </tr>",
          "  </table></body>",
          "</html>");

  @Test(expected = IllegalArgumentException.class)
  public void generateReportNull() throws IOException {
    ViolationReporter reporter = new HTMLReporter();

    reporter.generateViolationReport(null);
  }

  @Test
  public void generateReportEmptyList() throws IOException {
    ViolationReporter reporter = new HTMLReporter();

    String report = reporter.generateViolationReport(new ArrayList<>());
    report = report.replaceAll("\r", "").replaceAll("\n", "");
    assertEquals("Should return an empty report for an empty list",
            String.join("", emptyHTMLReport), report);

    report = reporter.generateViolationReport(new ArrayList<>());
    report = report.replaceAll("\r", "").replaceAll("\n", "");
    assertEquals("Should return an empty report for an empty list",
            String.join("", emptyHTMLReport), report);
  }

  @Test
  public void generateReportSingleViolation() throws IOException {
    AnalyzerRule rule = new AnalyzerRule();
    rule.setId("id1");
    rule.setSeverity(Severity.CRITICAL);
    rule.setDescription("This is the issue 'description' of the rule.");
    ConfigurationLine line = new ConfigurationLine("foo", "dispatcher.any", 25);
    Violation violation1 = new Violation(rule, "context!", line);

    String[] violationItems = new String[]{
            "    <tr>",
            "      <td>dispatcher.any</td>",
            "      <td>25</td>",
            "      <td>This is the issue 'description' of the rule.</td>",
            "      <td></td>",
            "      <td>CRITICAL</td>",
            "      <td></td>",
            "      <td>id1</td>",
            "      <td></td>",
            "      <td><a href=\"https://www.adobe.com/go/aem_cmcq_id1_en\">https://www.adobe.com/go/aem_cmcq_id1_en</a></td>",
            "    </tr>",
    };

    List<String> oneViolationReport = new ArrayList<>(emptyHTMLReport);
    for (String item: violationItems) {
      oneViolationReport.add(oneViolationReport.size() - 2, item);
    }

    ViolationReporter reporter = new HTMLReporter();
    String report = reporter.generateViolationReport(Collections.singletonList(violation1));
    report = report.replaceAll("\r", "").replaceAll("\n", "");

    assertEquals("Should matched expected report", String.join("", oneViolationReport), report);
  }

  @Test
  public void generateReportSingleCountedViolation() throws IOException {
    AnalyzerRule rule = new AnalyzerRule();
    rule.setId("id2");
    rule.setSeverity(Severity.CRITICAL);
    rule.setDescription("This is the issue 'description' of the rule.");
    ConfigurationLine line = new ConfigurationLine("foo", "dispatcher.any", 25);
    Violation violation1 = new Violation(rule, "context!", line);
    CountedRuleViolation countedViolation = new CountedRuleViolation(violation1);
    countedViolation.setRuleViolationCount(5);

    String[] violationItems = new String[]{
            "    <tr>",
            "      <td>dispatcher.any (5 occurrences)</td>",
            "      <td>25</td>",
            "      <td>This is the issue 'description' of the rule.</td>",
            "      <td></td>",
            "      <td>CRITICAL</td>",
            "      <td></td>",
            "      <td>id2</td>",
            "      <td></td>",
            "      <td><a href=\"https://www.adobe.com/go/aem_cmcq_id2_en\">https://www.adobe.com/go/aem_cmcq_id2_en</a></td>",
            "    </tr>",
    };

    List<String> oneViolationReport = new ArrayList<>(emptyHTMLReport);
    for (String item: violationItems) {
      oneViolationReport.add(oneViolationReport.size() - 2, item);
    }

    ViolationReporter reporter = new HTMLReporter();
    String report = reporter.generateViolationReport(Collections.singletonList(countedViolation));
    report = report.replaceAll("\r", "").replaceAll("\n", "");

    assertEquals("Should matched expected report", String.join("", oneViolationReport), report);
  }
}