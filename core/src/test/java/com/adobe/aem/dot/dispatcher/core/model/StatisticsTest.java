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

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StatisticsTest {
  private static DispatcherConfigTestHelper helper;

  @BeforeClass
  public static void before() {
    helper = new DispatcherConfigTestHelper();
  }

  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "statistics/" + DispatcherConstants.DISPATCHER_ANY);
    String anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      Statistics statistics = farm.getStatistics().getValue();
      assertNotNull(statistics);
      assertEquals("Simple test of source", 4, farm.getStatistics().getLineNumber());

      // Remaining asserts are more for code coverage than code checking.
      assertFalse("Try the equals()", statistics.equals("hello"));
      assertFalse("Try the equals()", statistics.equals(1));
      assertNotEquals("Try the equals()", statistics, farm.toString());
      assertNotEquals("Try the hashcode()", "hello", statistics.hashCode());
      // ==============================================================

      StatisticsCategories categories = statistics.getCategories();
      assertNotNull(categories);

      // Remaining asserts are more for code coverage than code checking.
      assertFalse("Try the equals()", categories.equals("hello"));
      assertFalse("Try the equals()", categories.equals(1));
      assertNotEquals("Try the equals()", categories, farm.toString());
      assertNotEquals("Try the hashcode()", "hello", categories.hashCode());
      // ==============================================================

      List<StatisticsRule> rules = categories.getRules();
      assertEquals("Should be 4 rules", 4, rules.size());

      // Check statistics values
      StatisticsRule first = rules.get(0);
      assertEquals("Check label", "search", first.getLabel());
      assertEquals("Glob", "*search.html", first.getGlob().getValue());
      assertTrue("correct any file",
              FilenameUtils.separatorsToSystem(anyPath + "/statistics.any").endsWith(first.getGlob().getFileName()));
      assertEquals("correct line number", 7, first.getGlob().getLineNumber());
      assertEquals("null include file", DispatcherConstants.DISPATCHER_ANY, first.getGlob().getIncludedFrom());

      StatisticsRule second = rules.get(1);
      assertEquals("Check label", "html", second.getLabel());
      assertEquals("Glob", "*.html", second.getGlob().getValue());
      assertEquals("correct line number", 8, second.getGlob().getLineNumber());

      StatisticsRule fourth = rules.get(3);
      assertEquals("Check label", "others", fourth.getLabel());
      assertEquals("Glob", "*", fourth.getGlob().getValue());
      assertEquals("correct line number", 12, fourth.getGlob().getLineNumber());

      assertNotNull("Should be a logger", fourth.getLogger());
      assertNotNull("Should be a class name", fourth.getSimpleClassName());
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void noBraceRuleCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "statistics/nobrace/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    Farm farm = config.getFarms().get(0).getValue();
    assertFalse("Should not be an author farm", farm.isAuthorFarm());
    ConfigurationValue<Statistics> statistics = farm.getStatistics();
    assertNotNull(statistics);
    assertEquals("Should still have read in 6 wrong rules", 6,
            statistics.getValue().getCategories().getRules().size());
  }

  @Test
  public void noBraceCategoriesCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "statistics/nobrace3/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    Farm farm = config.getFarms().get(0).getValue();
    assertFalse("Should not be an author farm", farm.isAuthorFarm());
    ConfigurationValue<Statistics> statistics = farm.getStatistics();
    assertNotNull(statistics);
    assertNotNull(statistics.getValue().getCategories());
    assertNull(statistics.getValue().getCategories().getRules());
  }

  @Test
  public void noBraceStatisticsCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "statistics/nobrace2/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    Farm farm = config.getFarms().get(0).getValue();
    assertFalse("Should not be an author farm", farm.isAuthorFarm());
    ConfigurationValue<Statistics> statistics = farm.getStatistics();
    assertNotNull(statistics);
    assertNotNull(statistics.getValue());
    assertNull(statistics.getValue().getCategories());
  }
}
