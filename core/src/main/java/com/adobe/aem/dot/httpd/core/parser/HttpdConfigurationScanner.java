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

package com.adobe.aem.dot.httpd.core.parser;

import com.adobe.aem.dot.common.ConfigurationScanner;

/**
 * Similar interface to java.util.Scanner, but provides Apache Httpd configuration specific enhancements.
 */
public class HttpdConfigurationScanner extends ConfigurationScanner {

  public HttpdConfigurationScanner(String configuration) {
    super(configuration);
  }

  /**
   * Httpd config files have different rules for what constitutes the end of a token (whitespace only).
   * @param character the character to test
   * @return true, if and only if the provided character is whitespace or the beginning of the next token
   */
  @Override
  protected boolean isWhitespaceOrBeginningOfNextToken(char character) {
    return Character.isWhitespace(character);
  }
}
