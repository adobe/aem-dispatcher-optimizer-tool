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

package com.adobe.aem.dot.dispatcher.core.resolver;

import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.FileResolver;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.common.util.PropertiesUtil;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IncludeResolver {
  private static final Logger logger = LoggerFactory.getLogger(IncludeResolver.class);

  private final String unresolvedConfig;
  private final String basePath;
  private final String repoPath;
  private final List<ConfigurationLine> config = new ArrayList<>();

  private long includeDepthCount = 0;

  public IncludeResolver(String unresolvedConfig, String basePath, String repoPath) {
    this.unresolvedConfig = unresolvedConfig;
    this.basePath = basePath;
    this.repoPath = repoPath;
  }

  public List<ConfigurationLine> resolve() throws ConfigurationSyntaxException, IOException {
    String[] configContent = this.unresolvedConfig.split("\\r?\\n|\\r");
    processLines(configContent);

    return config;
  }

  private void processLines(String[] lines) throws ConfigurationSyntaxException, IOException {
    processLines(lines, PathUtil.appendPaths(this.basePath, DispatcherConstants.DISPATCHER_ANY), null);
  }

  /**
   * Ingest the lines from a config file.  This could by an ANY file, or some $included file.
   * @param lines The lines of the file to ingest.
   * @param includeFile The file currently being ingested.
   * @param includedFrom The file which included the current "includeFile"
   * @return The number of lines processed so far, including any additional included files.
   * @throws ConfigurationSyntaxException Can throw this when a file is not readable.
   * @throws IOException Can throw this.
   */
  private long processLines(String[] lines, String includeFile, String includedFrom) throws
          ConfigurationSyntaxException, IOException {

    // Check if processing is increasing passed logical maximum values.
    checkForRunAwayProcessing(++includeDepthCount, config.size());

    int totalLines = lines.length;
    int lineCount = 0;
    for (String line : lines) {
      lineCount++;

      // Handle 'incorrect' symlinks on Windows - the file simply contains the relative path that it should link to.
      if (lineCount == 1 && SystemUtils.IS_OS_WINDOWS && line.trim().startsWith("../")) {
        logger.warn("Configuration line starts with \"../\". Assuming to be a Windows symlink indicator. File=\"{}\" Line={} Included from=\"{}\"",
                includeFile, lineCount, includedFrom == null ? "" : includedFrom);
        line = "$include \"" + line.trim() + "\"";
      }

      // if the line contains include then we need to resolve it
      if (!line.trim().startsWith("#") && StringUtils.contains(line, "$include")) {
        storeSurroundingIncludeText(line, true, includeFile, lineCount, includedFrom);

        String includeFolder = PathUtil.stripLastPathElement(includeFile);
        String fileToInclude = getFilePathFromInclude(line, includeFile);
        FileResolver fileResolver = new FileResolver(this.basePath, false);
        List<File> filesToInclude = fileResolver.resolveFiles(fileToInclude, includeFolder);

        for (File file : filesToInclude) {
          if (!file.canRead()) {
            throw new ConfigurationSyntaxException(MessageFormat.format(
                    "Unable to read {0}.  Make sure the file has read permissions enabled.",
                    fileToInclude), file.getPath(), -1);
          }
          if (file.isDirectory()) {
            logger.error("Skipping $include value because it is a directory. Directory=\"{}\".  Original line=\"{}\".",
                    file.getPath(), line);
            continue;
          }

          String includedFileContent = IOUtils.toString(new FileReader(file));
          totalLines += processLines(includedFileContent.split("\\r?\\n|\\r"), file.getPath(),
                  includeFile.substring(this.basePath.length() + 1));
        }

        storeSurroundingIncludeText(line, false, includeFile, lineCount, includedFrom);

      } else {
        addConfigLine(line, includeFile, lineCount, includedFrom);
      }
    }

    // File ($include) done processing so reduce the include depth.
    includeDepthCount--;

    return totalLines;
  }

  /**
   * getFileNameFromInclude will return the file path that's specified in the $include line.
   *
   * @param includeLine the line of the config that includes the $include file
   * @return the file path that's referenced by the $include
   * @throws ConfigurationSyntaxException - if the $include markup can not be parsed an error is thrown.  If the
   * $include is omitted, the includeLine is returned.
   */
  String getFilePathFromInclude(String includeLine, String includeFile) throws ConfigurationSyntaxException {
    if (includeLine.contains("$include")) {
      Pattern r = Pattern.compile("\"(.*)\"");
      Matcher m = r.matcher(includeLine);
      if (m.find()) {
        // now that we have the path of the file to include
        return m.group(1);
      } else {
        if (includeLine.contains("\"")) {
          logger.error("Unmatched double-quoted string encountered.  Token=\"{}\", File=\"{}\", Line={}", includeLine,
                  includeFile, -1);
          // Try to remedy missing quote.
          m = r.matcher(includeLine + "\"");
          if (m.find()) {
            // now that we have the path of the file to include
            return m.group(1);
          }
        }

        // If all else fails, return the rest of the string:
        String toInclude = includeLine.trim().substring("$include".length()).trim();
        if (StringUtils.isNotEmpty(toInclude.trim())) {
          return toInclude.trim();
        }

        throw new ConfigurationSyntaxException("Missing the $include value. Token " + includeLine, includeFile, -1);
      }
    } else {
      return includeLine;
    }
  }

  /**
   * Store the text around '$include "filename"' to our configuration.  It is assumed the filename is surrounded by
   * double quotes, and that only 1 $include is done on the line.  It is also assumed the remaining text should be
   * saved without processing.
   * @param line The line containing the $include
   * @param preInclude Whether to save the text before or after the $include
   * @param filename The filename containing the $include line.
   * @param lineCount The line number that the $include is on.
   */
  private void storeSurroundingIncludeText(String line, boolean preInclude, String filename, int lineCount,
                                           String includedFrom) {
    line = line.trim();
    int includeIndex = line.indexOf("$include");
    if ((preInclude && includeIndex > 0) || (!preInclude && includeIndex >= 0)) {
      String toSave;
      if (preInclude) {
        toSave = line.substring(0, includeIndex);
      } else {
        try {
          includeIndex = line.indexOf("\"", includeIndex + 1) + 1;
          while (includeIndex < line.length() && line.charAt(includeIndex) != '"') {
            includeIndex++;
          }
          toSave = line.substring(++includeIndex).trim();
        } catch(StringIndexOutOfBoundsException ex) {
          // Assumed that the ending double quote of the filename was at the end of the line.
          toSave = null;
        }
      }
      if (StringUtils.isNotEmpty(toSave)) {
        addConfigLine(toSave.trim(), filename, lineCount, includedFrom);
      }
    }
  }

  /**
   * Save non-null, non-empty, non-comment lines after trimming them.
   * @param content The line as read from the configuration file.
   * @param filename The path of the configuration file.
   * @param lineNumber The line number of the configuration file.
   */
  private void addConfigLine(String content, String filename, int lineNumber, String includedFrom) {
    if (content != null && StringUtils.isNotEmpty(content.trim()) && !content.trim().startsWith("#")) {
      config.add(new ConfigurationLine(content.trim(), this.getRelativePathToFile(filename), lineNumber, includedFrom));
    }
  }

  private String getRelativePathToFile(String filePath) {
    String prefixToRemove = this.repoPath + File.separator;
    return StringUtils.removeStart(filePath, prefixToRemove);
  }

  /**
   * Validate the size and state of the loading of the configuration to ensure the load is not surpassing the
   * logical maximum sizes.
   * @throws ConfigurationSyntaxException - throws this exception if maximum values are surpassed.
   */
  private void checkForRunAwayProcessing(long includeDepthCount, int configurationLines)
          throws ConfigurationSyntaxException {
    // Monitor the depth level of the include (stack overflow).
    long maximumIncludes = PropertiesUtil.getLongProperty(PropertiesUtil.MAX_INCLUDE_DEPTH_PROP, 50);
    if (includeDepthCount >= maximumIncludes) {
      String message = MessageFormat.format(
              "Maximum number of Dispatcher includes ({0}) encountered.  Check for circular file includes or raise the \"{1}\" property.",
              maximumIncludes, PropertiesUtil.MAX_INCLUDE_DEPTH_PROP);
      logger.error(message);
      throw new ConfigurationSyntaxException(message, null, -1);
    }

    // Monitor the number of configurations lines.
    long maximumLines = PropertiesUtil.getLongProperty(PropertiesUtil.MAX_LINES_PROP, 1000000);
    if (configurationLines > maximumLines) {
      String message = MessageFormat.format(
              "Maximum number of Dispatcher configuration lines ({0}) encountered.  Check for circular file includes or raise the \"{1}\" property.",
              maximumLines, PropertiesUtil.MAX_LINES_PROP);
      logger.error(message);
      throw new ConfigurationSyntaxException(message, null, -1);
    }
  }
}
