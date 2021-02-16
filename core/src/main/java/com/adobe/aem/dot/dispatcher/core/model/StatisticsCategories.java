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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entry from the Statistics categories config.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class StatisticsCategories {
  List<StatisticsRule> rules;

  private static final Logger logger = LoggerFactory.getLogger(StatisticsCategories.class);

  static StatisticsCategories parseCategories(ConfigurationReader reader) throws ConfigurationSyntaxException {
    StatisticsCategories categories = new StatisticsCategories();
    List<StatisticsRule> categoriesList = new ArrayList<>();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /categories block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MINOR);
      return new StatisticsCategories();
    }

    reader.next(); // Advance the reader's pointer passed the "{" marker.
    ConfigurationValue<String> currentToken;
    boolean hasMoreCategoriesToParse = true;
    while (reader.hasNext() && hasMoreCategoriesToParse) {
      currentToken = reader.nextName();
      if (currentToken.getValue().equals("}")) {
        // We've reached the end of the categories section
        hasMoreCategoriesToParse = false;
      } else {
        StatisticsRule rule = StatisticsRule.parseRule(reader);
        rule.setLabel(currentToken);
        categoriesList.add(rule);
      }
    }

    categories.setRules(categoriesList);

    return categories;
  }
}
