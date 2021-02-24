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

import com.adobe.aem.dot.common.ConfigurationSource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_VALUE_FILE_NAME;

/**
 * The <code>ConfigurationValue</code> class encapsulates a value from the Configuration.  It stores any type
 * of value along with the filename and the line number from where the value was extracted.
 * @param <E> type of the configuration value to store
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class ConfigurationValue<E> extends ConfigurationSource {
  E value;

  // Hide default constructor
  private ConfigurationValue() { }

  public ConfigurationValue(E value) {
    super();
    this.value = value;
  }

  public ConfigurationValue(E value, String fileName, int lineNumber) {
    super(fileName, lineNumber);

    this.value = value;
  }

  public ConfigurationValue(E value, ConfigurationSource source) {
    super(source.getFileName(), source.getLineNumber(), source.getIncludedFrom());

    this.value = value;
  }

  public ConfigurationValue(E value, String fileName, int lineNumber, String includedFrom) {
    super(fileName, lineNumber, includedFrom);

    this.value = value;
  }

  public void setValue(E value) {
    this.value = value;
  }

  @JsonIgnore
  public String getFileName() {
    return this.fileName;
  }

  @JsonIgnore
  public int getLineNumber() {
    return this.lineNumber;
  }

  @JsonIgnore
  public String getIncludedFrom() {
    return this.includedFrom;
  }

  public String toString() {
    return value == null ? "" : value.toString();
  }

  /**
   * Create a new ConfigurationSource based on the contents of this ConfigurationValue.
   * @return A new ConfigurationSource object
   */
  public ConfigurationSource getConfigurationSource() {
    return new ConfigurationSource(this.getFileName(), this.getLineNumber(), this.getIncludedFrom());
  }

  boolean isUsingDefault() {
    return getFileName().equals(DEFAULT_VALUE_FILE_NAME);
  }
}
