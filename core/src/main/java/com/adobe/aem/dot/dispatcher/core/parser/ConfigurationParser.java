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

package com.adobe.aem.dot.dispatcher.core.parser;

import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.parser.ConfigurationViolations;
import com.adobe.aem.dot.common.util.FeedbackProcessor;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.model.Farm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_VALUE_FILE_NAME;

public class ConfigurationParser {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationParser.class);

  /**
   * Parse the provided configurationString into a DispatcherConfiguration object.
   * @param configurationLines - a list of <code>ConfigurationLine</code> objects.
   * @return A <code>DispatcherConfiguration</code> object, as parsed from the provided String.
   */
  public ConfigurationParseResults<DispatcherConfiguration> parseConfiguration(
          List<ConfigurationLine> configurationLines) throws ConfigurationSyntaxException {
    ConfigurationViolations.clearViolations();

    ConfigurationCleaner.updateEnvironmentVariables(configurationLines, true);
    configurationLines = ConfigurationCleaner.cleanConfig(configurationLines);
    logger.trace("Clean config: \n{}", ConfigurationCleaner.prettifyConfig(configurationLines));

    ConfigurationCleaner.validateConfigurationLines(configurationLines);

    ConfigurationValue<String> name = null;
    ConfigurationValue<Boolean> ignoreEINTR = new ConfigurationValue<>(false, DEFAULT_VALUE_FILE_NAME, 0);
    List<ConfigurationValue<Farm>> farms = null;

    ConfigurationReader reader = new ConfigurationReader(configurationLines);

    while (reader.hasNext()) {
      ConfigurationValue<String> currentToken = reader.next();
      logger.trace("Token: {}", currentToken.getValue());
      switch (currentToken.getValue()) {
        case "/name":
          // Parse name
          name = reader.next();
          break;
        case "/ignoreEINTR":
          // Parse ignoreEINTR
          ignoreEINTR = reader.nextBoolean();
          break;
        case "/farms":
          // Parse farms
          farms = Farm.parseFarms(reader);
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown top level token.  Token=\"{}\"",
                  currentToken, Severity.MAJOR);
          reader.advancePastThisElement();
      }
    }

    DispatcherConfiguration config = new DispatcherConfiguration(name, ignoreEINTR, farms);
    return new ConfigurationParseResults<>(config, ConfigurationViolations.getViolations());
  }
}
