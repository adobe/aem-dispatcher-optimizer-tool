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
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StickyConnectionsTest {
  private static DispatcherConfigTestHelper helper;
  private static String anyPath;

  @BeforeClass
  public static void before() {
    helper = new DispatcherConfigTestHelper();
  }

  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "stickyConnections/" + DispatcherConstants.DISPATCHER_ANY);
    anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      StickyConnectionsFor sticky = farm.getStickyConnectionsFor().getValue();
      assertNotNull(sticky);
      assertEquals("Simple test of source", 2, farm.getStickyConnectionsFor().getLineNumber());

      List<ConfigurationValue<String>> paths = sticky.getPaths();
      assertEquals("Should be 5 paths", 5, paths.size());

      // HTTPONLY
      assertEquals("httponly check", true, sticky.getHttpOnly().getValue());
      checkIncludeSource(sticky.getHttpOnly(), "stickyConnections.any", 10);

      // SECURE
      assertEquals("secure check", true, sticky.getSecure().getValue());
      checkIncludeSource(sticky.getSecure(), "stickyConnections.any", 11);

      // Check paths
      assertEquals("Check path 1", "/products?lang=en", paths.get(0).getValue());
      checkIncludeSource(paths.get(0), "stickyConnectionsFor.any", 2);

      assertEquals("Check path 2", "/content/image?lang=en", paths.get(1).getValue());
      checkIncludeSource(paths.get(1), "stickyConnections.any", 4);

      assertEquals("Check path 3", "/content/video?lang=en", paths.get(2).getValue());
      checkIncludeSource(paths.get(2), "stickyConnections.any", 4);

      assertEquals("Check path 4", "/var/files/pdfs?lang=it", paths.get(3).getValue());
      checkIncludeSource(paths.get(3), "stickyConnections.any", 6);

      assertEquals("Check path 5", "/freestuff", paths.get(4).getValue());
      checkIncludeSource(paths.get(4), "stickyConnections.any", 7);
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void equalsAndHashCodeCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "stickyConnections/" + DispatcherConstants.DISPATCHER_ANY);
    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      Farm farm = config.getFarms().get(0).getValue();
      StickyConnectionsFor sticky = farm.getStickyConnectionsFor().getValue();
      assertNotNull(sticky);
      assertEquals("Simple test of source", 2, farm.getStickyConnectionsFor().getLineNumber());

      // Check equals & hashcode
      assertNotNull("Should be a logger", sticky.getLogger());
      assertNotNull("Should be a class name", sticky.getSimpleClassName());
      assertFalse("Try the equals()", sticky.equals("hello"));
      assertFalse("Try the equals()", sticky.equals(null));
      assertEquals("Try the equals()", sticky, sticky);
      assertFalse("Try the equals()", sticky.equals(1));
      assertNotEquals("Try the equals()", sticky, sticky.toString());
      assertNotEquals("Try the hashcode()", "hello", sticky.hashCode());

      absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "farm/complete/" + DispatcherConstants.DISPATCHER_ANY);
      results = helper.loadDispatcherConfiguration(absPath);
      config = results.getConfiguration();
      Farm farm2 = config.getFarms().get(0).getValue();
      StickyConnectionsFor sticky2 = farm2.getStickyConnectionsFor().getValue();
      assertNotNull(sticky2);
      assertEquals("Simple test of source", 57, farm2.getStickyConnectionsFor().getLineNumber());
      assertFalse("Try the equals()", sticky2.equals(sticky));

    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void defaultMultiplePathsCases() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "stickyConnections/defaults/" + DispatcherConstants.DISPATCHER_ANY);
    anyPath = PathUtil.stripLastPathElement(absPath);

    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull("Config should not be null", config);
    assertNotNull("Config's farms should not be null", config.getFarms());
    assertEquals("Should be 1 farm", 1, config.getFarms().size());

    Farm farm = config.getFarms().get(0).getValue();
    StickyConnectionsFor sticky = farm.getStickyConnectionsFor().getValue();
    assertNotNull(sticky);
    assertEquals("Simple test of source", 4, farm.getStickyConnectionsFor().getLineNumber());

    AssertHelper.assertValues(sticky.getSecure(), anyPath, false, 4, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(sticky.getHttpOnly(), anyPath, false, 4, DispatcherConstants.DISPATCHER_ANY, null);
  }

  @Test
  public void defaultSinglePathsCases() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "stickyConnections/defaults/" + DispatcherConstants.DISPATCHER_ANY);
    anyPath = PathUtil.stripLastPathElement(absPath);

    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull("Config should not be null", config);
    assertNotNull("Config's farms should not be null", config.getFarms());
    assertEquals("Should be 1 farm", 1, config.getFarms().size());

    Farm farm = config.getFarms().get(0).getValue();
    StickyConnectionsFor sticky = farm.getStickyConnectionsFor().getValue();
    assertNotNull(sticky);
    assertEquals("Simple test of source", 4, farm.getStickyConnectionsFor().getLineNumber());

    AssertHelper.assertValues(sticky.getSecure(), anyPath, false, 4, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(sticky.getHttpOnly(), anyPath, false, 4, DispatcherConstants.DISPATCHER_ANY, null);
  }

  private void checkIncludeSource(ConfigurationValue cv, String filename, int lineNumber) {

    assertTrue("correct any file",
            FilenameUtils.separatorsToSystem(anyPath + "/" + filename).endsWith(cv.getFileName()));
    assertEquals("correct line number", lineNumber, cv.getLineNumber());
    assertEquals("include file", DispatcherConstants.DISPATCHER_ANY, cv.getIncludedFrom());
  }
}
