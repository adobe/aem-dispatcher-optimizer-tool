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

import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CheckResultTest {

  @Test
  public void simpleCheck() throws ConfigurationSyntaxException {
    CheckResult result = new CheckResult(false, new ConfigurationSource("test.any", 12));

    assertFalse("created with false", result.isPassed());
    assertEquals("not set yet", 0, result.getDetails().size());

    result.setPassed(true);
    assertTrue("set to true", result.isPassed());
    result.setPassed(false);
    assertFalse("set to true", result.isPassed());

    List<String> list = new ArrayList<>();
    list.add("hi");
    result.setDetails(list);
    assertEquals("should be 1", 1, result.getDetails().size());

    list.add("there");
    result.setDetails(list);
    assertEquals("should be 2", 2, result.getDetails().size());

    list.clear();
    list.add("bye");
    result.setDetails(list);
    assertEquals("should be 1", 1, result.getDetails().size());

    result = new CheckResult(true, list, new ConfigurationSource("test.any", 1, "dispatcher.any"));
    assertTrue("set to true", result.isPassed());
    assertEquals("should be 1", 1, result.getDetails().size());
  }
}
