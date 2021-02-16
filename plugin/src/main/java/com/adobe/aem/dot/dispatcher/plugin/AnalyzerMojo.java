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

package com.adobe.aem.dot.dispatcher.plugin;

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.analyzer.ViolationVerbosity;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleListFactory;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.reporter.CSVReporter;
import com.adobe.aem.dot.common.reporter.HTMLReporter;
import com.adobe.aem.dot.dispatcher.core.DispatcherConfigurationFactory;
import com.adobe.aem.dot.dispatcher.core.analyzer.DispatcherAnalyzer;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.httpd.core.HttpdConfigurationFactory;
import com.adobe.aem.dot.httpd.core.analyzer.HttpdAnalyzer;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.PARTIAL;

/**
 * Goal which analyzes a dispatcher configuration and writes a list of violations
 * and opportunities for optimization to the console.
 */
@Mojo( name = "analyze", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class AnalyzerMojo extends AbstractMojo {
  @Parameter(readonly = true, defaultValue = "${project}")
  private MavenProject project;

  /**
   * Location of the dispatcher module. Defaults to the CWD, which works well if the
   * Dispatcher Optimizer plugin entry is added to the AEM project's dispatcher
   * module POM.
   */
  @Parameter( property = "analyze.dispatcherModuleDir", defaultValue = ".", required = true )
  private String dispatcherModuleDir;

  /**
   * Relative path to dispatcher.any from `dispatcherModuleDir` (above).
   */
  @Parameter( property = "analyze.dispatcherConfigPath", defaultValue = "src/conf.dispatcher.d" )
  private String dispatcherConfigPath;

  /**
   * Relative path to httpd.conf from `dispatcherModuleDir` (above).
   */
  @Parameter( property = "analyze.apacheHttpdConfigPath", defaultValue = "src/conf" )
  private String apacheHttpdConfigPath;

  /**
   * Path to folder holding additional rule files.  <Optional>
   */
  @Parameter( property = "analyze.optimizerRulesPath" )
  private String optimizerRulesPath;

  /**
   * Level of verbosity in the report.  <Optional>
   *   "full" - full report
   *   "partial" - report duplication once
   *   "none" - report duplication and similar rule violations only once
   */
  @Parameter( property = "analyze.reportVerbosity", defaultValue = "PARTIAL")
  private String reportVerbosity;

  private final static String REPORT_PATH = "/dispatcher-optimizer-tool";
  private final static String CSV_NAME = "/results.csv";
  private final static String HTML_NAME = "/results.html";

  /**
   * Execute the "analyze" goal of the Dispatcher Optimizer plugin.
   * @throws MojoExecutionException Thrown in the case of a significant violation.
   */
  public void execute() throws MojoExecutionException {

    String rulesFolder = null;
    if (StringUtils.isNotEmpty(this.optimizerRulesPath)) {
      File folder = FileUtils.getFile(optimizerRulesPath);
      rulesFolder = folder.getAbsolutePath().replace(".\\", "");
      getLog().debug("[Dispatcher Optimizer] Loading additional rule files from: " + rulesFolder);
    }

    // To more accurately report violations, with relative paths, convert path to base directory of the project.
    if (this.dispatcherModuleDir.equals(".")) {
      this.dispatcherModuleDir = project.getBasedir().getAbsolutePath();
    }
    getLog().info("[Dispatcher Optimizer] Analyzing Dispatcher config at path: " +
                          this.dispatcherModuleDir + File.separator + this.dispatcherConfigPath);

    ViolationVerbosity violationVerbosity = setViolationVerbosity(this.reportVerbosity.toUpperCase());

    try {
      getLog().debug("[Dispatcher Optimizer] Parsing dispatcher config...");

      DispatcherConfigurationFactory dispatcherFactory = new DispatcherConfigurationFactory();
      ConfigurationParseResults<DispatcherConfiguration> dispatcherResults = dispatcherFactory.parseConfiguration(this.dispatcherModuleDir,
              this.dispatcherConfigPath);
      DispatcherConfiguration dispatcherConfiguration = dispatcherResults.getConfiguration();

      // Collect the violations from the Dispatcher parsing/reading (i.e. not from rule violations)
      List<Violation> violationCollector = dispatcherResults.getViolations(violationVerbosity);

      getLog().debug("[Dispatcher Optimizer] Finished parsing dispatcher config!  Violations: " +
                             violationCollector.size());

      getLog().debug("[Dispatcher Optimizer] Parsing Apache Httpd config...");

      HttpdConfigurationFactory httpdConfigurationFactory = new HttpdConfigurationFactory();
      ConfigurationParseResults<HttpdConfiguration> httpdResults = httpdConfigurationFactory.getHttpdConfiguration(this.dispatcherModuleDir,
              this.apacheHttpdConfigPath);
      HttpdConfiguration httpdConfiguration = httpdResults.getConfiguration();

      getLog().debug("[Dispatcher Optimizer] Finished parsing Apache Httpd config!");

      getLog().debug("[Dispatcher Optimizer] Analyzing Dispatcher and Apache Httpd configurations for violations...");

      AnalyzerRuleList list = AnalyzerRuleListFactory.getAnalyzerRuleList(rulesFolder);

      // Analyze the dispatcher configuration against the loaded rules.
      DispatcherAnalyzer dispatcherAnalyzer = new DispatcherAnalyzer(list);
      violationCollector.addAll(dispatcherAnalyzer.getViolations(dispatcherConfiguration, violationVerbosity));

      // Analyze the Httpd configuration against the loaded rules, if it loaded.
      if (httpdConfiguration != null) {
        HttpdAnalyzer httpdAnalyzer = new HttpdAnalyzer(list);
        violationCollector.addAll(httpdAnalyzer.getViolations(httpdConfiguration, violationVerbosity));
      }

      getLog().debug("[Dispatcher Optimizer] Analysis complete!");

      getLog().info("[Dispatcher Optimizer] Violations detected: " + violationCollector.size());

      getLog().info("[Dispatcher Optimizer] Details: ");

      for (Violation violation : violationCollector) {
        // This would be the place to throw an exception to "break the build" if any blocker issues are identified
        getLog().info(violation.toString());
      }

      writeReports(violationCollector);
    }
    catch (ConfigurationException dce) {
      // Eventually, an exception caught here should be handled by "breaking the build"
      getLog().error("Dispatcher Optimizer plugin ConfigurationException:");
      getLog().error(dce);
    } catch (IOException ioException) {
      getLog().error("Dispatcher Optimizer plugin IOException:");
      getLog().error(ioException);
    }
  }

  private ViolationVerbosity setViolationVerbosity(String reportVerbosity) {
    ViolationVerbosity violationVerbosity;
    try {
      violationVerbosity = ViolationVerbosity.valueOf(reportVerbosity);
    } catch(IllegalArgumentException iaEx) {
      getLog().warn("[Dispatcher Optimizer] Invalid reportVerbosity setting: " + reportVerbosity);
      violationVerbosity = PARTIAL;
    }

    getLog().info("[Dispatcher Optimizer] Report verbosity set to: " + violationVerbosity.toString());

    return violationVerbosity;
  }

  // Write all reports, based on the violation list.
  private void writeReports(List<Violation> violations) throws IOException {
    Model model = project.getModel();
    Build build = model.getBuild();
    File targetDir = new File(build.getDirectory());

    try {
      String csvReport = new CSVReporter().generateViolationReport(violations);
      String csvReportPath = FilenameUtils.separatorsToSystem(targetDir + REPORT_PATH + CSV_NAME);
      writeReport(csvReportPath, csvReport);
    } finally {
      String htmlReport = new HTMLReporter().generateViolationReport(violations);
      String htmlReportPath = FilenameUtils.separatorsToSystem(targetDir + REPORT_PATH + HTML_NAME);
      writeReport(htmlReportPath, htmlReport);
    }
  }

  // Write a single report.
  private void writeReport(String reportPath, String report) throws IOException {
    getLog().info("Begin: Writing report to " + reportPath);

    File reportFile = new File(reportPath);
    File targetDir = reportFile.getParentFile();

    // Create directories, if needed
    if (!targetDir.exists() && !reportFile.getParentFile().mkdirs()) {
      getLog().warn(
              MessageFormat.format("Path creation failed for {0}.  Aborting creation of violation report.",
                      reportPath));
    } else {
      // Write report
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath))) {
        writer.write(report);
      }

      getLog().info("End: Wrote report to " + reportPath);
    }
  }
}
