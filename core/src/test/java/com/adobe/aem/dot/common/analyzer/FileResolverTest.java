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

package com.adobe.aem.dot.common.analyzer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import com.adobe.aem.dot.common.FileResolver;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.helpers.PathEncodingHelper;
import com.adobe.aem.dot.common.util.PathUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class FileResolverTest {
  private ListAppender<ILoggingEvent> listAppender;
  private FileResolver fileResolver;
  private final String missingBasePath = "/some/base/path";
  private String realPath;
  private String realBase;
  private String classPath;

  @Rule
  public EnvironmentVariablesRule environmentVariablesRule = new EnvironmentVariablesRule();

  @Before
  public void before() {
    fileResolver = new FileResolver(missingBasePath, false);

    classPath = PathEncodingHelper.getDecodedClassPath(this.getClass());
    realPath = PathUtil.stripFirstPathElement(classPath).substring(1);
    realPath = realPath.substring(0, realPath.length() - 1);
    realBase = File.separatorChar + PathUtil.split(classPath)[0];

    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(FileResolver.class);
  }

  @Test
  public void resolveFileWithBadEnvVarTest() {
    List<File> files = fileResolver.resolveFiles("../path/with/${ENVIRONMENT/variable", missingBasePath);
    assertTrue("files is empty", files.isEmpty());

    List<ILoggingEvent> logsList = listAppender.list;
    // Check that the 'unclosed' regex was logged - "regex(.*" - should be the first entry in the logs.
    assertEquals("Path variable contains unclosed environment variable placeholder (i.e. \"${...\").  Line=\"{}\"",
            logsList.get(0).getMessage());
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(0).getLevel());
  }

  @Test
  public void resolveFileWithEnvVarTest() {
    List<File> files =  fileResolver.resolveFiles("../path/with/${ENVIRONMENT}/variable", missingBasePath);
    assertTrue("files is empty", files.isEmpty());

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Environment variable is not set.  EnvVar=\"{}\"",
            logsList.get(0).getMessage());
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(0).getLevel());
    assertEquals("Skipping file with unresolved environment variable.  Path=\"{}\"",
            logsList.get(1).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(1).getLevel());
  }

  @Test
  public void resolveFileWithEnvVarSetTest() {
    //construct path to an  existing configuration file in test resources
    String workDir = Paths.get("src", "test", "resources", "dispatcher-includes", "src", "conf.dispatcher.d").toAbsolutePath().toString();
    //set ENV_VAR to 'filters' folder  in path above
    environmentVariablesRule.set("ENV_VAR", "filters");
    // include the file in 'filters' folder
    List<File> files =  fileResolver.resolveFiles("${ENV_VAR}/filter.any", workDir);
    assertTrue("files are not empty", files.size()  >0);
  }


  @Test
  public void resolveNonExistentDirectory() {
    List<File> files = fileResolver.resolveFiles("../path/with/*/variable", missingBasePath);
    assertTrue("files is empty", files.isEmpty());
  }

  @Test
  public void resolveWildCardFileTest() {
    List<File> files = fileResolver.resolveFiles(realPath + File.separator + "*", realBase);
    assertNotNull("Should not be null", files);
    assertTrue("files should have some files", files.size() > 0);
    assertTrue("files contain this class", files.toString().contains(this.getClass().getSimpleName()));
  }

  @Test
  public void resolveOtherWildCardFileTest() {
    String missingPath = PathUtil.stripFirstPathElement(classPath).substring(1) + "FileResolverTest.[not_there]";
    List<File> files = fileResolver.resolveFiles(missingPath, realBase);
    assertNotNull("Should not be null", files);
    assertEquals("files is empty", 0, files.size());

    files = fileResolver.resolveFiles(realPath + File.separator + "FileResolverTest.class[2]", realBase);
    assertNotNull("Should not be null", files);
    assertTrue("files should have some files", files.size() > 0);
    assertTrue("files contain this class", files.toString().contains(this.getClass().getSimpleName()));
  }

  @Test
  public void denyDirectoryPathTest() {
    List<File> files = fileResolver.resolveFiles(realPath, realBase);
    assertNotNull("Should not be null", files);
    assertTrue("files should NOT have some files", files.isEmpty());

    List<ILoggingEvent> logsList = listAppender.list;
    assertTrue(logsList.get(0).getMessage().startsWith("Cannot include a directory.  Use wildcards to include the contents.  Path="));
    assertEquals("Severity should be WARN.", Level.ERROR, logsList.get(0).getLevel());
  }

  @Test
  public void allowDirectoryPathTest() {
    String testDirectory = PathUtil.appendPaths(realPath, "rules");
    FileResolver fileResolverAllow = new FileResolver(realBase, true);
    List<File> files = fileResolverAllow.resolveFiles(testDirectory, realBase);
    assertNotNull("Should not be null", files);
    assertTrue("files should have some files", files.size() > 0);

    List<ILoggingEvent> logsList = listAppender.list;
    assertTrue(logsList.get(0).getMessage().startsWith("Including a directory is not recommended.  Instead, use wildcards.  Path="));
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(0).getLevel());
  }
}
