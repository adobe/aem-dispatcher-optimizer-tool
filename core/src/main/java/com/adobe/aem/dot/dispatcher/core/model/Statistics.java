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
 * Represents the Farm's statistics configuration.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class Statistics extends ConfigurationSource {
  private StatisticsCategories categories;

  private static final Logger logger = LoggerFactory.getLogger(Statistics.class);

  static Statistics parseStatistics(ConfigurationReader reader) throws ConfigurationSyntaxException {
    Statistics statistics = new Statistics();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /statistics block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MINOR);
      return new Statistics();
    }

    reader.next(); // Advance the reader's pointer passed the "{" marker.
    ConfigurationValue<String> currentToken;
    boolean hasMoreStatisticsDetailsToParse = true;
    while (reader.hasNext() && hasMoreStatisticsDetailsToParse) {
      currentToken = reader.next();
      switch(currentToken.getValue()) {
        case "/categories":
          // Parse categories
          logger.trace("statistics > categories");
          StatisticsCategories statsCategories = StatisticsCategories.parseCategories(reader);
          statistics.setCategories(statsCategories);
          break;
        case "}":
          logger.trace("end statistics section");
          hasMoreStatisticsDetailsToParse = false;
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /statistics level token.  Token=\"{}\".",
                  currentToken, Severity.MINOR);
          reader.advancePastThisElement();
      }
    }

    return statistics;
  }
}
