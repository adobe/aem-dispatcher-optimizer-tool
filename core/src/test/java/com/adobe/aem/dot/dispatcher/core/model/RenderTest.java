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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RenderTest {
  private static DispatcherConfigTestHelper helper;
  private ListAppender<ILoggingEvent> listAppender;

  @BeforeClass
  public static void beforeClass() {
    helper = new DispatcherConfigTestHelper();
  }

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(Render.class);
  }

  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "renders/" + DispatcherConstants.DISPATCHER_ANY);
    String anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertEquals("Violations", 4, results.getViolations(FULL).size());
      assertEquals("Violations", 2, results.getViolations(PARTIAL).size());
      assertEquals("Violations", 1, results.getViolations(MINIMIZED).size());
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      List<Render> renders = farm.getRenders().getValue();
      assertNotNull(renders);
      assertEquals("Simple test of source", 7, farm.getRenders().getLineNumber());

      assertEquals("Check render size", 4, renders.size());

      // Check first Render values
      Render first = renders.get(0);
      assertEquals("Check label", "myRenderer", first.getLabel());
      assertTrue("correct any file",
              FilenameUtils.separatorsToSystem(anyPath + "/" + DispatcherConstants.DISPATCHER_ANY).endsWith(first.getLabelData().getFileName()));
      assertEquals("correct line number", 9, first.getLabelData().getLineNumber());
      assertNull("null include file", first.getLabelData().getIncludedFrom());
      assertEquals("Check hostname 1", "aem.myCompany.com", first.getHostname().getValue());
      assertEquals("Check port 1 - default for secure=1", "443", first.getPort().getValue());
      assertEquals("Check timeout 1",  Integer.valueOf(0), first.getTimeout().getValue());

      // Check second Render values
      Render second = renders.get(1);
      assertEquals("Check label", "mySecondRenderer", second.getLabel());
      assertTrue("correct any file",
              FilenameUtils.separatorsToSystem(anyPath + "/" + DispatcherConstants.DISPATCHER_ANY).endsWith(second.getLabelData().getFileName()));
      assertEquals("correct line number", 18, second.getLabelData().getLineNumber());
      assertNull("null include file", second.getLabelData().getIncludedFrom());
      assertEquals("Check hostname 2", "127.0.0.1", second.getHostname().getValue());
      assertEquals("Check port 2", "4503", second.getPort().getValue());
      assertEquals("Check timeout 2", Integer.valueOf(20), second.getTimeout().getValue());
      assertEquals("Check receive timeout 2", Integer.valueOf(10), second.getReceiveTimeout().getValue());
      assertEquals("Check ipv4 2", true, second.getIpv4().getValue());
      assertEquals("Check secure 2", true, second.getSecure().getValue());
      assertEquals("Check always resolve 2", true, second.getAlwaysResolve().getValue());

      Render third = renders.get(2);
      assertEquals("Check port 3 - default for secure=0", "80", third.getPort().getValue());
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void noBraceRendererCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(
            this.getClass(), "renders/empty2/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    assertEquals("Violations", 2, results.getViolations(PARTIAL).size());
    Farm farm = config.getFarms().get(0).getValue();
    assertNotNull(farm);
    ConfigurationValue<List<Render>> renders = farm.getRenders();
    assertNotNull(renders);
    assertNotNull(renders.getValue());
    assertEquals("Should be empty", 0, renders.getValue().size());
  }

  @Test
  public void emptyRendererCheck() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "renders/empty/" + DispatcherConstants.DISPATCHER_ANY);
    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();

      assertNotNull("Config should not be null", config);
      assertEquals("Violations", 2, results.getViolations(MINIMIZED).size());
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      List<Render> renders = farm.getRenders().getValue();
      assertNotNull(renders);
      assertEquals("Simple test of source", 7, farm.getRenders().getLineNumber());

      assertEquals("Check render size", 4, renders.size());

      List<ILoggingEvent> logsList = listAppender.list;
      assertEquals("Should be 3 log entries", 3, logsList.size());
      assertTrue(logsList.get(0).getMessage().startsWith("Each render must begin with a '{' character."));
      assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void classNameAndLoggerCheck() {
    Render render = new Render();
    assertNotNull(render.getLogger());
    assertEquals("Render", render.getSimpleClassName());
  }

  @Test
  public void defaultCases() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "renders/defaults/" + DispatcherConstants.DISPATCHER_ANY);
    String anyPath = PathUtil.stripLastPathElement(absPath);

    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull("Config should not be null", config);
    assertEquals("Violations", 0, results.getViolations(FULL).size());
    assertNotNull("Config's farms should not be null", config.getFarms());
    assertEquals("Should be 1 farm", 1, config.getFarms().size());

    Farm farm = config.getFarms().get(0).getValue();
    List<Render> renders = farm.getRenders().getValue();
    assertNotNull(renders);
    assertEquals("Simple test of source", 5, farm.getRenders().getLineNumber());
    Render first = renders.get(0);
    Render second = renders.get(1);
    assertNotNull(first);
    assertNotNull(second);

    AssertHelper.assertValues(first.getPort(), anyPath, "80", 6,
            DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(first.getTimeout(), anyPath, 0, 6,
            DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(first.getReceiveTimeout(), anyPath, 600000, 6,
            DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(first.getIpv4(), anyPath, false, 6,
            DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(first.getSecure(), anyPath, false, 6,
            DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(first.getAlwaysResolve(), anyPath, false, 6,
            DispatcherConstants.DISPATCHER_ANY, null);

    AssertHelper.assertValues(second.getPort(), anyPath, "443", 7,
            DispatcherConstants.DISPATCHER_ANY, null);
  }
}
