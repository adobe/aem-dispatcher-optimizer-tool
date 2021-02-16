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

import java.util.List;

/**
 * Represents an /auth_checker object, which is a section in a /farm.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class AuthChecker extends LabeledConfigurationValue {
  private ConfigurationValue<String> url;
  private ConfigurationValue<List<Filter>> filter;
  private ConfigurationValue<List<Rule>> headers;     // Each /headers are in the form of a Rule (i.e. glob & type)

  private static final Logger logger = LoggerFactory.getLogger(AuthChecker.class);

  private AuthChecker() {  }

  Logger getLogger() {
    return logger;
  }

  String getSimpleClassName() {
    return getClass().getSimpleName();
  }

  static AuthChecker parseAuthChecker(ConfigurationReader reader)
          throws ConfigurationSyntaxException {
    AuthChecker authChecker = new AuthChecker();
    ConfigurationValue<String> token;

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /auth_checker block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MINOR);
      return new AuthChecker();
    }

    reader.next(); // Advance the reader's pointer passed the "{" marker.
    ConfigurationValue<String> currentToken;
    boolean hasMoreToProcess = true;
    while (reader.hasNext() && hasMoreToProcess) {
      currentToken = reader.next();
      switch (currentToken.getValue()) {
        case "/url":
          logger.trace("AuthChecker > url");
          token = reader.next();
          authChecker.setUrl(token);
          break;
        case "/filter":
          logger.trace("AuthChecker > filter");
          ConfigurationValue<List<Filter>> filters = Filter.parseFilters(reader);
          authChecker.setFilter(filters);
          break;
        case "/headers":
          logger.trace("AuthChecker > headers");
          ConfigurationValue<List<Rule>> tokens = Rule.parseRules(reader);
          authChecker.setHeaders(tokens);
          break;
        case "}":
          logger.trace("end auth_checker section");
          hasMoreToProcess = false;
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /auth_checker level token. Token=\"{}\".",
                  currentToken, Severity.MINOR);
          reader.advancePastThisElement();
      }
    }

    return authChecker;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    AuthChecker ac = (AuthChecker) o;

    return new MatchesBuilder()
            .append(getUrl(), ac.getUrl())
            .append(getFilter(), ac.getFilter())
            .append(getHeaders(), ac.getHeaders())
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(15, 39)
            .append(getUrl())
            .append(getFilter())
            .append(getHeaders())
            .toHashCode();
  }
}
