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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleListFactory;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.dispatcher.core.analyzer.RuleProcessor;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RuleProcessorTest {
  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(RuleProcessor.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullTest() {
    RuleProcessor processor = new RuleProcessor();
    processor.processRule(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void null2Test() throws IOException {
    RuleProcessor processor = new RuleProcessor();
    AnalyzerRuleList list = AnalyzerRuleListFactory.getAnalyzerRuleList();
    assertTrue(list.getEnabledRules().size() > 2);
    processor.processRule(list.getEnabledRules().get(0), null);
  }

  @Test
  public void noFarmsTest() throws IOException {
    DispatcherConfiguration config = DispatcherConfigTestHelper.getEmptyDispatcherConfig();
    AnalyzerRuleList list = AnalyzerRuleListFactory.getAnalyzerRuleList();
    RuleProcessor processor = new RuleProcessor();
    assertTrue(list.getEnabledRules().size() > 2);
    List<Violation> rules = processor.processRule(list.getEnabledRules().get(0), config);
    assertTrue("Should not have farms.", rules.isEmpty());

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Relevant farms: \"{}\"", logsList.get(0).getMessage());
    assertEquals("Severity should be DEBUG.", Level.DEBUG, logsList.get(0).getLevel());
    assertEquals("No farms apply to this rule. Skipping rule id=\"{}\"", logsList.get(1).getMessage());
    assertEquals("Severity should be INFO.", Level.INFO, logsList.get(1).getLevel());
  }
}
