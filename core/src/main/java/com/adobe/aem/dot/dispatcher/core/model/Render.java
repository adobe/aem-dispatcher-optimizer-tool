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

import java.util.ArrayList;
import java.util.List;

import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_BOOLEAN_FALSE;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_INT_ZERO;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_VALUE_FILE_NAME;

/**
 * Represents an individual entry from the Farm's list of /renders.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class Render extends LabeledConfigurationValue {
  private ConfigurationValue<String> hostname;
  private ConfigurationValue<String> port = new ConfigurationValue<>("443", // Set as secure by default.
          DEFAULT_VALUE_FILE_NAME, 0);
  private ConfigurationValue<Integer> timeout = DEFAULT_INT_ZERO;
  private ConfigurationValue<Integer> receiveTimeout = new ConfigurationValue<>(600000, DEFAULT_VALUE_FILE_NAME, 0);
  private ConfigurationValue<Boolean> ipv4 = DEFAULT_BOOLEAN_FALSE;
  private ConfigurationValue<Boolean> secure = DEFAULT_BOOLEAN_FALSE;
  private ConfigurationValue<Boolean> alwaysResolve = DEFAULT_BOOLEAN_FALSE;

  private static final Logger logger = LoggerFactory.getLogger(Render.class);

  Logger getLogger() {
    return logger;
  }

  String getSimpleClassName() {
    return getClass().getSimpleName();
  }

  static List<Render> parseRenders(ConfigurationReader reader) throws ConfigurationSyntaxException {
    List<Render> renders = new ArrayList<>();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /renders block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MAJOR);
      return new ArrayList<>();
    }

    reader.next(); // Advance the reader's pointer passed the "{" marker.
    ConfigurationValue<String> currentToken;
    boolean hasMoreRenders = true;
    while (reader.hasNext() && hasMoreRenders) {
      currentToken = reader.next();
      if (currentToken.getValue().equals("}")) {
        // We've reached the end of the farms section
        hasMoreRenders = false;
      } else if (!currentToken.getValue().startsWith("/") && !currentToken.getValue().startsWith("{")) {
        // Names should start with a /.  If no name, it should be an opening brace.
        FeedbackProcessor.error(logger, "Skipping unknown value in a render. Value=\"{0}\". Skipping value.",
                currentToken, Severity.MAJOR);
      } else {
        // If the next character is a open brace, the label (/0001) was skipped.
        boolean missingLabel = currentToken.getValue().equals("{");
        Render render = Render.parseRender(reader, missingLabel);
        if (missingLabel) {
          FeedbackProcessor.info(logger, "Label on Render is missing.", currentToken);
        } else {
          currentToken.setValue(currentToken.getValue().replace("/", ""));
          render.setLabel(currentToken);
        }
        renders.add(render);
      }
    }

    return renders;
  }

  private static Render parseRender(ConfigurationReader reader, boolean skipLabel) throws ConfigurationSyntaxException {
    Render render = new Render();
    ConfigurationValue<String> parentValue = null;

    // Expect { to begin the rule block, unless there is no label.
    if (!skipLabel) {
      if (!reader.isNextChar('{', false)) {
        FeedbackProcessor.error(logger,"Each render must begin with a '{' character.",
                reader.getCurrentConfigurationValue(), Severity.MAJOR);
        return new Render();
      }
      parentValue = reader.next();
    }

    boolean hasMoreRenderDetailsToParse = true;
    while (reader.hasNext() && hasMoreRenderDetailsToParse) {
      ConfigurationValue<String> nextToken = reader.next();
      if (parentValue == null) {
        parentValue = nextToken; // Might not be the exact line - but will be close.
      }
      switch(nextToken.getValue()) {
        case "/hostname":
          logger.trace("render > hostname");
          ConfigurationValue<String> hostname = reader.next();
          render.setHostname(hostname);
          break;
        case "/port":
          logger.trace("render > port");
          ConfigurationValue<String> port = reader.next();
          render.setPort(port);
          break;
        case "/timeout":
          logger.trace("render > timeout");
          ConfigurationValue<Integer> timeout = reader.nextInteger(0);
          render.setTimeout(timeout);
          break;
        case "/receiveTimeout":
          logger.trace("render > receiveTimeout");
          render.setReceiveTimeout(reader.nextInteger(600000));
          break;
        case "/ipv4":
          logger.trace("render > ipv4");
          render.setIpv4(reader.nextBoolean());
          break;
        case "/secure":
          logger.trace("render > secure");
          render.setSecure(reader.nextBoolean());
          break;
        case "/always-resolve":
          logger.trace("render > always-resolve");
          render.setAlwaysResolve(reader.nextBoolean());
          break;
        case "}":
          logger.trace("end render section");
          hasMoreRenderDetailsToParse = false;
          processDefaultValues(render, parentValue);
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /render level token.  Token=\"{}\".", nextToken,
                  Severity.MAJOR);
          reader.advancePastThisElement();
      }
    }

    return render;
  }

  /**
   * Process default values and set the source information to its parent's values
   * @param render The parent Render.
   */
  private static void processDefaultValues(Render render, final ConfigurationSource parentSource) {
    if (render.getPort().isUsingDefault()) {
      String defaultPort = "80";
      if (render.getSecure().getValue()) {
        defaultPort = "443";
      }
      render.setPort(new ConfigurationValue<>(defaultPort, parentSource));
    }
    if (render.getTimeout().isUsingDefault()) {
      render.setTimeout(new ConfigurationValue<>(render.getTimeout().getValue(), parentSource));
    }
    if (render.getReceiveTimeout().isUsingDefault()) {
      render.setReceiveTimeout(new ConfigurationValue<>(render.getReceiveTimeout().getValue(), parentSource));
    }
    if (render.getIpv4().isUsingDefault()) {
      render.setIpv4(new ConfigurationValue<>(render.getIpv4().getValue(), parentSource));
    }
    if (render.getSecure().isUsingDefault()) {
      render.setSecure(new ConfigurationValue<>(render.getSecure().getValue(), parentSource));
    }
    if (render.getAlwaysResolve().isUsingDefault()) {
      render.setAlwaysResolve(new ConfigurationValue<>(render.getAlwaysResolve().getValue(), parentSource));
    }
  }
}
