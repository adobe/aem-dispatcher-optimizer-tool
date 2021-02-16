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

import com.adobe.aem.dot.common.analyzer.CheckResult;
import com.adobe.aem.dot.common.analyzer.Condition;

/**
 * Check that an int value is equal to the value specified.
 */
public class IntEqualsCheck extends IntCheck {
  @Override
  public Condition getCondition() {
    return Condition.INT_EQUALS;
  }

  /**
   * Compare to see if the config value is == the check value.
   * @param configValue the value from the dispatcher configuration
   * @param checkValue the value from the optimizer <code>Check</code>
   * @return true if the configuration value is considered valid, false otherwise
   */
  public CheckResult compareInts(int configValue, int checkValue) {
    return new CheckResult(this.processFailIf(configValue == checkValue));
  }
}
