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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class VirtualHostsTest {
  private static DispatcherConfigTestHelper helper;

  @BeforeClass
  public static void before() {
    helper = new DispatcherConfigTestHelper();
  }

  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "virtualhosts/" + DispatcherConstants.DISPATCHER_ANY);
    String anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      List<ConfigurationValue<String>> virtualHosts = farm.getVirtualHosts();
      assertNotNull(virtualHosts);

      assertEquals("Should be 3 virtual hosts", 3, virtualHosts.size());

      AssertHelper.assertValues(virtualHosts.get(0), anyPath, "www.myCompany.com",
              3, "virtualhosts.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(virtualHosts.get(1), anyPath, "www.myCompany.ch",
              4, "virtualhosts.any", DispatcherConstants.DISPATCHER_ANY);
      AssertHelper.assertValues(virtualHosts.get(2), anyPath, "www.mySubDivision.*",
              5, "virtualhosts.any", DispatcherConstants.DISPATCHER_ANY);
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void objectChecks() {
    VirtualHosts virtualHosts = new VirtualHosts(null);
    assertNotEquals("does not equal 1", 1, virtualHosts);

    List<ConfigurationValue<String>> list = new ArrayList<>();
    list.add(new ConfigurationValue<>("hi"));
    VirtualHosts virtualHosts2 = new VirtualHosts(list);
    assertEquals("should be 1", 1, virtualHosts2.getVirtualHosts().size());
  }
}
