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

package com.adobe.aem.dot.common.util;

import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.parser.ConfigurationViolations;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * The FeedbackProcessor reports back to the customer through logs entries, and by collecting violations that will
 * be added to the final report.
 */
public class FeedbackProcessor {

  private static final Logger logger = LoggerFactory.getLogger(FeedbackProcessor.class);

  private static final int MAX_PATH_LENGTH = 120;

  /**
   * Log an error and optionally report a configuration violation.
   * @param logger The logger to use when logging the error.
   * @param message The message to log.  The message can take one {0} marker which will be replaced by the value's
   *                `getValue()` string.
   * @param value The `ConfigurationValue` that triggered this issue.
   * @param severity The `Severity` with which the Violation should be reported.  If `null`, no violation will
   *                 be reported.
   */
  public static void error(Logger logger, String message, ConfigurationValue<?> value, Severity severity) {
    if (value == null) {
      FeedbackProcessor.error(logger, message, "", null, severity);
    } else {
      FeedbackProcessor.error(logger, message,
              value.getValue() == null ? "null" : value.getValue().toString(),
              value.getConfigurationSource(), severity);
    }
  }

  /**
   * Log an error and optionally report a configuration violation.
   * @param logger The logger to use when logging the error.
   * @param message The message to log.  The message can take one {0} marker which will be replaced by value.
   * @param value The value to be inserted.
   * @param source The source of the configuration line.
   * @param severity The `Severity` with which the Violation should be reported.  If `null`, no violation will
   *                 be reported.
   */
  public static void error(Logger logger, String message, String value, ConfigurationSource source, Severity severity) {
    String processedMessage = processMessage(message, value);
    logger.error(appendFileInformation(processedMessage, source));
    if (severity != null && value != null) {
      ConfigurationViolations.addViolation(processedMessage, severity, source);
    }
  }

  /**
   * Log a warning and optionally report a configuration violation.
   * @param logger The logger to use when logging the error.
   * @param message The message to log.  The message can take one {0} marker which will be replaced by the value's
   *                `getValue()` string.
   * @param line The `ConfigurationLine` that triggered this issue.
   * @param severity The `Severity` with which the Violation should be reported.  If `null`, no violation will
   *                 be reported.
   */
  public static void warn(Logger logger, String message, ConfigurationLine line, Severity severity) {
    if (line == null) {
      FeedbackProcessor.warn(logger, message, "", null, severity);
    } else {
      FeedbackProcessor.warn(logger, message, line.getContents(), line.getConfigurationSource(), severity);
    }
  }

  /**
   * Log a warning and optionally report a configuration violation.
   * @param logger The logger to use when logging the error.
   * @param message The message to log.  The message can take one {0} marker which will be replaced by value.
   * @param value The value to be inserted.
   * @param source The source of the configuration line.
   * @param severity The `Severity` with which the Violation should be reported.  If `null`, no violation will
   *                 be reported.
   */
  public static void warn(Logger logger, String message, String value, ConfigurationSource source, Severity severity) {
    String processedMessage = processMessage(message, value);
    logger.warn(appendFileInformation(processedMessage, source));
    if (severity != null && source != null) {
      ConfigurationViolations.addViolation(processedMessage, severity, source);
    }
  }

  /**
   * Log a warning and optionally report a configuration violation.
   * @param logger The logger to use when logging the warning.
   * @param message The message to log.  The message can take one {0} marker which will be replaced by the value's
   *                `getValue()` string.
   * @param value The `ConfigurationValue` that triggered this issue.
   * @param severity The `Severity` with which the Violation should be reported.  If `null`, no violation will
   *                 be reported.
   */
  public static void warn(Logger logger, String message, ConfigurationValue<?> value, Severity severity) {
    String processedMessage = processMessage(message, value);
    logger.warn(appendFileInformation(processedMessage, value));
    if (severity != null && value != null) {
      ConfigurationViolations.addViolation(processedMessage, severity, value.getConfigurationSource());
    }
  }

  /**
   * Log some information.
   * @param logger The logger to use when logging the information.
   * @param message The message to log.  The message can take one {0} marker which will be replaced by the value's
   *                `getValue()` string.
   * @param value The `ConfigurationValue` that triggered this case.
   */
  public static void info(Logger logger, String message, ConfigurationValue<?> value) {
    String processedMessage = processMessage(message, value);
    logger.info(appendFileInformation(processedMessage, value));
  }

  private static String processMessage(String message, ConfigurationValue<?> value) {
    if (value != null) {
      return processMessage(message, value.getValue() != null ? value.getValue().toString() : null);
    }
    return processMessage(message, "");
  }

  private static String processMessage(String message, String value) {
    // See if value.getValue() should be inserted.
    if (message.contains("{}")) {
      message = message.replace("{}", "{0}");
    }
    if (message.contains("{1}") || message.contains("{}")) {
      logger.warn("Illegal format of 'message' used in FeedbackProcessor.  Message=\"{}\"", message);
    }
    if (message.contains("{0}")) {
      try {
        message = MessageFormat.format(message, value);
      } catch (IllegalArgumentException iaEx) {
        logger.warn("Illegal call to FeedbackProcessor.  String=\"{}\"", message);
        // Skip the insertion of the getValue() string and move on.
      }
    }

    return message;
  }

  private static String appendFileInformation(String message, ConfigurationSource source) {
    if (source == null) {
      return message;
    }

    String fileName = source.getFileName();
    if (fileName.length() > MAX_PATH_LENGTH) {
      fileName = "..." + fileName.substring(fileName.length() - MAX_PATH_LENGTH);
    }

    StringBuilder builder = new StringBuilder(message);
    builder.append(" File=\"");
    builder.append(fileName);
    builder.append("\" Number=");
    builder.append(source.getLineNumber());
    if (StringUtils.isNotEmpty(source.getIncludedFrom())) {
      builder.append(" Included From=\"")
              .append(source.getIncludedFrom())
              .append("\"");
    }

    return builder.toString();
  }
}
