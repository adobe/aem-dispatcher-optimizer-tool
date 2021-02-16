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

package com.adobe.aem.dot.common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;

public class ConfigurationFileFinder {
  private String configurationFileName;

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationFileFinder.class);

  private ConfigurationFileFinder() {}

  public ConfigurationFileFinder(String configurationFileName) {
    if (StringUtils.isEmpty(configurationFileName)) {
      throw new IllegalArgumentException("Provided configuration file name was null or empty.");
    }
    this.configurationFileName = configurationFileName;
  }

  /**
   * Given a directory to start a search, try to locate a configuration file.  If multiple files are found, the
   * first one encountered will be used.
   * @param baseDir The directory to start searching for the configuration file.
   * @param checkCurrentDirectoryOnFailure Specify whether, if a file cannot be found under the 'baseDir', a search
   *                                       under the current working directory should be executed.
   * @return The full path to the directory that the found configuration file resides in.
   * @throws ConfigurationException Thrown upon errors with the base (repo) directory.
   */
  public File findMainConfigurationFile(String baseDir, boolean checkCurrentDirectoryOnFailure)
          throws ConfigurationException {
    if (StringUtils.isEmpty(baseDir)) {
      throw new IllegalArgumentException("Provided REPO directory was null or empty.");
    }

    File source = new File(baseDir);
    if (!source.exists() || !source.isDirectory()) {
      throw new ConfigurationException(
              MessageFormat.format("Provided REPO directory '{0}' does not exist or is not a directory.",
                      baseDir));
    }

    IOFileFilter fileFilter = this.getConfigurationFileFilter(configurationFileName);
    Collection<File> files = FileUtils.listFiles(source, fileFilter, TrueFileFilter.INSTANCE);
    if (files.isEmpty()) {
      if (!checkCurrentDirectoryOnFailure) {
        logger.error("\"{}\" was not found within repo directory (repo=\"{}\")", configurationFileName, baseDir);
        return null;
      }
      logger.warn("\"{}\" was not found within repo directory (repo=\"{}\"). Looking under current directory.",
              configurationFileName, baseDir);
      return this.findMainConfigurationFile(".", false);
    }

    if (files.size() > 1) {
      logger.warn("More than 1 \"{}\" file was found in repo url (count={}).  Using first file.", configurationFileName,
              files.size());
    }

    // Use the first configuration file found (TrueFileFilter.INSTANCE ensures it is a file).  Since it was just found,
    // it is assumed to exist.
    return (File) files.toArray()[0];
  }

  private IOFileFilter getConfigurationFileFilter(String configFileName) {
    return new IOFileFilter() {
      @Override
      public boolean accept(File file) {
        return accept(file, "");
      }

      @Override
      public boolean accept(File file, String s) {
        return file.getName().equals(configFileName);
      }
    };
  }
}
