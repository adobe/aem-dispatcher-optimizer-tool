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

package com.adobe.aem.dot.dispatcher.core;

import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.ConfigurationFileFinder;
import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.parser.ConfigurationViolations;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationParser;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import com.adobe.aem.dot.dispatcher.core.resolver.IncludeResolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;

public class DispatcherConfigurationFactory {
  private final Logger logger = LoggerFactory.getLogger(DispatcherConfigurationFactory.class);

  /**
   * The parseConfiguration method builds a DispatcherConfiguration object by parsing dispatcher.any.
   * The dispatcher.any file is the entry point into the configuration.  The dispatcher.any file's
   * location is passed in via the dispatcherAnyFilePath parameter.
   * The method also returns a list of violations that were encountered.
   *
   * @param repoPath - this is the "mount" point for the project source.  This is essentially the root folder of the
   *                 output of the archetype.
   * @param dispatcherAnyFilePath - the path to the folder that contains the dispatcher.any file.
   * @return <code>ConfigurationParseResults</code> the instantiated dispatcher configuration and violations.
   * @throws ConfigurationException Can throw this if an error is encountered.
   */
  public ConfigurationParseResults<DispatcherConfiguration> parseConfiguration(String repoPath,
                                                                               String dispatcherAnyFilePath)
          throws ConfigurationException, IllegalArgumentException {
    if (StringUtils.isEmpty(repoPath)) {
      throw new IllegalArgumentException("The repo folder is not set.");
    }

    // first check to see if the repo that we're given exists (the mount point)
    File repoFile = new File(repoPath);
    if (!repoFile.exists()) {
      throw new ConfigurationException(MessageFormat.format("The `{0}` folder does not exist", repoPath));
    }

    File dispatcherAnyFile;
    if (StringUtils.isEmpty(dispatcherAnyFilePath)) {
      logger.info("The Dispatcher configuration directory is not set.");
      dispatcherAnyFile = FileUtils.getFile(repoPath, DispatcherConstants.DISPATCHER_ANY);
    } else {
      // now we can see if the 'anyDir' exists, this directory contains the dispatcher.any file
      dispatcherAnyFile = FileUtils.getFile(repoPath,
              PathUtil.appendPaths(dispatcherAnyFilePath, DispatcherConstants.DISPATCHER_ANY));
    }

    // If ANY file was not found in specified location, try to locate it somewhere under the 'repoPath' location.
    if (!dispatcherAnyFile.exists()) {
      logger.info("Unable to locate the \"{}\" file at path=\"{}\". Attempting to find another location.",
              DispatcherConstants.DISPATCHER_ANY, dispatcherAnyFile.getPath());
      ConfigurationFileFinder finder = new ConfigurationFileFinder(DispatcherConstants.DISPATCHER_ANY);
      dispatcherAnyFile = finder.findMainConfigurationFile(repoPath, false);

      // Config file was not found.
      if (dispatcherAnyFile == null) {
        ConfigurationViolations.addViolation("Could not find Dispatcher configuration file.", Severity.MAJOR,
                new ConfigurationSource(repoPath, 0));
        return new ConfigurationParseResults<>(null, ConfigurationViolations.getViolations());
      }
    }

    logger.info("Loading configuration from file.  File=\"{}\"", dispatcherAnyFile.getPath());

    try {
      String configStr = IOUtils.toString(dispatcherAnyFile.toURI().toURL(), StandardCharsets.UTF_8);
      IncludeResolver resolver = new IncludeResolver(configStr, dispatcherAnyFile.getParent(), repoPath);
      List<ConfigurationLine> config = resolver.resolve();
      ConfigurationParser parser = new ConfigurationParser();

      // Parse the provided configuration into a DispatcherConfiguration object
      return parser.parseConfiguration(config);
    } catch (ConfigurationSyntaxException e) {
      throw new ConfigurationException("Unable to parse the dispatcher configuration: " + dispatcherAnyFilePath, e);
    } catch (IOException e) {
      throw new ConfigurationException("Unable to process the dispatcher configuration file: " +
                                                  dispatcherAnyFilePath, e);
    }
  }
}
