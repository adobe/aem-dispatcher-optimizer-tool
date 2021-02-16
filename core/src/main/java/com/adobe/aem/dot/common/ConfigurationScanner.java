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

package com.adobe.aem.dot.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Similar interface to java.util.Scanner, but provides configuration-specific string scanning enhancements.
 */
public class ConfigurationScanner {
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationScanner.class);

  protected String configuration;
  protected int currentCharIndex = -1;

  public ConfigurationScanner(String configuration) {
    this.configuration = configuration;
  }

  /**
   * Returns true if this scanner has another token in its configuration string.
   * @return true if and only if this scanner has another token
   */
  public boolean hasNext() {
    return this.getIndexOfNextToken() != -1;
  }

  /**
   * Finds and returns the next complete dispatcher configuration token from this scanner.
   * @return the next token, a String
   */
  public String next() {
    return this.next(false);
  }

  /**
   * Finds and returns the next complete Apache configuration token from this scanner, optionally keeping any quotes
   * (single or double) that were present around the token was it was interpreted.
   * @param preserveQuotes if true, quotes wrapping the token will be left as-is
   * @return the next token, a String
   */
  public String next(boolean preserveQuotes) {
    int beginIndex = this.getIndexOfNextToken();

    if (beginIndex == -1) {
      // No additional tokens to parse
      return null;
    }

    char beginChar = this.configuration.charAt(beginIndex);
    boolean isQuotedToken = (beginChar == '"' || beginChar == '\'');

    // Find the end of this token
    int endIndex = this.getEndIndexOfToken(beginIndex, isQuotedToken, beginChar);

    // Update character pointer to the endIndex of this token
    this.currentCharIndex = endIndex;
    String nextToken = this.configuration.substring(beginIndex, endIndex + 1);

    // Quoted string was not closed, close it now (already logged).
    if (isQuotedToken && beginChar != nextToken.charAt(nextToken.length() - 1)) {
      nextToken += beginChar;
    }

    if (preserveQuotes) {
      return nextToken;
    } else {
      return this.getStringWithoutQuotes(nextToken);
    }
  }

  /**
   * Is the provided character either whitespace or the beginning of a new token?
   * @param character - the character to test
   * @return true if the provided char is determined to be whitespace or the beginning of a new token
   */
  protected boolean isWhitespaceOrBeginningOfNextToken(char character) {
    return Character.isWhitespace(character) || ((character == '/' ||
            character == '{' || character == '}' ||
            character == '"' || character == '\''));
  }

  private int getIndexOfNextToken() {
    // Read ahead to see if there are any non-whitespace chars remaining before the end of the string
    int lookAheadIndex = this.currentCharIndex + 1;
    while (lookAheadIndex < this.configuration.length()) {
      if (!Character.isWhitespace(this.configuration.charAt(lookAheadIndex))) {
        // Found the start of a new token
        return lookAheadIndex;
      }
      lookAheadIndex++;
    }
    // Reached the end of the config string, no next token found
    return -1;
  }

  private int getEndIndexOfToken(int startIndex, boolean isQuoted, char quoteChar) {
    int lookAheadIndex = startIndex + 1;
    char previousChar = this.configuration.charAt(startIndex);
    while (lookAheadIndex < this.configuration.length()) {
      char currentChar = this.configuration.charAt(lookAheadIndex);
      if (isQuoted) {
        // Search until we find the matching closing quote.
        // Note: this method does not support escaping.
        if (currentChar == quoteChar && previousChar != '\\') {
          // This is the end of the token
          return lookAheadIndex;
        }
      } else {
        // Stop when we find whitespace or the beginning of the next token
        if (this.isWhitespaceOrBeginningOfNextToken(currentChar)) {
          // Whitespace or new token found, so the endIndex is the previous character
          return lookAheadIndex - 1;
        }
      }
      previousChar = currentChar;
      lookAheadIndex++;
    }

    if (isQuoted) {
      logger.error("Closing quote never found. Assuming the rest of the line is intended string. Line=\"{}\"",
              this.configuration);
    }

    // Reached the end of the string
    return lookAheadIndex - 1;
  }

  private String getStringWithoutQuotes(String optionallyQuotedToken) {
    if ((optionallyQuotedToken.startsWith("\"") && optionallyQuotedToken.endsWith("\"")) ||
            (optionallyQuotedToken.startsWith("'") && optionallyQuotedToken.endsWith("'"))) {
      // Remove quotes, if quoted
      try {
        return optionallyQuotedToken.substring(1, optionallyQuotedToken.length() - 1);
      } catch(IndexOutOfBoundsException ioobEx) {
        logger.error("Token confused the quote parser. Token=\"{}\"", optionallyQuotedToken);
      }
    }
    // Otherwise, return string as-is
    return optionallyQuotedToken;
  }
}
