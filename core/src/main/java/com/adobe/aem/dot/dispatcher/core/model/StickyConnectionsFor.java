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

import java.util.Collections;
import java.util.List;

import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_BOOLEAN_FALSE;

/**
 * Represents a StickyConnectionsFor and StickyConnections, which is a section inside a Farm.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class StickyConnectionsFor extends LabeledConfigurationValue {
  private List<ConfigurationValue<String>> paths;
  private ConfigurationValue<Boolean> httpOnly = DEFAULT_BOOLEAN_FALSE;
  private ConfigurationValue<Boolean> secure = DEFAULT_BOOLEAN_FALSE;

  private static final Logger logger = LoggerFactory.getLogger(StickyConnectionsFor.class);

  private StickyConnectionsFor() {  }

  public StickyConnectionsFor(List<ConfigurationValue<String>> paths, ConfigurationValue<Boolean> httpOnly,
                              ConfigurationValue<Boolean> secure) {
    this.paths = paths;
    this.httpOnly = httpOnly;
    this.secure = secure;
  }

  Logger getLogger() {
    return logger;
  }

  String getSimpleClassName() {
    return getClass().getSimpleName();
  }

  static StickyConnectionsFor parseStickyConnectionsFor(ConfigurationReader reader)
          throws ConfigurationSyntaxException {
    StickyConnectionsFor stickyConnectionsFor = new StickyConnectionsFor();
    ConfigurationValue<Boolean> tokenBoolean;

    // Expect { to begin the block
    ConfigurationValue<String> parentValue = reader.next();
    if (!parentValue.getValue().equals("{")) {
      // Process path without a /paths section.
      logger.trace("StickyConnectionsFor > raw single path");
      stickyConnectionsFor.setPaths(Collections.singletonList(parentValue));
      processDefaultValues(stickyConnectionsFor, parentValue);
    } else {
      boolean hasMoreToProcess = true;
      while (reader.hasNext() && hasMoreToProcess) {
        ConfigurationValue<String> currentToken = reader.next();
        switch (currentToken.getValue()) {
          case "/paths":
            logger.trace("StickyConnectionsFor > paths");
            List<ConfigurationValue<String>> strings = reader.nextStringList();
            stickyConnectionsFor.setPaths(strings);
            break;
          case "/httpOnly":
            logger.trace("StickyConnectionsFor > httpOnly");
            tokenBoolean = reader.nextBoolean();
            stickyConnectionsFor.setHttpOnly(tokenBoolean);
            break;
          case "/secure":
            logger.trace("StickyConnectionsFor > secure");
            tokenBoolean = reader.nextBoolean();
            stickyConnectionsFor.setSecure(tokenBoolean);
            break;
          case "}":
            logger.trace("end vanity_urls section");
            hasMoreToProcess = false;
            processDefaultValues(stickyConnectionsFor, parentValue);
            break;
          default:
            FeedbackProcessor.error(logger, "Skipping unknown /stickyConnectionsFor level token.  Token=\"{}\".",
                    currentToken, Severity.MINOR);
            reader.advancePastThisElement();
        }
      }
    }

    return stickyConnectionsFor;
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

    StickyConnectionsFor scf = (StickyConnectionsFor) o;

    return new MatchesBuilder()
            .append(getPaths(), scf.getPaths())
            .append(getHttpOnly(), scf.getHttpOnly())
            .append(getSecure(), scf.getSecure())
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(49, 63)
            .append(getPaths())
            .append(getHttpOnly())
            .append(getSecure())
            .toHashCode();
  }

  /**
   * Process default values and set the source information to its parent's values
   * @param sticky The parent StickyConnectionsFor.
   */
  private static void processDefaultValues(StickyConnectionsFor sticky, final ConfigurationSource parentSource) {
    if (sticky.getHttpOnly().isUsingDefault()) {
      sticky.setHttpOnly(new ConfigurationValue<>(sticky.getHttpOnly().getValue(), parentSource));
    }
    if (sticky.getSecure().isUsingDefault()) {
      sticky.setSecure(new ConfigurationValue<>(sticky.getSecure().getValue(), parentSource));
    }
  }
}
