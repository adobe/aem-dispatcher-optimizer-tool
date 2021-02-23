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
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationReader;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an individual Vanity Url referenced by the Farm.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class HealthCheck extends ConfigurationSource {
  private ConfigurationValue<String> url;

  private static final Logger logger = LoggerFactory.getLogger(HealthCheck.class);

  static HealthCheck parseHealthCheck(ConfigurationReader reader) throws ConfigurationSyntaxException {
    HealthCheck healthCheck = new HealthCheck();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /health_check block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MAJOR);
      return new HealthCheck();
    }

    reader.next(); // Advance the reader's pointer passed the "{" marker.
    ConfigurationValue<String> currentToken;
    boolean hasMoreDetailsToProcess = true;
    while (reader.hasNext() && hasMoreDetailsToProcess) {
      currentToken = reader.next();
      switch (currentToken.getValue()) {
        case "/url":
          logger.trace("health_check > url");
          ConfigurationValue<String> url = reader.next();
          healthCheck.setUrl(url);
          break;
        case "}":
          logger.trace("end health_check section");
          hasMoreDetailsToProcess = false;
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /health_check level token.  Token=\"{}\".",
                  currentToken, Severity.MAJOR);
          reader.advancePastThisElement();
      }
    }
    return healthCheck;
  }
}
