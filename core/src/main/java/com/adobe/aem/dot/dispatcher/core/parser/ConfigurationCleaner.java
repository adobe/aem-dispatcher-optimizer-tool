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
import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.common.FileResolver;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.util.FeedbackProcessor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class ConfigurationCleaner {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationCleaner.class);

  private ConfigurationCleaner() {}

  static List<ConfigurationLine> cleanConfig(List<ConfigurationLine> configurationLines) {
    // Split by line break
    List<ConfigurationLine> configLines = new ArrayList<>();

    // Tidy each line. Remove empty lines & comments
    for (ConfigurationLine line : configurationLines) {
      String lineTrimmed = trimWhitespaceAndComments(line.getContents());
      if (shouldLineBeIncluded(lineTrimmed)) {
        // Add non-empty trimmed lines to the temp line list
        configLines.add(new ConfigurationLine(lineTrimmed, line.getFileName(), line.getLineNumber(),
                line.getIncludedFrom()));
      }
    }

    return configLines;
  }

  /**
   * Pass each line through some basic tests and output any possible errors.
   * @param configurationLines A list of <code>ConfigurationLine</code> objects.
   */
  static void validateConfigurationLines(List<ConfigurationLine> configurationLines) {
    Map<String, Integer> braces = new HashMap<>();
    for (ConfigurationLine line: configurationLines) {
      // Check for mismatched quotes.
      long dqCount = line.getContents().chars().filter(ch -> ch == '"').count();
      long sqCount = line.getContents().chars().filter(ch -> ch == '\'').count();
      int escapedCount = StringUtils.countMatches(line.getContents(), "\\\"");
      if ((dqCount - escapedCount) > 0 && (dqCount - escapedCount) % 2 != 0) {
        FeedbackProcessor.warn(logger, "Unmatched quote (\").  Line=\"{}\"", line, Severity.MINOR);
      }
      escapedCount = StringUtils.countMatches(line.getContents(), "\\'");
      if ((sqCount - escapedCount) > 0 && (sqCount - escapedCount) % 2 != 0) {
        FeedbackProcessor.warn(logger, "Unmatched quote (').  Line=\"{}\"", line, Severity.MINOR);
      }

      long openBraceCount = line.getContents().chars().filter(ch -> ch == '{').count();
      long closeBraceCount = line.getContents().chars().filter(ch -> ch == '}').count();
      String filename = line.getFileName();
      int oldCount = braces.get(filename) != null ? braces.get(filename) : 0;
      Integer newCount = oldCount + (int) openBraceCount - (int) closeBraceCount;
      braces.put(filename, newCount);
    }

    // Identify and log any unclosed brace issues
    for (Map.Entry<String, Integer> nextCount : braces.entrySet()) {
      if (nextCount.getValue() != 0) {
        String fileWithUnclosedBrace = nextCount.getKey();
        logger.warn("Unclosed brace encountered in file=\"{}\"", fileWithUnclosedBrace);
      }
    }
  }

  static void updateEnvironmentVariables(List<ConfigurationLine> configurationLines, boolean replace) {
    Set<String> variablesNotResolved = new HashSet<>();
    for (ConfigurationLine line : configurationLines) {
      String resolvedEnv = FileResolver.resolveEnvironmentVariables(line.getContents(), replace, variablesNotResolved);
      if (!resolvedEnv.equals(line.getContents())) {
        line.setContents(resolvedEnv);
      }
    }
    if (!variablesNotResolved.isEmpty()) {
      List<String> sorted = variablesNotResolved.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
      logger.warn("Environment variables were not resolved. EnvVars=\"{}\"", StringUtils.join(sorted, ", "));
    }
  }

  private static boolean shouldLineBeIncluded(String line) {
    // Don't include empty lines or comments
    return line.trim().length() > 0 && !line.trim().startsWith("#");
  }

  private static String trimWhitespaceAndComments(String input) {
    StringBuilder cleaned = new StringBuilder();
    boolean inDoubleQuote = false;
    boolean inSingleQuote = false;
    boolean lastEscapeChar = false;
    input = input.trim();

    for (char nextChar: input.toCharArray()) {
      boolean isDoubleQuote = false;
      boolean isSingleQuote = false;

      if (nextChar == '\\') {
        lastEscapeChar = true;
      }
      if (nextChar == '"' && !lastEscapeChar) {
        isDoubleQuote = true;
      } else if (nextChar == '\'' && !lastEscapeChar) {
        isSingleQuote = true;
      }

      if (!inSingleQuote && isDoubleQuote) {
        inDoubleQuote = !inDoubleQuote;
      } else if (!inDoubleQuote && isSingleQuote) {
        inSingleQuote = !inSingleQuote;
      } else if (!inDoubleQuote && !inSingleQuote && nextChar == '#') {
        break;
      }

      if (nextChar != '\\') {
        lastEscapeChar = false;
      }

      cleaned.append(nextChar);
    }

    return cleaned.toString().trim();
  }

  static String prettifyConfig(List<ConfigurationLine> configLines) {
    int indent = 0;
    StringBuilder result = new StringBuilder();
    for (ConfigurationLine configLine: configLines) {
      String line = configLine.getContents();
      if (line.trim().startsWith("}")) {
        indent--;
      }

      int i;
      for (i = 0; i < indent; i++) {
        result.append("  ");
      }
      result.append(line.trim());
      result.append(System.lineSeparator());

      if (!(line.contains("{") && line.contains("}"))) {
        if (line.contains("{")) {
          indent++;
        } else if (!line.trim().startsWith("}") && line.contains("}")) {
          indent--;
        }
      }
    }

    return result.toString();
  }
}
