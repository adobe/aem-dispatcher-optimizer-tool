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

package com.adobe.aem.dot.dispatcher.core.model;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.util.FeedbackProcessor;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.FULL;
import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.MINIMIZED;
import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.PARTIAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RuleTest {
  private static DispatcherConfigTestHelper helper;
  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(FilterRule.class);
  }

  @BeforeClass
  public static void beforeClass() {
    helper = new DispatcherConfigTestHelper();
  }

  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "rule/" + DispatcherConstants.DISPATCHER_ANY);
    String anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 2 farms", 2, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      Cache cache = farm.getCache().getValue();
      assertNotNull(cache);
      assertEquals("Simple test of source", 5, farm.getCache().getLineNumber());

      // These 3 asserts are more for code coverage than code checking.
      assertFalse("Try the equals()", cache.equals("hello"));
      assertNotEquals("Try the equals()", null, cache);
      assertEquals("equals itself", cache, cache);
      assertNotEquals("Try the equals()", "hello", cache.toString());
      assertNotEquals("Try the hashcode()", "hello", cache.hashCode());
      // ====================================================================

      List<FilterRule> rules = cache.getRules().getValue();
      assertNotNull(rules);

      assertEquals("Check filter size", 4, rules.size());

      // Check Rule values
      FilterRule first = rules.get(0);
      assertEquals("Check label 1", "0000", first.getLabel());
      assertEquals("Check glob 1", "*", first.getGlob());
      assertEquals("Check type 1", RuleType.ALLOW, first.getType());
      assertTrue("correct any file",
              FilenameUtils.separatorsToSystem(anyPath + "/rules.any").endsWith(first.getLabelData().getFileName()));
      assertEquals("correct line number", 5, first.getLabelData().getLineNumber());
      assertEquals("correct included from", DispatcherConstants.DISPATCHER_ANY,
              first.getLabelData().getIncludedFrom());
      assertFalse("different class", first.equals(new FeedbackProcessor()));
      assertFalse("null", first.equals(null));

      assertNotNull("Should be a logger", first.getLogger());
      assertNotNull("Should be a class name", first.getSimpleClassName());

      FilterRule second = rules.get(1);
      assertEquals("Check label 2", "stout", second.getLabel());
      assertEquals("Check glob 2", "/en/news/'*?lang=en", second.getGlob());
      assertEquals("Check type 2", RuleType.DENY, second.getType());
      assertTrue("correct any file",
              FilenameUtils.separatorsToSystem(anyPath + "/rules.any").endsWith(second.getLabelData().getFileName()));
      assertEquals("correct line number", 5, second.getLabelData().getLineNumber());
      assertEquals("correct included from", DispatcherConstants.DISPATCHER_ANY,
              second.getLabelData().getIncludedFrom());
      assertEquals("equals itself", second, second);

      FilterRule third = rules.get(2);
      assertNull("Null type", third.getType());
      assertEquals("Equals without type", "Label=notype,Method=get", third.toString());

      FilterRule fourth = rules.get(3);
      assertTrue("Check label 3", StringUtils.isEmpty(fourth.getLabel()));
      assertEquals("Check glob 3", "*/private/*", fourth.getGlob());
      assertEquals("Check type 3", RuleType.DENY, fourth.getType());
      assertNull("null label data", fourth.getLabelData());

      assertNotEquals("hashcode", 0, fourth.hashCode());
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  // No brace after /cache
  @Test
  public void noBraceCacheCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "rule/nobrace/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    Farm farm = config.getFarms().get(0).getValue();
    assertFalse("Should not be an author farm", farm.isAuthorFarm());
    ConfigurationValue<Cache> cache = farm.getCache();
    assertNotNull(cache);
    assertNotNull(cache.getValue());
    assertNull(cache.getValue().getRules());
  }

  // No brace after /rules
  @Test
  public void noBraceRuleCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "rule/nobrace2/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    assertEquals("Violations", 4, results.getViolations(FULL).size());
    assertEquals("Violations", 3, results.getViolations(PARTIAL).size());
    assertEquals("Violations", 3, results.getViolations(MINIMIZED).size());
    Farm farm = config.getFarms().get(0).getValue();
    assertFalse("Should not be an author farm", farm.isAuthorFarm());
    ConfigurationValue<Cache> cache = farm.getCache();
    assertNotNull(cache);
    assertNotNull(cache.getValue());
    assertEquals("/tmp/dispatcher-website.stat?lang=en", cache.getValue().getStatfile().getValue());
    assertEquals("/opt/dispatcher/cache", cache.getValue().getDocroot().getValue());
    assertNotNull(cache.getValue().getRules());
    assertEquals(0, cache.getValue().getRules().getValue().size());
  }

  // No brace after the label of a rule.
  @Test
  public void noBraceRuleCheck2() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "rule/nobrace3/" + DispatcherConstants.DISPATCHER_ANY);
    helper.loadDispatcherConfiguration(absPath);

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Should be 7 log entries", 7, logsList.size());
    assertTrue(logsList.get(0).getMessage().startsWith("Each filter rule must begin with a '{' character."));
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());

    assertTrue(logsList.get(1).getMessage().startsWith("Each filter rule must begin with a '{' character."));
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(1).getLevel());

    assertTrue(logsList.get(2).getMessage().startsWith("A reserved token was used to label a FilterRule indicating a section may have been closed incorrectly.  Label=\"glob\""));
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(2).getLevel());
  }
}
