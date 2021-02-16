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
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.util.FeedbackProcessor;
import com.adobe.aem.dot.common.util.MatchesBuilder;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationReader;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_ENCODE_VALUE;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_HEADER_VALUE;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_VALUE_FILE_NAME;

/**
 * Represents a SessionManagement, a section with a Farm configuration.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class SessionManagement extends LabeledConfigurationValue {
  private ConfigurationValue<String> directory; // Mandatory
  private ConfigurationValue<String> encode = new ConfigurationValue<>(DEFAULT_ENCODE_VALUE, DEFAULT_VALUE_FILE_NAME, 0);
  private ConfigurationValue<String> header = new ConfigurationValue<>(DEFAULT_HEADER_VALUE, DEFAULT_VALUE_FILE_NAME, 0);
  private ConfigurationValue<Integer> timeout = new ConfigurationValue<>(800, DEFAULT_VALUE_FILE_NAME, 0);

  private static final Logger logger = LoggerFactory.getLogger(SessionManagement.class);

  private SessionManagement() {  }

  Logger getLogger() {
    return logger;
  }

  String getSimpleClassName() {
    return getClass().getSimpleName();
  }

  public String getDirectory() {
    return this.directory == null ? null : this.directory.getValue();
  }

  public void setDirectory(ConfigurationValue<String> directory) {
    this.directory = directory;
  }

  public ConfigurationValue<String> getEncode() {
    return this.encode;
  }

  public void setEncode(ConfigurationValue<String> encode) {
    this.encode = encode;
  }

  public ConfigurationValue<String> getHeader() {
    return this.header;  // Cannot be null.
  }

  public void setHeader(ConfigurationValue<String> header) {
    this.header = header;
  }

  public ConfigurationValue<Integer> getTimeout() {
    return this.timeout;  // Cannot be null.
  }

  public void setTimeout(ConfigurationValue<Integer> timeout) {
    this.timeout = timeout;
  }

  static SessionManagement parseSessionManagement(ConfigurationReader reader)
          throws ConfigurationSyntaxException {
    SessionManagement sessionManagement = new SessionManagement();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /sessionManagement block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MINOR);
      return new SessionManagement();
    }

    ConfigurationValue<String> parentValue = reader.next(); // Advance the reader's pointer passed the "{" marker.
    ConfigurationValue<String> token;
    boolean hasMoreToProcess = true;
    while (reader.hasNext() && hasMoreToProcess) {
      ConfigurationValue<String> currentToken = reader.next();
      switch (currentToken.getValue()) {
        case "/directory":
          logger.trace("SessionManagement > directory");
          token = reader.next();
          sessionManagement.setDirectory(token);
          break;
        case "/encode":
          // Parse file
          logger.trace("SessionManagement > encode");
          token = reader.next();
          sessionManagement.setEncode(token);
          break;
        case "/header":
          // Parse file
          logger.trace("SessionManagement > header");
          token = reader.next();
          sessionManagement.setHeader(token);
          break;
        case "/timeout":
          // Parse file
          logger.trace("SessionManagement > timeout");
          ConfigurationValue<Integer> tokenInt = reader.nextInteger(800);
          sessionManagement.setTimeout(tokenInt);
          break;
        case "}":
          logger.trace("end /sessionmanagement section");
          hasMoreToProcess = false;
          if (sessionManagement.getDirectory() == null) {
            FeedbackProcessor.error(logger,"SessionManagement is missing mandatory 'directory' value.",
                    currentToken, Severity.MINOR);
          }
          processDefaultValues(sessionManagement, parentValue);
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /sessionmanagement level token.  Token=\"{}\".",
                  currentToken, Severity.MINOR);
          reader.advancePastThisElement();
      }
    }

    return sessionManagement;
  }
  /**
   * Consider two rules equal if both the Glob and Type are the same (disregard Name).
   * @param o an object to compare
   * @return true if the Rules should be considered equal
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    SessionManagement sm = (SessionManagement) o;

    return new MatchesBuilder()
            .append(getDirectory(), sm.getDirectory())
            .append(getEncode(), sm.getEncode())
            .append(getHeader(), sm.getHeader())
            .append(getTimeout(), sm.getTimeout())
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(13, 21)
            .append(getDirectory())
            .append(getEncode())
            .append(getHeader())
            .append(getTimeout())
            .toHashCode();
  }

  /**
   * Process default values and set the source information to its parent's values
   * @param management The parent SessionManagement.
   */
  private static void processDefaultValues(SessionManagement management, final ConfigurationSource parentSource) {
    if (management.getEncode().isUsingDefault()) {
      management.setEncode(new ConfigurationValue<>(management.getEncode().getValue(), parentSource));
    }
    if (management.getHeader().isUsingDefault()) {
      management.setHeader(new ConfigurationValue<>(management.getHeader().getValue(), parentSource));
    }
    if (management.getTimeout().isUsingDefault()) {
      management.setTimeout(new ConfigurationValue<>(management.getTimeout().getValue(), parentSource));
    }
  }
}
