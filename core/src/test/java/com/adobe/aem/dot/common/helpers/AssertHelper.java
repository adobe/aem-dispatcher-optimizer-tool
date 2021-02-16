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

package com.adobe.aem.dot.common.helpers;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AssertHelper {

  /**
   * Compare a string to the contents of a file.
   * @param path The path to the Json file to extract the contents from.
   * @param testConfig The string to compare to the file contents.
  */
  public static void assertJsonFileEquals(String path, String testConfig) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      File goldFile = FileUtils.getFile(path);
      String gold = FileUtils.readFileToString(goldFile, "utf-8");

      // Format isn't exactly the same.  Remove spaces and newlines and compare.
      gold = gold.replaceAll(" ", "").replaceAll(System.getProperty("line.separator"), "");
      testConfig = testConfig.replaceAll(" ", "").replaceAll(System.getProperty("line.separator"), "");

      JsonNode goldJson = mapper.readTree(gold);
      JsonNode testJson = mapper.readTree(testConfig);
      assertEquals("File contents do not match provided string.", goldJson, testJson);
    } catch(IOException ioEx) {
        fail("Error while reading file " + path + " : " + ioEx.getClass().getName() + " : " +
                     ioEx.getLocalizedMessage());
    }
  }

  /**
   * Assert the values of the configuration value and its source information.
   * @param cv The ConfigurationValue
   * @param anyPath The path to its ANY file
   * @param value The expected value of the ConfigurationValue
   * @param lineNumber The expected line number
   * @param anyName The expected ANY file name
   * @param includedFrom The name of the file that included the ANY file
   */
  public static void assertValues(ConfigurationValue<?> cv, String anyPath, Object value, int lineNumber,
                                  String anyName, String includedFrom) {
    assertEquals("Should be good value", value, cv.getValue());
    assertTrue("correct any file",
            FilenameUtils.separatorsToSystem(anyPath + "/" + anyName).endsWith(cv.getFileName()));
    assertEquals("correct line number", lineNumber, cv.getLineNumber());
    assertEquals("correct included from", includedFrom, cv.getIncludedFrom());
    assertNotEquals("correct included from", "hello", cv);
  }

  /**
   * Create a log appender, and attach it to a class' logger.  Then use that appender to assert whether log entries
   * were in fact created.
   * Assert log entries as follows:
   *    List<ILoggingEvent> logsList = listAppender.list;
   *    assertEquals("The exception message (\"${\"). {}", logsList.get(0).getMessage());
   *    assertEquals("Severity should be WARN.", Level.WARN, logsList.get(0).getLevel());
   *
   * @param clazz Class of the logger to monitor
   * @return A ListAppender (attached to the logger)
   */
  public static ListAppender<ILoggingEvent> getLogAppender(Class<?> clazz) {
    Logger testLogger = (Logger) LoggerFactory.getLogger(clazz);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    testLogger.addAppender(listAppender);

    return listAppender;
  }
}
