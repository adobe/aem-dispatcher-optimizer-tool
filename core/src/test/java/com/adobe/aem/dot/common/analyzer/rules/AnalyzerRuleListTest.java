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
import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.common.analyzer.Check;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.util.GoUrlUtil;
import com.adobe.aem.dot.dispatcher.core.analyzer.FarmType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AnalyzerRuleListTest {
  private JSONRuleReader jsonRuleReader;
  private AnalyzerRuleList testRuleList;
  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void before() throws IOException {
    jsonRuleReader = new JSONRuleReader();
    testRuleList = AnalyzerRuleListFactory.getAnalyzerRuleList();

    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(AnalyzerRuleList.class);
  }

  @Test
  public void addListCases() {
    AnalyzerRuleList internalRuleList = null;
    AnalyzerRuleList internalRuleList2 = null;
    try {
      internalRuleList = jsonRuleReader.readInternalRules();
      internalRuleList2 = jsonRuleReader.readInternalRules();
    } catch(IOException ioEx) {
      fail("Should not have been an exception: " + ioEx.getLocalizedMessage());
    }

    // Set up first rule to be "close" - different rule id, shared properties, shared farm type.
    List<AnalyzerRule> list = internalRuleList2.getRules();
    List<FarmType> farmTypes = new ArrayList<>();
    farmTypes.add(FarmType.AUTHOR);
    farmTypes.add(FarmType.PUBLISH);
    list.get(0).setId(list.get(0).getId() + "AnalyzerRuleListTest");
    list.get(0).setFarmTypeList(farmTypes);

    // Set up second rule to be different but not "close" - no shared farm type.
    List<FarmType> farmType2 = new ArrayList<>();
    farmType2.add(FarmType.AUTHOR);
    list.get(1).setId(list.get(1).getId() + "AnalyzerRuleListTest2");
    list.get(1).setFarmTypeList(farmType2);

    assertTrue("Empty should include mergeMode", testRuleList.toString().contains("\"mergeMode\" : \"EXTEND\""));
    testRuleList.addRules(internalRuleList, "AnalyzerRuleListTest");
    // Add same (2 rules slightly modified) list - should get many replacements.
    testRuleList.addRules(internalRuleList2, "AnalyzerRuleListTest3");

    assertEquals("Should be two more.", 2 + list.size(), testRuleList.getRules().size());

    List<ILoggingEvent> logsList = listAppender.list;
    assertTrue(logsList.size() >= 17);

    // Find all WARN'ings.
    List<ILoggingEvent> warnings = logsList.stream()
                                           .filter(p -> p.getLevel() == Level.WARN)
                                           .collect(Collectors.toList());
    assertEquals(1, warnings.size());
    assertTrue(warnings.get(0).getMessage().contains("Possible collision with existing rule id"));

    // Test the core rule's documentation URL.
    for (AnalyzerRule rule: internalRuleList.getRules()) {
      String docURL = rule.getDocumentationURL();
      // Quick Format...
      String expected = String.format(GoUrlUtil.GO_URL_TEMPLATE, StringUtils.substringAfter(rule.getId(), ":"));
      assertEquals(expected.toLowerCase(), docURL);
    }
  }

  @Test
  public void shouldReadInternalRules() {
    assertTrue("Should have at least 9 core rules available", testRuleList.getEnabledRules().size() >= 9);
    List<Check> checks = testRuleList.getEnabledRules().get(0).getChecks();
    assertTrue("First rules should have 1 check available", checks.size() > 0);
  }

  @Test
  public void shouldContainFarmSpecificRules() {
    List<AnalyzerRule> rules = testRuleList.getEnabledMultiFarmRules();
    assertNotNull("Multi farm rule list should not be null", rules);
    assertTrue("Multi farm rule list should have at least 1 rule.", rules.size() > 0);

    rules = testRuleList.getEnabledSingleFarmRules();
    assertNotNull("Single farm rule list should not be null", rules);
    assertTrue("Single farm rule list should have at least 7 rules", rules.size() > 6);
  }
}
