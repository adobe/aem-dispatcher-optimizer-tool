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
import com.adobe.aem.dot.common.ConfigurationScanner;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.httpd.core.model.Directive;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DirectoryFactoryTest {
  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(ConfigurationScanner.class);
  }

  @Test
  public void testGoodEndQuote() {
    ConfigurationLine line = new ConfigurationLine(
            "LogFormat \"%h %l %u %t \\\"%r\\\" %>s %b \\\"%{Referer}i\\\" \\\"%{User-Agent}i\\\"\" combined",
            "skylab.conf", 10);
    Directive logDirective = DirectiveFactory.getDirectiveInstance(line);
    assertNotNull(logDirective);
    assertEquals("LogFormat", logDirective.getName());
    assertEquals(2, logDirective.getArguments().size());
    assertEquals("%h %l %u %t \\\"%r\\\" %>s %b \\\"%{Referer}i\\\" \\\"%{User-Agent}i\\\"", logDirective.getArguments().get(0));
    assertEquals("combined", logDirective.getArguments().get(1));

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals(0, logsList.size());
  }

  @Test
  public void testNoEndQuote() {
    ConfigurationLine line = new ConfigurationLine(
            "LogFormat \"%h %l %u %t \\\"%r\\\" %>s %b \\\"%{Referer}i\\\" \\\"%{User-Agent}i\\\" combined",
            "skylab.conf", 10);
    Directive logDirective = DirectiveFactory.getDirectiveInstance(line);
    assertNotNull(logDirective);
    assertEquals("LogFormat", logDirective.getName());
    assertEquals(1, logDirective.getArguments().size());
    assertEquals("%h %l %u %t \\\"%r\\\" %>s %b \\\"%{Referer}i\\\" \\\"%{User-Agent}i\\\" combined", logDirective.getArguments().get(0));

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Closing quote never found. Assuming the rest of the line is intended string. Line=\"{}\"",
            logsList.get(0).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());
  }

  @Test
  public void testExtraQuote() {
    // Second argument will be combined" => since the token did not start with a quote, additional quotes are included
    // as part of the token.
    ConfigurationLine line = new ConfigurationLine(
            "LogFormat \"%h %l %u %t \\\"%r\\\" %>s %b \\\"%{Referer}i\\\" \\\"%{User-Agent}i\\\"\" combined\"",
            "skylab.conf", 10);
    Directive logDirective = DirectiveFactory.getDirectiveInstance(line);
    assertNotNull(logDirective);
    assertEquals("LogFormat", logDirective.getName());
    assertEquals(2, logDirective.getArguments().size());
    assertEquals("%h %l %u %t \\\"%r\\\" %>s %b \\\"%{Referer}i\\\" \\\"%{User-Agent}i\\\"", logDirective.getArguments().get(0));
    assertEquals("combined\"", logDirective.getArguments().get(1));

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals(0, logsList.size());
  }

  @Test
  public void testNullValues() {
    ConfigurationLine line = new ConfigurationLine(null,"skylab.conf", 11);
    assertNull(DirectiveFactory.getDirectiveInstance(line));
    assertNull(DirectiveFactory.getDirectiveInstance(null));
    assertNull(DirectiveFactory.getSectionInstance(null, null));
  }
}
