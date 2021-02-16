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

package com.adobe.aem.dot.httpd.core.parser;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.parser.ConfigurationViolations;
import com.adobe.aem.dot.httpd.core.model.Directive;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.adobe.aem.dot.httpd.core.model.Section;
import com.adobe.aem.dot.httpd.core.model.SectionType;
import com.adobe.aem.dot.httpd.core.model.VirtualHost;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpdConfigurationParserTest {
  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(HttpdConfigurationParser.class);
  }

  @Test
  public void shouldNormalizeMultilineConfig() throws IOException, URISyntaxException {
    HttpdConfigurationParser parser = new HttpdConfigurationParser("/");
    URL multilineInputURL = HttpdConfigurationParserTest.class.getResource("basic_multiline_httpd.conf");

    List<ConfigurationLine> configLines = parser.normalizeConfig(new File(multilineInputURL.toURI()), "/", true);

    // With comments and empty lines removed, the size of the list should be 4
    assertEquals("Expect the multiline directives to be parsed into single ConfigurationLines", 4,
            configLines.size());

    assertEquals("Expect the first multiline directive to be correct", "AllowOverride None",
            configLines.get(1).getContents());
    assertEquals("Expect the first multiline directive to know its line number", 3,
            configLines.get(1).getLineNumber());

    assertEquals("Expect the second multiline directive to be correct", "Require all granted",
            configLines.get(2).getContents());
    assertEquals("Expect the second multiline directive to know its line number", 8,
            configLines.get(2).getLineNumber());
  }

  @Test
  public void shouldParseBasicHttpdConf() throws URISyntaxException, IOException {
    URL inputURL = HttpdConfigurationParserTest.class.getResource("basic_httpd.conf");

    HttpdConfigurationParser parser = new HttpdConfigurationParser("/");
    ConfigurationParseResults<HttpdConfiguration> results = parser.parseConfiguration(new File(inputURL.toURI()));
    HttpdConfiguration httpdConfiguration = results.getConfiguration();

    // Count the top level directives
    assertEquals("Expect the total number of top level directives to be correct", 15, httpdConfiguration.getDirectives().size());

    // First directive
    Directive first = httpdConfiguration.getDirectives().get(0);
    assertEquals("Expect the 1st directive to be parsed", "ServerRoot", first.getName());
    assertEquals("Expect the 1st directive's arguments to be parsed", "/etc/httpd", first.getArguments().get(0));
    assertEquals("Expect the 1st directive's arguments to be parsed", 1, first.getArguments().size());
    assertEquals("Expect the 1st directive to know it's origin line number", 31, first.getConfigurationSource().getLineNumber());
    assertTrue("Expect the 1st directive to know it's origin file name", first.getConfigurationSource().getFileName().endsWith("basic_httpd.conf"));

    // 2nd directive
    Directive second = httpdConfiguration.getDirectives().get(1);
    assertEquals("Expect the 2nd directive to be parsed", "Listen", second.getName());
    assertEquals("Expect the 2nd directive's arguments to be parsed", "80", second.getArguments().get(0));
    assertEquals("Expect the 2nd directive's arguments to be parsed", 1, second.getArguments().size());
    assertEquals("Expect the 2nd directive to know it's origin line number", 42, second.getConfigurationSource().getLineNumber());

    // last directive
    Directive last = httpdConfiguration.getDirectives().get(httpdConfiguration.getDirectives().size() - 1);
    assertEquals("Expect the last directive to be parsed", "EnableSendfile", last.getName());
    assertEquals("Expect the last directive's arguments to be parsed", "on", last.getArguments().get(0));
    assertEquals("Expect the last directive's arguments to be parsed", 1, last.getArguments().size());
    assertEquals("Expect the last directive to know it's origin line number", 282, last.getConfigurationSource().getLineNumber());

    // Sections
    assertEquals("Expect the number of sections to be correct", 5, httpdConfiguration.getSections().size());

    // First section
    Section firstSection = httpdConfiguration.getSections().get(0);
    assertEquals("First section should be a Directory section", SectionType.DIRECTORY, firstSection.getType());

    // 4th section
    Section fourthSection = httpdConfiguration.getSections().get(3);
    assertEquals("Fourth section should be a Files section", SectionType.FILES, fourthSection.getType());
    assertEquals("Fourth section should know its line number", 171, fourthSection.getConfigurationSource().getLineNumber());
  }

  @Test
  public void shouldFollowIncludesDuringNormalize() throws URISyntaxException, IOException {
    HttpdConfigurationParser parser = new HttpdConfigurationParser("/");
    File httpdFile = new File(HttpdConfigurationParserTest.class.getResource("httpd_with_includes.conf").toURI());

    List<ConfigurationLine> configLines = parser.normalizeConfig(httpdFile, httpdFile.getParentFile().getAbsolutePath(),
            false);

    assertEquals("Expect a total of 165 configuration lines", 165, configLines.size());
  }

  @Test
  public void windowsBadSymLinkNoFileTest() throws URISyntaxException, IOException {
    if (SystemUtils.IS_OS_WINDOWS) {
      HttpdConfigurationParser parser = new HttpdConfigurationParser("/");
      File httpdFile = new File(HttpdConfigurationParserTest.class.getResource("httpd_bad_windows_includes.conf").toURI());

      String parentPath = httpdFile.getParentFile().getAbsolutePath();
      try {
        parser.normalizeConfig(httpdFile, parentPath, false);
        fail("FileNotFoundException Exception should have been thrown.");
      } catch(FileNotFoundException ignore) {
      }

      List<ILoggingEvent> logsList = listAppender.list;
      assertEquals("WINDOWS: Sym Link \"{}\" did not resolve correctly. Trying \"{}\"",
              logsList.get(0).getMessage());
      assertEquals("WINDOWS: Sym Link could not be resolved. Verify the creation of symbolic links. Symlink=\"{}\" Line=\"{}\"",
              logsList.get(1).getMessage());
      assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(1).getLevel());
    }
  }

  @Test
  public void shouldParseConfigWithIncludesAndVirtualHosts() throws URISyntaxException, IOException {
    HttpdConfigurationParser parser = new HttpdConfigurationParser("/");
    File httpdFile = new File(HttpdConfigurationParserTest.class.getResource("httpd_with_includes.conf").toURI());

    // Parse the config files into an HttpConfiguration object
    ConfigurationParseResults<HttpdConfiguration> results = parser.parseConfiguration(httpdFile);
    HttpdConfiguration httpdConfiguration = results.getConfiguration();

    assertEquals("Expect the total number of top level directives to be correct", 42,
            httpdConfiguration.getDirectives().size());
    assertEquals("Expect the total number of VirtualHosts to be correct", 2, httpdConfiguration.getVirtualHosts().size());
    assertEquals("Expect the total number of Sections to be correct", 5, httpdConfiguration.getSections().size());

    // Publish VirtualHost
    VirtualHost vhost1 = httpdConfiguration.getVirtualHosts().get(0);
    assertEquals("Expect the total number of directives in the pub vhost to be correct", 36, vhost1.getDirectives().size());
    assertEquals("Expect the vhost to know it's line number", 5, vhost1.getConfigurationSource().getLineNumber());
    assertTrue("Expect the vhost to know it's origin filename", vhost1.getConfigurationSource().getFileName().endsWith("mysite_publish.vhost"));
  }

  @Test(expected = FileNotFoundException.class)
  public void badConfigToNormalizeTest() throws IOException {
    HttpdConfigurationParser parser = new HttpdConfigurationParser("/");
    List<ConfigurationLine> configLines = parser.normalizeConfig(
            new File("/does/not/exist"), "/this/is/not/here", true);
    assertTrue("Should be empty", configLines.isEmpty());

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Configuration File ({}) was not found.", logsList.get(0).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());

    configLines = parser.normalizeConfig(new File("/does/$not/exist"), "/this/is/not/here", true);
    assertTrue("Should be empty", configLines.isEmpty());
    assertEquals("Configuration File ({}) was not found. Appears to contain a misrepresented or unset environment variable.",
            logsList.get(1).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(1).getLevel());

    // optional = false.  That means include (file) MUST exist.
    parser.normalizeConfig(
            new File("/does/not/exist"), "/this/is/not/here", false);
  }

  /*
  The Include directive, that doesn't include a file, throws a violation.  The same scenario but with a
  IncludeOptional directive, should move on quietly.
  This test is for both scenarios with a direct include, and one with a wildcard (*).
   */
  @Test
  public void noFileToIncludeTest() throws URISyntaxException, IOException {
    HttpdConfigurationParser parser = new HttpdConfigurationParser("/");
    String[] confPaths = new String[]{
            "no_file_to_include/httpd_with_failed_include.conf",            // Direct include
            "no_file_to_include/httpd_with_failed_wildcard_include.conf"    // Wildcard include
    };

    for (String nextTestPath : confPaths) {
      // Clear violations and logs for each file.
      ConfigurationViolations.clearViolations();
      listAppender = AssertHelper.getLogAppender(HttpdConfigurationParser.class);

      File httpdFile = new File(HttpdConfigurationParserTest.class.getResource(nextTestPath).toURI());

      String errMsg = "Include directive must include existing files.  Check path, or use IncludeOptional.";

      // Parse the config files into an HttpConfiguration object
      parser.parseConfiguration(httpdFile);

      List<ILoggingEvent> logsList = listAppender.list;
      assertEquals(1, logsList.size());
      assertTrue("Include error should have been logged.",
              logsList.get(0).getMessage().startsWith(errMsg));
      assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());

      // Check violation list
      assertEquals(1, ConfigurationViolations.getViolations().size());
      Violation violation = ConfigurationViolations.getViolations().get(0);
      assertNotNull(violation);
      assertTrue(violation.getContext().startsWith(errMsg));
      assertEquals(Severity.MINOR, violation.getAnalyzerRule().getSeverity());
    }
  }

  @Test
  public void loadInfiniteIncludeConfigTest() throws URISyntaxException, IOException {
    HttpdConfigurationParser parser = new HttpdConfigurationParser("/");
    String infinitePath = "infinite_include/httpd_with_infinite_includes.conf";
    File httpdFile = new File(HttpdConfigurationParserTest.class.getResource(infinitePath).toURI());

    // Parse the config files into an HttpConfiguration object
    try {
      parser.parseConfiguration(httpdFile);
      fail("Finite include should be caught.");
    } catch(IOException ioEx) {
      assertTrue(ioEx.getMessage().contains("Maximum number of Httpd includes"));
    }

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Include error should have been logged.",
            "Maximum number of Httpd includes (50) encountered.  Check for circular file includes or raise the \"dot.maximum.configuration.include.depth\" property.",
            logsList.get(0).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());
  }

  @Test
  public void loadLargeIncludeConfigTest() throws URISyntaxException {
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(
            ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    Level originalLevel = root.getLevel();
    root.setLevel(Level.WARN);

    HttpdConfigurationParser parser = new HttpdConfigurationParser("/");
    String largePath = "httpd_with_large_includes.conf";
    File httpdFile = new File(HttpdConfigurationParserTest.class.getResource(largePath).toURI());
    HttpdConfiguration httpdConfiguration = null;
    try {
      ConfigurationParseResults<HttpdConfiguration> results = parser.parseConfiguration(httpdFile);
      httpdConfiguration = results.getConfiguration();
      fail("Finite include should be caught.");
    } catch(IOException ioEx) {
      assertTrue(ioEx.getMessage().contains("Maximum number of Httpd configuration lines"));
    }
    assertNull(httpdConfiguration);

    root.setLevel(originalLevel);

    // Many (many!) incorrect SymLink INFO's are logged on Windows.  So evaluate the last log entry.
    List<ILoggingEvent> logsList = listAppender.list;
    ILoggingEvent lastEvent = logsList.get((logsList).size() - 1);
    assertEquals("Include error should have been logged.",
            "Maximum number of Httpd configuration lines (1,000,000) encountered.  Check for circular file includes or raise the \"dot.maximum.configuration.lines\" property.",
            lastEvent.getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, lastEvent.getLevel());
  }
}
