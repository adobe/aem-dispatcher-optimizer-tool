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
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.FULL;
import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.PARTIAL;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_ENCODE_VALUE;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_HEADER_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SessionManagementTest {
  private static DispatcherConfigTestHelper helper;
  private String anyPath;
  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(SessionManagement.class);
  }

  @BeforeClass
  public static void beforeClass() {
    helper = new DispatcherConfigTestHelper();
  }

  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "sessionmanagement/" + DispatcherConstants.DISPATCHER_ANY);
    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      SessionManagement sessionManagement = farm.getSessionManagement().getValue();
      assertNotNull(sessionManagement);
      assertEquals("Simple test of source", 4, farm.getSessionManagement().getLineNumber());

      // Check SessionManagement values
      assertTrue("Check label", StringUtils.isEmpty(sessionManagement.getLabel()));
      assertEquals("Directory", "/usr/local/apache/.sessions", sessionManagement.getDirectory());
      assertEquals("Encode", "md6", sessionManagement.getEncode().getValue());
      assertEquals("Header", DEFAULT_HEADER_VALUE, sessionManagement.getHeader().getValue());
      assertEquals("Check timeout", Integer.valueOf(805), sessionManagement.getTimeout().getValue());

      // Remaining asserts are more for code coverage than code checking.
      assertNotNull("Should be a logger", sessionManagement.getLogger());
      assertNotNull("Should be a class name", sessionManagement.getSimpleClassName());
      assertFalse("Try the equals()", sessionManagement.equals("hello"));
      assertEquals("Try the equals()", sessionManagement, sessionManagement);
      assertFalse("Try the equals()", sessionManagement.equals(1));
      assertNotEquals("Try the equals()", sessionManagement, sessionManagement.toString());
      assertNotEquals("Try the hashcode()", "hello", sessionManagement.hashCode());

      absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "farm/complete/" + DispatcherConstants.DISPATCHER_ANY);
      ConfigurationParseResults<DispatcherConfiguration> results2 = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config2 = results2.getConfiguration();
      assertNotNull("Config should not be null", config2);
      assertNotNull("Config's farms should not be null", config2.getFarms());
      assertEquals("Should be 1 farm", 1, config2.getFarms().size());

      Farm farm2 = config2.getFarms().get(0).getValue();
      SessionManagement sessionManagement2 = farm2.getSessionManagement().getValue();
      assertNotNull(sessionManagement2);
      assertEquals("Simple test of source", 16, farm2.getSessionManagement().getLineNumber());
      assertNotEquals("Try the equals()", sessionManagement2, sessionManagement);

    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void noBraceCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "sessionmanagement/nobrace/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    Farm farm = config.getFarms().get(0).getValue();
    assertFalse("Should not be an author farm", farm.isAuthorFarm());
    ConfigurationValue<SessionManagement> sessionManagement = farm.getSessionManagement();
    assertNotNull(sessionManagement);
    assertNotNull(sessionManagement.getValue());
    assertNull(sessionManagement.getValue().getDirectory());
    assertEquals("Should be default", DEFAULT_ENCODE_VALUE, sessionManagement.getValue().getEncode().getValue());
    assertEquals("Should be default", Integer.valueOf(800), sessionManagement.getValue().getTimeout().getValue());
  }

  @Test
  public void nullCheck() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "sessionmanagement/empty_values/" + DispatcherConstants.DISPATCHER_ANY);
    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      assertEquals("Should be 1 violation", 1, results.getViolations(FULL).size()); // Missing default
      assertEquals("mandatory", "SessionManagement is missing mandatory 'directory' value.",
              results.getViolations(PARTIAL).get(0).getContext());

      Farm farm = config.getFarms().get(0).getValue();
      SessionManagement sessionManagement = farm.getSessionManagement().getValue();
      assertNotNull(sessionManagement);
      assertEquals("Simple test of source", 4, farm.getSessionManagement().getLineNumber());

      assertNull(sessionManagement.getDirectory());
      assertEquals("md5 default", DEFAULT_ENCODE_VALUE, sessionManagement.getEncode().getValue());
      assertEquals("HTTP:authorization default", DEFAULT_HEADER_VALUE,
              sessionManagement.getHeader().getValue());
      assertEquals("800 default", Integer.valueOf(800), sessionManagement.getTimeout().getValue());

      List<ILoggingEvent> logsList = listAppender.list;
      assertTrue(logsList.get(0).getMessage().startsWith("SessionManagement is missing mandatory 'directory' value."));
      assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());

    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void defaultCases() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "sessionmanagement/defaults/" + DispatcherConstants.DISPATCHER_ANY);
    anyPath = PathUtil.stripLastPathElement(absPath);

    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull("Config should not be null", config);
    assertNotNull("Config's farms should not be null", config.getFarms());
    assertEquals("Should be 1 farm", 1, config.getFarms().size());

    Farm farm = config.getFarms().get(0).getValue();
    SessionManagement management = farm.getSessionManagement().getValue();
    assertNotNull(management);
    assertEquals("Simple test of source", 4, farm.getSessionManagement().getLineNumber());

    AssertHelper.assertValues(management.getEncode(), anyPath, DEFAULT_ENCODE_VALUE, 4, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(management.getHeader(), anyPath, DEFAULT_HEADER_VALUE, 4, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(management.getTimeout(), anyPath, 800, 4, DispatcherConstants.DISPATCHER_ANY, null);
  }
}
