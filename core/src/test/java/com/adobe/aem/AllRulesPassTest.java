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

package com.adobe.aem;

import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.ConfigurationException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AllRulesPassTest {

  /**
   * Test the DOT with a dispatcher configuration which passes all the rules.
   */
  @Test
  public void allRulesShouldPass() throws IOException, ConfigurationException {
    // Get path to test-projects/test-project-all-rules-pass/
    String testProjectPath = AllRulesFailTest.getPathToTestModule(this.getClass(), "test-projects/test-project-all-rules-pass");

    List<Violation> violations = AllRulesFailTest.analyzeDispatcherModule(testProjectPath);
    assertEquals("Should have no violations", 0, violations.size());
  }
}
