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

package com.adobe.aem.dot.common.parser;

import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.util.PathUtil;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.adobe.aem.dot.common.util.PropertiesUtil.DISP_VIOLATION_ELEMENT_PROP;
import static com.adobe.aem.dot.common.util.PropertiesUtil.getProperty;

/**
 * The ConfigurationViolations class records violations that occur during the parsing and the reading of the
 * Dispatcher configuration.  Based on the message, the violation is created with the appropriate information.
 */
public class ConfigurationViolations {
  @Getter
  private final static List<Violation> violations = new ArrayList<>();

  // List of Syntax violations, with their IDs, mapping to the error message snippet.
  public static final String UNKNOWN_VIOLATION_ID = "DOTRules:Syntax0---syntax-violation";
  public static final Map<String, String> parsingRuleMap = Stream.of(new String[][] {
          {"DOTRules:Disp-S1---brace-missing", " begin with a '{' character." },
          {"DOTRules:Disp-S2---token-unexpected", "Skipping unknown "},
          {"DOTRules:Disp-S3---quote-unmatched", "Unmatched quote "},
          {"DOTRules:Disp-S4---brace-unclosed", "Unclosed brace encountered."},
          {"DOTRules:Disp-S5---mandatory-missing", " is missing mandatory "},
          {"DOTRules:Disp-S6---property-deprecated", " is deprecated."},
          {"DOTRules:Disp-S7---no-dispatcher-config", "Could not find Dispatcher configuration file."},
          {"DOTRules:Httpd-S1---include-failed", "Include directive must include existing files."}
  }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

  public static void clearViolations() {
    violations.clear();
  }

  /**
   * Add a parsing violation to the list of configuration violations.
   * @param context The context/description of the violation
   * @param severity The severity of the violation
   * @param source The `ConfigurationSource` indicating where the violation occurred
   */
  public static void addViolation(String context, Severity severity, ConfigurationSource source) {
    String ruleId = UNKNOWN_VIOLATION_ID;
    for (Map.Entry<String, String> ruleEntry : parsingRuleMap.entrySet()) {
      if (context.contains(ruleEntry.getValue())) {
        ruleId = ruleEntry.getKey();
        break;
      }
    }

    // Set the Violation rule defaults
    String type = "Code Smell";
    List<String> tags = Arrays.asList("beta", "dispatcher");
    String element = "";
    try {
      element = getProperty(DISP_VIOLATION_ELEMENT_PROP);
    } catch(IOException ignore) {
    }

    AnalyzerRule rule = new AnalyzerRule();
    rule.setType(type);
    rule.setTags(tags);
    rule.setEffort("");
    rule.setSeverity(severity);
    rule.setDescription(context);
    rule.setId(ruleId);
    rule.setElement(element);
    rule.setEnabled(true);
    rule.setOrigin(PathUtil.getLastPathElement(source != null ? source.getFileName() : ""));

    violations.add(new Violation(rule, context, source));
  }
}
