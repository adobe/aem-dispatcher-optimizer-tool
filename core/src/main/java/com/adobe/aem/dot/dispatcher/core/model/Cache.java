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

import java.util.List;

import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_BOOLEAN_FALSE;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_INT_ZERO;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_MODE_VALUE;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_VALUE_FILE_NAME;

/**
 * Represents the Cache configuration of each Farm.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class Cache {
  private static final Logger logger = LoggerFactory.getLogger(Cache.class);

  private ConfigurationValue<String> docroot;
  private ConfigurationValue<String> statfile;
  private ConfigurationValue<Integer> statfileslevel = DEFAULT_INT_ZERO;
  private ConfigurationValue<Boolean> allowAuthorized = DEFAULT_BOOLEAN_FALSE;
  private ConfigurationValue<Boolean> serveStaleOnError = DEFAULT_BOOLEAN_FALSE;
  private ConfigurationValue<Integer> gracePeriod = DEFAULT_INT_ZERO;
  private ConfigurationValue<Boolean> enableTTL = DEFAULT_BOOLEAN_FALSE;
  private ConfigurationValue<List<Rule>> rules;
  private ConfigurationValue<List<Rule>> invalidate;
  private ConfigurationValue<String> invalidateHandler;
  private ConfigurationValue<List<Rule>> allowedClients;
  private ConfigurationValue<List<Rule>> ignoreUrlParams;
  private List<ConfigurationValue<String>> headers;
  private ConfigurationValue<String> mode = new ConfigurationValue<>(DEFAULT_MODE_VALUE, DEFAULT_VALUE_FILE_NAME, 0);

  static Cache parseCache(ConfigurationReader reader) throws ConfigurationSyntaxException {
    Cache cache = new Cache();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /cache block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MAJOR);
      return new Cache();
    }

    ConfigurationValue<String> parentToken = reader.next(); // Advance the reader's pointer passed the "{" marker.
    boolean hasMoreCacheDetailsToProcess = true;
    while (reader.hasNext() && hasMoreCacheDetailsToProcess) {
      ConfigurationValue<String> currentToken = reader.next();
      switch(currentToken.getValue()) {
        case "/docroot":
          logger.trace("cache > docroot");
          ConfigurationValue<String> docRoot = reader.next();
          cache.setDocroot(docRoot);
          break;
        case "/statfile":
          logger.trace("cache > statfile");
          ConfigurationValue<String> statFile = reader.next();
          cache.setStatfile(statFile);
          break;
        case "/statfileslevel":
          logger.trace("cache > statfileslevel");
          cache.setStatfileslevel(reader.nextInteger(0));
          break;
        case "/allowAuthorized":
          logger.trace("cache > allowAuthorized");
          ConfigurationValue<Boolean> allowAuthorized = reader.nextBoolean();
          cache.setAllowAuthorized(allowAuthorized);
          break;
        case "/serveStaleOnError":
          logger.trace("cache > serveStaleOnError");
          ConfigurationValue<Boolean> serveStaleOnError = reader.nextBoolean();
          cache.setServeStaleOnError(serveStaleOnError);
          break;
        case "/gracePeriod":
          logger.trace("cache > gracePeriod");
          cache.setGracePeriod(reader.nextInteger(0));
          break;
        case "/enableTTL":
          logger.trace("cache > enableTTL");
          ConfigurationValue<Boolean> enableTTL = reader.nextBoolean();
          cache.setEnableTTL(enableTTL);
          break;
        case "/rules":
          logger.trace("cache > rules");
          ConfigurationValue<List<Rule>> rules = Rule.parseRules(reader);
          cache.setRules(rules);
          break;
        case "/invalidate":
          logger.trace("cache > invalidate");
          ConfigurationValue<List<Rule>> invalidateRules = Rule.parseRules(reader);
          cache.setInvalidate(invalidateRules);
          break;
        case "/invalidateHandler":
          logger.trace("cache > invalidateHandler");
          ConfigurationValue<String> invalidateHandler = reader.next();
          cache.setInvalidateHandler(invalidateHandler);
          break;
        case "/mode":
          logger.trace("cache > mode");
          ConfigurationValue<String> mode = reader.next();
          cache.setMode(mode);
          break;
        case "/allowedClients":
          logger.trace("cache > allowedClients");
          ConfigurationValue<List<Rule>> allowedClients = Rule.parseRules(reader);
          cache.setAllowedClients(allowedClients);
          break;
        case "/ignoreUrlParams":
          logger.trace("cache > ignoreUrlParams");
          ConfigurationValue<List<Rule>> ignoreUrlParams = Rule.parseRules(reader);
          cache.setIgnoreUrlParams(ignoreUrlParams);
          break;
        case "/headers":
          logger.trace("cache > headers");
          List<ConfigurationValue<String>> headers = reader.nextStringList();
          cache.setHeaders(headers);
          break;
        case "}":
          logger.trace("end cache section");
          hasMoreCacheDetailsToProcess = false;
          Cache.processDefaultValues(cache, parentToken);
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /cache level token. Token=\"{}\".", currentToken,
                  Severity.MAJOR);
          reader.advancePastThisElement();
      }
    }

    return cache;
  }

  /**
   * Process default values and set the source information to its parent's values.
   * @param cache The parent cache.
   */
  private static void processDefaultValues(Cache cache, final ConfigurationSource parentSource) {
    if (cache.getStatfileslevel().isUsingDefault()) {
      cache.setStatfileslevel(new ConfigurationValue<>(cache.getStatfileslevel().getValue(), parentSource));
    }
    if (cache.getAllowAuthorized().isUsingDefault()) {
      cache.setAllowAuthorized(new ConfigurationValue<>(cache.getAllowAuthorized().getValue(), parentSource));
    }
    if (cache.getServeStaleOnError().isUsingDefault()) {
      cache.setServeStaleOnError(new ConfigurationValue<>(cache.getServeStaleOnError().getValue(), parentSource));
    }
    if (cache.getGracePeriod().isUsingDefault()) {
      cache.setGracePeriod(new ConfigurationValue<>(cache.getGracePeriod().getValue(), parentSource));
    }
    if (cache.getEnableTTL().isUsingDefault()) {
      cache.setEnableTTL(new ConfigurationValue<>(cache.getEnableTTL().getValue(), parentSource));
    }
    if (cache.getMode().isUsingDefault()) {
      cache.setMode(new ConfigurationValue<>(cache.getMode().getValue(), parentSource));
    }
  }
}
