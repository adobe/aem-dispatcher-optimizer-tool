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
import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConfigurationSyntaxExceptionTest {
  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(ConfigurationSource.class);
  }

  @Test
  public void badFileName() {
    ConfigurationSource source = new ConfigurationSource(null, 12);
    assertEquals("Check Line Number", 12, source.getLineNumber());
    assertNull("Check Include", source.getIncludedFrom());

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Null or empty filename value.", logsList.get(0).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());
  }

  @Test
  public void badLineWithInclude() {
    ConfigurationSource source = new ConfigurationSource("disp.any", -1, "included.any");
    assertEquals("Check Include", "disp.any", source.getFileName());
    assertEquals("Check Include", "included.any", source.getIncludedFrom());

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Invalid line number value.  File=\"{}\"", logsList.get(0).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());
  }

  @Test
  public void basicsTest() {
    ConfigurationSyntaxException csEx = new ConfigurationSyntaxException("Error message", "disp.any", 123);
    assertEquals("Error message", "Error message", csEx.getMessage());
    assertEquals("Line Number", 123, csEx.getLineNumber());
    assertEquals("File Name", "disp.any", csEx.getFilename());
    assertNull("Include From", csEx.getIncludedFrom());
    assertEquals("toString", "ConfigurationSyntaxException: Error message File=\"disp.any\" Line=123",
            csEx.toString());

    csEx = new ConfigurationSyntaxException("Error message again", "disp2.any", 1234, "include.any");
    assertEquals("Error message", "Error message again", csEx.getMessage());
    assertEquals("Line Number", 1234, csEx.getLineNumber());
    assertEquals("File Name", "disp2.any", csEx.getFilename());
    assertEquals("Include Name", "include.any", csEx.getIncludedFrom());
    assertEquals("toString",
            "ConfigurationSyntaxException: Error message again File=\"disp2.any\" Line=1234 Included From=\"include.any\"",
            csEx.toString());
  }
}
