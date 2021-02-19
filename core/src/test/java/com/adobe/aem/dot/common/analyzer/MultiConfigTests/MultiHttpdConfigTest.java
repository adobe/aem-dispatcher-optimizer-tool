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

import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleListFactory;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.httpd.core.HttpdConstants;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.adobe.aem.dot.httpd.core.parser.HttpdConfigurationParser;
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

public class MultiHttpdConfigTest {
  private AnalyzerRuleList analyzerRuleList;
  private String targetReportPath = null;       // Only determine the target/dot-report path once per test.
  List<AnalysisData> configurationReportData = new ArrayList<>();   // Store PARTIAL data for final HTML index file.


  @Before
  public void before() throws IOException {
    analyzerRuleList = AnalyzerRuleListFactory.getAnalyzerRuleList();
  }

  /**
   * This test is a harness to parse many, many HTTPD configurations.  If the configurations
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
   * variable (i.e. targetDir="/Sample-Dispatcher-Configs-1/Folder-1"; )
   */
  @Test
  public void parseHTTPDConfigsWithinADirectory() {
    List<AnalysisData> parseViolationsByConfig = new ArrayList<>();   // Store FULL data for final HTML index file.
    String targetDir = "";
    URL configurations = this.getClass().getResource("configurations" + targetDir);
    if (configurations == null) {
      System.out.println("[INFO] Configurations do not exist. Skipping parseHTTPDConfigsWithinADirectory() test.");
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
    Collection<File> files = FileUtils.listFiles(source, httpdConfFilter, TrueFileFilter.INSTANCE);
    for (File nextConfDDirectory : files) {
      if (!nextConfDDirectory.exists()) {
        System.out.println("[WARN] HTTPD.CONF file did not exist: " + nextConfDDirectory.getPath());
        continue;
      }
      if (nextConfDDirectory.isDirectory()) {
        System.out.println("[WARN] HTTPD.CONF file is a directory: " + nextConfDDirectory.getPath());
        continue;
      }

      String result = parseHttpdConfFolder(nextConfDDirectory, parseViolationsByConfig);
      if (StringUtils.isNotEmpty(result)) {
        failures.add(result);
      }
    }

    if (MultiConfigHelper.runRuleViolationChecks) {
      if (!parseViolationsByConfig.isEmpty()) {
        parseViolationsByConfig.sort(Collections.reverseOrder());
        System.out.println("============= APACHE HTTPD VIOLATIONS: Parsing and Rules ====================");
        parseViolationsByConfig.forEach(value -> System.out.println(value.toString()));
      }

      String assertMessage = "A portion of the " + files.size() + " configurations do violate the rules.";
      MultiConfigHelper.writeIndexHtml(configurationReportData, "httpdIndex.html", targetReportPath);

      assertEquals(assertMessage, 48, parseViolationsByConfig.size());  // Lots of violations.
    } else {
      if (!failures.isEmpty()) {
        System.out.println("================================================");
        System.out.println(failures.stream()
                                   .map(String::valueOf)
                                   .collect(Collectors.joining("\n")));
      }

      String assertMessage = "None of the " + files.size() + " configurations should have failed.";
      assertEquals(assertMessage, 0, failures.size());
    }
  }

  // ========================================================================================
  // End of tests.  The remainder are private helper method.
  // ========================================================================================

  private String parseHttpdConfFolder(File httpdConf, List<AnalysisData> parseViolationsByConfig) {
    String failure = null;
    String directoryName = "<not set>";
    try {
      String absPathToConfigFile = httpdConf.getAbsolutePath();
      int coreIndex = Math.max(absPathToConfigFile.indexOf("/core"), absPathToConfigFile.indexOf("\\core"));
      String relativeConfPath = PathUtil.stripLastPathElement(absPathToConfigFile.substring(coreIndex + 1));
      if (StringUtils.isEmpty(targetReportPath)) {
        int index = Math.max(absPathToConfigFile.indexOf("/test-classes"), absPathToConfigFile.indexOf("\\test-classes"));
        targetReportPath = PathUtil.appendPaths(absPathToConfigFile.substring(0, index), "dot-reports");
      }

      directoryName = MultiConfigHelper.getConfigurationDirectoryId(relativeConfPath);
      System.out.println("Processing CONFD file with id: " + directoryName + " ============== START");

      // Set the repo path to the first config-specific directory (first one under .../configurations)
      String repoPath = "";
      File configurations = httpdConf;
      while (!configurations.getParentFile().getParent().endsWith("configurations")) {
        configurations = configurations.getParentFile();
        repoPath = configurations.getPath();
      }

      HttpdConfigurationParser parser = new HttpdConfigurationParser(repoPath);
      ConfigurationParseResults<HttpdConfiguration> results = parser.parseConfiguration(httpdConf);
      HttpdConfiguration httpdConfiguration = results.getConfiguration();
      if (httpdConfiguration == null) {
        failure = "Parse failed on: " + relativeConfPath;
      }

      if (MultiConfigHelper.runRuleViolationChecks) {
        AnalysisData data = MultiConfigHelper.getViolationCounts(directoryName, results, relativeConfPath,
                targetReportPath, analyzerRuleList, configurationReportData);
        if (data != null) {
          parseViolationsByConfig.add(data);
        }
      }

    } catch (Exception ex) {
      String cause = null;
      if (ex.getCause() != null) {
        cause = ex.getCause().getLocalizedMessage();
        ex.getCause().printStackTrace(System.err);
      } else {
        ex.printStackTrace(System.err);
      }
      String message = StringUtils.isNotEmpty(cause) ? cause : ex.getLocalizedMessage();
      failure = "Failure: Config=" + directoryName + ". Error: " +
                        ex.getClass().getName() + ":" + message;
    }

    return failure;
  }

  /**
   * Build a filter to find "httpd.conf" files.
   */
  private
  final IOFileFilter httpdConfFilter = new IOFileFilter() {
    @Override
    public boolean accept(File file) {
      return file.getName().equals(HttpdConstants.HTTPD_CONF);
    }

    @Override
    public boolean accept(File file, String s) {
      return file.getName().equals(HttpdConstants.HTTPD_CONF);
    }
  };
}
