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

package com.adobe.aem.dot.httpd.core.model;

import com.adobe.aem.dot.common.ConfigurationSource;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DirectiveTest {

  @Test
  public void testHashCode() {
    List<String> arguments = new ArrayList<>();
    arguments.add("HereIsOne");
    Directive directive = new Directive("name", arguments);
    Directive directive2 = new Directive("name", arguments, new ConfigurationSource());
    assertEquals(directive2.hashCode(), directive.hashCode());
  }

  @Test
  public void testEquals() {
    List<String> arguments = new ArrayList<>();
    arguments.add("HereIsOne");
    Directive directive = new Directive("name", arguments);
    Directive directive2 = new Directive("name", arguments, new ConfigurationSource());
    assertEquals(directive, directive);
    assertEquals(directive2, directive);
    assertFalse(directive == null);
  }
}
