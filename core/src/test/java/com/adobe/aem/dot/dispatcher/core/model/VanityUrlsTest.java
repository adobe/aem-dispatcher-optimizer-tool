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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VanityUrlsTest {
  private static DispatcherConfigTestHelper helper;
  private static String anyPath;

  @BeforeClass
  public static void before() {
    helper = new DispatcherConfigTestHelper();
  }

  @Test
  public void basicCases() {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "vanity_urls/" + DispatcherConstants.DISPATCHER_ANY);
    anyPath = PathUtil.stripLastPathElement(absPath);

    try {
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertNotNull("Config should not be null", config);
      assertNotNull("Config's farms should not be null", config.getFarms());
      assertEquals("Should be 1 farm", 1, config.getFarms().size());

      Farm farm = config.getFarms().get(0).getValue();
      VanityUrls vanity = farm.getVanityUrls().getValue();
      assertNotNull(vanity);
      assertEquals("Simple test of source", 3, farm.getVanityUrls().getLineNumber());

      assertEquals("Check url", "/libs/granite/dispatcher/content/very_vane.html", vanity.getUrl().getValue());
      checkIncludeSource(vanity.getUrl(), "vanity_urls.any", 4);

      assertEquals("Check file", "/tmp/conceited", vanity.getFile().getValue());
      checkIncludeSource(vanity.getFile(), "vanity_urls.any", 5);

      assertEquals("Check delay", Integer.valueOf(350), vanity.getDelay().getValue());
      checkIncludeSource(vanity.getDelay(), "vanity_urls.any", 9);
    } catch(ConfigurationException dcEx) {
      Assert.fail("Failure: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void noBraceCheck() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "vanity_urls/nobrace/" + DispatcherConstants.DISPATCHER_ANY);
    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull(config);
    Farm farm = config.getFarms().get(0).getValue();
    assertFalse("Should not be an author farm", farm.isAuthorFarm());
    ConfigurationValue<VanityUrls> vanityUrls = farm.getVanityUrls();
    assertNotNull(vanityUrls);
    assertNotNull(vanityUrls.getValue());
  }

  private void checkIncludeSource(ConfigurationValue<?> cv, String filename, int lineNumber) {
    assertTrue("correct any file",
            FilenameUtils.separatorsToSystem(anyPath + "/" + filename).endsWith(cv.getFileName()));
    assertEquals("correct line number", lineNumber, cv.getLineNumber());
    assertEquals("include file", DispatcherConstants.DISPATCHER_ANY, cv.getIncludedFrom());
  }
}
