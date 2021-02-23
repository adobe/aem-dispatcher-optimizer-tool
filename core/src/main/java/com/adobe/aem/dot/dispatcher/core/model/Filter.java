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
import com.adobe.aem.dot.common.util.MatchesBuilder;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationReader;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an individual filter from the Filter section of each Farm configuration.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class Filter extends LabeledConfigurationValue {
  private ConfigurationValue<RuleType> type;
  private ConfigurationValue<String> url;
  private ConfigurationValue<String> extension;
  private ConfigurationValue<String> selectors;
  private ConfigurationValue<String> suffix;
  private ConfigurationValue<String> path;
  private ConfigurationValue<String> method;
  private ConfigurationValue<String> query;
  private ConfigurationValue<String> glob;

  private static final Logger logger = LoggerFactory.getLogger(Filter.class.getName());

  Logger getLogger() {
    return logger;
  }

  String getSimpleClassName() {
    return getClass().getSimpleName();
  }

  public RuleType getType() {
    return this.type == null ? null : this.type.getValue();
  }

  public void setType(ConfigurationValue<RuleType> type) {
    this.type = type;
  }

  public String getUrl() {
    return this.url == null ? null : this.url.getValue();
  }

  public void setUrl(ConfigurationValue<String> value) {
    this.url = value;
  }

  public String getExtension() {
    return this.extension == null ? null : this.extension.getValue();
  }

  public void setExtension(ConfigurationValue<String> value) {
    this.extension = value;
  }

  public String getSelectors() {
    return this.selectors == null ? null : this.selectors.getValue();
  }

  public void setSelectors(ConfigurationValue<String> value) {
    this.selectors = value;
  }

  public String getSuffix() {
    return this.suffix == null ? null : this.suffix.getValue();
  }

  public void setSuffix(ConfigurationValue<String> value) {
    this.suffix = value;
  }

  public String getPath() {
    return this.path == null ? null : this.path.getValue();
  }

  public void setPath(ConfigurationValue<String> value) {
    this.path = value;
  }

  public String getMethod() {
    return this.method == null ? null : this.method.getValue();
  }

  public void setMethod(ConfigurationValue<String> value) {
    this.method = value;
  }

  public String getQuery() {
    return this.query == null ? null : this.query.getValue();
  }

  public void setQuery(ConfigurationValue<String> value) {
    this.query = value;
  }

  public String getGlob() {
    return this.glob == null ? null : this.glob.getValue();
  }

  public void setGlob(ConfigurationValue<String> value) {
    this.glob = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    Filter filter = (Filter) o;

    return new MatchesBuilder()
            .append(getType(), filter.getType())
            .append(getUrl(), filter.getUrl())
            .append(getExtension(), filter.getExtension())
            .append(getSelectors(), filter.getSelectors())
            .append(getPath(), filter.getPath())
            .append(getMethod(), filter.getMethod())
            .append(getQuery(), filter.getQuery())
            .append(getGlob(), filter.getGlob())
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(39, 137)
            .append(getType())
            .append(getUrl())
            .append(getExtension())
            .append(getSelectors())
            .append(getPath())
            .append(getMethod())
            .append(getQuery())
            .append(getGlob())
            .toHashCode();
  }

  static ConfigurationValue<List<Filter>> parseFilters(ConfigurationReader reader) throws ConfigurationSyntaxException {
    List<Filter> filters = new ArrayList<>();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /filter block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MAJOR);
      return new ConfigurationValue<>(new ArrayList<>());
    }

    ConfigurationValue<String> firstToken = reader.next(); // Advance the reader's pointer passed the "{" marker.
    boolean hasMoreFilters = true;
    ConfigurationValue<String> currentToken;
    while (reader.hasNext() && hasMoreFilters) {
      currentToken = reader.next();
      if (currentToken.getValue().equals("}")) {
        // We've reached the end of the /filter section
        hasMoreFilters = false;
      } else if (!currentToken.getValue().startsWith("/") && !currentToken.getValue().startsWith("{")) {
        // Names should start with a /.  If no name, it should be an opening brace.
        FeedbackProcessor.error(logger, "Skipping unknown token in filter. Value=\"{0}\". Skipping value.",
                currentToken, Severity.MAJOR);
      } else {
        // If the next character is a open brace, the label (/0001) was skipped.
        boolean missingLabel = currentToken.getValue().equals("{");
        Filter filter = Filter.parseFilter(reader, missingLabel);
        if (missingLabel) {
          FeedbackProcessor.info(logger, "Label on Filter is missing.", currentToken);
        } else {
          currentToken.setValue(currentToken.getValue().replace("/", ""));
          filter.setLabel(currentToken);
        }
        filters.add(filter);
      }
    }

    return new ConfigurationValue<>(filters,
            firstToken.getFileName(), firstToken.getLineNumber(), firstToken.getIncludedFrom());
  }

  private static Filter parseFilter(ConfigurationReader reader, boolean skipLabel)
          throws ConfigurationSyntaxException {
    Filter filter = new Filter();
    ConfigurationValue<String> nextToken;

    // Expect { to begin the filter block, unless there is no label.
    if (!skipLabel) {
      if (!reader.isNextChar('{', false)) {
        FeedbackProcessor.error(logger,"Each filter must begin with a '{' character.",
                reader.getCurrentConfigurationValue(), Severity.MAJOR);
        return new Filter();
      }
      reader.next(); // Advance the reader's pointer passed the "{" marker.
    }

    boolean hasMoreFilterDetailsToProcess = true;
    while (reader.hasNext() && hasMoreFilterDetailsToProcess) {
      nextToken = reader.next();
      switch(nextToken.getValue()) {
        case "/type":
          // Parse type
          logger.trace("filter > type");
          ConfigurationValue<String> type = reader.next();
          RuleType ruleType = "deny".equalsIgnoreCase(type.getValue()) ? RuleType.DENY : RuleType.ALLOW;
          filter.setType(new ConfigurationValue<>(ruleType, type.getFileName(), type.getLineNumber(),
                  type.getIncludedFrom()));
          break;
        case "/url":
          // Parse url
          logger.trace("filter > url");
          ConfigurationValue<String> url = reader.next();
          filter.setUrl(url);
          break;
        case "/extension":
          // Parse extension
          logger.trace("filter > extension");
          ConfigurationValue<String> extension = reader.next();
          filter.setExtension(extension);
          break;
        case "/path":
          // Parse path
          logger.trace("filter > path");
          ConfigurationValue<String> path = reader.nextString();
          filter.setPath(path);
          break;
        case "/selectors":
          // Parse selectors
          logger.trace("filter > selectors");
          ConfigurationValue<String> selectors = reader.next();
          filter.setSelectors(selectors);
          break;
        case "/suffix":
          // Parse suffix
          logger.trace("filter > suffix");
          ConfigurationValue<String> suffix = reader.next();
          filter.setSuffix(suffix);
          break;
        case "/method":
          // Parse method
          logger.trace("filter > method");
          ConfigurationValue<String> method = reader.next();
          filter.setMethod(method);
          break;
        case "/query":
          // Parse query
          logger.trace("filter > query");
          ConfigurationValue<String> query = reader.next();
          filter.setQuery(query);
          break;
        case "/glob":
          // Parse glob
          logger.trace("filter > glob");
          ConfigurationValue<String> glob = reader.nextString();
          filter.setGlob(glob);
          break;
        case "}":
          logger.trace("end filter section");
          hasMoreFilterDetailsToProcess = false;
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /filter level token. Token=\"{}\".",
                  nextToken, Severity.MAJOR);
          reader.advancePastThisElement();
      }
    }

    return filter;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    appendIfNotEmpty(str, "Type", getType() == null ? null : getType().toString(), true);
    appendIfNotEmpty(str, "URL", getUrl(), false);
    appendIfNotEmpty(str, "Extension", getExtension(), false);
    appendIfNotEmpty(str, "Selectors", getSelectors(), false);
    appendIfNotEmpty(str, "Suffix", getSuffix(), false);
    appendIfNotEmpty(str, "Path", getPath(), false);
    appendIfNotEmpty(str, "Method", getMethod(), false);
    appendIfNotEmpty(str, "Query", getQuery(), false);
    appendIfNotEmpty(str, "Glob", getGlob(), false);
    return str.toString();
  }

  private void appendIfNotEmpty(StringBuilder sb, String name, String value, boolean firstValue) {
    if (StringUtils.isNotEmpty(value)) {
      if (!firstValue) {
        sb.append(",");
      }
      sb.append(name);
      sb.append("=");
      sb.append(value);
    }
  }

  /* ------------------------------------------------------------------------
   * Deserializer section for
   *
   private ConfigurationValue<RuleType> type;
   private ConfigurationValue<String> url;
   private ConfigurationValue<String> extension;
   private ConfigurationValue<String> selectors;
   private ConfigurationValue<String> suffix;
   private ConfigurationValue<String> path;
   private ConfigurationValue<String> method;
   private ConfigurationValue<String> query;
   private ConfigurationValue<String> glob;
   */
  public void setType(String type) {
    RuleType ruleType = RuleType.valueOf(type);
    this.type = new ConfigurationValue<>(ruleType);
  }

  public void setURL(String value) {
    this.url = new ConfigurationValue<>(value);
  }

  public void setExtension(String value) {
    this.extension = new ConfigurationValue<>(value);
  }

  public void setSelectors(String value) {
    this.selectors = new ConfigurationValue<>(value);
  }

  public void setSuffix(String value) {
    this.suffix = new ConfigurationValue<>(value);
  }

  public void setPath(String value) {
    this.path = new ConfigurationValue<>(value);
  }

  public void setMethod(String value) {
    this.method = new ConfigurationValue<>(value);
  }

  public void setQuery(String value) {
    this.query = new ConfigurationValue<>(value);
  }

  public void setGlob(String value) {
    this.glob = new ConfigurationValue<>(value);
  }
  /* ------------------------------------------------------------------------ */
}
