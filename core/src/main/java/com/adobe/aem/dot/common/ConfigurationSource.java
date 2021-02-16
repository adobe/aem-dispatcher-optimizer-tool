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

import com.adobe.aem.dot.common.util.MatchesBuilder;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ConfigurationValueSource</code> class encapsulates the filename and the line number of a
 * configuration source.  Other classes can expand it to have that information tagged with its data.
 */
@Getter
public class ConfigurationSource {
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationSource.class);

  protected String fileName = null;
  protected int lineNumber = -1;
  protected String includedFrom = null;

  public ConfigurationSource() {}

  public ConfigurationSource(String fileName, int lineNumber) {
    constructProperties(fileName, lineNumber, null);
  }

  public ConfigurationSource(String fileName, int lineNumber, String includedFrom) {
    constructProperties(fileName, lineNumber, includedFrom);
  }

  private void constructProperties(String fileName, int lineNumber, String includedFrom) {
    this.fileName = fileName;
    this.lineNumber = lineNumber;
    if (fileName != null && !fileName.endsWith(
            FilenameUtils.separatorsToSystem("\\" + DispatcherConstants.DISPATCHER_ANY))) {
      this.includedFrom = includedFrom;
    }
    if (StringUtils.isEmpty(fileName)) {
      logger.error("Null or empty filename value.");
    }
    if (lineNumber < 0) {
      logger.error("Invalid line number value.  File=\"{}\"", fileName == null ? "<unset>" : fileName);
    }
  }

  @JsonIgnore
  public String getFileName() {
    return fileName;
  }

  @JsonIgnore
  public int getLineNumber() {
    return lineNumber;
  }

  @JsonIgnore
  public String getIncludedFrom() {
    return includedFrom;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConfigurationSource value = (ConfigurationSource) o;

    return new MatchesBuilder()
                   .append(getFileName(), value.getFileName())
                   .append(getIncludedFrom(), value.getIncludedFrom())
                   .append(getLineNumber(), value.getLineNumber())
                   .isEquals();
  }

  /**
   * Return an identifier for this file including a relative path to the file.
   * @return a string identifying this file
   */
  public String getFileLocation() {
    return "dispatcher:" + this.getFileName();
  }
}
