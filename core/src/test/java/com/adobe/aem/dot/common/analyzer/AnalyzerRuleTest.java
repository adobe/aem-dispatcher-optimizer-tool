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

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.model.Farm;
import com.adobe.aem.dot.dispatcher.core.model.FilterRule;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AnalyzerRuleTest {

  @SuppressWarnings("unchecked")
  @Test
  public void shouldDetermineCheckTargetBasicCases() {
    DispatcherConfigTestHelper helper = new DispatcherConfigTestHelper();
    DispatcherConfiguration config = null;
    ConfigurationParseResults<DispatcherConfiguration> dispatcherResults = null;

    try {
      String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "AnalyzerRuleTest/" + DispatcherConstants.DISPATCHER_ANY);
      dispatcherResults = helper.loadDispatcherConfiguration(absPath);
      assertNotNull(dispatcherResults);
      config = dispatcherResults.getConfiguration();
      assertEquals(0, dispatcherResults.getViolations(ViolationVerbosity.MINIMIZED).size());
    } catch(ConfigurationException dcEx) {
      Assert.fail("Config should have loaded correctly: " + dcEx.getLocalizedMessage());
    }

    assertNotNull(config);
    assertEquals(0, dispatcherResults.getViolations(ViolationVerbosity.FULL).size());
    List<ConfigurationValue<Farm>> farms = config.getFarms();
    assertNotNull(farms);
    Farm farm = farms.get(0).getValue();

    AnalyzerRule testRule1 = new AnalyzerRule();
    testRule1.setElement("farm.cache.gracePeriod");
    assertEquals(AnalyzerRule.DOC_URL_DEFAULT, testRule1.getDocumentationURL());

    // Ensure the rule can determine the check target from the given farm
    Object checkTarget = testRule1.determineCheckTarget(farm);
    int gracePeriod = Integer.parseInt(checkTarget.toString());
    assertEquals("Should extract gracePeriod value from the farm", 8, gracePeriod);

    // Change the rule's element to farm.cache.statfileslevel
    testRule1.setElement("farm.cache.statfileslevel");
    checkTarget = testRule1.determineCheckTarget(farm);
    int statFilesLevel = Integer.parseInt(checkTarget.toString());
    assertEquals("Should extract statfileslevel value from the farm", 2, statFilesLevel);

    // Change the rule's element to farm.filter
    testRule1.setElement("farm.filter");
    checkTarget = testRule1.determineCheckTarget(farm);
    ConfigurationValue<List<FilterRule>> filtersTarget = (ConfigurationValue<List<FilterRule>>) checkTarget;
    assertEquals("Should extract the list of filters from the farm", "/content/test-filter/*",
            filtersTarget.getValue().get(0).getGlob());
  }

  @Test
  public void elementCheck() {
    AnalyzerRule testRule1 = new AnalyzerRule();
    assertNull("element not set yet", testRule1.getElement());
    assertNull("element not set yet", testRule1.determineCheckTarget(null));

    // Less than one "."
    testRule1.setElement("hi");
    assertNull("no periods", testRule1.determineCheckTarget(null));
    assertFalse("not farm.", testRule1.isMultiFarmRule());
    assertFalse("not farm.", testRule1.isSingleFarmRule());

    testRule1.setElement("hi.there");
    assertNull("one period", testRule1.determineCheckTarget(null));

    testRule1.setElement("hi.there.you");
    assertNull("hi != farm", testRule1.determineCheckTarget(null));

    testRule1.setElement("farm.there.you");
    assertNull("there aint right", testRule1.determineCheckTarget(null));

    testRule1.setElement("farm.cache.harvest.hay");
    assertNull("two many for farm.cache", testRule1.determineCheckTarget(null));

    testRule1.setElement("farm.filter.harvest.hay.mound");
    assertNull("not bad, but farm is null", testRule1.determineCheckTarget(null));
  }

  @Test
  public void typeOfRuleCheck() {
    AnalyzerRule testRule1 = new AnalyzerRule();
    assertFalse("Default should be false", testRule1.isDispatcherRule());
    assertFalse("Default should be false", testRule1.isApacheHttpdRule());
  }

  @Test
  public void propertySetCheck() {
    AnalyzerRule testRule1 = new AnalyzerRule();
    testRule1.setType("propertySetCheck-Type");
    assertEquals("Type should be set", "propertySetCheck-Type", testRule1.getType());

    testRule1.setTags(Collections.singletonList("propertySetCheck-Tags"));
    assertEquals("Tags should be set", "propertySetCheck-Tags", testRule1.getTags().get(0));

    testRule1.setEffort("propertySetCheck-Effort");
    assertEquals("Effort should be set", "propertySetCheck-Effort", testRule1.getEffort());
  }

  @Test
  public void determineCheckTargetCheck() {
    Farm farm = new Farm();
    AnalyzerRule testRule1 = new AnalyzerRule();
    testRule1.setElement("farm.cache.donotthinkso");
    Object obj = testRule1.determineCheckTarget(farm);
    assertNull("donotthinkso should not have a getter", obj);

  }
}
