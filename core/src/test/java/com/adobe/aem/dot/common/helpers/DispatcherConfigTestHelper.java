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

package com.adobe.aem.dot.common.helpers;

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.DispatcherConfigurationFactory;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.model.Farm;
import com.adobe.aem.dot.httpd.core.HttpdConfigurationFactory;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DispatcherConfigTestHelper {

  private static DispatcherConfiguration getEmptyDispatcherConfig(String configName) {
    ConfigurationValue<String> name = new ConfigurationValue<>(configName,
            "DispatcherConfigTestHelper.any", 1);
    Farm farm = new Farm();
    List<ConfigurationValue<Farm>> farms = new ArrayList<>();
    farms.add(new ConfigurationValue<>(farm));

    return new DispatcherConfiguration(name, farms);
  }

  public static DispatcherConfiguration getEmptyDispatcherConfig() {
    return DispatcherConfigTestHelper.getEmptyDispatcherConfig("dispatcher");
  }

  public static String getAbsolutePath(String path) {
    File file = new File(path);
    return FilenameUtils.separatorsToSystem(file.getAbsolutePath());
  }

  public ConfigurationParseResults<DispatcherConfiguration> loadDispatcherConfiguration(String fullPath)
          throws ConfigurationException {
    // Split the path into 2 parts to simulate a repo & configPath: assumption is that the path will contain "/test..."
    int index = Math.max(fullPath.indexOf("/test"), fullPath.indexOf("\\test"));
    String repo = fullPath.substring(0, index);
    String anyPath = PathUtil.stripLastPathElement(fullPath.substring(index + 1));

    return loadDispatcherConfiguration(repo, anyPath);
  }

  public ConfigurationParseResults<HttpdConfiguration> loadHttpdConfiguration(String fullPath)
          throws ConfigurationException {
    // Split the path into 2 parts to simulate a repo & configPath: assumption is that the path will contain "/test..."
    int index = Math.max(fullPath.indexOf("/test"), fullPath.indexOf("\\test"));
    String repo = fullPath.substring(0, index);
    String confPath = PathUtil.stripLastPathElement(fullPath.substring(index + 1));

    return loadHttpdConfiguration(repo, confPath);
  }

  /**
   * Load a single configuration.
   *
   * @param repoURL A path to the repo (project root)
   * @param anyDir The path from teh repoURL to the any file directory
   * @return The <code>DispatcherConfiguration</code> loaded.
   * @throws ConfigurationException Could throw this.
   */
  private ConfigurationParseResults<DispatcherConfiguration> loadDispatcherConfiguration(String repoURL, String anyDir)
          throws ConfigurationException {
    DispatcherConfigurationFactory factory = new DispatcherConfigurationFactory();
    return factory.parseConfiguration(repoURL, anyDir);
  }

  /**
   * Load a single configuration.
   *
   * @param repoURL A path to the repo (project root)
   * @param anyDir The path from teh repoURL to the any file directory
   * @return The <code>DispatcherConfiguration</code> loaded.
   * @throws ConfigurationException Could throw this.
   */
  private ConfigurationParseResults<HttpdConfiguration> loadHttpdConfiguration(String repoURL, String anyDir)
          throws ConfigurationException {
    HttpdConfigurationFactory factory = new HttpdConfigurationFactory();
    return factory.getHttpdConfiguration(repoURL, anyDir);
  }

  /**
   * Find the resource's test files.  Ensure URL Encoding is decoded for the java operations.
   * @param clazz The class of the tests.
   * @param resourcePath The sub-path to the resource required.
   * @return The absolute path to the resource.
   */
  public static String getConfigFileAbsolutePath(Class<?> clazz, String resourcePath) {
    String decodedPath = PathEncodingHelper.getDecodedPath(resourcePath);
    URL configurations = clazz.getResource(decodedPath);
    if (configurations == null) {
      Assert.fail("Could not find config file: " + clazz.getResource("").getPath());
    }
    decodedPath = configurations.getPath();

    // The 'getPath()' can reintroduce encoding.
    decodedPath = PathEncodingHelper.getDecodedPath(decodedPath);
    File anyFile = new File(decodedPath);

    if (!anyFile.exists() || anyFile.isDirectory()) {
      Assert.fail("Resource did not exist " + configurations.getPath());
    }

    return anyFile.getAbsolutePath();
  }

  /**
   * Extract the project root path and append the testDirectoryPath to it.  This assume it is called with a Class
   * from the core project.
   * @param clazz A class from the core project
   * @param testDirectoryPath The path from the project root path
   * @return A concatenated path.
   */
  public static String getPathFromProjectRoot(Class<?> clazz, String testDirectoryPath) {
    String classPath = PathEncodingHelper.getDecodedClassPath(clazz);
    int targetIndex = classPath.indexOf("/core/target/");
    String projectPath = PathEncodingHelper.getDecodedPath(classPath.substring(0, targetIndex));
    return PathUtil.appendPaths(projectPath, FilenameUtils.separatorsToSystem(testDirectoryPath));
  }
}
