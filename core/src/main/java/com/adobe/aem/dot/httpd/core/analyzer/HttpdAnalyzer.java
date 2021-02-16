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

package com.adobe.aem.dot.httpd.core.analyzer;

import com.adobe.aem.dot.common.Configuration;
import com.adobe.aem.dot.common.analyzer.Analyzer;
import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.analyzer.ViolationVerbosity;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpdAnalyzer extends Analyzer {

  private final Logger logger = LoggerFactory.getLogger(HttpdAnalyzer.class);

  public HttpdAnalyzer(AnalyzerRuleList analyzerRuleList) throws IOException {
    super(analyzerRuleList);
  }

  /**
   * Analyze the provided DispatcherConfiguration object for violations. The set of rules applied to the config is
   * determined by enabled rules in the extensible AnalyzerRuleList field that are applicable to the dispatcher.
   * @param config - the DispatcherConfiguration object to analyze
   * @param verbosity - specifies the amount of verbosity that the violation list should contain.
   *                  FULL - give the list in its entirety
   *                  PARTIAL - collapse all violations that are exactly the same, and specify how many time they occurred
   *                  MINIMIZED - collapse as with PARTIAL, and also only report each type of violation only once
   * @return a list of violations, reduced as requested.
   */
  public List<Violation> getViolations(Configuration config, ViolationVerbosity verbosity) {
    List<Violation> allViolations = analyzeConfig(config);
    if (verbosity == ViolationVerbosity.FULL) {
      return allViolations;
    }

    List<Violation> list = reduceViolationList(allViolations, verbosity == ViolationVerbosity.MINIMIZED);
    logger.info("Compressed ({}) Apache Httpd Violation Count={}.", verbosity.toString(), list.size());
    return list;
  }

  /**
   * Analyze the provided HttpdConfiguration object for violations. The set of rules applied to the config is
   * determined by enabled rules in the extensible AnalyzerRuleList field that are applicable to Httpd.
   * @param config - the HttpdConfiguration object to analyze
   * @return a list of violations
   */
  private List<Violation> analyzeConfig(Configuration config) {
    if (!(config instanceof HttpdConfiguration)) {
     throw new IllegalArgumentException("Incorrect configuration type used to find Apache Httpd violations");
    }

    logger.trace("Begin: Analyzing Apache Httpd configuration");
    HttpdRuleProcessor ruleProcessor = new HttpdRuleProcessor();

    List<Violation> violations = new ArrayList<>();

    // Handle Apache Httpd rules
    for (AnalyzerRule httpdRule : this.getAnalyzerRuleList().getHttpdEnabledRules()) {
      List<Violation> ruleViolations = ruleProcessor.processRule(httpdRule, (HttpdConfiguration) config);
      violations.addAll(ruleViolations);
    }

    logger.info("End: Finished analyzing Apache Httpd configuration. Full Violation Count={}.", violations.size());

    return violations;
  }
}
