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

/**
 * Abstract class to facilitate checks involving boolean values.
 */
public class BooleanCheck extends Check {
  @Override
  public Condition getCondition() {
    return Condition.BOOLEAN_EQUALS;
  }

  @SuppressWarnings("unchecked")
  @Override
  public CheckResult performCheck(Object configurationValue) {
    // Compare boolean values, greater than or equal
    if (configurationValue instanceof ConfigurationValue<?>) {
      ConfigurationValue<Boolean> configurationBoolean = (ConfigurationValue<Boolean>) configurationValue;
      boolean configValue = Boolean.parseBoolean(configurationValue.toString());
      boolean checkValue = Boolean.parseBoolean(this.getValue());

      return new CheckResult(this.processFailIf(configValue == checkValue),
              configurationBoolean.getConfigurationSource());
    }

    // Fail this check, since the provided object is unusable
    return new CheckResult(this.processFailIf(false));
  }
}
