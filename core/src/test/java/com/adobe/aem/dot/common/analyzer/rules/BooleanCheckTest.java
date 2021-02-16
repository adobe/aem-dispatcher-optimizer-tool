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

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.common.analyzer.Check;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.analyzer.ViolationVerbosity;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.analyzer.FarmType;
import com.adobe.aem.dot.dispatcher.core.analyzer.RuleProcessor;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.BooleanCheck;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class BooleanCheckTest {

  @Test
  public void shouldEvaluateBooleanCheck() {
    DispatcherConfiguration configTrue = null;
    DispatcherConfiguration configFalse = null;

    try {
      DispatcherConfigTestHelper helper = new DispatcherConfigTestHelper();
      String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "BooleanCheckTest/true_values/" + DispatcherConstants.DISPATCHER_ANY);
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      configTrue = results.getConfiguration();
      assertEquals(0, results.getViolations(ViolationVerbosity.MINIMIZED).size());

      absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "BooleanCheckTest/false_values/" + DispatcherConstants.DISPATCHER_ANY);
      results = helper.loadDispatcherConfiguration(absPath);
      configFalse = results.getConfiguration();
      assertEquals(0, results.getViolations(ViolationVerbosity.MINIMIZED).size());
    } catch(ConfigurationException dcEx) {
      Assert.fail("Config should have loaded correctly. " + dcEx.getLocalizedMessage());
    }

    assertNotNull(configTrue);
    assertNotNull(configFalse);

    RuleProcessor ruleProcessor = new RuleProcessor();

    Check serveStaleCheck = new BooleanCheck();
    serveStaleCheck.setValue("true");

    AnalyzerRule serveStaleRule = new AnalyzerRule();
    serveStaleRule.setElement("farm.cache.serveStaleOnError");
    serveStaleRule.setDescription("Enable serveStaleOnError");
    serveStaleRule.setFarmTypeList(Collections.singletonList(FarmType.PUBLISH));
    serveStaleRule.setChecks(Collections.singletonList(serveStaleCheck));

    // ServeStaleOnError set to true
    List<Violation> violations = ruleProcessor.processRule(serveStaleRule, configTrue);
    assertEquals("Expect there to be no violations", 0, violations.size());

    // ServeStaleOnError set to false
    violations = ruleProcessor.processRule(serveStaleRule, configFalse);
    assertEquals("Expect there to be 1 violation", 1, violations.size());

    // Swap test to check the negative value.
    serveStaleCheck.setFailIf(true);
    violations = ruleProcessor.processRule(serveStaleRule, configFalse);
    assertEquals("Expect there to be 0 violation", 0, violations.size());

    // Remaining asserts are more for code coverage than code checking.
    assertFalse("Try the equals()", serveStaleCheck.equals("hello"));
    assertNotEquals("Try the equals()", "hello", serveStaleCheck.toString());
    assertEquals("Try the equals()", serveStaleCheck, serveStaleCheck);
    assertNotEquals("Try the hashcode()", "hello", serveStaleCheck.hashCode());
  }
}
