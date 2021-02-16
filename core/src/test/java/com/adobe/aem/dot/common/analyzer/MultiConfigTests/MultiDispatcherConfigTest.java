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

package com.adobe.aem.dot.common.analyzer.MultiConfigTests;

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleListFactory;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class MultiDispatcherConfigTest {
  private DispatcherConfigTestHelper helper;
  private AnalyzerRuleList analyzerRuleList;
  private String targetReportPath = null;       // Only determine the target/dot-report path once per test.

  @Before
  public void before() throws IOException {
    helper = new DispatcherConfigTestHelper();
    analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleList();
  }

  /**
   * This test is a harness to parse many, many configurations.  If the configurations
   * are not set up, the test will not test anything.  Nothing under the 'configurations' file will be saved
   * in GIT (see .gitignore file)
   * The configurations to test should be in
   * core/src/test/resources/com/adobe/aem/common/analyzer/MultiConfigTests/configurations and can be of any folder
   * depth - the test will search for "dispatcher.any" files and iterate over them all.  The second directory in
   * depth will be used as an identifier (i.e. 'configurations/group1/configurationId1/conf...' will have
   * 'configurationId1' as its reporting identifier).
   * If using many zip files, each containing one configurations, the following steps may simplify the extraction
   * process:
   * - place the zip files in core/src/test/resources/com/adobe/aem/common/analyzer/MultiConfigTests/configurations
   * - cd to that 'analyzer/MultiConfigTests/configurations' folder
   * - run ../scripts/unzip_configs.sh <folder>
   *     - will unzip each configuration
   *     - will move that zip to the 'zipfiles' folder
   * - the desired result will be configurations, each in their own folder, in the 'configurations' folders
   * - 'unzip_configs.sh' will alert you and show you how to do 2 further changes:
   *     - rename folders & files that end with a period (mvn copying resources does not like that).
   *     - remove any '.git' folders in the sample data to avoid GIT confusion
   * Please update the scripts & instructions to handle any errors you run into.
   *
   * Environment variables, such as ENV_NAME, can be used in the configuration.  Set those to have the path
   * resolve them, if desired.
   *
   * Then execute the test below.
   *
   * If inspection of a particular configuration is desired, temporarily update the targetConfiguration
   * variable (i.e. targetConfiguration="/Sample-Dispatcher-Configs-1/Folder-1"; )
   */
  @Test
  public void parseDispatcherConfigurationsWithinADirectory() {
    List<AnalysisData> parseViolationsByConfig = new ArrayList<>();   // Store FULL data for final HTML index file.
    List<AnalysisData> configurationReportData = new ArrayList<>();   // Store PARTIAL data for final HTML index file.
    String targetConfiguration = "";
    URL configurations = this.getClass().getResource("configurations" + targetConfiguration);
    if (configurations == null) {
      System.out.println("[INFO] Configurations do not exist." +
                                 " Skipping processConfigurationsWithinADirectory() test.");
      return;
    }
    File source = new File(configurations.getPath());
    if (!source.exists() || !source.isDirectory()) {
      System.out.println("[INFO] Source could not be opened: " + configurations.getPath()
                                 + ". Skipping processConfigurationsWithinADirectory() test.");
      return;
    }

//    if (1 == 1) { // Quick way to avoid processing all the configurations.
//      return;
//    }

    List<String> failures = new ArrayList<>();
    Collection<File> files = FileUtils.listFiles(source, dispatcherAnyFilter, TrueFileFilter.INSTANCE);
    for (File nextAnyFile : files) {
      if (!nextAnyFile.exists()) {
        System.out.println("[WARN] Configuration did not exist: " + nextAnyFile.getPath());
        continue;
      }

      String directoryName = "<not set>";
      try {
        String absPathToConfigFile = nextAnyFile.getAbsolutePath();
        int coreIndex = Math.max(absPathToConfigFile.indexOf("/core"), absPathToConfigFile.indexOf("\\core"));
        String relativeConfPath = PathUtil.stripLastPathElement(absPathToConfigFile.substring(coreIndex + 1));
        if (StringUtils.isEmpty(targetReportPath)) {
          int index = Math.max(absPathToConfigFile.indexOf("/test-classes"), absPathToConfigFile.indexOf("\\test-classes"));
          targetReportPath = PathUtil.appendPaths(absPathToConfigFile.substring(0, index), "dot-reports");
        }

        directoryName = MultiConfigHelper.getConfigurationDirectoryId(relativeConfPath);
        System.out.println("Processing ANY file with id: " + directoryName + " ============== START");

        ConfigurationParseResults<DispatcherConfiguration> results = helper.loadDispatcherConfiguration(absPathToConfigFile);
        DispatcherConfiguration nextConfig = results.getConfiguration();
        if (nextConfig == null) {
          failures.add("Failure: Config=" + relativeConfPath + ". Error: Null on parseConfiguration.");
        } else if (nextConfig.getFarms() == null || nextConfig.getFarms().size() == 0) {
          // Do not fail if no farms were found.
          System.out.println("[ERROR] Config=" + relativeConfPath + ". Error: No farms found.");
        }

        // If requested, run the rules against the configs that were just read in.
        if (MultiConfigHelper.runRuleViolationChecks) {
          AnalysisData data = MultiConfigHelper.getViolationCounts(directoryName, results, relativeConfPath,
                  targetReportPath, analyzerRuleList, configurationReportData);
          if (data != null) {
            parseViolationsByConfig.add(data);
          }
        }
      } catch (Exception | ConfigurationException ex) {
        String message = ex.toString();
        if (ex.getCause() != null) {
          message = ex.getCause().toString();
        }
        //ex.printStackTrace(System.out);
        failures.add("Failure: Config=" + directoryName + ". Error: " + message);
      }
    }

    if (MultiConfigHelper.runRuleViolationChecks) {
      if (!parseViolationsByConfig.isEmpty()) {
        parseViolationsByConfig.sort(Collections.reverseOrder());
        System.out.println("============= DISPATCHER VIOLATIONS: Parsing and Rules ====================");
        parseViolationsByConfig.forEach(value -> System.out.println(value.toString()));
      }

      String assertMessage = "A portion of the " + files.size() + " configurations do violate the rules.";
      assertEquals(assertMessage, 513, parseViolationsByConfig.size());  // Lots of violations.

      MultiConfigHelper.writeIndexHtml(configurationReportData, "dispatcherIndex.html", targetReportPath);
    } else {
      if (!failures.isEmpty()) {
        System.out.println("============= DISPATCHER VIOLATIONS: Parsing only ====================");
        System.out.println(failures.stream()
                                   .map(String::valueOf)
                                   .collect(Collectors.joining("\n")));
      }

      String assertMessage = "None of the " + files.size() + " configurations should have failed.";
      assertEquals(assertMessage, 0, failures.size());
    }
  }

  /**
   * Build a filter to find "dispatcher.any" files.
   */
  private
  final IOFileFilter dispatcherAnyFilter = new IOFileFilter() {
    @Override
    public boolean accept(File file) {
      return file.getName().equals(DispatcherConstants.DISPATCHER_ANY);
    }

    @Override
    public boolean accept(File file, String s) {
      return file.getName().equals(DispatcherConstants.DISPATCHER_ANY);
    }
  };
}
