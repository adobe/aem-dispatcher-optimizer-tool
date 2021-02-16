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

import com.adobe.aem.dot.common.ConfigurationSource;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of processing a single <code>Check</code>.
 */
@Getter
@Setter
public class CheckResult {
  private boolean passed;
  private List<String> details;
  private ConfigurationSource configurationSource;

  public CheckResult(boolean passed) {
    this.passed = passed;
    this.details = new ArrayList<>();
    this.configurationSource = null;
  }

  public CheckResult(boolean passed, ConfigurationSource configurationSource) {
    this.passed = passed;
    this.details = new ArrayList<>();
    this.configurationSource = configurationSource;
  }

  public CheckResult(boolean passed, List<String> details, ConfigurationSource configurationSource) {
    this.passed = passed;
    this.details = details;
    this.configurationSource = configurationSource;
  }

  public static CheckResult failWithoutContext() {
    return new CheckResult(false, new ConfigurationSource("<configuration not found>", 0));
  }
}
