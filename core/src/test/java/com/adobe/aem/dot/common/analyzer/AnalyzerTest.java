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

package com.adobe.aem.dot.common.analyzer;

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleListFactory;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.analyzer.DispatcherAnalyzer;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.httpd.core.analyzer.HttpdAnalyzer;
import com.adobe.aem.dot.httpd.core.helpers.HttpdConfigurationTestHelper;
import com.adobe.aem.dot.httpd.core.model.Directive;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.FULL;
import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.MINIMIZED;
import static com.adobe.aem.dot.common.analyzer.ViolationVerbosity.PARTIAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AnalyzerTest {
  private AnalyzerRuleList analyzerRuleList;
  private DispatcherAnalyzer dispatcherAnalyzer;
  private HttpdAnalyzer httpdAnalyzer;

  @Before
  public void before() throws IOException {
    analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleList();
    dispatcherAnalyzer = new DispatcherAnalyzer(analyzerRuleList);
    httpdAnalyzer = new HttpdAnalyzer(analyzerRuleList);
  }

  @Test
  public void shouldFindViolations() {
    try {
      DispatcherConfigTestHelper helper = new DispatcherConfigTestHelper();
      String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "AnalyzerTest/" + DispatcherConstants.DISPATCHER_ANY);
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertEquals(0, results.getViolations(ViolationVerbosity.MINIMIZED).size());
      List<Violation> violations = dispatcherAnalyzer.getViolations(config, FULL);
      assertEquals("Should have 6 violations", 6, violations.size());
      assertTrue("First violation should contain MAJOR", violations.get(0).toString().contains("MAJOR"));

    } catch(ConfigurationException dcEx) {
      Assert.fail("Config should have loaded correctly: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void shouldFindSortedDispatcherViolations() {
    try {
      DispatcherConfigTestHelper helper = new DispatcherConfigTestHelper();
      String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
              "AnalyzerTest/" + DispatcherConstants.DISPATCHER_ANY);
      ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPath);
      DispatcherConfiguration config = results.getConfiguration();
      assertEquals(0, results.getViolations(ViolationVerbosity.MINIMIZED).size());
      List<Violation> violations = dispatcherAnalyzer.getViolations(config, MINIMIZED);
      assertEquals("Should have 2 violations, from 6", 2, violations.size());
      assertTrue("First violation should contain MAJOR", violations.get(0).toString().contains("MAJOR"));

      violations = dispatcherAnalyzer.getViolations(config, PARTIAL);
      assertEquals("Should have 2 violations, from 6", 2, violations.size());
      assertTrue("First violation should contain MAJOR", violations.get(0).toString().contains("MAJOR"));

    } catch(ConfigurationException dcEx) {
      Assert.fail("Config should have loaded correctly: " + dcEx.getLocalizedMessage());
    }
  }

  @Test
  public void shouldFindSortedHttpdViolations() {
    DispatcherConfigTestHelper helper = new DispatcherConfigTestHelper();
    String absPath = DispatcherConfigTestHelper.getPathFromProjectRoot(this.getClass(),
            "test-projects/test-project-all-rules-fail/dispatcher/src/conf/httpd.conf");

    HttpdConfiguration config = null;
    try {
      ConfigurationParseResults<HttpdConfiguration> results = helper.loadHttpdConfiguration(absPath);
      assertNotNull(results);
      config = results.getConfiguration();
    } catch(ConfigurationException dcEx1) {
      Assert.fail("Config should have loaded correctly: " + dcEx1.getLocalizedMessage());
    }

    List<Violation> violations = httpdAnalyzer.getViolations(config, MINIMIZED);
    assertEquals("Should have 1 violation", 1, violations.size());
    assertTrue("First violation should contain MAJOR", violations.get(0).toString().contains("MAJOR"));

    violations = httpdAnalyzer.getViolations(config, PARTIAL);
    assertEquals("Should have 1 violation", 1, violations.size());
    assertTrue("First violation should contain MAJOR", violations.get(0).toString().contains("MAJOR"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void dispatcherNullConfigTest() {
    dispatcherAnalyzer.getViolations(null, FULL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void httpdNullConfigTest() {
    httpdAnalyzer.getViolations(null, FULL);
  }

  @Test
  public void shouldExtendMultipleRules() {
    String path = "src/test/resources/rule-lists";
    File file = new File(path);
    String absolutePath = file.getAbsolutePath();
    try {
      // Provide a path to a folder with more rule files which will be merged with the 'core' rules.
      analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleList(FilenameUtils.separatorsToSystem(absolutePath));

      List<AnalyzerRule> newRules = analyzerRuleList.getEnabledRules();
      assertTrue("Should have more than 10 rules available", newRules.size() > 11);

      // Since aa_rules.json has the same rule id as the first 'core' rule, it will replace it.
      // glob: "*" should be replaced with glob: "TEST*"
      List<Check> checks = newRules.get(0).getChecks();
      assertEquals("First rule should have 1 (overwritten) check available", 1, checks.size());
      assertTrue("First index should have TEST", checks.get(0).getValueString().contains("TEST"));
      checks = newRules.get(1).getChecks();
      assertEquals("Second rule should have 1 checks available", 1, checks.size());

      // Core statfileslevel test (1) should be intact. aa_rules has a statfileslevel test as well, but the id
      // is different (AEMRules2:Disp-2) and the description is different, so it is appended to the end of the
      // rule list.
      Check check = newRules.get(1).getChecks().get(0);
      assertEquals("Second rule should have 1 checks available", 1, checks.size());
      assertEquals("Second rule's first check should have value 2", "2", check.getValue());

      // See if value 8, from aa_rules.json, was added.
      check = newRules.get(9).getChecks().get(0);
      assertEquals("Second rule's first check should have value 8", "8", check.getValue());

      // Uncomment to dump the combined rule list JSON
      //System.out.println(analyzerRuleList.toJsonString());

      AssertHelper.assertJsonFileEquals(path + "/gold/core_aa_bb.json",
              analyzerRuleList.toJsonString());
    } catch(IOException ioEx) {
      fail(ioEx.getClass().getName() + " : " + ioEx.getLocalizedMessage());
    }
  }

  @Test
  public void shouldReplaceRules() {
    String path = "src/test/resources/rule-lists/replace";
    try {
      // Provide a path to a folder with more rule files which will be merged with the 'core' rules.
      analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleList(FilenameUtils.separatorsToSystem(path));
      List<AnalyzerRule> newRules = analyzerRuleList.getEnabledRules();
      assertEquals("Should have 2 rule available", 2, newRules.size());

      AssertHelper.assertJsonFileEquals(path + "/../gold/cc_rules_result.json", analyzerRuleList.toJsonString());
    } catch(IOException ioEx) {
      fail(ioEx.getClass().getName() + " : " + ioEx.getLocalizedMessage());
    }
  }

  @Test
  public void shouldReplaceRulesWithInputStream() {
    try {
      // Try with non-existing resource
      InputStream nonExistentRulesFile = getClass().getClassLoader().getResourceAsStream(FilenameUtils.separatorsToSystem(
              "this/path/doesnt/exist.json"));
      analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleListFromInputStream(nonExistentRulesFile);

      // There should still be 9 rules, since the above InputStream is null
      assertNull("InputStream should be null", nonExistentRulesFile);
      assertEquals("Expect 9 rules", 9, analyzerRuleList.getRules().size());

      // Try again with a real rules file
      InputStream replacementRules = getClass().getClassLoader().getResourceAsStream(FilenameUtils.separatorsToSystem(
              "rule-lists/input-stream-test/rules-replace-test.json"));
      analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleListFromInputStream(replacementRules);

      assertNotNull("InputStream should not be null", replacementRules);
      assertEquals("Expect 2 rules", 2, analyzerRuleList.getRules().size());
      assertEquals("Expect last rule to be a BLOCKER", Severity.BLOCKER, analyzerRuleList.getRules().get(1).getSeverity());
      assertEquals("Expect last rule description to be correct", "REPLACED DOTRules:Httpd-1.", analyzerRuleList.getRules().get(1).getDescription());
      assertEquals("Expect last rule to be disabled", false, analyzerRuleList.getRules().get(1).isEnabled());
    } catch (Exception e) {
      fail("Should not have thrown an exception: " + e.getLocalizedMessage());
    }
  }

  @Test
  public void shouldExtendRulesWithInputStream() {
    try {
      InputStream extendRules = getClass().getClassLoader().getResourceAsStream(FilenameUtils.separatorsToSystem(
              "rule-lists/input-stream-test/rules-extend-test.json"));
      analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleListFromInputStream(extendRules);

      assertNotNull("InputStream should not be null", extendRules);
      assertEquals("Expect 10 rules", 10, analyzerRuleList.getRules().size());
      assertEquals("Expect last rule description to match", "NEW RULE!", analyzerRuleList.getRules().get(9).getDescription());
      assertEquals("Expect last rule ID to match", "NEWRules3:Disp-10", analyzerRuleList.getRules().get(9).getId());
    } catch (Exception e) {
      fail("Should not have thrown an exception: " + e.getLocalizedMessage());
    }
  }

  @Test
  public void nullRulesFolder() {
    try {
      analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleList(null);
      List<AnalyzerRule> newRules = analyzerRuleList.getEnabledRules();

      // Providing a null path should result in the same as just loading the core rules.
      assertTrue("Should have more than 3 rules available", newRules.size() > 3);
      List<Check> checks = newRules.get(0).getChecks();
      assertTrue("First rules should have 1 check available", checks.size() > 0);
    } catch(IOException ioEx) {
      fail(ioEx.getClass().getName() + " : " + ioEx.getLocalizedMessage());
    }
  }

  @Test
  public void nonExistentRulesFolder() {
    try {
      analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleList("/some/random/path/rule");
      List<AnalyzerRule> newRules = analyzerRuleList.getEnabledRules();
      // Providing a null path should result in the same as just loading the core rules.
      assertTrue("Should have more than 3 rules available", newRules.size() > 3);
      List<Check> checks = newRules.get(0).getChecks();
      assertTrue("First rules should have 1 check available", checks.size() > 0);
    } catch(IOException ioEx) {
      fail(ioEx.getClass().getName() + " : " + ioEx.getLocalizedMessage());
    }
  }

  @Test
  public void testGetDifferentRuleTypes() {
    assertTrue("Should have at least 1 Apache rules available",
            analyzerRuleList.getHttpdEnabledRules().size() >= 1);
    assertTrue("Should have at least 7 single farm rules available",
            analyzerRuleList.getEnabledSingleFarmRules().size() >= 7);
    assertTrue("Should have at least 1 multi farm rules available",
            analyzerRuleList.getEnabledMultiFarmRules().size() >= 1);
  }

  @Test
  public void testApacheHttpdRules() {
    HttpdConfiguration config = HttpdConfigurationTestHelper.getBasicHttpdConfiguration();

    List<Violation> violations = httpdAnalyzer.getViolations(config, FULL);
    assertEquals("Should have detected 0 violations", 0, violations.size());

    // Add a require all granted directive
    Directive requireAllGranted = new Directive("Require", Arrays.asList("all", "granted"));
    config.getVirtualHosts().get(0).getSections().get(0).setDirectives(Collections.singletonList(requireAllGranted));

    violations = httpdAnalyzer.getViolations(config, FULL);
    assertEquals("Should have detected 1 violations", 1, violations.size());

    // Change the Directory to be non-root
    HttpdConfiguration config2 = HttpdConfigurationTestHelper.getBasicHttpdConfiguration(
            Collections.singletonList("/mnt/var/www/default"));

    violations = httpdAnalyzer.getViolations(config2, FULL);
    assertEquals("Should have detected 0 violations, now that the directory is not root",
            0, violations.size());
  }

  @Test
  public void testReduceViolationList() throws IOException {
    AnalyzerRuleList list = AnalyzerRuleListFactory.getAnalyzerRuleList();
    List<Violation> violations = new ArrayList<>();
    assertTrue("Should have some rules", list.getEnabledRules().size() > 4);

    violations.add(new Violation(list.getEnabledRules().get(0), "context1",
            new ConfigurationSource("file1", 1, "included1")));
    violations.add(new Violation(list.getEnabledRules().get(1), "context2",
            new ConfigurationSource("file2", 1, "included2")));
    violations.add(new Violation(list.getEnabledRules().get(1), "context3",
            new ConfigurationSource("file2", 1, "included2")));
    violations.add(new Violation(list.getEnabledRules().get(1), "context4",
            new ConfigurationSource("file3", 1, "included3")));
    violations.add(new Violation(list.getEnabledRules().get(1), "context5",
            new ConfigurationSource("file4", 1, "included3")));

    List<Violation> counted = Analyzer.reduceViolationList(violations, true);
    assertNotNull(counted);
    assertEquals("Five violations should now be 2", 2, counted.size());

    CountedRuleViolation first = (CountedRuleViolation) counted.get(0);
    CountedRuleViolation second = (CountedRuleViolation) counted.get(1);
    assertEquals("First should have count 1", 1, first.getRuleViolationCount());
    assertEquals("First should have context1", "context1", first.getContext());
    assertEquals("Second should have count 3", 3, second.getRuleViolationCount());
    assertEquals("Second should have context2", "context2", second.getContext());
    assertEquals("Second should have rule Disp-2", "DOTRules:Disp-2---statfileslevel", second.getAnalyzerRule().getId());

    counted = Analyzer.reduceViolationList(violations, false);
    assertNotNull(counted);
    assertEquals("Five violations should now be 4", 4, counted.size());

    first = (CountedRuleViolation) counted.get(0);
    second = (CountedRuleViolation) counted.get(1);
    assertEquals("First should have count -1", -1, first.getRuleViolationCount());
    assertEquals("First should have context1", "context1", first.getContext());
    assertEquals("Second should have count -1", -1, second.getRuleViolationCount());
    assertEquals("Second should have context2", "context2", second.getContext());
    assertEquals("Second should have rule Disp-2", "DOTRules:Disp-2---statfileslevel", second.getAnalyzerRule().getId());

    assertEquals("Third should have context4", "context4", counted.get(2).getContext());
    assertEquals("Fourth should have context5", "context5", counted.get(3).getContext());
  }
}
