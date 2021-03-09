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

package com.adobe.aem.dot.httpd.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.ConfigurationFileFinder;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.helpers.PathEncodingHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.adobe.aem.dot.httpd.core.model.SectionType;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HttpdConfigurationFactoryTest {
  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(ConfigurationFileFinder.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void httpdNullRepoTest() throws ConfigurationException {
    HttpdConfigurationFactory hcf = new HttpdConfigurationFactory();

    hcf.getHttpdConfiguration(null, "/hi");
  }

  @Test(expected = ConfigurationException.class)
  public void httpdNullConfigTest() throws ConfigurationException {
    HttpdConfigurationFactory hcf = new HttpdConfigurationFactory();

    hcf.getHttpdConfiguration("/hi", null);
  }

  @Test(expected = ConfigurationException.class)
  public void httpdNonExistentRepoTest() throws ConfigurationException {
    HttpdConfigurationFactory hcf = new HttpdConfigurationFactory();

    hcf.getHttpdConfiguration("/hi", "/there");
  }

  @Test
  public void httpdFolderWithHttpdFileTest() throws ConfigurationException {
    HttpdConfigurationFactory hcf = new HttpdConfigurationFactory();
    String classPath = PathEncodingHelper.getDecodedClassPath(this.getClass());

    // Provide valid repo path without any 'httpd.conf' files.
    ConfigurationParseResults<HttpdConfiguration> results = hcf.getHttpdConfiguration(
            classPath + File.separator + "helpers", "hi");
    assertNull("config not found - null", results);

    List<ILoggingEvent> logsList = listAppender.list;
    assertTrue(logsList.get(0).getFormattedMessage().startsWith("\"httpd.conf\" was not found within repo directory"));
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());
  }

  @Test
  public void httpdParseConfigTest() throws ConfigurationException {
    HttpdConfigurationFactory hcf = new HttpdConfigurationFactory();
    String classPath = PathEncodingHelper.getDecodedClassPath(this.getClass());

    int targetIndex = classPath.indexOf("/core/target/");
    String testProjectPath = classPath.substring(0, targetIndex) +
            FilenameUtils.separatorsToSystem("/test-projects/test-project-all-rules-fail");

    ConfigurationParseResults<HttpdConfiguration> results = hcf.getHttpdConfiguration(testProjectPath,
            "dispatcher/src/conf");
    assertNotNull(results);
    HttpdConfiguration config = results.getConfiguration();
    assertNotNull("Should not be null", config);
    assertEquals("Check level", SectionType.TOP_LEVEL, config.getType());
    assertTrue("Should have virtual hosts", config.getVirtualHosts().size() > 0);
    assertTrue("Should have directives", config.getDirectives().size() > 0);
    assertTrue("Should have sections", config.getSections().size() > 0);
  }

  @Test
  public void httpdFindAndParseConfigTest() throws ConfigurationException {
    HttpdConfigurationFactory hcf = new HttpdConfigurationFactory();
    String classPath = PathEncodingHelper.getDecodedClassPath(this.getClass());

    int targetIndex = classPath.indexOf("/core/target/");
    String testProjectPath = classPath.substring(0, targetIndex) +
            FilenameUtils.separatorsToSystem("/test-projects/test-project-all-rules-fail");

    // Exact config directory is wrong - make the system find the config file under 'testProjectPath'.

    ConfigurationParseResults<HttpdConfiguration> results = hcf.getHttpdConfiguration(testProjectPath,
            "dispatcher2");
    HttpdConfiguration config = results.getConfiguration();
    assertNotNull("Should not be null", config);
    assertEquals("Check level", SectionType.TOP_LEVEL, config.getType());
    assertTrue("Should have virtual hosts", config.getVirtualHosts().size() > 0);
    assertTrue("Should have directives", config.getDirectives().size() > 0);
    assertTrue("Should have sections", config.getSections().size() > 0);
  }
}
