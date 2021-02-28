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

package com.adobe.aem;

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.analyzer.Violation;
import com.adobe.aem.dot.common.analyzer.ViolationVerbosity;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleListFactory;
import com.adobe.aem.dot.common.helpers.PathEncodingHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.dispatcher.core.DispatcherConfigurationFactory;
import com.adobe.aem.dot.dispatcher.core.analyzer.DispatcherAnalyzer;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.httpd.core.HttpdConfigurationFactory;
import com.adobe.aem.dot.httpd.core.analyzer.HttpdAnalyzer;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test the DOT with a dispatcher configuration which fails all the rules.
 */
public class AllRulesFailTest {

  @Test
  public void allRulesShouldFail() throws IOException, ConfigurationException {
    // Get path to test-projects/test-project-all-rules-fail/
    String testProjectPath = getPathToTestModule(this.getClass(), "test-projects/test-project-all-rules-fail/");

    List<Violation> violations = analyzeDispatcherModule(testProjectPath);
    // Collect all unique rule ids
    Set<String> violationRuleIds = violations.stream()
            .map(violation -> violation.getAnalyzerRule().getId())
            .collect(Collectors.toSet());

    String[] expectedRuleIds = {
            "DOTRules:Disp-S1---brace-missing",
            "DOTRules:Disp-S2---token-unexpected",
            "DOTRules:Disp-S3---quote-unmatched",
            "DOTRules:Disp-S4---brace-unclosed",
            "DOTRules:Disp-S5---mandatory-missing",
            "DOTRules:Disp-S6---property-deprecated",
            "DOTRules:Httpd-S1---include-failed",
            "DOTRules:Disp-1---ignoreUrlParams-allow-list",
            "DOTRules:Disp-2---statfileslevel",
            "DOTRules:Disp-3---gracePeriod",
            "DOTRules:Disp-4---default-filter-deny-rules",
            "DOTRules:Disp-5---serveStaleOnError",
            "DOTRules:Disp-6---suffix-allow-list",
            "DOTRules:Disp-7---selector-allow-list",
            "DOTRules:Disp-8---unique-farm-name",
            "DOTRules:Httpd-1---require-all-granted"
    };

    for (String expectedRuleId : expectedRuleIds) {
      if (!violationRuleIds.contains(expectedRuleId)) {
        // An expected ruleId was not found
        Assert.fail("One or more of the expected rule IDs was not found in the list of Violations: " + expectedRuleId +
                " (total unique violations found: " + violationRuleIds.size() + ", expected " + expectedRuleIds.length + ")");
      }
    }
  }

  /**
   * Helper method to report all Violations given a path to a sample/test dispatcher module.
   */
  public static List<Violation> analyzeDispatcherModule(String testProjectPath)
          throws ConfigurationException, IOException {
    // Parse the configurations
    DispatcherConfigurationFactory dispatcherFactory = new DispatcherConfigurationFactory();
    ConfigurationParseResults<DispatcherConfiguration> results = dispatcherFactory.parseConfiguration(testProjectPath,
            null);
    // Note any parsing violations
    List<Violation> violations = results.getViolations(ViolationVerbosity.MINIMIZED);

    DispatcherConfiguration dispatcherConfiguration = results.getConfiguration();

    HttpdConfigurationFactory httpdConfigurationFactory = new HttpdConfigurationFactory();
    ConfigurationParseResults<HttpdConfiguration> httpdResults =
            httpdConfigurationFactory.getHttpdConfiguration(testProjectPath, null);
    HttpdConfiguration httpdConfiguration = httpdResults.getConfiguration();
    violations.addAll(httpdResults.getViolations(ViolationVerbosity.MINIMIZED));

    // Analyze the configurations
    AnalyzerRuleList rules = AnalyzerRuleListFactory.getAnalyzerRuleList();
    DispatcherAnalyzer dispatcherAnalyzer = new DispatcherAnalyzer(rules);
    HttpdAnalyzer httpdAnalyzer = new HttpdAnalyzer(rules);
    violations.addAll(dispatcherAnalyzer.getViolations(dispatcherConfiguration,
            ViolationVerbosity.MINIMIZED));
    violations.addAll(httpdAnalyzer.getViolations(httpdConfiguration,
            ViolationVerbosity.MINIMIZED));

    return violations;
  }

  /**
   * Get the absolute path of the test module indicated by testDirectoryPath.
   */
  public static String getPathToTestModule(Class<?> clazz, String testDirectoryPath) {
    String classPath = PathEncodingHelper.getDecodedClassPath(clazz);
    int targetIndex = classPath.indexOf("/core/target/");
    return classPath.substring(0, targetIndex) + File.separatorChar + FilenameUtils.separatorsToSystem(testDirectoryPath);
  }
}
