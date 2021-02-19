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

import com.adobe.aem.dot.common.util.FeedbackProcessor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ConfigurationSourceTest {
  private static final String fileName = "hello.txt";
  private static final int lineNumber = 12;
  private static final String includeName = "included.txt";

  @Test
  public void basicsCheck() {
    ConfigurationSource cvs1 = new ConfigurationSource();
    assertNull("should be null", cvs1.getFileName());
    assertEquals("should not be set", -1, cvs1.getLineNumber());
    assertNull("should be null", cvs1.getIncludedFrom());

    ConfigurationSource cvs2 = new ConfigurationSource(fileName, lineNumber, includeName);

    assertEquals("should be what was just set", includeName, cvs2.getIncludedFrom());
    assertEquals("should be what was just set", lineNumber, cvs2.getLineNumber());
    assertEquals("should be what was just set", fileName, cvs2.getFileName());

    assertNotEquals("should be the same", cvs1, cvs2);
  }

  @Test
  public void badConstruction1() {
    ConfigurationSource cs = new ConfigurationSource(fileName, -1);
    // Providing negative line numbers no longer throws an exception
    assertNotNull(cs);
  }

  @Test
  public void badConstruction2() {
    ConfigurationSource cs = new ConfigurationSource(null, lineNumber);
    // Providing a null filename no longer throws an exception
    assertNotNull(cs);
  }

  @Test
  public void equalsTest() {
    ConfigurationSource cs1 = new ConfigurationSource(null, lineNumber);
    ConfigurationSource cs2 = new ConfigurationSource("filename.txt", lineNumber + 1);

    assertEquals(cs1, cs1);
    assertNotEquals(cs1, cs2);
    assertNotEquals(cs1, null);
    assertNotEquals(cs1, new FeedbackProcessor());
  }
}
