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
import com.adobe.aem.dot.common.util.FeedbackProcessor;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationReader {
  private final List<ConfigurationLine> configuration;
  private ConfigurationIndex configurationIndex = new ConfigurationIndex(0, 0);

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationReader.class);

  ConfigurationReader(List<ConfigurationLine> configuration) {
    this.configuration = configuration;
  }

  private int getLineIndex() {
    return this.configurationIndex.getLineIndex();
  }

  private int getCharIndex() {
    return this.configurationIndex.getCharIndex();
  }

  private String getCurrentFileName() {
    return this.configuration.get(this.getLineIndex()).getFileName();
  }

  private int getCurrentLineNumber() {
    return this.configuration.get(this.getLineIndex()).getLineNumber();
  }

  private String getCurrentIncludedFrom() {
    return this.configuration.get(this.getLineIndex()).getIncludedFrom();
  }

  public ConfigurationValue<?> getCurrentConfigurationValue() {
    return new ConfigurationValue<>(this.configuration.get(this.getLineIndex()).getContents(),
            this.getCurrentFileName(), this.getCurrentLineNumber(), this.getCurrentIncludedFrom());
  }

  /**
   * Increment the configuration index to the next character, or to the next line.
   * @return Whether the increment caused the reader to go to the next line.
   */
  private boolean incrementIndex() {
    if (this.hasNext()) {
      ConfigurationLine currentLine = this.configuration.get(getLineIndex());
      String currentContents = currentLine.getContents();

      if (getCharIndex() < currentContents.length() - 1) {
        this.configurationIndex.incrementCharIndex();
        return false;
      } else {
        this.configurationIndex.incrementLineIndex();
        return true;
      }
    }

    return false;
  }

  public boolean hasNext() {
    if (getLineIndex() == this.configuration.size() - 1) {
      ConfigurationLine currentLine = this.configuration.get(getLineIndex());
      String currentContents = currentLine.getContents();
      return this.getCharIndex() != currentContents.length() - 1;
    }

    return true;
  }

  /**
   * Finds and returns the next complete dispatcher configuration token from this scanner.
   * @return the next token, a String
   */
  public ConfigurationValue<String> next() throws ConfigurationSyntaxException {
    return this.next(false);
  }

  /**
   * Finds and returns the next complete dispatcher configuration token from this scanner.
   * @return the next token, a Boolean
   */
  public ConfigurationValue<Boolean> nextBoolean() throws ConfigurationSyntaxException {
    ConfigurationValue<String> nextToken = this.next();
    Boolean positive = nextToken.getValue().equals("1") || nextToken.getValue().equals("true");
    return new ConfigurationValue<>(positive, this.getCurrentFileName(), getCurrentLineNumber(),
            getCurrentIncludedFrom());
  }

  /**
   * Finds and returns the next complete dispatcher configuration token from this scanner.
   * @return the next token, a Boolean
   */
  public ConfigurationValue<Integer> nextInteger(Integer defaultInt) throws ConfigurationSyntaxException {
    Integer value = defaultInt;
    ConfigurationValue<String> nextToken = this.next();

    try {
      value = Integer.parseInt(nextToken.getValue());
    } catch (NumberFormatException nfe) {
      FeedbackProcessor.error(logger, "Skipping unknown integer value. Value=\"{0}\"", nextToken,
              Severity.MAJOR);
    }
    return new ConfigurationValue<>(value, nextToken.getFileName(), nextToken.getLineNumber(),
            getCurrentIncludedFrom());
  }

  /**
   * Finds and returns the next complete dispatcher configuration name token from this scanner, removing the "/" prefix
   * if present.
   * @return the next token, a String, with its "/" prefix removed
   */
  public ConfigurationValue<String> nextName() throws ConfigurationSyntaxException {
    ConfigurationValue<String> nameWithOptionalSlash = this.next();
    if (nameWithOptionalSlash != null) {
      nameWithOptionalSlash.setValue(nameWithOptionalSlash.getValue().replace("/", ""));
    }
    return nameWithOptionalSlash;
  }

  /**
   * Finds and returns the next string.  A string means the text in the following quotes.  If no double
   * or single quotes are used, read until the end of the line, or until a comment character is encountered.
   * @return the next token, a String, with its "/" prefix removed
   */
  public ConfigurationValue<String> nextString() throws ConfigurationSyntaxException {
    int currentLineNumber = this.getCurrentLineNumber();
    ConfigurationValue<String> nextString = this.next(true);
    // Handle normal quoted string.
    if (nextString.getValue().startsWith("\"") || nextString.getValue().startsWith("'")) {
      return getStringWithoutQuotes(nextString);
    }

    // Check whether the last scan left the scanner on the same line.  If so, there is more to read.
    char nextChar = this.getChar();
    StringBuilder builder = new StringBuilder(nextString.getValue());
    while (currentLineNumber == this.getCurrentLineNumber() && !Character.isWhitespace(nextChar)) {
      builder.append(nextChar);
      this.incrementIndex();
      nextChar = this.getChar();
    }

    nextString.setValue(builder.toString());
    return nextString;
  }

  /**
   * Finds and returns the next complete configuration token, optionally keeping any quotes
   * (single or double) that were present around the token was it was interpreted.
   * @param preserveQuotes if true, quotes wrapping the token will be left as-is
   * @return the next token, a <code>ConfigurationValue<String></code>
   */
  public ConfigurationValue<String> next(boolean preserveQuotes) throws ConfigurationSyntaxException {
    if (!this.advanceToNextToken()) {
      // No additional tokens to parse
      return null;
    }

    char beginChar = this.getChar();
    boolean isQuotedToken = (beginChar == '"' || beginChar == '\'');

    // Find the end of this token
    ConfigurationValue<String> nextToken = this.extractNextToken(isQuotedToken, beginChar);

    if (preserveQuotes) {
      return nextToken;
    } else {
      return this.getStringWithoutQuotes(nextToken);
    }
  }

  /**
   * Finds and returns the next list of Strings from this scanner. Supports format: `{ "string1" "string2" "string3" }`.
   * @return a list of Strings, as parsed from the scanner.
   * @throws ConfigurationSyntaxException if the scanner does not find the required format.
   */
  public List<ConfigurationValue<String>> nextStringList() throws ConfigurationSyntaxException {
    List<ConfigurationValue<String>> list = new ArrayList<>();

    // Expect { to begin the list section
    boolean openingBrace = true;
    ConfigurationValue<String> currentToken = this.next();
    if (!currentToken.getValue().equals("{")) {
      FeedbackProcessor.warn(logger,"Each 'string list' block should begin with a '{' character.",
              currentToken, Severity.MAJOR);
      openingBrace = false;
      list.add(currentToken);
    }

    long braceCount = 1;  // Inside string list (even if no opening brace)

    boolean hasMoreListItems = true;
    while (this.hasNext() && hasMoreListItems) {
      if (this.isNextChar('}', true)) {
        braceCount--;
        // If next char is a closing brace, and we've hit the final one, then the string list is complete.  If there
        // was an opening brace, read past this one.  If not, leave the pointer.
        if (braceCount == 0) {
          hasMoreListItems = false;
          if (openingBrace) {
            this.next();  // Push reading pointer passed the closing }.
          }
        } else {
          this.next();  // Push reading pointer passed this brace (inside string list)
        }
      } else if (this.isNextChar('/', true)) {
        // A '/' character indicates the start of a new token, and the end of the string list.
        hasMoreListItems = false;
      } else {
        currentToken = this.next();
        logger.trace("list item parse currentToken: {}", currentToken.getValue());
        if (currentToken.getValue().equals("{")) {
          braceCount++;
        } else {
          list.add(currentToken);
        }
      }
    }

    logger.trace("string list size: {}", list.size());

    return list;
  }

  /**
   * Quietly ignore the next bracketed section.  In general, no messages or errors will be logged.  If the
   * next read value is not an opening brace ({), the call will have no affect.
   */
  public void advancePastThisElement() {
    if (!isNextChar('{', false)) {
      return;
    }

    int originalLineIndex = this.getLineIndex();
    int originalCharIndex = this.getCharIndex();
    ConfigurationValue<?> startToken;

    // Clear the "{"
    try {
      startToken = this.next(false);
    } catch(ConfigurationSyntaxException csEx) {
      // Since `isNextChar()` succeeded, this should never happen.  If it does, leave reader pointer untouched.
      return;
    }

    long braceCount = 1;
    while (braceCount > 0) {
      try {
        ConfigurationValue<?> nextToken = this.next(true);
        // If next value is empty, it means the end of the configuration has been reached.
        if (nextToken == null) {
          // The braces were unmatched. Avoid missing the entirety of the configuration: reset the original pointers.
          FeedbackProcessor.error(logger, "Unclosed brace encountered.", startToken, Severity.MAJOR);
          this.configurationIndex = new ConfigurationIndex(originalCharIndex, originalLineIndex);
          this.incrementIndex(); // Passed the "startToken" value = "{".
          return;
        }
        String nextValue = nextToken.getValue().toString();
        if (nextValue.equals("{")) {
          braceCount++;
        } else if (nextValue.equals("}")) {
          braceCount--;
        }
      } catch(ConfigurationSyntaxException ignored) {
        // keep reading...
      }
    }
  }

  public boolean isNextChar(char charToCheck, boolean checkForString) {
    String currentLine = this.configuration.get(this.getLineIndex()).getContents();
    int charIndex = this.getCharIndex();
    if (charIndex >= currentLine.length()) {
      return false;
    }
    char nextChar = currentLine.charAt(charIndex);

    while (Character.isWhitespace(nextChar)) {
      charIndex++;
      if (charIndex == currentLine.length()) {
        return false;
      }
      nextChar = currentLine.charAt(charIndex);
    }

    if (nextChar != charToCheck) {
      return false;
    }

    if (!checkForString) {
      return true;
    }

    // Check if currently within quotes.
    int quoteIndex = currentLine.indexOf("\"");
    // If no quote, or first quote comes after the '#' char.
    if (quoteIndex < 0 || quoteIndex > charIndex) {
      return true;
    }
    boolean inQuote = true;
    while (quoteIndex > 0 && quoteIndex < charIndex) {
      try {
        quoteIndex = currentLine.indexOf("\"", quoteIndex + 1);
        if (quoteIndex >= 0 && quoteIndex < charIndex) {
          inQuote = !inQuote;
        }
      } catch (StringIndexOutOfBoundsException oobEx) {
        quoteIndex = -1;
        // Line finished with a double-quote.  'inQuote' is not affected.
      }
    }

    return !inQuote;
  }

  private boolean advanceToNextToken() {
    // Read ahead to see if there are any non-whitespace chars remaining before the end of the string
    while (this.hasNext()) {
      if (!Character.isWhitespace(getChar())) {
        // Found the start of a new token
        return true;
      }

      this.incrementIndex();
    }

    // Reached the end of the config string, no next token found
    return false;
  }

  private char getChar() {
    if (this.isNextChar('#', true)) {
      this.configurationIndex.setCharIndex(0);
      this.configurationIndex.incrementLineIndex();
      return this.getChar();
    }
    return this.configuration.get(this.getLineIndex()).getContents().charAt(this.getCharIndex());
  }

  /**
   * Get the next token.  A token cannot span lines including a quoted token.
   * @param isQuoted Whether the token appears quoted.
   * @param quoteChar The character used to quote (single or double quote)
   * @return A <code>ConfigurationValue</code>, string with filename and line number.
   */
  private ConfigurationValue<String> extractNextToken(boolean isQuoted, char quoteChar) {
    StringBuilder nextTokenBuilder = new StringBuilder();
    char lastChar = Character.MIN_VALUE;
    char nextChar = this.getChar();

    // If we are starting a label or a quoted string, save the char and increment.
    if (nextChar == '/' || (isQuoted && nextChar == quoteChar)) {
      nextTokenBuilder.append(nextChar);
      if (this.incrementIndex()) {
        // The line ended with a starting token (slash or quote) - return a blank string.
        return new ConfigurationValue<>(nextTokenBuilder.toString(), this.getCurrentFileName(),
                this.getCurrentLineNumber(), this.getCurrentIncludedFrom());
      }
      nextChar = this.getChar();
    }

    while (this.hasNext() && this.getCharactersLeftInCurrentLine() != 0) {
      if (isQuoted) {
        if (nextChar == quoteChar && lastChar != '\\') {
          // Done - found matching, non-escaped quote.
          return getTokenAndIncrement(nextTokenBuilder.append(nextChar).toString());
        }
      } else if (nextTokenBuilder.toString().length() == 0 && (nextChar == '{' || nextChar == '}')) {
        return getTokenAndIncrement(nextTokenBuilder.append(nextChar).toString());
      } else if (Character.isWhitespace(nextChar)) {
        // Whitespace found so the endIndex is the previous character.
        return getTokenAndIncrement(nextTokenBuilder.toString());
      } else if (this.isBeginningOfNextToken(nextChar)) {
        return new ConfigurationValue<>(nextTokenBuilder.toString(), this.getCurrentFileName(), this.getCurrentLineNumber(),
                this.getCurrentIncludedFrom());
      }

      nextTokenBuilder.append(nextChar);
      lastChar = nextChar;

      // The closing quote did not appear on this line.  Assume it should be at the end.
      if (isQuoted && this.getCharactersLeftInCurrentLine() == 1) {
        logger.error("Unterminated string encountered.  Token=\"{}\", File=\"{}\", Line={}",
                nextTokenBuilder.toString(), this.getCurrentFileName(), this.getCurrentLineNumber());
        nextTokenBuilder.append(nextChar);
        this.incrementIndex();
        return new ConfigurationValue<>(nextTokenBuilder.toString(), this.getCurrentFileName(), this.getCurrentLineNumber(),
                this.getCurrentIncludedFrom());
      }

      boolean endOfLine = this.getCharactersLeftInCurrentLine() == 1;
      this.incrementIndex();

      // Token do not span lines.  Parsing is finished.
      if (endOfLine) {
        break;
      }
      nextChar = this.getChar();
    }

    return new ConfigurationValue<>(nextTokenBuilder.toString(), this.getCurrentFileName(), this.getCurrentLineNumber(),
            this.getCurrentIncludedFrom());
  }

  private boolean isBeginningOfNextToken(char character) {
    return ((character == '/' ||
            character == '{' || character == '}' ||
            character == '"' || character == '\''));
  }

  private int getCharactersLeftInCurrentLine() {
    return this.configuration.get(this.getLineIndex()).getContents().length() - this.getCharIndex();
  }

  private ConfigurationValue<String> getStringWithoutQuotes(ConfigurationValue<String> optionallyQuotedToken) {
    String value = optionallyQuotedToken.getValue();
    if (value.startsWith("\"") || value.startsWith("'")) {
      value = value.substring(1);
    }
    if (value.endsWith("\"") || value.endsWith("'")) {
      // Remove quotes since quoted
      value = value.substring(0, value.length() - 1);
    }

    optionallyQuotedToken.setValue(value);
    return optionallyQuotedToken;
  }

  /**
   * Create a ConfigurationValue<String> with the current values.  Incrementing can change the line number and
   * file name which will make them the wrong values.  Capture the correct value, then increment.
   * @param value The value of the ConfigurationValue<String>
   * @return ConfigurationValue<String>
   */
  private ConfigurationValue<String> getTokenAndIncrement(String value) {
    ConfigurationValue<String> token = new ConfigurationValue<>(value, this.getCurrentFileName(),
            this.getCurrentLineNumber(), this.getCurrentIncludedFrom());
    this.incrementIndex();
    return token;
  }
}