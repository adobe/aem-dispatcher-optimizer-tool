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

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a list of analyzer rules.
 */
public class AnalyzerRuleListFactory {
  /**
   * Get the list of default Analyzer Rules
   * @throws IOException Can throw exception if reading files present a problem.
   */
  public static AnalyzerRuleList getAnalyzerRuleList() throws IOException {
    JSONRuleReader jsonRuleReader = new JSONRuleReader();
    return jsonRuleReader.readInternalRules();
  }

  /**
   * Get the list of default Analyzer Rules combined with any additional rules defined in the `rulesDirectory`.
   * @param rulesDirectory A folder with addition rules to read.
   * @throws IOException Can throw exception if reading files present a problem.
   */
  public static AnalyzerRuleList getAnalyzerRuleList(final String rulesDirectory) throws IOException {
    JSONRuleReader jsonRuleReader = new JSONRuleReader();
    return jsonRuleReader.readInternalRulesAndRulesFromFiles(rulesDirectory);
  }

  /**
   * Get the list of default Analyzer Rules combined with any additional rules defined in the provided InputStream.
   * @param externalRules An input stream with additional rules to read.
   * @throws IOException if IO issues are encountered.
   */
  public static AnalyzerRuleList getAnalyzerRuleListFromInputStream(final InputStream externalRules) throws IOException {
    JSONRuleReader jsonRuleReader = new JSONRuleReader();
    return jsonRuleReader.readInternalRulesAndRulesFromInputStream(externalRules);
  }
}

