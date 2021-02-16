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

public class FilterTest {
  private static DispatcherConfigTestHelper helper;
  private ListAppender<ILoggingEvent> listAppender;

  @BeforeClass
  public static void beforeClass() {
    helper = new DispatcherConfigTestHelper();
  }

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(Filter.class);
  }


  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "filter/" + DispatcherConstants.DISPATCHER_ANY);
    String anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertEquals("Violations", 5, results.getViolations(FULL).size());
      assertEquals("Violations", 4, results.getViolations(PARTIAL).size());
      assertEquals("Violations", 2, results.getViolations(MINIMIZED).size());
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      List<Filter> filters = farm.getFilter().getValue();
      assertNotNull(filters);

      assertEquals("Check filter size", 7, filters.size());

      // Check Filter values
      Filter first = filters.get(0);
      assertEquals("Check label 1", "0001", first.getLabel());
      assertNull("Url 1 is null", first.getUrl());
      assertEquals("Check glob 1", "*", first.getGlob());
      assertEquals("Check type 1", RuleType.DENY, first.getType());
      assertTrue("FileName 1",
              FilenameUtils.separatorsToSystem(anyPath + "/filter.any").endsWith(first.getLabelData().getFileName()));
      assertEquals("FileName 1", 2, first.getLabelData().getLineNumber());
      assertEquals("FileName 1", "secondary.any", first.getLabelData().getIncludedFrom());

      assertNotNull("Should be a logger", first.getLogger());
      assertNotNull("Should be a class name", first.getSimpleClassName());
      assertNotEquals("Try the equals()", null, first);
      assertEquals("Try the equals()", first, first);
      assertNotEquals("Try the equals()", new FeedbackProcessor(), first);  // Equals on different class.

      Filter second = filters.get(1);
      assertEquals("Check label 2", "0002", second.getLabel());
      assertNull("Glob 2 is null", second.getGlob());
      assertEquals("Check glob 2", "/libs/cq/workflow/content/console*", second.getUrl());
      assertEquals("Check type 2", RuleType.ALLOW, second.getType());

      Filter third = filters.get(2);
      assertEquals("Check label 3", "hi", third.getLabel());
      assertNull("Glob 3 is null", third.getGlob());
      assertEquals("Check glob 3", "*.asp", third.getUrl());
      assertEquals("Check type 3", RuleType.DENY, third.getType());
      assertEquals("Check path 3", "a_path", third.getPath());

      Filter fourth = filters.get(3);
      assertEquals("Check label 4", "there", fourth.getLabel());
      assertNull("Glob 4 is null", fourth.getGlob());
      assertEquals("Check glob 4", "/content/[.]*.form.html", fourth.getUrl());
      assertEquals("Check type 4", RuleType.ALLOW, fourth.getType());
      assertEquals("Check extension 4", "(json|xml)", fourth.getExtension());
      assertEquals("Check method 4", "POST", fourth.getMethod());
      assertNull("Check query 4", fourth.getQuery());
      assertNull("Check suffix 4", fourth.getSuffix());
      assertNull("Check selectors 4", fourth.getSelectors());

      Filter fifth = filters.get(4);
      assertEquals("Check label 5", "tester", fifth.getLabel());
      assertNull("Glob 5 is null", fifth.getGlob());
      assertEquals("Check glob 5", "/huh", fifth.getUrl());
      assertEquals("Check type 5", RuleType.DENY, fifth.getType());
      assertEquals("Check query 5", "this_is_a_weird_query", fifth.getQuery());
      assertEquals("Check selectors 5", "(feed|rss|pages|languages|blueprint|infinity|tidy)",
              fifth.getSelectors());
      assertEquals("Check selectors 5", "end_with_this", fifth.getSuffix());

      fifth.setGlob("*");
      assertEquals("Check glob", "*", fifth.getGlob());

      Filter sixth = filters.get(5);
      assertNull("Null type", sixth.getType());
      assertTrue("Very empty", sixth.toString().isEmpty());

      Filter seventh = filters.get(6);
      assertNull("Null type", seventh.getType());
      assertEquals("Equals without type", ",Suffix=top,Method=get,Glob=*top", seventh.toString());
      assertFalse("different class", seventh.equals(new FeedbackProcessor()));
      assertFalse("null", seventh.equals(null));
      assertNotEquals("hashcode", 0, seventh.hashCode());
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void emptyFilterCheck() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "filter/empty/" + DispatcherConstants.DISPATCHER_ANY);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();

      assertNotNull("Config should not be null", config);
      assertEquals("Violations", 1, results.getViolations(PARTIAL).size());
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      List<Filter> filters = farm.getFilter().getValue();
      assertNotNull(filters);

      assertEquals("Check filter size", 3, filters.size());
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void noBraceCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "filter/nobrace/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    assertEquals("Violations", 2, results.getViolations(MINIMIZED).size());
    Farm farm = config.getFarms().get(0).getValue();
    assertFalse("Should not be an author farm", farm.isAuthorFarm());
    assertEquals("A label", "no_brace_on_filters", farm.getLabel());
    assertNotNull(farm.getFilter());
    assertEquals(0, farm.getFilter().getValue().size());

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Should be 1 log entries", 1, logsList.size());
    assertTrue(logsList.get(0).getMessage().startsWith("Each /filter block must begin with a '{' character."));
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());
  }
}
