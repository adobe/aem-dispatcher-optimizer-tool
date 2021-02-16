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

import java.util.List;

import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.FULL;
import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.MINIMIZED;
import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.PARTIAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ClientHeadersTest {
  private static DispatcherConfigTestHelper helper;
  private static String anyPath;

  @BeforeClass
  public static void before() {
    helper = new DispatcherConfigTestHelper();
  }

  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "clientheaders/" + DispatcherConstants.DISPATCHER_ANY);
    anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertEquals("Violations", 0, results.getViolations(FULL).size());
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      List<ConfigurationValue<String>> clientHeaders = farm.getClientHeaders();
      assertNotNull(clientHeaders);

      assertEquals("Should be 38 client headers", 38, clientHeaders.size());

      AssertHelper.assertValues(clientHeaders.get(0), anyPath, "CSRF-Token", 1,
              "clientheaders.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(clientHeaders.get(1), anyPath, "X-Forwarded-Proto", 2,
              "clientheaders.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(clientHeaders.get(2), anyPath, "referer",3,
              "clientheaders.any", DispatcherConstants.DISPATCHER_ANY);

      AssertHelper.assertValues(clientHeaders.get(34), anyPath, "lock-token", 35,
              "clientheaders.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(clientHeaders.get(35), anyPath, "x-expected-entity-length", 36,
              "clientheaders.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(clientHeaders.get(36), anyPath, "destination", 37,
              "clientheaders.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(clientHeaders.get(37), anyPath, "PATH", 38,
              "clientheaders.any", DispatcherConstants.DISPATCHER_ANY);

      AssertHelper.assertValues(clientHeaders.get(37), anyPath, "PATH", 38,
              "clientheaders.any", DispatcherConstants.DISPATCHER_ANY);

    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void noBraceCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "clientheaders/nobrace/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    assertNotNull(results);
    assertNotNull(results.getViolations(PARTIAL));
    assertEquals("string list not starting with a {", 1, results.getViolations(MINIMIZED).size());

    assertNotNull(results.getConfiguration());
    DispatcherConfiguration config = results.getConfiguration();
    Farm farm = config.getFarms().get(0).getValue();
    List<ConfigurationValue<String>> clientHeaders = farm.getClientHeaders();
    assertNotNull(clientHeaders);
    assertEquals(5, clientHeaders.size());  // List was read regardless of Violation

    // While here, test that a string list stopped when encountering the next "/ section".
    List<ConfigurationValue<String>> virtualHosts = farm.getVirtualHosts();
    assertNotNull(virtualHosts);
    assertEquals(1, virtualHosts.size());
  }
}
