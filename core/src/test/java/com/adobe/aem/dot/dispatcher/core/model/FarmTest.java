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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FarmTest {

  private static DispatcherConfigTestHelper helper;
  private static String anyPath;

  @BeforeClass
  public static void before() {
    helper = new DispatcherConfigTestHelper();
  }

  @Test
  public void noFarmsBraceCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "farm/empty/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    assertEquals("2 violations", 2, results.getViolations(FULL).size());
    assertEquals("test empty farms config", config.getName().getValue());
    assertEquals("No Farms", 0, config.getFarms().size()); // It did not fail processing, but should be empty.
  }

  @Test
  public void emptyFarmCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "farm/empty2/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    assertEquals("Violations", 1, results.getViolations(PARTIAL).size());
    Farm farm = config.getFarms().get(0).getValue();
    assertFalse("Should not be an author farm", farm.isAuthorFarm());
    assertEquals("A label", "empty_farm", farm.getLabel());
    assertEquals("A label", "test empty farm config", config.getName().getValue());
  }

  @Test
  public void farmSimpleCheck() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "farm/" + DispatcherConstants.DISPATCHER_ANY);
    anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertEquals("Violations", 2, results.getViolations(MINIMIZED).size());
      assertNotNull("Config should not be null", config);
      assertEquals("name should be correct", "test_simple_config", config.getName().getValue());
      assertTrue("ignoreEINTR should be true", config.getIgnoreEINTR().getValue());
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());
      Farm farm = config.getFarms().get(0).getValue();

      assertFalse("Should be an author farm", farm.isAuthorFarm());
      assertTrue("Should be an author farm", farm.isPublishFarm());

      AssertHelper.assertValues(farm.getRetryDelay(), anyPath, 200, 10, "simple.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(farm.getNumberOfRetries(), anyPath, 10, 12, "simple.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(farm.getUnavailablePenalty(), anyPath, 2, 13, "simple.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(farm.getHomePage(), anyPath, "deprecated", 13, "simple.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(farm.getFailOver(), anyPath, true, 15, "simple.any", DispatcherConstants.DISPATCHER_ANY);

      assertNotNull(farm.getLogger());
      assertEquals("Farm", farm.getSimpleClassName());
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void defaultCases() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "farm/defaults/" + DispatcherConstants.DISPATCHER_ANY);
    anyPath = PathUtil.stripLastPathElement(absPath);

    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull("Config should not be null", config);
    assertEquals("Violations", 1, results.getViolations(FULL).size());
    assertEquals("1 violations", 1, results.getViolations(PARTIAL).size());
    assertNotNull("Config's farms should not be null", config.getFarms());
    assertEquals("Should be 1 farm", 1, config.getFarms().size());

    Farm farm = config.getFarms().get(0).getValue();

    AssertHelper.assertValues(farm.getPropagateSyndPost(), anyPath, false, 4, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(farm.getRetryDelay(), anyPath, 1, 4, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(farm.getNumberOfRetries(), anyPath, 5, 4, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(farm.getUnavailablePenalty(), anyPath, 1, 4, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(farm.getFailOver(), anyPath, false, 4, DispatcherConstants.DISPATCHER_ANY, null);
  }

  @Test
  public void testFarmType() throws ConfigurationException {
    Farm farm = new Farm();

    // Basic cases
    farm.setLabel(new ConfigurationValue<>("publishfarm"));

    assertTrue(farm.isPublishFarm());
    assertFalse(farm.isAuthorFarm());

    farm.setLabel(new ConfigurationValue<>("thepublish"));

    assertTrue(farm.isPublishFarm());
    assertFalse(farm.isAuthorFarm());

    farm.setLabel(new ConfigurationValue<>("an_author_farm"));

    assertTrue(farm.isAuthorFarm());
    assertFalse(farm.isPublishFarm());

    farm.setLabel(new ConfigurationValue<>("a_author"));

    assertTrue(farm.isAuthorFarm());
    assertFalse(farm.isPublishFarm());

    // More difficult to determine
    farm.setLabel(new ConfigurationValue<>("acme", "path/to/acme_publish-farm.any", 1));

    assertTrue(farm.isPublishFarm());
    assertFalse(farm.isAuthorFarm());

    farm.setLabel(new ConfigurationValue<>("acme", "path/to/acme_author-farm.any", 1));

    assertTrue(farm.isAuthorFarm());
    assertFalse(farm.isPublishFarm());

    // Use the render to determine
    String absPathToComplete = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "farm/complete/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPathToComplete);
    farm = results.getConfiguration().getFarms().get(0).getValue();

    // Should be able to determine that it's a publish instance by port
    String port = farm.getRenders().getValue().get(0).getPort().getValue();
    assertEquals("Port should be set correctly", "4503", port);

    assertTrue(farm.isPublishFarm());
    assertFalse(farm.isAuthorFarm());

    // Test another config - with a telltale hostname
    absPathToComplete = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "farm/determine_type_with_hostname/" + DispatcherConstants.DISPATCHER_ANY);
    results = helper.loadDispatcherConfiguration(absPathToComplete);
    farm = results.getConfiguration().getFarms().get(0).getValue();

    // Should be able to determine that it's a publish instance by hostname
    String hostname = farm.getRenders().getValue().get(0).getHostname().getValue();
    assertEquals("Hostname should be set correctly", "_ENV___PUBLISH_IP__", hostname);

    assertTrue(farm.isPublishFarm());
    assertFalse(farm.isAuthorFarm());

    // Test another config - with a telltale hostname
    absPathToComplete = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "farm/determine_type_with_hostname2/" + DispatcherConstants.DISPATCHER_ANY);
    results = helper.loadDispatcherConfiguration(absPathToComplete);
    farm = results.getConfiguration().getFarms().get(0).getValue();

    // Should be able to determine that it's a author instance by hostname
    hostname = farm.getRenders().getValue().get(0).getHostname().getValue();
    assertEquals("Hostname should be set correctly", "_ENV___AUTHOR_IP__", hostname);

    assertTrue(farm.isAuthorFarm());
    assertFalse(farm.isPublishFarm());
  }
}
