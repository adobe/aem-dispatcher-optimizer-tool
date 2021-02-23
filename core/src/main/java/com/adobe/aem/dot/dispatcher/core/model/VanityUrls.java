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

import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_VALUE_FILE_NAME;

/**
 * Represents an individual Vanity Url referenced by the Farm.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class VanityUrls extends ConfigurationSource {
  private ConfigurationValue<String> url;
  private ConfigurationValue<String> file;
  private ConfigurationValue<Integer> delay = new ConfigurationValue<>(300, DEFAULT_VALUE_FILE_NAME, 0);

  private static final Logger logger = LoggerFactory.getLogger(VanityUrls.class);

  static VanityUrls parseVanityUrls(ConfigurationReader reader) throws ConfigurationSyntaxException {
    VanityUrls vanityUrls = new VanityUrls();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /vanity_urls block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MAJOR);
      return new VanityUrls();
    }

    reader.next(); // Advance the reader's pointer passed the "{" marker.
    ConfigurationValue<String> currentToken;
    boolean hasMoreVanityUrlDetailsToProcess = true;
    while (reader.hasNext() && hasMoreVanityUrlDetailsToProcess) {
      currentToken = reader.next();
      switch (currentToken.getValue()) {
        case "/url":
          // Parse url
          logger.trace("vanity_urls > url");
          ConfigurationValue<String> url = reader.next();
          vanityUrls.setUrl(url);
          break;
        case "/file":
          // Parse file
          logger.trace("vanity_urls > file");
          ConfigurationValue<String> file = reader.next();
          vanityUrls.setFile(file);
          break;
        case "/delay":
          // Parse delay
          logger.trace("vanity_urls > delay");
          ConfigurationValue<Integer> delay = reader.nextInteger(0);
          vanityUrls.setDelay(delay);
          break;
        case "}":
          logger.trace("end vanity_urls section");
          hasMoreVanityUrlDetailsToProcess = false;
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /vanity_urls level token.  Token=\"{}\".",
                  currentToken, Severity.MAJOR);
          reader.advancePastThisElement();
      }
    }
    return vanityUrls;
  }
}
