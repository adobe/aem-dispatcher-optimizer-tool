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

package com.adobe.aem.dot.common.analyzer.rules;

import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.dispatcher.core.analyzer.FarmType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a list of analyzer rules.
 */
@Getter
public class AnalyzerRuleList {
  private MergeMode mergeMode = MergeMode.EXTEND;
  private List<AnalyzerRule> rules;

  private static final Logger logger = LoggerFactory.getLogger(AnalyzerRuleList.class);

  /**
   * Default Constructor.  This should only be used by the factory.
   */
  AnalyzerRuleList() {
  }

  /**
   * Add rules to the existing set of rules.
   * @param ruleList The rules to be added.
   * @param origin A short indicator of the origin of the next rules for logging clarity.  Usually file name.
   */
  void addRules(AnalyzerRuleList ruleList, String origin) {
    if (this.rules == null || ruleList.getMergeMode() == MergeMode.REPLACE) {
      this.rules = ruleList.getRules();
      mergeMode = ruleList.getMergeMode();
    } else {
      for (AnalyzerRule nextRule: ruleList.getRules()) {
        nextRule.setOrigin(origin);
        AnalyzerRule originalRuleId = findMatchingRuleById(nextRule);
        // If exactly the same id, then replace the old one with the new one.
        if (originalRuleId != null) {
          int idIndex = rules.indexOf(originalRuleId);
          rules.remove(idIndex);
          rules.add(idIndex, nextRule);
          logger.info("Replacing existing rule id=\"{}\" with rule from origin=\"{}\".", nextRule.getId(), origin);
        } else {
          AnalyzerRule close = getCloseMatch(nextRule);
          if (close != null) {
            logger.warn("Adding rule=\"{}\" from origin=\"{}\".  Possible collision with existing rule id=\"{}\".",
                    nextRule.getId(), origin, close.getId());
          }
          rules.add(nextRule);
        }
      }
    }
  }

  /**
   * Get all the rules which are currently loaded and set to `enabled`.
   * @return A list of enabled rules.
   */
  @JsonIgnore
  public List<AnalyzerRule> getEnabledRules() {
    return this.getRules().stream()
                   .filter(AnalyzerRule::isEnabled)
                   .collect(Collectors.toList());
  }

  /**
   * Get all the enabled rules which operate on a list of multiple Farms.
   * @return List of <code>AnalyzerRule</code> objects where the Check's require a List of Farms.
   */
  @JsonIgnore
  public List<AnalyzerRule> getEnabledMultiFarmRules() {
    return this.getEnabledRules()
                   .stream()
                   .filter(AnalyzerRule::isMultiFarmRule)
                   .collect(Collectors.toList());
  }

  /**
   * Get all the enabled rules which operate on a single Farm at a time.
   * @return List of <code>AnalyzerRule</code> objects where the Checks require an element of a single Farm.
   */
  @JsonIgnore
  public List<AnalyzerRule> getEnabledSingleFarmRules() {
    return this.getEnabledRules()
                   .stream()
                   .filter(AnalyzerRule::isSingleFarmRule)
                   .collect(Collectors.toList());
  }

  /**
   * Get all the enabled rules which apply to the Apache Httpd configuration.
   * @return List of <code>AnalyzerRule</code> objects where the Checks indicate an HTTPD setting.
   */
  @JsonIgnore
  public List<AnalyzerRule> getHttpdEnabledRules() {
    return this.getEnabledRules()
                   .stream()
                   .filter(AnalyzerRule::isApacheHttpdRule)
                   .collect(Collectors.toList());
  }

  /**
   * Interate of the existing rules and find any that have the same id as the received rule.
   * @param rule The rule to use to match ids.
   * @return A matching rule or null.
   */
  private AnalyzerRule findMatchingRuleById(AnalyzerRule rule) {
    return rules.stream()
            .filter(checkRule -> rule.getId().equals(checkRule.getId()))
            .findAny()
            .orElse(null);
  }

  /**
   * Find the first matching rule.  A rule match occurs when both rules have these same properties:
   * - farm type list (list must have the same values)
   * - severity
   * - enabled
   * - description
   *
   * @param comparisonRule The rule to find a match for
   * @return AnalyzerRule The matching rule, or null.
   */
  private AnalyzerRule getCloseMatch(AnalyzerRule comparisonRule) {
    AnalyzerRule match = null;
    for (AnalyzerRule nextRule: rules) {
      if (nextRule.getSeverity() == comparisonRule.getSeverity() &&
              nextRule.isEnabled() == comparisonRule.isEnabled() &&
              nextRule.getDescription().equals(comparisonRule.getDescription())) {
        boolean farmTypeMatch = true;
        for (FarmType type : nextRule.getFarmTypeList()) {
          if (!comparisonRule.getFarmTypeList().contains(type)) {
            farmTypeMatch = false;
            break;
          }
        }
        if (farmTypeMatch) {
          match = nextRule;
          break;
        }
      }
    }

    return match;
  }

  public String toString() {
    return toJsonString();
  }

  /**
   * Return the object in its JSON form.
   * @return JSON
   */
  public String toJsonString() {
    ObjectMapper mapper = new ObjectMapper();
    String asJson = "";

    try {
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      asJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      logger.error("Json processing failed.", e);
    }

    return asJson;
  }
}

