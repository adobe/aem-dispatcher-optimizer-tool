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

package com.adobe.aem.dot.dispatcher.core;

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.helpers.PathEncodingHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.model.Farm;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DispatcherConfigurationFactoryTest {

  @Test(expected = IllegalArgumentException.class)
  public void nullRepoTest() throws ConfigurationException {
    DispatcherConfigurationFactory dcf = new DispatcherConfigurationFactory();

    dcf.parseConfiguration(null, "/hi");
  }

  @Test
  public void nullConfigTest() {
    DispatcherConfigurationFactory dcf = new DispatcherConfigurationFactory();

    try {
      dcf.parseConfiguration("/hi", null);
      fail("Exception should be thrown.");
    } catch(ConfigurationException dcEx) {
      assertEquals("The `/hi` folder does not exist", dcEx.toString());
    }
  }

  @Test(expected = ConfigurationException.class)
  public void nonExistentRepoTest() throws ConfigurationException {
    DispatcherConfigurationFactory dcf = new DispatcherConfigurationFactory();

    dcf.parseConfiguration("/hi", "/there");
  }

  @Test(expected = ConfigurationException.class)
  public void folderWithNoAnyFileTest() throws ConfigurationException {
    DispatcherConfigurationFactory dcf = new DispatcherConfigurationFactory();
    String classPath = PathEncodingHelper.getDecodedClassPath(this.getClass());
    // Provide valid repo path without any 'dispatcher.any' files.
    dcf.parseConfiguration(classPath + File.separator + "util", "hi");
  }

  @Test
  public void parseConfigTest() throws ConfigurationException {
    DispatcherConfigurationFactory dcf = new DispatcherConfigurationFactory();
    String classPath = PathEncodingHelper.getDecodedClassPath(this.getClass());

    int targetIndex = classPath.indexOf("/core/target/");
    assertTrue("Should be in the path", targetIndex > 0);
    String testProjectPath = classPath.substring(0, targetIndex) +
            FilenameUtils.separatorsToSystem("/test-projects/test-project-all-rules-pass");
    testProjectPath = PathEncodingHelper.getDecodedPath(testProjectPath);

    ConfigurationParseResults<DispatcherConfiguration> results = dcf.parseConfiguration(testProjectPath,
            "dispatcher/src/conf.dispatcher.d");
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull("Should not be null", config);
    assertNotNull("Name should not be null", config.getName());
    assertEquals("Check Name", "skylab-server", config.getName().getValue());
    assertTrue("Should have farms", config.getFarms().size() > 0);
    Farm authorFarm = config.getFarms().get(0).getValue();
    if (!authorFarm.getLabel().contains("author")) {
      authorFarm = config.getFarms().get(1).getValue();
    }
    assertTrue("Farm should have client headers", authorFarm.getClientHeaders().size() > 0);
    assertTrue("Farm should have virtual hosts", authorFarm.getVirtualHosts().size() > 0);
    assertTrue("Farm should have renders", authorFarm.getRenders().getValue().size() > 0);
    assertTrue("Farm should have filters", authorFarm.getFilter().getValue().size() > 0);
    assertNotNull("Farm should have cache", authorFarm.getCache());
  }

  @Test
  public void findAndParseConfigTest() throws ConfigurationException {
    DispatcherConfigurationFactory dcf = new DispatcherConfigurationFactory();
    String classPath = PathEncodingHelper.getDecodedClassPath(this.getClass());

    int targetIndex = classPath.indexOf("/core/target/");
    assertTrue("Should be in the path", targetIndex > 0);
    String testProjectPath = classPath.substring(0, targetIndex) +
            FilenameUtils.separatorsToSystem("/test-projects/test-project-all-rules-pass");
    testProjectPath = PathEncodingHelper.getDecodedPath(testProjectPath);

    ConfigurationParseResults<DispatcherConfiguration> results = dcf.parseConfiguration(testProjectPath, "dispatcher3");
    DispatcherConfiguration config = results.getConfiguration();
    assertNotNull("Should not be null", config);
    assertNotNull("Name should not be null", config.getName());
    assertEquals("Check Name", "skylab-server", config.getName().getValue());
    assertTrue("Should have farms", config.getFarms().size() > 0);
    Farm authorFarm = config.getFarms().get(0).getValue();
    if (!authorFarm.getLabel().contains("author")) {
      authorFarm = config.getFarms().get(1).getValue();
    }
    assertTrue("Farm should have client headers", authorFarm.getClientHeaders().size() > 0);
    assertTrue("Farm should have virtual hosts", authorFarm.getVirtualHosts().size() > 0);
    assertTrue("Farm should have renders", authorFarm.getRenders().getValue().size() > 0);
    assertTrue("Farm should have filters", authorFarm.getFilter().getValue().size() > 0);
    assertNotNull("Farm should have cache", authorFarm.getCache());
  }
}
