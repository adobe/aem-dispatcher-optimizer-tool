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

package com.adobe.aem.dot.common.analyzer;

import com.adobe.aem.dot.common.Configuration;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Analyze a DispatcherConfiguration for violations.
 */
public abstract class Analyzer {

  @Getter
  private AnalyzerRuleList analyzerRuleList;

  private static final Logger logger = LoggerFactory.getLogger(Analyzer.class);

  public Analyzer(AnalyzerRuleList analyzerRuleList) {
    this.analyzerRuleList = analyzerRuleList;
  }

  public abstract List<Violation> getViolations(Configuration config, ViolationVerbosity verbosity);

  /**
   * Take a full list of violations and remove the exact duplicates (violations included from different files). If
   * requested reduce each rule violation to a single instance, keeping track of how many times that rule was violated.
   * @param violations List of violations to reduce.
   * @param reportEachRuleOnlyOnce Whether each rule should only be reported once (true), or if each violation
   *                               reported individually.
   * @return A reduced list of violations including count.
   */
  public static List<Violation> reduceViolationList(List<Violation> violations, boolean reportEachRuleOnlyOnce) {
    Set<Violation> violationSet = new TreeSet<>(violations);  // Sort and make unique...
    logger.info("Unique Violation Count={}.", violationSet.size());
    List<Violation> countedViolations = new ArrayList<>();

    // If the request is to report identical rule violations occurring at difference source locations, then return
    // the list as it is now.
    if (!reportEachRuleOnlyOnce) {
      return violationSet.stream()
                     .map(CountedRuleViolation::new)
                     .collect(Collectors.toList());
    }

    // Reduce the list to only report each rule violation once, with number of times it was violated.
    Iterator<Violation> it = violationSet.iterator();
    Map<String, Integer> violationCount = new HashMap<>();
    while (it.hasNext()) {
      Violation nextViolation = it.next();
      AnalyzerRule nextRule = nextViolation.getAnalyzerRule();
      if (violationCount.containsKey(nextRule.getId())) {
        // Previously encountered rule violation - just increment count.
        violationCount.put(nextRule.getId(), violationCount.get(nextRule.getId()) + 1);
      } else {
        // New Rule Violation - initialize count and record it.
        violationCount.put(nextRule.getId(), 1);
        countedViolations.add(new CountedRuleViolation(nextViolation));
      }
    }

    // Set the count for each violation.
    for (Violation violation: countedViolations) {
      CountedRuleViolation crv = (CountedRuleViolation) violation;
      crv.setRuleViolationCount(violationCount.get(violation.getAnalyzerRule().getId()));
    }

    return countedViolations;
  }
}
