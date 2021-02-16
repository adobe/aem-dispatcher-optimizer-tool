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

import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.common.analyzer.Check;
import com.adobe.aem.dot.common.analyzer.CheckResult;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.adobe.aem.dot.httpd.core.model.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes a Rule with an HttpdConfiguration and returns any number of Violations which are found.
 */
public class HttpdRuleProcessor {
  private static final Logger logger = LoggerFactory.getLogger(HttpdRuleProcessor.class);

  public List<Violation> processRule(AnalyzerRule rule, HttpdConfiguration config) {
    List<Violation> violations = new ArrayList<>();

    HttpdCheckTargetLocator targetLocator = new HttpdCheckTargetLocator(rule.getElement());
    List<Section> sectionsToCheck = targetLocator.determineCheckTargets(config);

    // Iterate through each check that is part of this rule
    // All checks must pass in order for this configuration to be considered violation-free.
    for (Check check : rule.getChecks()) {
      for (Section sectionToCheck : sectionsToCheck) {
        CheckResult checkResult = check.performCheck(sectionToCheck);
        logger.info("RuleId=\"{}\" Result=\"{}\" CheckElement=\"{}\" CheckCondition=\"{}\" CheckValue=\"{}\"",
                rule.getId(), checkResult.isPassed() ? "Pass" : "Fail", rule.getElement(), check.getCondition(),
                check.getValueString());

        if (!checkResult.isPassed()) {
          violations.add(prepareViolation(rule, checkResult, sectionToCheck));
        }
        // otherwise: check passed!
      }
    }

    return violations;
  }

  private Violation prepareViolation(AnalyzerRule rule, CheckResult checkResult, Section section) {
    // If the check has context, include it in the Violation's context.
    String violationContext = !checkResult.getDetails().isEmpty() ? " " + checkResult.getDetails() : "";
    return new Violation(rule, violationContext, section.getConfigurationSource());
  }
}
