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

import lombok.Getter;
import lombok.Setter;

/**
 * When compacting the list of violations, an option is to count the number of times a rule is violated, instead
 * of listing each one.
 */
public class CountedRuleViolation extends Violation {
  @Getter
  @Setter
  private int ruleViolationCount = -1;  // When compacting, indicate how many violations this rule had

  public CountedRuleViolation(Violation violation) {
    super(violation.getAnalyzerRule(), violation.getContext(), violation.getConfigurationSource());
  }
}

