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
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.analyzer.FarmType;
import com.adobe.aem.dot.dispatcher.core.analyzer.RuleProcessor;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.FilterListIncludesCheck;
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.FilterListStartsWithCheck;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.model.FilterRule;
import com.adobe.aem.dot.dispatcher.core.model.RuleType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class AMSFiltersRuleTest {
  private DispatcherConfiguration configTrue = null;
  private DispatcherConfiguration configFalse = null;

  private AnalyzerRule amsFiltersRule;
  private RuleProcessor ruleProcessor;
  private FilterRule denyAllUrls;
  private FilterRule anyAllowWithMethodCondition;
  private Check check1;
  private Check check2;

  @Before
  public void before() {
    // Set up rule
    // 1st check
    this.check1 = new FilterListStartsWithCheck();
    // Expect a DENY of /url "*" as the first filter
    denyAllUrls = new FilterRule();
    denyAllUrls.setType(new ConfigurationValue<>(RuleType.DENY, "AMSFiltersRuleTest.any", 1));
    denyAllUrls.setUrl(new ConfigurationValue<>("*", "AMSFiltersRuleTest.any", 2));
    this.check1.setFilterValue(denyAllUrls);

    // 2nd check
    this.check2 = new FilterListIncludesCheck();
    // Expect a DENY of a number of selectors & extensions next
    FilterRule denyContentGrabbing = new FilterRule();
    denyContentGrabbing.setType(new ConfigurationValue<>(RuleType.DENY, "AMSFiltersRuleTest.any", 3));
    denyContentGrabbing.setExtension(new ConfigurationValue<>("(json|xml|html|feed)", "AMSFiltersRuleTest.any", 4));
    denyContentGrabbing.setSelectors(new ConfigurationValue<>("(feed|rss|pages|languages|blueprint|infinity|tidy|sysview|docview|query|[0-9-]+|jcr:content)", "AMSFiltersRuleTest.any", 5));
    this.check2.setFilterValue(denyContentGrabbing);

    amsFiltersRule = new AnalyzerRule();
    amsFiltersRule.setElement("farm.filter");
    amsFiltersRule.setChecks(Arrays.asList(this.check1, this.check2));
    amsFiltersRule.setFarmTypeList(Collections.singletonList(FarmType.PUBLISH));
    amsFiltersRule.setDescription("The default deny rules from the AMS-style dispatcher module of the AEM project Maven archetype should be left in-place and extended as needed.");

    /*
     * Set up rule with check - allow filters with some method
     * {
     *   "condition" : "FILTER_LIST_INCLUDES",
     *   "filterValue" : {
     *     "type" : "ALLOW",
     *     "method" : "regex(.*)"
     *   }
     * }
     */
    anyAllowWithMethodCondition = new FilterRule();
    anyAllowWithMethodCondition.setType(new ConfigurationValue<>(RuleType.ALLOW, "AMSFiltersRuleTest.any", 6));
    anyAllowWithMethodCondition.setMethod(new ConfigurationValue<>("regex(.*)", "AMSFiltersRuleTest.any", 7));

    ruleProcessor = new RuleProcessor();

    // Remaining asserts are more for code coverage than code checking.
    assertFalse("Try the equals()", check1.equals("hello"));
    assertNotEquals("Try the equals()", "hello", check1.toString());
    assertEquals("Try the equals()", check1, check1);
    assertNotEquals("Try the hashcode()", "hello", check1.hashCode());

    try {
      DispatcherConfigTestHelper helper = new DispatcherConfigTestHelper();

      String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "AMSFiltersRuleTest/true_values/" + DispatcherConstants.DISPATCHER_ANY);
      ConfigurationParseResults<DispatcherConfiguration> dispResults = helper.loadDispatcherConfiguration(absPath);
      configTrue = dispResults.getConfiguration();

      absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "AMSFiltersRuleTest/false_values/" + DispatcherConstants.DISPATCHER_ANY);
      dispResults = helper.loadDispatcherConfiguration(absPath);
      configFalse = dispResults.getConfiguration();

      assertNotNull(configTrue);
      assertNotNull(configFalse);
    } catch(ConfigurationException dcEx) {
      Assert.fail("Config should have loaded correctly. " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void shouldDetectIncorrectlyOrderedAMSRules() {
    // Set up Filters in a mixed up order
    List<Violation> violations = ruleProcessor.processRule(amsFiltersRule, configFalse);

    assertEquals("Should be 1 violation", 1, violations.size());

    String firstViolationDescription = violations.get(0).getAnalyzerRule().getDescription();

    assertEquals("The default deny rules from the AMS-style dispatcher module of the AEM project Maven archetype should be left in-place and extended as needed.", firstViolationDescription);

    // Now, put the Filters in the correct order
    violations = ruleProcessor.processRule(amsFiltersRule, configTrue);
    assertEquals("Should be no violations", 0,  violations.size());

    // Swap test to check the negative value.
    this.check1.setFailIf(true);
    violations = ruleProcessor.processRule(amsFiltersRule, configTrue);
    assertEquals("Should be 1 violation", 1,  violations.size());
  }

  @Test
  public void shouldDetectLackOfMethodInEmptyConfig() {
    DispatcherConfiguration config = DispatcherConfigTestHelper.getEmptyDispatcherConfig();

    Check anyMethod = new FilterListIncludesCheck();
    anyMethod.setFilterValue(anyAllowWithMethodCondition);

    // Replace checks with the one just created.
    amsFiltersRule.setChecks(Collections.singletonList(anyMethod));

    // Rule rules on empty config - should fail.
    List<Violation> violations = ruleProcessor.processRule(amsFiltersRule, config);
    // No violations since no farms would be found.
    assertEquals("Should be 0 violations", 0, violations.size());

    // Swap test to check the negative value.
    anyMethod.setFailIf(true);
    violations = ruleProcessor.processRule(amsFiltersRule, config);
    assertEquals("Should still be no violations", 0,  violations.size());
  }

  @Test
  public void shouldDetectLackOfMethodInConfig() {
    Check anyMethod = new FilterListIncludesCheck();
    anyMethod.setFilterValue(anyAllowWithMethodCondition);

    // Replace checks with the one just created.
    amsFiltersRule.setChecks(Collections.singletonList(anyMethod));
    // Config rule with selector and extension - should fail (no method).
    List<Violation> violations = ruleProcessor.processRule(amsFiltersRule, configFalse);
    assertEquals("Should have 1 violation of checks.", 1, violations.size());

    // Filter has method - should pass.
    violations = ruleProcessor.processRule(amsFiltersRule, configTrue);
    assertEquals("Should have no violations.", 0, violations.size());

    // Set rule to require an extension - will fail.
    anyAllowWithMethodCondition.setExtension(new ConfigurationValue<>("regex(.*)   ",
            "AMSFiltersRuleTest.any", 16)); // Also ensure the value is trimmed.
    anyMethod.setFilterValue(anyAllowWithMethodCondition);
    amsFiltersRule.setChecks(Collections.singletonList(anyMethod));
    violations = ruleProcessor.processRule(amsFiltersRule, configFalse);
    assertEquals("Should have one violations.", 1, violations.size());
  }

  @Test
  public void shouldDetectEmptyFilters() {
    DispatcherConfiguration config = DispatcherConfigTestHelper.getEmptyDispatcherConfig();

    List<Violation> violations = ruleProcessor.processRule(amsFiltersRule, config);

    // No violations since no farms would be found.
    assertEquals("Should be 0 violations", 0, violations.size());
  }
}
