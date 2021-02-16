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

package com.adobe.aem.dot.app.service;

import com.adobe.aem.dot.app.writers.ReportWriter;
import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.analyzer.ViolationVerbosity;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleListFactory;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.reporter.CSVReporter;
import com.adobe.aem.dot.common.reporter.ViolationReporter;
import com.adobe.aem.dot.dispatcher.core.DispatcherConfigurationFactory;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.analyzer.DispatcherAnalyzer;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.httpd.core.HttpdConfigurationFactory;
import com.adobe.aem.dot.httpd.core.HttpdConstants;
import com.adobe.aem.dot.httpd.core.analyzer.HttpdAnalyzer;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service which coordinates the analysis of a dispatcher config at a particular path,
 * and prepares results in the desired format.
 */
@Component
public class ConfigurationOptimizerService {

  private final HttpdAnalyzer httpdAnalyzer;
  private final DispatcherAnalyzer dispatcherAnalyzer;
  private final ViolationReporter reporter;
  private final ReportWriter writer;

  private final String repoURL;
  private String anyDir;
  private String httpdConfDir;
  private final ViolationVerbosity verbosity;

  private final Logger logger = LoggerFactory.getLogger(ConfigurationOptimizerService.class);

  @Autowired
  public ConfigurationOptimizerService(@Value("${REPOSITORY_URL}") String repoURL,
                                       @Value("${DISPATCHER_ANY_CONFIG_PATH}") String anyDir,
                                       @Value("${OPTIMIZER_RULES_PATH}") String rulesDir,
                                       @Value("${HTTPD_CONF_CONFIG_PATH}") String httpdConfDir,
                                       @Value("${REPORT_VERBOSITY}") ViolationVerbosity verbosity,
                                       ReportWriter writer) throws IOException {
    this.repoURL = cleanPath(repoURL);
    if (StringUtils.isEmpty(this.repoURL)) {
      throw new IllegalArgumentException("The repository URL value cannot be empty or null.");
    }
    if (StringUtils.isEmpty(anyDir.trim())) {
      logger.info("The directory (DISPATCHER_ANY_CONFIG_PATH) containing the ANY file was not specified. An attempt will be made to find a \"{}\" file.",
              DispatcherConstants.DISPATCHER_ANY);
    } else {
      this.anyDir = cleanPath(anyDir);
    }

    if (StringUtils.isEmpty(httpdConfDir)) {
      logger.info("The directory (HTTPD_CONF_CONFIG_PATH) containing the CONF file was not specified. An attempt will be made to find a \"{}\" file.",
              HttpdConstants.HTTPD_CONF);
    } else {
      this.httpdConfDir = cleanPath(httpdConfDir);
    }

    this.reporter = new CSVReporter();
    this.writer = writer;

    final AnalyzerRuleList analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleList(rulesDir);
    this.httpdAnalyzer = new HttpdAnalyzer(analyzerRuleList);
    this.dispatcherAnalyzer = new DispatcherAnalyzer(analyzerRuleList);
    this.verbosity = verbosity;
  }

  /**
   * Read, analyze, and report on optimizations for the provided config.
   * @throws ConfigurationException May throw ConfigurationException
   * @throws IOException May throw IOException
   */
  public void run() throws ConfigurationException, IOException {
    if (!this.hasPreconditionsMetToRun()) {
      // Preconditions are not met. Print help text and exit
      this.printHelpText();
      return;
    }

    logger.trace("Begin DispatcherConfigService");

    DispatcherConfigurationFactory factory = new DispatcherConfigurationFactory();
    ConfigurationParseResults<DispatcherConfiguration> results = factory.parseConfiguration(this.repoURL, this.anyDir);
    List<Violation> violationCollector = new ArrayList<>(results.getViolations(this.verbosity));

    // Analyze the dispatcher configuration for violations
    DispatcherConfiguration dispatcherConfiguration = results.getConfiguration();
    if (dispatcherConfiguration != null) {
      violationCollector.addAll(this.dispatcherAnalyzer.getViolations(dispatcherConfiguration, this.verbosity));
    }

    // Analyze the Apache Httpd configuration for violations
    HttpdConfigurationFactory httpdConfigFactory = new HttpdConfigurationFactory();
    ConfigurationParseResults<HttpdConfiguration> httpdResults = httpdConfigFactory.getHttpdConfiguration(this.repoURL,
            this.httpdConfDir);
    if (httpdResults != null) {
      violationCollector.addAll(httpdResults.getViolations(this.verbosity));

      HttpdConfiguration httpdConfiguration = httpdResults.getConfiguration();
      if (httpdConfiguration != null) {
        violationCollector.addAll(this.httpdAnalyzer.getViolations(httpdConfiguration, this.verbosity));
      }
    } else {
      logger.warn("Httpd configuration failed to parse correctly.");  // Probably already logged as error.
    }

    // Generate a report in a chosen format (JSON, CSV, etc.)
    String report = this.reporter.generateViolationReport(violationCollector);
    logger.debug("Analysis result: \n{}", report);

    // Output the report
    writer.writeReport(report);

    logger.trace("End DispatcherConfigService");
  }

  public String getEffectiveConfiguration(DispatcherConfiguration config) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    StringWriter writer = new StringWriter();
    mapper.writeValue(writer, config);
    return writer.toString();
  }

  private static String cleanPath(String path) {
    String cleaned = path;
    if (StringUtils.isNotEmpty(path)) {
      cleaned = cleaned.trim();
      if (path.endsWith(File.separator)) {
        cleaned = StringUtils.removeEnd(path, File.separator);
        return cleanPath(cleaned);
      }
    }

    return cleaned;
  }

  private boolean hasPreconditionsMetToRun() {
    // this.repoURL must not be empty to proceed
    return this.repoURL != null && !this.repoURL.trim().isEmpty();
  }

  private void printHelpText() {
    logger.error("This validator operates with an externalized configuration and must be run with the following environment variables set:");
    logger.error("- REPOSITORY_URL: local path to an AEM (archetype based) project containing a dispatcher module");
    logger.error("- DISPATCHER_ANY_CONFIG_PATH: relative path from REPOSITORY_URL to the dispatcher.any config file");
    logger.error("Optionally:");
    logger.error("- HTTPD_CONF_CONFIG_PATH: relative path from REPOSITORY_URL to the httpd.conf config file");
  }
}
