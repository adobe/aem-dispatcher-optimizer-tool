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
import com.adobe.aem.dot.dispatcher.core.analyzer.conditions.IsUniqueLabelCheck;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
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

public class IsUniqueLabelCheckTest {
  private DispatcherConfiguration configTrue = null;
  private DispatcherConfiguration configFalse = null;

  @Before
  public void setup() {
    try {
      DispatcherConfigTestHelper helper = new DispatcherConfigTestHelper();

      String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "IsUniqueLabelCheckTest/true_values/" + DispatcherConstants.DISPATCHER_ANY);
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      configTrue = results.getConfiguration();

      absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "IsUniqueLabelCheckTest/false_values/" + DispatcherConstants.DISPATCHER_ANY);
      results = helper.loadDispatcherConfiguration(absPath);
      configFalse = results.getConfiguration();

      assertNotNull(configTrue);
      assertNotNull(configFalse);
    } catch(ConfigurationException dcEx) {
      Assert.fail("Config should have loaded correctly. " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void shouldDetectDuplicateItems() {
    RuleProcessor ruleProcessor = new RuleProcessor();

    Check isUniqueLabelCheck = new IsUniqueLabelCheck();

    AnalyzerRule isUniqueRule = new AnalyzerRule();
    isUniqueRule.setElement("farm");
    isUniqueRule.setDescription("Farm labels must be unique");
    isUniqueRule.setFarmTypeList(Arrays.asList(FarmType.AUTHOR, FarmType.PUBLISH));
    isUniqueRule.setChecks(Collections.singletonList(isUniqueLabelCheck));

    List<Violation> violations = ruleProcessor.processMultiFarmRule(isUniqueRule, configTrue);

    assertEquals("Expect there to be no violations", 0, violations.size());

    isUniqueLabelCheck.setFailIf(true);
    violations = ruleProcessor.processMultiFarmRule(isUniqueRule, configTrue);
    assertEquals("Expect there to be 1 violation", 1, violations.size());
    isUniqueLabelCheck.setFailIf(false);

    violations = ruleProcessor.processMultiFarmRule(isUniqueRule, configFalse);
    assertEquals("Expect there to be 1 violation", 1, violations.size());
    assertEquals("Expect the context to match", "Configuration items labeled [skylab] failed the IS_UNIQUE_LABEL check.", violations.get(0).getContext());

    isUniqueLabelCheck.setFailIf(true);
    violations = ruleProcessor.processMultiFarmRule(isUniqueRule, configFalse);
    assertEquals("Expect there to be no violations", 0, violations.size());

    // Remaining asserts are more for code coverage than code checking.
    assertFalse("Try the equals()", isUniqueLabelCheck.equals("hello"));
    assertNotEquals("Try the equals()", "hello", isUniqueLabelCheck.toString());
    assertEquals("Try the equals()", isUniqueLabelCheck, isUniqueLabelCheck);
    assertNotEquals("Try the hashcode()", "hello", isUniqueLabelCheck.hashCode());
  }
}
