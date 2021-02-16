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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationValueTest {
  private static final String fileName = "hello.txt";
  private static final int lineNumber = 12;
  private static final String includeName = "included.txt";
  private ConfigurationValue<String> stringValue;

  @Before
  public void before() {
    stringValue = new ConfigurationValue<>("value1", fileName, lineNumber, includeName);
  }

  @Test
  public void basicsCheck() {
    assertNotEquals("Try the hashcode()", "hello", stringValue.hashCode());
  }

  @Test
  public void defaultsCheck() {
    ConfigurationValueDefaults defaults = new ConfigurationValueDefaults();
    assertNotNull(defaults);
    assertEquals("boolean false", Boolean.FALSE, ConfigurationValueDefaults.DEFAULT_BOOLEAN_FALSE.getValue());
    assertEquals("int zero", Integer.valueOf(0), ConfigurationValueDefaults.DEFAULT_INT_ZERO.getValue());
    assertEquals("int one", Integer.valueOf(1), ConfigurationValueDefaults.DEFAULT_INT_ONE.getValue());
  }

  @Test
  public void nullCheck() {
    ConfigurationValue<String> nullString = new ConfigurationValue<>(null);
    assertTrue(nullString.toString().isEmpty());
  }
}
