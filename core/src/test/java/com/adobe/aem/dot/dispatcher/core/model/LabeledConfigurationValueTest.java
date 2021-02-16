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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class LabeledConfigurationValueTest {
  private final Logger logger = LoggerFactory.getLogger(LabeledConfigurationValueTest.class);
  private ConfigurationValue<String> reserved = null;
  private ConfigurationValue<String> nullLabel = null;
  private ListAppender<ILoggingEvent> listAppender;

  private class TestValue extends LabeledConfigurationValue {
    public Logger getLogger() {
      return logger;
    }
    public String getSimpleClassName() {
      return "TestValue";
    }
  }

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(LabeledConfigurationValueTest.class);

    reserved = new ConfigurationValue<>("virtualhosts", "reserved", 3, "reserved2");
    nullLabel = new ConfigurationValue<>(null, "nullFile", 4, "null2");
  }

  @Test
  public void reservedNameCheck() {
    TestValue tvs = new TestValue();
    tvs.setLabel(null);   // Ensure it does not fall down.
    tvs.setLabel(nullLabel);   // Ensure it does not fall down.
    tvs.setLabel(reserved);

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Closed incorrectly line.",
            "A reserved token was used to label a TestValue indicating a section may have been closed " +
                    "incorrectly.  Label=\"virtualhosts\" File=\"reserved\" Number=3 Included From=\"reserved2\"",
            logsList.get(0).getMessage());
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(0).getLevel());
  }
}
