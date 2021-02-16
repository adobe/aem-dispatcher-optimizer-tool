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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PropertiesUtilTest {

  @Test
  public void getLongTest() {
    long def = PropertiesUtil.getLongProperty("NOT_THERE", 12120);
    assertEquals(def, 12120);

    // Not a long value.
    def = PropertiesUtil.getLongProperty("logging.level.com.adobe.aem.dot.dispatcher", 12123);
    assertEquals(def, 12123);
  }
}
