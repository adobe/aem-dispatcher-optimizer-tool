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

package com.adobe.aem.dot.dispatcher.core.analyzer;

import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.common.analyzer.Check;
import com.adobe.aem.dot.common.analyzer.CheckResult;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.model.Farm;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Processes a Rule with a DispatcherConfiguration and returns any number of Violations which are found.
 */
public class RuleProcessor {
  private static final Logger logger = LoggerFactory.getLogger(RuleProcessor.class);

  public List<Violation> processRule(AnalyzerRule rule, DispatcherConfiguration config) {
    List<Violation> violations = new ArrayList<>();

    // Only check farms that are relevant for this particular rule
    List<ConfigurationValue<Farm>> relevantFarms = getRelevantFarms(rule, config);
    String relevantFarmNames = relevantFarms.stream()
            .map(farm -> farm.getValue().getLabel())
            .collect(Collectors.joining(", "));
    logger.debug("Relevant farms: \"{}\"", relevantFarmNames);

    // Are there any farms to check?
    if (StringUtils.isEmpty(relevantFarmNames)) {
      logger.info("No farms apply to this rule. Skipping rule id=\"{}\"", rule.getId());
      // Return empty violations list
      return Collections.emptyList();
    }

    // Iterate through each farm
    for (ConfigurationValue<Farm> farmConfigValue : relevantFarms) {
      Farm farm = farmConfigValue.getValue();

      // Iterate through each check that is part of this rule
      // All checks must pass in order for this configuration to be considered violation-free.
      for (Check check : rule.getChecks()) {
        // This object is the target of our configuration check
        Object checkTarget = rule.determineCheckTarget(farm);

        String target = "";
        if (checkTarget instanceof ConfigurationValue<?>) {
          ConfigurationSource configurationSource = ((ConfigurationValue<?>) checkTarget).getConfigurationSource();
          target = PathUtil.getLastPathElement(configurationSource.getFileName()) + ":" +
                           configurationSource.getLineNumber();
        }

        // Perform the check
        CheckResult checkResult = check.performCheck(checkTarget);
        logger.info("RuleId=\"{}\" Result=\"{}\" CheckElement=\"{}\" CheckCondition=\"{}\" CheckValue=\"{}\" File=\"{}\"",
                rule.getId(), checkResult.isPassed() ? "Pass" : "Fail", rule.getElement(), check.getCondition(),
                check.getValueString(), target);

        if (!checkResult.isPassed()) {
          if (checkResult.getConfigurationSource() == null) {
            // This means that the configuration value was not found in the config.
            // Fall back to the Farm's configurationSource.
            checkResult.setConfigurationSource(farmConfigValue.getConfigurationSource());
          }
          violations.add(prepareViolation(rule, check, checkResult, farm));

        }
        // otherwise: check passed!
      }
    }

    return violations;
  }

  /**
   * Process a rule that involves multiple farms.
   * @param rule - AnalyzerRule which involves more than 1 farm
   * @param config - the provided dispatcher configuration to check
   * @return a List of Violations, if and only if any are detected in the provided configuration
   */
  public List<Violation> processMultiFarmRule(AnalyzerRule rule, DispatcherConfiguration config) {
    List<ConfigurationValue<Farm>> relevantFarms = getRelevantFarms(rule, config);
    List<Violation> violations = new ArrayList<>();

    for (Check check : rule.getChecks()) {
      // Pass all relevant farms to checks in this type of Rule
      CheckResult checkResult = check.performCheck(relevantFarms);
      if (!checkResult.isPassed()) {
        String context = "Configuration items labeled " + checkResult.getDetails() + " failed the " + check.getCondition() + " check.";
        violations.add(new Violation(rule, context, checkResult.getConfigurationSource()));
      }
      // otherwise: check passed!
    }

    return violations;
  }

  private List<ConfigurationValue<Farm>> getRelevantFarms(AnalyzerRule rule, DispatcherConfiguration config) {
    if (rule == null || config == null) {
      throw new IllegalArgumentException("The rule and config parameters cannot be null.");
    }
    return config.getFarms()
            .stream()
            .filter(farm -> rule.getFarmTypeList() != null &&
                    ((farm.getValue().isPublishFarm() && rule.getFarmTypeList().contains(FarmType.PUBLISH)) ||
                    (farm.getValue().isAuthorFarm() && rule.getFarmTypeList().contains(FarmType.AUTHOR))))
            .collect(Collectors.toList());
  }

  private Violation prepareViolation(AnalyzerRule rule, Check check, CheckResult checkResult, Farm farm) {
    // If the check has context, include it in the Violation's context.
    String checkContext = check.getContext() != null ? " " + check.getContext() : "";
    String checkResultDetails = !checkResult.getDetails().isEmpty() ? " " + checkResult.getDetails() : "";
    String violationContext = "Farm \"" + farm.getLabel() + "\" has its " + rule.getElement() + " misconfigured." +
                                      checkContext + checkResultDetails;
    return new Violation(rule, violationContext, checkResult.getConfigurationSource());
  }
}
