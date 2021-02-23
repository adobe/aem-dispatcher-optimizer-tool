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
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationReader;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A rule for use in the Farm's Statistics section.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class StatisticsRule extends LabeledConfigurationValue {
  private ConfigurationValue<String> glob;

  private static final Logger logger = LoggerFactory.getLogger(StatisticsRule.class);

  Logger getLogger() {
    return logger;
  }

  String getSimpleClassName() {
    return getClass().getSimpleName();
  }

  static StatisticsRule parseRule(ConfigurationReader reader) throws ConfigurationSyntaxException {
    StatisticsRule rule = new StatisticsRule();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each StatisticsRule block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MAJOR);
      return new StatisticsRule();
    }

    reader.next(); // Advance the reader's pointer passed the "{" marker.
    ConfigurationValue<String> currentToken;
    boolean hasMoreRuleDetailsToProcess = true;
    while (reader.hasNext() && hasMoreRuleDetailsToProcess) {
      currentToken = reader.next();
      switch(currentToken.getValue()) {
        case "/glob":
          // Parse glob
          logger.trace("StatisticsRule > glob");
          ConfigurationValue<String> glob = reader.next();
          rule.setGlob(glob);
          break;
        case "}":
          logger.trace("end StatisticsRule section");
          hasMoreRuleDetailsToProcess = false;
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /StatisticsRule level token.  Token=\"{}\".",
                  currentToken, Severity.MAJOR);
          reader.advancePastThisElement();
      }
    }

    return rule;
  }
}
