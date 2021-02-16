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

package com.adobe.aem.dot.dispatcher.core.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReservedTokensUtilTest {

  @Test
  public void basicTest() {
    ReservedTokensUtil rtUtils = new ReservedTokensUtil();
    assertTrue("Is reserved", rtUtils.isReservedToken("rules"));  // Intentional static usage here.
    assertTrue("Is reserved", ReservedTokensUtil.isReservedToken("/retryDelay"));
    assertFalse("Is reserved", ReservedTokensUtil.isReservedToken("made_it_up"));
    assertFalse("Is reserved", ReservedTokensUtil.isReservedToken("/made_it_up"));
  }
}
