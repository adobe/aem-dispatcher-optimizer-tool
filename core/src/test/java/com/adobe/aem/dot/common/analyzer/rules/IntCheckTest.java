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
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.IntEqualsCheck;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.IntGreaterOrEqualCheck;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class IntCheckTest {
  private DispatcherConfiguration configTrue = null;
  private DispatcherConfiguration configFalse = null;
  private RuleProcessor ruleProcessor;
  private AnalyzerRule rule1;
  private Check check1;

  @Before
  public void setup() {
    // Set up rule
    this.check1 = new IntGreaterOrEqualCheck();
    this.check1.setValue("2");

    this.rule1 = new AnalyzerRule();
    this.rule1.setElement("farm.cache.statfileslevel");
    this.rule1.setFarmTypeList(Collections.singletonList(FarmType.PUBLISH));
    this.rule1.setChecks(Collections.singletonList(this.check1));

    this.ruleProcessor = new RuleProcessor();

    try {
      DispatcherConfigTestHelper helper = new DispatcherConfigTestHelper();

      String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "IntCheckTest/true_values/" + DispatcherConstants.DISPATCHER_ANY);
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      configTrue = results.getConfiguration();
      assertEquals(0, results.getViolations(ViolationVerbosity.MINIMIZED).size());

      absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "IntCheckTest/false_values/" + DispatcherConstants.DISPATCHER_ANY);
      results = helper.loadDispatcherConfiguration(absPath);
      configFalse = results.getConfiguration();
      assertEquals(0, results.getViolations(ViolationVerbosity.MINIMIZED).size());

      assertNotNull(configTrue);
      assertNotNull(configFalse);
    } catch(ConfigurationException dcEx) {
      Assert.fail("Config should have loaded correctly. " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void shouldDetectStatlevel0() {
    List<Violation> violations = this.ruleProcessor.processRule(this.rule1, configFalse);
    assertEquals("Expect there to be 1 violation", 1, violations.size());

    this.check1.setFailIf(true);
    violations = this.ruleProcessor.processRule(this.rule1, configFalse);
    assertEquals("Expect there to be 0 violations", 0, violations.size());

    // Remaining asserts are more for code coverage than code checking.
    assertFalse("Try the equals()", check1.equals("hello"));
    assertNotEquals("Try the equals()", "hello", check1.toString());
    assertEquals("Try the equals()", check1, check1);
    assertNotEquals("Try the hashcode()", "hello", check1.hashCode());
  }

  @Test
  public void shouldDetectStatlevel2() {
    List<Violation> violations = this.ruleProcessor.processRule(this.rule1, configTrue);
    assertEquals("Expect there to be no violations", 0, violations.size());

    this.check1.setFailIf(true);
    violations = this.ruleProcessor.processRule(this.rule1, configTrue);
    assertEquals("Expect there to be 1 violation", 1, violations.size());
  }

  @Test
  public void shouldDetectServeStaleOnError0() {
    Check exactIntCheck = new IntEqualsCheck();
    exactIntCheck.setValue("10");

    AnalyzerRule exactIntRule = new AnalyzerRule();
    exactIntRule.setElement("farm.cache.statfileslevel");
    exactIntRule.setDescription("Set statfileslevel to 10");
    exactIntRule.setFarmTypeList(Collections.singletonList(FarmType.PUBLISH));
    exactIntRule.setChecks(Collections.singletonList(exactIntCheck));
    List<Violation> violations;

    violations = this.ruleProcessor.processRule(exactIntRule, configTrue);

    assertEquals("Expect there to be no violations", 0, violations.size());

    violations = this.ruleProcessor.processRule(exactIntRule, configFalse);

    assertEquals("Expect there to be 1 violation", 1, violations.size());

    exactIntCheck.setFailIf(true);
    violations = this.ruleProcessor.processRule(exactIntRule, configFalse);
    assertEquals("Expect there to be 0 violations", 0, violations.size());
  }
}
