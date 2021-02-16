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
import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.PARTIAL;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_MODE_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CacheTest {
  private static DispatcherConfigTestHelper helper;
  private static String anyPath;

  @BeforeClass
  public static void before() {
    helper = new DispatcherConfigTestHelper();
  }

  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "cache/" + DispatcherConstants.DISPATCHER_ANY);
    anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertEquals("Violations", 0, results.getViolations(FULL).size());
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      Cache cache = farm.getCache().getValue();
      assertNotNull(cache);
      assertEquals("Simple test of source", 2, farm.getCache().getLineNumber());

      AssertHelper.assertValues(cache.getDocroot(), anyPath, "/opt/dispatcher/cache", 3, "cache.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(cache.getStatfileslevel(), anyPath, 3, 7, "cache.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(cache.getStatfile(), anyPath, "/tmp/dispatcher-website.stat?lang=en", 4, "cache.any",
              DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(cache.getServeStaleOnError(), anyPath, true, 6, "cache.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(cache.getAllowAuthorized(), anyPath, true, 7, "cache.any", DispatcherConstants.DISPATCHER_ANY);
      assertEquals("list of 2", 2, cache.getInvalidate().getValue().size());
      AssertHelper.assertValues(cache.getInvalidateHandler(), anyPath, "/opt/dispatcher/scripts/invalidate.sh?lang=en", 13, "cache.any",
              DispatcherConstants.DISPATCHER_ANY);
      assertEquals("list of 3", 3, cache.getAllowedClients().getValue().size());
      assertEquals("list of 4", 4, cache.getIgnoreUrlParams().getValue().size());
      assertEquals("list of 38", 38, cache.getHeaders().size());
      AssertHelper.assertValues(cache.getMode(), anyPath, "0200", 30, "cache.any", DispatcherConstants.DISPATCHER_ANY);
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void defaultCases() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "cache/defaults/" + DispatcherConstants.DISPATCHER_ANY);
    anyPath = PathUtil.stripLastPathElement(absPath);

    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull("Config should not be null", config);
    assertEquals("Violations", 0, results.getViolations(PARTIAL).size());
    assertNotNull("Config's farms should not be null", config.getFarms());
    assertEquals("Should be 1 farm", 1, config.getFarms().size());

    Farm farm = config.getFarms().get(0).getValue();
    Cache cache = farm.getCache().getValue();
    assertNotNull(cache);

    AssertHelper.assertValues(cache.getStatfileslevel(), anyPath, 0, 5, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(cache.getAllowAuthorized(), anyPath, false, 5, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(cache.getServeStaleOnError(), anyPath, false, 5, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(cache.getGracePeriod(), anyPath, 0, 5, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(cache.getEnableTTL(), anyPath, false, 5, DispatcherConstants.DISPATCHER_ANY, null);
    AssertHelper.assertValues(cache.getMode(), anyPath, DEFAULT_MODE_VALUE, 5, DispatcherConstants.DISPATCHER_ANY, null);
  }
}
