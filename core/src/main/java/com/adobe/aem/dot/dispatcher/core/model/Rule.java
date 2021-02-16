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
 * Represents a glob type Rule, which is used by the ignoreUrlParams configuration
 * and others.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class Rule extends LabeledConfigurationValue {
  private ConfigurationValue<String> glob;
  private ConfigurationValue<RuleType> type;

  private static final Logger logger = LoggerFactory.getLogger(Rule.class);

  public Rule() {  }

  Logger getLogger() {
    return logger;
  }

  String getSimpleClassName() {
    return getClass().getSimpleName();
  }

  public String getGlob() {
    return this.glob == null ? null : this.glob.getValue();
  }

  public void setGlob(ConfigurationValue<String> glob) {
    this.glob = glob;
  }

  public RuleType getType() {
    return this.type == null ? null : this.type.getValue();
  }

  public void setType(ConfigurationValue<RuleType> type) {
    this.type = type;
  }

  /* Only here for test deserialization. */
  public void setGlob(String value) {
    this.glob = new ConfigurationValue<>(value);
  }

  /* Only here for test deserialization. */
  public void setType(String type) {
    this.type = new ConfigurationValue<>(RuleType.valueOf(type));
  }

  static ConfigurationValue<List<Rule>> parseRules(ConfigurationReader reader) throws ConfigurationSyntaxException {
    List<Rule> rules = new ArrayList<>();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /rules block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MINOR);
      return new ConfigurationValue<>(new ArrayList<>());
    }

    ConfigurationValue<String> firstToken = reader.next(); // Advance the reader's pointer passed the "{" marker.
    boolean hasMoreRules = true;
    ConfigurationValue<String> currentToken;
    while (reader.hasNext() && hasMoreRules) {
      currentToken = reader.next();
      if (currentToken.getValue().equals("}")) {
        // We've reached the end of the rules-style section
        hasMoreRules = false;
      } else if (!currentToken.getValue().startsWith("/") && !currentToken.getValue().startsWith("{")) {
        // Names should start with a /.  If no name, it should be an opening brace.
        FeedbackProcessor.error(logger, "Skipping unknown value in a rule. Value=\"{0}\". Skipping value.",
                currentToken, Severity.MINOR);
      } else {
        // If the next character is a open brace, the label (/0001) was skipped.
        boolean missingLabel = currentToken.getValue().equals("{");
        Rule rule = Rule.parseRule(reader, missingLabel);
        if (missingLabel) {
          FeedbackProcessor.info(logger, "Label on Rule is missing.", currentToken);
        } else {
          currentToken.setValue(currentToken.getValue().replace("/", ""));
          rule.setLabel(currentToken);
        }
        rules.add(rule);
      }
    }

    return new ConfigurationValue<>(rules,
            firstToken.getFileName(), firstToken.getLineNumber(), firstToken.getIncludedFrom());
  }

  private static Rule parseRule(ConfigurationReader reader, boolean skipLabel) throws ConfigurationSyntaxException {
    Rule rule = new Rule();
    ConfigurationValue<String> nextToken;

    // Expect { to begin the rule block, unless there is no label.
    if (!skipLabel) {
      if (!reader.isNextChar('{', false)) {
        FeedbackProcessor.error(logger,"Each rule must begin with a '{' character.",
                reader.getCurrentConfigurationValue(), Severity.MINOR);
        return new Rule();
      }
      reader.next(); // Advance the reader's pointer passed the "{" marker.
    }

    boolean hasMoreRuleDetailsToProcess = true;
    while (reader.hasNext() && hasMoreRuleDetailsToProcess) {
      nextToken = reader.next();
      switch(nextToken.getValue()) {
        case "/type":
          // Parse type
          logger.trace("rule > type");
          ConfigurationValue<String> type = reader.next();
          RuleType ruleType = "deny".equalsIgnoreCase(type.getValue()) ? RuleType.DENY : RuleType.ALLOW;
          rule.setType(new ConfigurationValue<>(ruleType, type.getFileName(), type.getLineNumber(),
                  type.getIncludedFrom()));
          break;
        case "/glob":
          // Parse glob
          logger.trace("rule > glob");
          ConfigurationValue<String> glob = reader.nextString();
          rule.setGlob(glob);
          break;
        case "}":
          logger.trace("end rule section");
          hasMoreRuleDetailsToProcess = false;
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /rule level token.  Token=\"{}\".", nextToken,
                  Severity.MINOR);
          reader.advancePastThisElement();
      }
    }

    return rule;
  }

  /**
   * Consider two rules equal if both the Glob and Type are the same (disregard Name).
   * @param o an object to compare
   * @return true if the Rules should be considered equal
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    Rule rule = (Rule) o;

    return new MatchesBuilder()
            .append(getGlob(), rule.getGlob())
            .append(getType(), rule.getType())
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(7, 91)
            .append(getGlob())
            .append(getType())
            .toHashCode();
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    if (StringUtils.isNotEmpty(this.getGlob())) {
      str.append("Glob=").append(this.getGlob());
    }
    if (null != this.getType()) {
      str.append(",Type=").append(this.getType());
    }
    if (StringUtils.isNotEmpty(this.getLabel())) {
      str.append(",Label=").append(this.getLabel());
    }
    return str.toString();
  }
}
