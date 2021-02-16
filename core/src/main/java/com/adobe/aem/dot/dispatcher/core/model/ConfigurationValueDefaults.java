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

public class ConfigurationValueDefaults {

  public static final String DEFAULT_VALUE_FILE_NAME = "default_value";

  static final ConfigurationValue<Boolean> DEFAULT_BOOLEAN_FALSE =
          new ConfigurationValue<>(false, DEFAULT_VALUE_FILE_NAME, 0);

  static final ConfigurationValue<Integer> DEFAULT_INT_ZERO =
          new ConfigurationValue<>(0, DEFAULT_VALUE_FILE_NAME, 0);
  static final ConfigurationValue<Integer> DEFAULT_INT_ONE =
          new ConfigurationValue<>(1, DEFAULT_VALUE_FILE_NAME, 0);

  public static final String DEFAULT_ENCODE_VALUE = "md5";
  public static final String DEFAULT_HEADER_VALUE = "HTTP:authorization";
  public static final String DEFAULT_MODE_VALUE = "0755";
}
