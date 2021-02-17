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

package com.adobe.aem.dot.common.analyzer.MultiConfigTests;

import com.adobe.aem.dot.common.Configuration;
import com.adobe.aem.dot.common.analyzer.Analyzer;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.analyzer.ViolationVerbosity;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.reporter.HTMLReporter;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.analyzer.DispatcherAnalyzer;
import com.adobe.aem.dot.httpd.core.analyzer.HttpdAnalyzer;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * The MultiConfig tests grew over time from just parsing a couple Dispatcher configs, to a lot of them, to
 * parsing Httpd configs, to also running analysis, to also generating a report.  The patch works shows a little.
 * This class encompasses the common methods for the 2 tests including path manipulation, violation counting
 * and report generation.
 */
public class MultiConfigHelper {

  public static final boolean runRuleViolationChecks = false;   // Change to TRUE to run analysis and output an html report

  // Find good identifiers from the (variable) path
  public static String getConfigurationDirectoryId(String relativeConfPath) {
    String directoryId = "unknown";
    String[] pathParts = PathUtil.split(relativeConfPath);
    int i;
    for (i = 0; i < pathParts.length - 2; i++) {
      if (pathParts[i].equals("configurations")) {
        directoryId = pathParts[i+2];
        break;
      }
    }

    return directoryId;
  }

  /**
   * Return the final folder of a path.
   * @param path A path
   * @return folder Final folder of a path
   */
  public static String getLastPathElement(String path, boolean includeSlash) {
    int lastSlash = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
    return path.substring(lastSlash + (includeSlash ? 0 : 1));
  }

  /**
   * Analyze a Dispatcher Configuration and output the totals for the different severity of violations of best
   * practice and parsing errors.
   * @param results Results of configuration ingestion (violations & configuration)
   * @return String containing totals
   * @throws IOException Thrown if reading common rules encounters a problem.
   */
  public static AnalysisData getViolationCounts(String configDirName, ConfigurationParseResults<?> results,
                                                String configurationPath, String targetReportPath,
                                                AnalyzerRuleList ruleList, List<AnalysisData> configurationReportData)
          throws IOException {
    Configuration config = results.getConfiguration();
    Analyzer analyzer;
    String reportFilePath;
    String reportPath = PathUtil.appendPaths(targetReportPath, "reports");
    if (config instanceof HttpdConfiguration) {
      analyzer = new HttpdAnalyzer(ruleList);
      reportFilePath = PathUtil.appendPaths(PathUtil.appendPaths(reportPath, configDirName), "httpd.html");
    } else {
      analyzer = new DispatcherAnalyzer(ruleList);
      reportFilePath = PathUtil.appendPaths(PathUtil.appendPaths(reportPath, configDirName), "dispatcher.html");
    }

    // Write HTML report with PARTIAL verbosity
    List<Violation> violations = analyzer.getViolations(config, ViolationVerbosity.PARTIAL);
    violations.addAll(results.getViolations(ViolationVerbosity.PARTIAL));
    String reportName = MultiConfigHelper.writeHTMLReport(violations, reportFilePath, configurationPath);
    AnalysisData data = MultiConfigHelper.getAnalysisData(configDirName, violations, reportName);
    if (data != null) {
      configurationReportData.add(data);
    }

    // Count violations with FULL verbosity
    violations = analyzer.getViolations(config, ViolationVerbosity.FULL);
    violations.addAll(results.getViolations(ViolationVerbosity.FULL));
    return MultiConfigHelper.getAnalysisData(configDirName, violations, reportName);
  }

  public static AnalysisData getAnalysisData(String configDirName, List<Violation> violations, String reportName) {
    long blockerCount = violations.stream()
                                .filter(v -> v.getAnalyzerRule().getSeverity() == Severity.BLOCKER)
                                .count();
    long criticalCount = violations.stream()
                                 .filter(v -> v.getAnalyzerRule().getSeverity() == Severity.CRITICAL)
                                 .count();
    long majorCount = violations.stream()
                              .filter(v -> v.getAnalyzerRule().getSeverity() == Severity.MAJOR)
                              .count();
    long minorCount = violations.stream()
                              .filter(v -> v.getAnalyzerRule().getSeverity() == Severity.MINOR)
                              .count();
    long infoCount = violations.stream()
                             .filter(v -> v.getAnalyzerRule().getSeverity() == Severity.INFO)
                             .count();
    if (blockerCount + criticalCount + majorCount + minorCount + infoCount > 0) {
      return new AnalysisData(configDirName, (int) blockerCount, (int) criticalCount, (int) majorCount, (int) minorCount,
              (int) infoCount, reportName);
    }

    return null;
  }

