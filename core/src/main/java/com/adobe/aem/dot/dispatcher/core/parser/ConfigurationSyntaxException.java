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

package com.adobe.aem.dot.dispatcher.core.parser;

import org.apache.commons.lang3.StringUtils;

public class ConfigurationSyntaxException extends Exception {
  private String filename;
  private int lineNumber;
  private String includedFrom;

  public ConfigurationSyntaxException(String errorMessage, String filename, int lineNumber) {
    super(errorMessage);

    this.filename = filename;
    this.lineNumber = lineNumber;
    this.includedFrom = null;
  }

  public ConfigurationSyntaxException(String errorMessage, String filename, int lineNumber, String includedFrom) {
    super(errorMessage);

    this.filename = filename;
    this.lineNumber = lineNumber;
    this.includedFrom = includedFrom;
  }

  public String getFilename() {
    return filename;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getIncludedFrom() {
    return includedFrom;
  }

  public String toString() {
    StringBuilder asString = new StringBuilder();
    asString.append(this.getClass().getSimpleName())
            .append(": ")
            .append(getMessage());
    if (StringUtils.isNotEmpty(filename)) {
      asString.append(" File=\"")
              .append(filename)
              .append("\"");
    }
    if (lineNumber >= 1) {
      asString.append(" Line=")
              .append(lineNumber);
    }
    if (StringUtils.isNotEmpty(includedFrom)) {
      asString.append(" Included From=\"")
              .append(includedFrom)
              .append("\"");
    }

    return asString.toString();
  }
}
