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

package com.adobe.aem.dot.httpd.core;

import lombok.Getter;

/**
 * Represents the result of reading a single effective line from a configuration file. An "effective" line may span
 * more than one line in the source file, but it's contents are all part of a single directive.
 */
@Getter
public class ConfigFileEntryReadResult {
  private final String contents;
  private final int lineNumber;
  private final int totalNumberOfLinesRead;

  public ConfigFileEntryReadResult(String contents, int lineNumber, int totalNumberOfLinesRead) {
    this.contents = contents;
    this.lineNumber = lineNumber;
    this.totalNumberOfLinesRead = totalNumberOfLinesRead;
  }

  /**
   * Does this instance represent an Apache include directive?
   * @return true if and only if this is an include directive
   */
  public HttpdIncludeType isApacheIncludeDirective() {
    // Matches both Include and IncludeOptional
    if (contents == null) {
      return HttpdIncludeType.NONE;
    } else if (contents.startsWith("IncludeOptional")) {
      return HttpdIncludeType.OPTIONAL;
    } else if (contents.startsWith("Include")) {
      return HttpdIncludeType.INCLUDE;
    }

    return HttpdIncludeType.NONE;
  }
}
