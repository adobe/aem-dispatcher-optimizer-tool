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

package com.adobe.aem.dot.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.helpers.PathEncodingHelper;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationFileFinderTest {
  private ListAppender<ILoggingEvent> listAppender;
  private ConfigurationFileFinder finder;

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(ConfigurationFileFinder.class);

    finder = new ConfigurationFileFinder(DispatcherConstants.DISPATCHER_ANY);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullTest() throws IllegalArgumentException {
    finder = new ConfigurationFileFinder(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullTest2() throws ConfigurationException {
    finder.findMainConfigurationFile(null, false);
  }

  @Test(expected = ConfigurationException.class)
  public void nonExistentTest() throws ConfigurationException {
    finder.findMainConfigurationFile("/this/does/not/exist", false);
  }

  @Test
  public void folderWithNoAnyFileTest() throws ConfigurationException {
    String classPath = PathEncodingHelper.getDecodedClassPath(this.getClass());
    classPath = PathUtil.appendPaths(classPath, "util");
    assertNull(finder.findMainConfigurationFile(classPath, false));
  }

  @Test
  public void foundFileTest() throws ConfigurationException {
    String classPath = PathEncodingHelper.getDecodedClassPath(this.getClass());
    int targetIndex = classPath.indexOf("/core/target/");
    String testProjectPath = classPath.substring(0, targetIndex) +
            FilenameUtils.separatorsToSystem("/test-projects/test-project-all-rules-fail");

    File anyFile = finder.findMainConfigurationFile(testProjectPath, false);
    String anyPath = anyFile.getPath();
    assertTrue("Should be a good file", anyPath.endsWith(DispatcherConstants.DISPATCHER_ANY));
    assertNotNull("Should be non-null file.", anyFile);
    assertTrue("Any file should exist.", anyFile.exists());
  }

  @Test
  public void foundCWDFileTest() throws ConfigurationException {
    String classPath = PathEncodingHelper.getDecodedClassPath(this.getClass()) + "analyzer/scripts";

    // Exactly like the 'folderWithNoAnyFileTest' but with a 'true' parameter (takes a while).
    File anyFile = finder.findMainConfigurationFile(classPath, true);
    String anyPath = anyFile.getPath();
    assertTrue("Not empty or null", StringUtils.isNotEmpty(anyPath));

    List<ILoggingEvent> logsList = listAppender.list;
    assertTrue("Contains basic text",
            logsList.get(0).getMessage().contains("\"{}\" was not found within repo directory"));
    assertTrue("Contains basic text",
            logsList.get(0).getMessage().contains(". Looking under current directory."));
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(0).getLevel());

    assertTrue("Contains basic text",
            logsList.get(1).getMessage().contains("More than 1 \"{}\" file was found in repo url (count={}"));
    assertTrue("Contains basic text",
            logsList.get(1).getMessage().contains(").  Using first file."));
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(1).getLevel());
  }
}
