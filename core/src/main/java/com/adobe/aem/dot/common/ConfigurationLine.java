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

package com.adobe.aem.dot.common;

import lombok.Getter;

/**
 * Represents a single line in a configuration file.
 */
@Getter
public class ConfigurationLine extends ConfigurationSource {
  private String contents;

  public ConfigurationLine(String contents, String fileName, int lineNumber) {
    super(fileName, lineNumber);
    this.contents = contents;
  }

  public ConfigurationLine(String contents, String fileName, int lineNumber, String includedFrom) {
    super(fileName, lineNumber, includedFrom);
    this.contents = contents;
  }

  public void setContents(String contents) {
    this.contents = contents;
  }

  public ConfigurationSource getConfigurationSource() {
    return new ConfigurationSource(this.getFileName(), this.getLineNumber(), this.includedFrom);
  }

  /**
   * Determine if this object's contents contains actual configuration data.
   * @return true if and only if the contents of this object are not empty and not a comment.
   */
  public boolean hasConfigurationContents() {
    return this.contents != null && !this.contents.isEmpty() && !this.contents.startsWith("#");
  }

}
