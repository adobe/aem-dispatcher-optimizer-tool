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
import com.adobe.aem.dot.common.analyzer.ViolationVerbosity;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.FULL;
import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.MINIMIZED;
import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.PARTIAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AuthCheckerTest {
  private static DispatcherConfigTestHelper helper;

  @BeforeClass
  public static void before() {
    helper = new DispatcherConfigTestHelper();
  }

  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "auth_checker/" + DispatcherConstants.DISPATCHER_ANY);
    String anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertEquals(0, results.getViolations(ViolationVerbosity.MINIMIZED).size());
      assertNotNull("Config should not be null", config);
      assertEquals("Violations", 0, results.getViolations(FULL).size());
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      assertTrue("Should be an author farm", farm.isAuthorFarm());
      assertFalse("Should be an author farm", farm.isPublishFarm());
      AuthChecker checker = farm.getAuthChecker().getValue();
      assertNotNull(checker);
      assertEquals("Simple test of source", 7, farm.getAuthChecker().getLineNumber());

      AssertHelper.assertValues(checker.getUrl(), anyPath, "/bin/check", 9, "auth_checker.any",
              DispatcherConstants.DISPATCHER_ANY);

      // Check Filter values
      assertEquals("Check filter size", 2, checker.getFilter().getValue().size());
      assertEquals("Check label 1", "0000", checker.getFilter().getValue().get(0).getLabel());
      assertEquals("Check glob 1", "*", checker.getFilter().getValue().get(0).getGlob());
      assertEquals("Check type 1", RuleType.DENY, checker.getFilter().getValue().get(0).getType());
      assertEquals("Check label 2", "0001", checker.getFilter().getValue().get(1).getLabel());
      assertEquals("Check glob 2", "/content/secure/*.html", checker.getFilter().getValue().get(1).getGlob());
      assertEquals("Check type 2", RuleType.ALLOW, checker.getFilter().getValue().get(1).getType());
      AssertHelper.assertValues(checker.getFilter().getValue().get(1).getLabelData(), anyPath, "0001",
              21, "auth_checker.any", DispatcherConstants.DISPATCHER_ANY);

      // Check Header values
      assertEquals("Check header size", 2, checker.getHeaders().getValue().size());
      assertEquals("Check label 1", "0003", checker.getHeaders().getValue().get(0).getLabel());
      assertEquals("Check glob 1", "*", checker.getHeaders().getValue().get(0).getGlob());
      assertEquals("Check type 1", RuleType.DENY, checker.getHeaders().getValue().get(0).getType());
      assertEquals("Check label 2", "whatever", checker.getHeaders().getValue().get(1).getLabel());
      assertEquals("Check glob 2", "Set-Cookie:*", checker.getHeaders().getValue().get(1).getGlob());
      assertEquals("Check type 2", RuleType.ALLOW, checker.getHeaders().getValue().get(1).getType());

    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void equalsAndHashCodeCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "auth_checker/" + DispatcherConstants.DISPATCHER_ANY);
    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertEquals(0, results.getViolations(ViolationVerbosity.MINIMIZED).size());
      Farm farm = config.getFarms().get(0).getValue();
      assertTrue("Should be an author farm", farm.isAuthorFarm());
      assertFalse("Should be an author farm", farm.isPublishFarm());
      AuthChecker checker = farm.getAuthChecker().getValue();
      assertNotNull(checker);
      assertEquals("Simple test of source", 7, farm.getAuthChecker().getLineNumber());

      // Check
      assertNotNull("Should be a logger", checker.getLogger());
      assertNotNull("Should be a class name", checker.getSimpleClassName());
      assertFalse("Try the equals()", checker.equals("hello"));
      assertEquals("Try the equals on itself", checker, checker);
      assertNotEquals("Try the equals()", "hello", checker.toString());
      assertNotEquals("Try the hashcode()", "hello", checker.hashCode());

      assertFalse("Try the equals()", checker.getHeaders().equals("hello"));
      assertEquals("Try the equals on itself", checker.getHeaders(), checker.getHeaders());
      assertNotEquals("Try the equals()", "hello", checker.getHeaders().toString());
      assertNotEquals("Try the hashcode()", "hello", checker.getHeaders().hashCode());

      absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "farm/complete/" + DispatcherConstants.DISPATCHER_ANY);
      results = helper.loadDispatcherConfiguration(absPath);
      assertEquals("Violations", 1, results.getViolations(PARTIAL).size());
      config = results.getConfiguration();
      Farm farm2 = config.getFarms().get(0).getValue();
      AuthChecker checker2 = farm2.getAuthChecker().getValue();
      assertFalse("Try the equals()", checker2.equals(checker));
      assertEquals("Simple test of source", 65, farm2.getAuthChecker().getLineNumber());

    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void noBraceAuthCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "auth_checker/nobrace/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    assertEquals("Violations 2, down from 4", 2, results.getViolations(MINIMIZED).size());
    Farm farm = config.getFarms().get(0).getValue();
    assertTrue("Should be an author farm", farm.isAuthorFarm());
    assertFalse("Should be an author farm", farm.isPublishFarm());
    ConfigurationValue<AuthChecker> checker = farm.getAuthChecker();
    assertNotNull(checker);
    assertNotNull(checker.getValue());
    assertNull(checker.getValue().getUrl());
    assertNull(checker.getValue().getFilter());
    assertNull(checker.getValue().getHeaders());
    assertEquals(0, checker.getValue().getLabel().length());    // It did not fail processing, but should be empty.
  }
}
