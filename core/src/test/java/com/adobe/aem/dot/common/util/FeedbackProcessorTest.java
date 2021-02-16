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

package com.adobe.aem.dot.common.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.parser.ConfigurationViolations;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FeedbackProcessorTest {
  private Logger testLogger;
  private ListAppender<ILoggingEvent> listAppender;
  private ConfigurationValue<String> label1 = null;
  private ConfigurationValue<String> nullLabel = null;

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    testLogger = (Logger) LoggerFactory.getLogger(FeedbackProcessor.class);
    listAppender = AssertHelper.getLogAppender(FeedbackProcessor.class);

    label1 = new ConfigurationValue<>("test label", "filename", 1, "test");
    nullLabel = new ConfigurationValue<>(null, "nullFile", 4, "null2");

    assertNotNull(testLogger);
    ConfigurationViolations.clearViolations();
  }

  @Test
  public void testBadFormatting() {
    FeedbackProcessor.error(testLogger, "Just testing '{}' errors {1} {2}", label1, Severity.INFO);

    List<ILoggingEvent> logsList = listAppender.list;
    // Check that the 'unclosed' regex was logged - "regex(.*" - should be the first entry in the logs.
    assertEquals("Illegal format of 'message' used in FeedbackProcessor.  Message=\"{}\"",
            logsList.get(0).getMessage());
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(0).getLevel());
    assertEquals("Just testing {0} errors {1} {2} File=\"filename\" Number=1 Included From=\"test\"",
            logsList.get(1).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(1).getLevel());
  }

  @Test
  public void testBadFormatting2() {
    ConfigurationViolations.clearViolations();
    FeedbackProcessor.error(testLogger, "Just testing '{}' errors '{}'", label1, Severity.INFO);

    List<ILoggingEvent> logsList = listAppender.list;
    // Check that the 'unclosed' regex was logged - "regex(.*" - should be the first entry in the logs.
    assertEquals("Just testing {0} errors {0} File=\"filename\" Number=1 Included From=\"test\"",
            logsList.get(0).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());

    assertEquals(1, ConfigurationViolations.getViolations().size());
    ConfigurationViolations.clearViolations();
    assertEquals(0, ConfigurationViolations.getViolations().size());
  }

  @Test
  public void testNullInfo() {
    FeedbackProcessor.error(testLogger, "Just testing null value \"{}\".", null, null);
    assertEquals(0, ConfigurationViolations.getViolations().size());

    FeedbackProcessor.error(testLogger, "Just testing null label \"{}\" errors {1} {2}", nullLabel, null);
    assertEquals(0, ConfigurationViolations.getViolations().size());

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Just testing null value \"\".", logsList.get(0).getMessage());
    assertEquals("Illegal format of 'message' used in FeedbackProcessor.  Message=\"{}\"",
            logsList.get(1).getMessage());
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(1).getLevel());
    assertEquals("Just testing null label \"null\" errors {1} {2} File=\"nullFile\" Number=4 Included From=\"null2\"",
            logsList.get(2).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(2).getLevel());
  }

  @Test
  public void testWarnWithNull() {
    FeedbackProcessor.warn(testLogger, "testing null line \"{}\".", (ConfigurationLine) null, Severity.INFO);
    FeedbackProcessor.warn(testLogger, "testing null value \"{}\".", (ConfigurationValue<?>) null, Severity.INFO);

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("testing null line \"\".", logsList.get(0).getMessage());
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(0).getLevel());

    assertEquals("testing null value \"\".", logsList.get(1).getMessage());
    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(1).getLevel());
  }
}
