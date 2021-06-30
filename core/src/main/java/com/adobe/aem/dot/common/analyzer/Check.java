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

import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.BooleanCheck;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.FilterListIncludesCheck;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.FilterListStartsWithCheck;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.IntEqualsCheck;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.IntGreaterOrEqualCheck;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.IsUniqueLabelCheck;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.RuleListIncludesCheck;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.RuleListStartsWithCheck;
import com.adobe.aem.dot.dispatcher.core.model.FilterRule;
import com.adobe.aem.dot.dispatcher.core.model.GlobRule;
import com.adobe.aem.dot.httpd.core.analyzer.conditions.HasDirectiveCheck;
import com.adobe.aem.dot.httpd.core.model.Directive;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Check that will be executed against a DispatcherConfiguration. There can be multiple Checks for each Rule.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "condition"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = IntGreaterOrEqualCheck.class, name = "INT_GREATER_OR_EQUAL"),
        @JsonSubTypes.Type(value = IntEqualsCheck.class, name = "INT_EQUALS"),
        @JsonSubTypes.Type(value = BooleanCheck.class, name = "BOOLEAN_EQUALS"),
        @JsonSubTypes.Type(value = RuleListStartsWithCheck.class, name = "RULE_LIST_STARTS_WITH"),
        @JsonSubTypes.Type(value = RuleListIncludesCheck.class, name = "RULE_LIST_INCLUDES"),
        @JsonSubTypes.Type(value = FilterListStartsWithCheck.class, name = "FILTER_LIST_STARTS_WITH"),
        @JsonSubTypes.Type(value = FilterListIncludesCheck.class, name = "FILTER_LIST_INCLUDES"),
        @JsonSubTypes.Type(value = IsUniqueLabelCheck.class, name = "IS_UNIQUE_LABEL"),
        @JsonSubTypes.Type(value = HasDirectiveCheck.class, name = "HAS_DIRECTIVE")
})
@Getter
@Setter
public abstract class Check {
  private String value;
  private GlobRule ruleValue;
  private FilterRule filterValue;
  private Directive directiveValue;
  private String context;
  private boolean failIf = false;

  private static final Logger logger = LoggerFactory.getLogger(Check.class);

  public abstract CheckResult performCheck(Object configurationValue);

  @JsonIgnore
  public abstract Condition getCondition();

  /**
   * Common method to access a String representation of the value of this Check.
   * @return String representing the check value.
   */
  @JsonIgnore
  public String getValueString() {
    if (this.getValue() != null) {
      return this.getValue();
    }
    if (this.getRuleValue() != null) {
      return this.getRuleValue().toString();
    }
    if (this.getFilterValue() != null) {
      return this.getFilterValue().toString();
    }
    if (this.getDirectiveValue() != null) {
      return this.getDirectiveValue().toString();
    }
    return null;
  }

  protected boolean processFailIf(boolean check) {
    if (this.failIf) {
      return !check;
    }
    return check;
  }
}
