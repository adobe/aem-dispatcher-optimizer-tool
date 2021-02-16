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

package com.adobe.aem.dot.dispatcher.core.parser;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.model.Farm;
import com.adobe.aem.dot.dispatcher.core.resolver.IncludeResolver;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationReaderTest {
  private ListAppender<ILoggingEvent> listAppender;
  private ListAppender<ILoggingEvent> listAppender2;
  private String classPath;
  private static DispatcherConfigTestHelper helper;

  @BeforeClass
  public static void beforeClass() {
    helper = new DispatcherConfigTestHelper();
  }

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(ConfigurationReader.class);
    listAppender2 = AssertHelper.getLogAppender(ConfigurationCleaner.class);

    classPath = this.getClass().getResource("").getPath();
  }

  @Test
  public void nextIntegerTest() {
    String configStr = "/delay sure extra\n";  // Some extra to make it act like a real dispatcher.any line.
    try {
      IncludeResolver resolver = new IncludeResolver(configStr, classPath, null);
      List<ConfigurationLine> config = resolver.resolve();
      ConfigurationReader reader = new ConfigurationReader(config);
      ConfigurationValue<String> label = reader.next(false);
      assertEquals("should equal label", "/delay", label.getValue());
      ConfigurationValue<Integer> nextInt = reader.nextInteger(100);
      assertEquals("should equal default", Integer.valueOf(100), nextInt.getValue());

      List<ILoggingEvent> logsList = listAppender.list;
      assertTrue("Parsing error should have been logged.",
              logsList.get(0).getMessage().startsWith("Skipping unknown integer value. Value=\"sure\""));
      assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());
    } catch(IOException | ConfigurationSyntaxException ex) {
      Assert.fail("Failure: " + ex.getLocalizedMessage());
    }
  }

  @Test
  public void nextPreserveQuotesTest() {
    String configStr = "/quote \"quoted string\" extra";

    try {
      IncludeResolver resolver = new IncludeResolver(configStr, classPath, null);
      List<ConfigurationLine> config = resolver.resolve();
      ConfigurationReader reader = new ConfigurationReader(config);
      ConfigurationValue<String> label = reader.next(false);
      assertEquals("should equal label", "/quote", label.getValue());
      ConfigurationValue<String> quotedString = reader.next(true);
      assertEquals("should equal default", "\"quoted string\"", quotedString.getValue());
    } catch(IOException | ConfigurationSyntaxException ex) {
      Assert.fail("Failure: " + ex.getLocalizedMessage());
    }
  }

  @Test
  public void noEndingBraceTest() throws ConfigurationException {
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            "no_ending_brace/" + DispatcherConstants.DISPATCHER_ANY);

    ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
    DispatcherConfiguration config = results.getConfiguration();

    List<ILoggingEvent> logsList = listAppender2.list;
    assertTrue("Mismatched brace",
            logsList.get(0).getMessage().startsWith("Unclosed brace encountered in file"));
    String path = FilenameUtils.separatorsToSystem("dispatcher\\core\\parser\\no_ending_brace\\dispatcher.any");
    assertTrue("Contains correct folder", logsList.get(0).getFormattedMessage().contains(path));
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(0).getLevel());

    assertNotNull("Config should not be null", config);
    assertNotNull("Config's farms should not be null", config.getFarms());
    assertEquals("Should be 1 farm", 1, config.getFarms().size());

    Farm farm = config.getFarms().get(0).getValue();
    assertTrue("Should be an author farm", farm.isAuthorFarm());
    assertFalse("Should be an author farm", farm.isPublishFarm());

    List<ConfigurationValue<String>> clientHeaders = farm.getClientHeaders();
    assertNotNull("Should be some client headers", clientHeaders);
    assertEquals("Should be 2 client headers.", 2, clientHeaders.size());
  }
}
