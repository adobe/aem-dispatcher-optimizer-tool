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
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class to facilitate checks involving int values.
 */
public abstract class IntCheck extends Check {
  private static final Logger logger = LoggerFactory.getLogger(IntCheck.class);

  @SuppressWarnings("unchecked")
  @Override
  public CheckResult performCheck(Object configurationValue) {
    if (!(configurationValue instanceof ConfigurationValue<?>)) {
      // configurationValue must be defined and of the correct type to proceed.
      return new CheckResult(this.processFailIf(false));
    }

    ConfigurationValue<Integer> integerConfigurationValue = (ConfigurationValue<Integer>) configurationValue;

    try {
      // Compare integer values, greater than or equal
      int configValue = Integer.parseInt(integerConfigurationValue.toString());
      int checkValue = Integer.parseInt(this.getValue());

      CheckResult result = compareInts(configValue, checkValue);
      result.setConfigurationSource(integerConfigurationValue.getConfigurationSource());
      return result;
    } catch (NumberFormatException exception) {
      logger.error("Value could not be compared as an integer. Value=\"{}\"", integerConfigurationValue);
      return new CheckResult(this.processFailIf(false));
    }
  }

  /**
   * Abstract method to describe contract of comparing two int values.
   * @param configValue the value from the dispatcher configuration
   * @param checkValue the value from the optimizer <code>Check</code>
   * @return true if the configuration value is considered valid, false otherwise
   */
  public abstract CheckResult compareInts(int configValue, int checkValue);
}
