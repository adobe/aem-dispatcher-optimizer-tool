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

package com.adobe.aem.dot.common.parser;

import com.adobe.aem.dot.common.Configuration;
import com.adobe.aem.dot.common.analyzer.Analyzer;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.analyzer.ViolationVerbosity;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConfigurationParseResults<C extends Configuration> {
  @Getter
  private final C configuration;

  private final List<Violation> violations;

  private final Logger logger = LoggerFactory.getLogger(ConfigurationParseResults.class);

  public ConfigurationParseResults(C configuration, List<Violation> violations) {
    this.configuration = configuration;
    this.violations = violations;
  }

  public List<Violation> getViolations(ViolationVerbosity verbosity) {
    if (verbosity == ViolationVerbosity.FULL) {
      return this.violations;
    }

    List<Violation> list = Analyzer.reduceViolationList(this.violations,
            verbosity == ViolationVerbosity.MINIMIZED);
    logger.info("Compressed ({}) Dispatcher Configuration parsing Violation Count={}.", verbosity.toString(), list.size());
    return list;
  }
}