  public static String writeHTMLReport(List<Violation> violations, String reportPath, String configPath) {
    // Do not output any report if there are no violations.
    if (violations.isEmpty()) {
      return "Error";
    }

    HTMLReporter htmlReporter = new HTMLReporter();
    htmlReporter.setConfigurationPath(configPath);
    htmlReporter.setVerbosity(ViolationVerbosity.PARTIAL);
    String htmlReport = htmlReporter.generateViolationReport(violations);
    File reportFile = new File(reportPath);
    File parentFolder = reportFile.getParentFile();

    // If the target directory had more than 1 config file, they get more than 1 report and a readme.txt.
    if (reportFile.exists()) {
      String stamp = Long.toString((new Date()).getTime());
      reportPath = reportPath.replace(".html", "." + stamp + ".html");

      String readmePath = PathUtil.appendPaths(parentFolder.getAbsolutePath(), "readme.txt");
      try(FileWriter readmeWriter = new FileWriter(readmePath)) {
        readmeWriter.write("Multiple files indicate more than 1 configuration file was found in the directory.");
      } catch(IOException ignore) {}
    } else if (!parentFolder.exists() && !parentFolder.mkdirs()) {
      System.out.println("Could not create HTML report path: " + parentFolder.getAbsolutePath());
      return "Error";
    }

    // Finally, write report
    try {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath))) {
        writer.write(htmlReport);
      }
    } catch(IOException ioEx) {
      System.out.println("Could not create HTML report: " + reportPath + ". Cause: " + ioEx.getMessage());
      return "Error";
    }

    return PathUtil.getLastPathElement(reportPath);
  }

  public static void writeIndexHtml(List<AnalysisData> reportData, String indexName, String targetReportPath) {
    File report = new File(PathUtil.appendPaths(targetReportPath, indexName));
    if (report.exists() && !report.delete()) {
      System.out.println("Could not delete existing Index HTML: " + report.getAbsolutePath());
    }

    Collections.sort(reportData);
    Collections.reverse(reportData);

    StringBuilder indexContents = new StringBuilder()
                                          .append("<html>")
                                          .append("<head><style>")
                                          .append(System.lineSeparator())
                                          .append("    table, th, td { border: 1px solid black; }")
                                          .append(System.lineSeparator())
                                          .append("    tr:nth-child(even) {background: #CCC}")
                                          .append("</style></head>")
                                          .append("<body>")
                                          .append("  <table cellspacing=\"0\" cellpadding=\"2px\">")
                                          .append("<tr><th>Config Dir</th><th>Blocker</th><th>Critical</th><th>Major</th><th>Minor</th><th>Info</th><th>Link</th></tr>");

    for (AnalysisData data: reportData) {
      if (data == null) {
        continue;
      }
      indexContents.append("<tr><td>")
              .append(data.getConfigurationDirName())
              .append("</td><td>")
              .append(data.get(0))
              .append("</td><td>")
              .append(data.get(1))
              .append("</td><td>")
              .append(data.get(2))
              .append("</td><td>")
              .append(data.get(3))
              .append("</td><td>")
              .append(data.get(4))
              .append("</td><td>")
              .append("<a href=\"reports/")
              .append(data.getConfigurationDirName())
              .append("/")
              .append(data.getReportName())
              .append("\">See report</a>")
              .append("</td></tr>")
              .append(System.lineSeparator());
    }

    indexContents.append("  </table></body></html>");

    // Finally, write index
    try {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(report.getAbsolutePath()))) {
        writer.write(indexContents.toString());
      }
    } catch(IOException ioEx) {
      System.out.println("Could not create Index HTML: " + report.getAbsolutePath() + ". Cause: " + ioEx.getMessage());
    }
  }
}
