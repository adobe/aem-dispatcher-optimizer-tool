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

package com.adobe.aem.dot.httpd.core;

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.ConfigurationFileFinder;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.adobe.aem.dot.httpd.core.parser.HttpdConfigurationParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

public class HttpdConfigurationFactory {
  private final Logger logger = LoggerFactory.getLogger(HttpdConfigurationFactory.class);

  /**
   * The getHttpdConfiguration function builds a HttpdConfiguration object by parsing the project's httpd.conf.
   * The httpd.conf file is the entry point into the configuration.  The httpd.conf file's
   * location is passed in via the httpdConfDirectoryPath parameter.
   *
   * @param repoPath - this is the "mount" point for the project source.  This is essentially the root folder of the
   *                 output of the archetype.
   * @param httpdConfDirectoryPath - the path to the folder that contains the httpd.conf file
   * @return <code>ConfigurationParseResults</code> the instantiated httpd configuration and violation list.
   * @throws ConfigurationException Can be thrown if an error is encountered
   */
  public ConfigurationParseResults<HttpdConfiguration> getHttpdConfiguration(String repoPath,
                                                                             String httpdConfDirectoryPath)
          throws ConfigurationException, IllegalArgumentException {
    if (StringUtils.isEmpty(repoPath)) {
      throw new IllegalArgumentException("The repo folder is not set.");
    }
    if (StringUtils.isEmpty(httpdConfDirectoryPath)) {
      logger.info("The Httpd configuration directory is not set.");
    }

    // First check to see if the repo that we're given exists (the mount point)
    File repoFile = new File(repoPath);
    if (!repoFile.exists()) {
      throw new ConfigurationException(MessageFormat.format("The `{0}` folder does not exist", repoPath));
    }

    // Next, see if the 'httpdConfDirectoryPath' exists, this directory contains the httpd.conf file
    File httpdConfFile = FileUtils.getFile(repoPath, httpdConfDirectoryPath + File.separator + HttpdConstants.HTTPD_CONF);

    if (!httpdConfFile.exists()) {
      logger.info("Unable to locate the \"{}\" file at \"{}\". Attempting to find another location.",
              HttpdConstants.HTTPD_CONF, httpdConfFile.getPath());

      ConfigurationFileFinder finder = new ConfigurationFileFinder(HttpdConstants.HTTPD_CONF);
      httpdConfFile = finder.findMainConfigurationFile(repoPath, false);

      // Config file was not found.
      if (httpdConfFile == null) {
        return null;
      }
    }

    logger.info("Loading configuration from file.  File=\"{}\"", httpdConfFile.getPath());

    try {
      // Parse the provided configuration file into a HttpdConfiguration object
      HttpdConfigurationParser parser = new HttpdConfigurationParser(repoPath);
      return parser.parseConfiguration(httpdConfFile);
    }
    catch (IOException e) {
      throw new ConfigurationException("Unable to process the Apache Httpd configuration file: " + httpdConfDirectoryPath, e);
    }
  }
}
