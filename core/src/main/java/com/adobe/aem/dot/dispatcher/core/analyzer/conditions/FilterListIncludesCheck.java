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

package com.adobe.aem.dot.dispatcher.core.analyzer.conditions;

import com.adobe.aem.dot.common.analyzer.Check;
import com.adobe.aem.dot.common.analyzer.CheckResult;
import com.adobe.aem.dot.common.analyzer.Condition;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.FilterRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Check that a Filter list includes a particular Filter.
 */
public class FilterListIncludesCheck extends Check {
  private static final Logger logger = LoggerFactory.getLogger(FilterListIncludesCheck.class);

  @Override
  public Condition getCondition() {
    return Condition.FILTER_LIST_INCLUDES;
  }

  @SuppressWarnings("unchecked")
  @Override
  public CheckResult performCheck(Object configurationValue) {
    if (!(configurationValue instanceof ConfigurationValue<?>)) {
      // configurationValue must be defined and of the correct type to proceed.
      return new CheckResult(this.processFailIf(false));
    }

    try {
      ConfigurationValue<List<FilterRule>> wrappedConfigFilters = (ConfigurationValue<List<FilterRule>>) configurationValue;
      List<FilterRule> configFilters = wrappedConfigFilters.getValue();
      FilterRule checkFilter = this.getFilterValue();
      return new CheckResult(this.processFailIf(configFilters != null && !configFilters.isEmpty() &&
              configFilters.contains(checkFilter)),
              wrappedConfigFilters.getConfigurationSource());
    } catch(ClassCastException ccEx) {
      logger.error("Value could not be cast to a filter list. Value=\"{}\"", configurationValue.toString());
    }

    return new CheckResult(this.processFailIf(false));
  }
}
