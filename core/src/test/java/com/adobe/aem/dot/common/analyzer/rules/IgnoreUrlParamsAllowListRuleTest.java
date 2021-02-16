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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.common.analyzer.Check;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.analyzer.ViolationVerbosity;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.util.MatchesBuilder;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.analyzer.FarmType;
import com.adobe.aem.dot.dispatcher.core.analyzer.RuleProcessor;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.RuleListIncludesCheck;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.RuleListStartsWithCheck;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.model.Rule;
import com.adobe.aem.dot.dispatcher.core.model.RuleType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class IgnoreUrlParamsAllowListRuleTest {
  private DispatcherConfiguration configTrue = null;
  private DispatcherConfiguration configFalse = null;
  private AnalyzerRule rule1;
  private Check check1;

  @Before
  public void before() {
    // Set up ignoreUrlParams rule
    this.check1 = new RuleListStartsWithCheck();
    // Expect a ALLOW of * as the first rule (which means all params get ignored)
    this.check1.setRuleValue(createRule("0001", "*", RuleType.ALLOW));

    rule1 = new AnalyzerRule();
    rule1.setElement("farm.cache.ignoreUrlParams");
    rule1.setChecks(Collections.singletonList(this.check1));
    rule1.setFarmTypeList(Collections.singletonList(FarmType.PUBLISH));
    rule1.setDescription("ignoreUrlParams should be configured in an allow list manner, by specifying a `/0001 { /glob \"*\" /type \"allow\" }` rule first then \"deny\"-ing specific known parameters");

    try {
      DispatcherConfigTestHelper helper = new DispatcherConfigTestHelper();

      String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "IgnoreUrlParamsAllowListRuleTest/true_values/" + DispatcherConstants.DISPATCHER_ANY);
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      configTrue = results.getConfiguration();
      assertEquals(0, results.getViolations(ViolationVerbosity.MINIMIZED).size());

      absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "IgnoreUrlParamsAllowListRuleTest/false_values/" + DispatcherConstants.DISPATCHER_ANY);
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
  public void shouldDetectIgnoreUrlParamsDenyList() {
    RuleProcessor ruleProcessor = new RuleProcessor();
    List<Violation> violations = ruleProcessor.processRule(rule1, configFalse);
    String firstViolationDescription = violations.get(0).getAnalyzerRule().getDescription();
    String firstViolationContext = violations.get(0).getContext();

    assertEquals("Expect there to be 1 violation", 1, violations.size());
    assertEquals("Expect single violation's validation description to match",
            "ignoreUrlParams should be configured in an allow list manner, by specifying a `/0001 { /glob \"*\" /type \"allow\" }` rule first then \"deny\"-ing specific known parameters",
            firstViolationDescription);
    assertEquals("Expect single violation context to match",
            "Farm \"skylab\" has its farm.cache.ignoreUrlParams misconfigured.",
            firstViolationContext);

    this.check1.setFailIf(true);
    violations = ruleProcessor.processRule(rule1, configFalse);
    assertEquals("Expect there to be 0 violations", 0, violations.size());
    this.check1.setFailIf(false);
    violations = ruleProcessor.processRule(rule1, configFalse);
    assertEquals("Expect there to be 1 violation", 1, violations.size());

    // Remaining asserts are more for code coverage than code checking.
    assertFalse("Try the equals()", check1.equals("hello"));
    assertNotEquals("Try the equals()", "hello", check1.toString());
    assertEquals("Try the equals()", check1, check1);
    assertNotEquals("Try the hashcode()", "hello", check1.hashCode());
  }

  @Test
  public void shouldDetectEmptyIgnoreUrlParamsList() {
    RuleProcessor ruleProcessor = new RuleProcessor();

    List<Violation> violations = ruleProcessor.processRule(rule1, configFalse);
    String firstViolationDescription = violations.get(0).getAnalyzerRule().getDescription();
    String firstViolationContext = violations.get(0).getContext();

    assertEquals("Expect there to be 1 violation", 1, violations.size());
    assertEquals("Expect single violation's validation description to match", "ignoreUrlParams should be configured in an allow list manner, by specifying a `/0001 { /glob \"*\" /type \"allow\" }` rule first then \"deny\"-ing specific known parameters", firstViolationDescription);
    assertEquals("Expect single violation context to match",
            "Farm \"skylab\" has its farm.cache.ignoreUrlParams misconfigured.", firstViolationContext);

    this.check1.setFailIf(true);
    violations = ruleProcessor.processRule(rule1, configFalse);
    assertEquals("Expect there to be 0 violations", 0, violations.size());
  }

  @Test
  public void shouldDetectIgnoreUrlParamsAllowList() {
    RuleProcessor ruleProcessor = new RuleProcessor();
    List<Violation> violations = ruleProcessor.processRule(rule1, configTrue);

    assertEquals("Expect there to be no violations", 0, violations.size());

    this.check1.setFailIf(true);
    violations = ruleProcessor.processRule(rule1, configTrue);
    assertEquals("Expect there to be 1 violation", 1, violations.size());
    this.check1.setFailIf(false);

    // Set up regex rules - looking for any DENY value in a rule list.
    Check checkIncludes = new RuleListIncludesCheck();
    // Expect a ALLOW of * as the first rule (which means all params get ignored)
    checkIncludes.setRuleValue(createRule(null, "regex(.*)", RuleType.DENY));

    rule1.setChecks(Collections.singletonList(checkIncludes));
    rule1.setDescription("ignoreUrlParams should have some DENY values.");

    violations = ruleProcessor.processRule(rule1, configTrue);
    assertEquals("Expect there be no violations", 0, violations.size());

    checkIncludes.setFailIf(true);
    violations = ruleProcessor.processRule(rule1, configTrue);
    assertEquals("Expect there be 1 violation", 1, violations.size());
  }

  @Test
  public void shouldDetectLackOfAnyDenyUrlParamsAllowList() {
    RuleProcessor ruleProcessor = new RuleProcessor();
    // Set up regex rules - looking for any DENY value in a rule list.
    Check checkIncludes = new RuleListIncludesCheck();
    // Expect a ALLOW of * as the first rule (which means all params get ignored)
    checkIncludes.setRuleValue(createRule(null, "regex(.*)", RuleType.DENY));

    rule1.setChecks(Collections.singletonList(checkIncludes));
    rule1.setDescription("ignoreUrlParams should have some DENY values.");

    List<Violation> violations = ruleProcessor.processRule(rule1, configFalse);
    String firstViolationDescription = violations.get(0).getAnalyzerRule().getDescription();
    String firstViolationContext = violations.get(0).getContext();

    assertEquals("Expect there be 1 violations", 1, violations.size());
    assertEquals("Expect single violation's validation description to match",
            "ignoreUrlParams should have some DENY values.", firstViolationDescription);
    assertEquals("Expect single violation context to match",
            "Farm \"skylab\" has its farm.cache.ignoreUrlParams misconfigured.", firstViolationContext);

    violations = ruleProcessor.processRule(rule1, configTrue);
    assertEquals("Expect there be no violations", 0, violations.size());

    checkIncludes.setFailIf(true);
    violations = ruleProcessor.processRule(rule1, configTrue);
    assertEquals("Expect there be 1 violation", 1, violations.size());
  }

  @Test
  public void shouldDetectBadRegex() {
    RuleProcessor ruleProcessor = new RuleProcessor();

    // Set up basic rule.
    Check checkIncludes = new RuleListIncludesCheck();
    rule1.setDescription("ignoreUrlParams should have valid regex");

    // Get Logback Logger: create and start a ListAppender
    ListAppender<ILoggingEvent>  listAppender = AssertHelper.getLogAppender(MatchesBuilder.class);

    // Process rule with unclosed regex
    checkIncludes.setRuleValue(createRule(null, "regex(.*", RuleType.DENY));
    rule1.setChecks(Collections.singletonList(checkIncludes));
    List<Violation> violations = ruleProcessor.processRule(rule1, configTrue);
    assertEquals("Expect there be 1 violations", 1, violations.size());

    checkIncludes.setFailIf(true);
    violations = ruleProcessor.processRule(rule1, configTrue);
    assertEquals("Expect there be 0 violation", 0, violations.size());

    // Process rule with unclosed regex
    checkIncludes.setFailIf(false);
    checkIncludes.setRuleValue(createRule(null, "regex(*)", RuleType.DENY));
    rule1.setChecks(Collections.singletonList(checkIncludes));
    violations = ruleProcessor.processRule(rule1, configFalse);
    assertEquals("Expect there be 1 violations", 1, violations.size());

    List<ILoggingEvent> logsList = listAppender.list;

    // Check that the 'unclosed' regex was logged - "regex(.*" - should be the first entry in the logs.
    assertEquals("Invalid regex expression.  Error: regex() not closed correctly.  Expression=\"{}\"",
            logsList.get(0).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());

    // Check that the incorrect regex was logged - "regex(*)" - should be the second entry in the logs.
    assertEquals("Invalid regex expression.  Error: regex() not closed correctly.  Expression=\"{}\"", logsList.get(2).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(2).getLevel());
  }

  private Rule createRule(String label, String glob, RuleType type) {
    Rule rule = new Rule();
    rule.setLabel(new ConfigurationValue<>(label, "IgnoreUrlParamsAllowListRuleTest.any", 1));
    rule.setGlob(new ConfigurationValue<>(glob, "IgnoreUrlParamsAllowListRuleTest.any", 2));
    rule.setType(new ConfigurationValue<>(type, "IgnoreUrlParamsAllowListRuleTest.any", 3));
    return rule;
  }
}
