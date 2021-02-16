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

import org.apache.commons.lang3.StringUtils;

public class GoUrlUtil {

  private static final int MAX_KEY_LENGTH_FOR_LINK = 20;
  public static final String GO_URL_TEMPLATE = "https://www.adobe.com/go/aem_cmcq_%s_en";

  public static String getDocumentationGoURL(String id, String defaultValue) {
    if (StringUtils.isEmpty(id)) {
      return defaultValue;
    }

    id = id.contains(":") ? StringUtils.substringAfter(id, ":") : id;
    id = StringUtils.left(id, MAX_KEY_LENGTH_FOR_LINK);
    id = id.toLowerCase();
    id = id.replace(":", "-");
    id = id.replaceAll("\\s", "_");

    return String.format(GO_URL_TEMPLATE, id);
  }
}
