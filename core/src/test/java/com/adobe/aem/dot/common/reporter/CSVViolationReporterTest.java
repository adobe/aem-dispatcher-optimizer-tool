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
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class CSVViolationReporterTest {

  @Test(expected = IllegalArgumentException.class)
  public void generateReportNull() throws IOException {
    ViolationReporter reporter = new CSVReporter();

    reporter.generateViolationReport(null);
  }

  @Test
  public void generateReportEmptyList() throws IOException {
    ViolationReporter reporter = new CSVReporter();

    String report = reporter.generateViolationReport(new ArrayList<>());
    assertEquals("Should return an empty report for an empty list",
            "File Location,Line Number,Issue,Type,Severity,Effort,Rule,Tags,Documentation",
            report);

    report = reporter.generateViolationReport(new ArrayList<>());
    assertEquals("Should return an empty report for an empty list",
            "File Location,Line Number,Issue,Type,Severity,Effort,Rule,Tags,Documentation",
            report);
  }

  @Test
  public void generateReportSingleViolation() throws IOException {
    AnalyzerRule rule = new AnalyzerRule();
    rule.setId("id1");
    rule.setSeverity(Severity.CRITICAL);
    rule.setDescription("This is the issue 'description' of the rule.");
    ConfigurationLine line = new ConfigurationLine("foo", "dispatcher.any", 25);
    Violation violation1 = new Violation(rule, "context!", line);
    ViolationReporter reporter = new CSVReporter();
    String report = reporter.generateViolationReport(Collections.singletonList(violation1));
    assertEquals("Should return a report matching expectations",
            "File Location,Line Number,Issue,Type,Severity,Effort,Rule,Tags,Documentation" +
                    System.lineSeparator() +
                    "dispatcher.any,25,\"This is the issue 'description' of the rule.\",,CRITICAL,,id1,,https://www.adobe.com/go/aem_cmcq_id1_en",
            report);
  }

  @Test
  public void generateReportSingleCountedViolation() throws IOException {
    AnalyzerRule rule = new AnalyzerRule();
    rule.setId("id3");
    rule.setSeverity(Severity.CRITICAL);
    rule.setDescription("This is the issue 'description' of the rule.");
    ConfigurationLine line = new ConfigurationLine("foo", "dispatcher.any", 25);
    Violation violation1 = new Violation(rule, "context!", line);
    CountedRuleViolation countedViolation = new CountedRuleViolation(violation1);
    countedViolation.setRuleViolationCount(6);

    ViolationReporter reporter = new CSVReporter();
    String report = reporter.generateViolationReport(Collections.singletonList(countedViolation));
    assertEquals("Should return a report matching expectations",
            "File Location,Line Number,Issue,Type,Severity,Effort,Rule,Tags,Documentation" +
                    System.lineSeparator() +
                    "dispatcher.any (6 occurrences),25,\"This is the issue 'description' of the rule.\",,CRITICAL,,id3,,https://www.adobe.com/go/aem_cmcq_id3_en",
            report);
  }
}