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

package com.adobe.aem.dot.httpd.core.parser;

import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.common.FileResolver;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.common.parser.ConfigurationViolations;
import com.adobe.aem.dot.common.util.FeedbackProcessor;
import com.adobe.aem.dot.common.util.PropertiesUtil;
import com.adobe.aem.dot.httpd.core.ConfigFileEntryReadResult;
import com.adobe.aem.dot.httpd.core.HttpdIncludeType;
import com.adobe.aem.dot.httpd.core.model.Directive;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.adobe.aem.dot.httpd.core.model.Section;
import com.adobe.aem.dot.httpd.core.model.VirtualHost;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Handles parsing of Apache Http Server config files.
 */
public class HttpdConfigurationParser {

  private static final Logger logger = LoggerFactory.getLogger(HttpdConfigurationParser.class);

  private final String repoPath;

  private long includeDepthCount = 0;

  public HttpdConfigurationParser(String repoPath) {
    this.repoPath = repoPath;
  }

  /**
   * Parse the provided configuration file into an HttpdConfiguration object.
   * @param configFile - A file object, where the contents conforms to the Apache Http Server config spec (httpd.conf)
   * @return A model of the parsed configuration, represented as an HttpdConfiguration object
   * @throws IOException when a file access issue is encountered
   */
  public ConfigurationParseResults<HttpdConfiguration> parseConfiguration(File configFile) throws IOException {
    // Start with a clean violation list.  Violations can be reported throughout the parsing process.
    ConfigurationViolations.clearViolations();

    List<ConfigurationLine> configLines = this.normalizeConfig(configFile,
            configFile.getParentFile().getPath(), true);

    // Parse ConfigurationLine items into an HttpdConfiguration object
    HttpdConfiguration config = this.parseConfigurationLines(configLines);

    return new ConfigurationParseResults<>(config, ConfigurationViolations.getViolations());
  }

  /**
   * Distill the given configuration file into a list of ConfigurationLine items. Comments will be omitted, lines will
   * be trimmed of whitespace, and configurations which span multiple lines (via "\") will be brought together in a
   * single ConfigurationLine item. For multiline items, the sourceLineNumber will point to the first line.
   * @param configFile The file to read and parse the configuration data from. Usually httpd.conf or a file it includes
   * @param basePath The path of the "top" configuration, usually the current working directory of httpd.conf
   * @param optional If set to true, a violation will not be raised if this file cannot be found
   * @return a list of ConfigurationLine items in the order that they were read.
   * @throws IOException when there is an issue reading from a file.
   * @throws FileNotFoundException when configuration file is not found, or is a directory.
   */
  protected List<ConfigurationLine> normalizeConfig(File configFile, String basePath, boolean optional)
          throws IOException, FileNotFoundException {
    List<ConfigurationLine> configurationLines = new ArrayList<>();

    verifyConfigFile(configFile, optional);

    // Increment track of include depth.
    includeDepthCount++;

    // Read the file's contents into memory
    FileReader reader = new FileReader(configFile);
    String configurationString = IOUtils.toString(reader);
    if (SystemUtils.IS_OS_WINDOWS && configurationString.startsWith("../")) {
      configurationLines.addAll(handleIncorrectSymLink(configFile, configurationString, basePath, optional));
    } else {
      String absolutePath = configFile.getAbsolutePath();
      String relativeFilePath = this.getRelativeFilePath(absolutePath, this.repoPath);

      int currentLineNumber = 0;
      try (Scanner scanner = new Scanner(configurationString)) {
        // Read a single line at a time
        while (scanner.hasNextLine()) {
          ConfigFileEntryReadResult readResult = this.getNextCompleteConfigurationLine(scanner, currentLineNumber);
          HttpdIncludeType includeType = readResult.isApacheIncludeDirective();
          if (includeType == HttpdIncludeType.NONE) {
            // This directive is NOT an include. Add it to the list
            configurationLines.add(new ConfigurationLine(readResult.getContents(), relativeFilePath, readResult.getLineNumber()));
          } else {
            // Handle include. Recursively call normalizeConfig with the included file(s)
            String toInclude = this.getPatternFromIncludeDirective(readResult.getContents());
            if (StringUtils.isNotEmpty(toInclude)) {
              List<File> includedFiles = this.getFilesToInclude(toInclude, basePath);

              // Check whether no files were found.  That is a error, unless $IncludeOptional was used.
              if (includedFiles.isEmpty() && includeType == HttpdIncludeType.INCLUDE) {
                FeedbackProcessor.error(logger,
                        "Include directive must include existing files.  Check path, or use IncludeOptional.",
                        "", new ConfigurationSource(relativeFilePath, readResult.getLineNumber()),
                        toInclude.contains("${") ? null : Severity.MAJOR);   // Not a violation if path has EnvVar.
              } else {
                configurationLines.addAll(includeConfigurationFiles(includedFiles, configurationLines.size(),
                        includeType, basePath, configFile, currentLineNumber));
              }
            }
          }

          // Increment currentLineNumber by the total number of lines read
          currentLineNumber = currentLineNumber + readResult.getTotalNumberOfLinesRead();
        }
      }
    }

    // File (include) done processing so reduce the include depth.
    includeDepthCount--;

    // Filter out non-configuration lines (comments and empty lines)
    return configurationLines
            .stream()
            .filter(ConfigurationLine::hasConfigurationContents)
            .collect(Collectors.toList());
  }

  private HttpdConfiguration parseConfigurationLines(List<ConfigurationLine> lines) {
    HttpdConfiguration topLevelConfiguration = new HttpdConfiguration();
    Iterator<ConfigurationLine> lineIterator = lines.iterator();

    this.buildHttpdConfigSection(lineIterator, topLevelConfiguration);

    return topLevelConfiguration;
  }

  private void buildHttpdConfigSection(Iterator<ConfigurationLine> lineIterator, Section currentSection) {
    // Iterate through each line, until we reach the end of the current section
    while (lineIterator.hasNext()) {
      ConfigurationLine line = lineIterator.next();

      logger.trace("Determining how to parse config line with contents=\"{}\"", line.getContents());

      // Have we reached the end of this section?
      if (this.isEndOfCurrentSection(line)) {
        logger.trace("End of the current section reached. SectionName={}", currentSection.getName());
        return;
      }

      // Handle conditionals, regular directives, and section directives separately
      if (this.isConditionalDirective(line)) {
        // Consider all conditions will be evaluated as TRUE.
        // Call this method recursively without adding a new Section, to add the contents of this condition into the
        // current section's context.
        buildHttpdConfigSection(lineIterator, currentSection);
      }
      else if (this.isSectionDirective(line)) {
        // This is a Section directive
        Section newSection = DirectiveFactory.getSectionInstance(line, currentSection);
        // Is it a VirtualHost type of Section? And is the current section an HttpConfiguration?
        if (newSection instanceof VirtualHost && currentSection instanceof HttpdConfiguration) {
          HttpdConfiguration httpdConfiguration = (HttpdConfiguration) currentSection;
          VirtualHost newVirtualHost = (VirtualHost) newSection;
          httpdConfiguration.getVirtualHosts().add(newVirtualHost);
          logger.trace("Added a new VirtualHost. VirtualHostName={} Arguments={}", newSection.getName(), newSection.getArguments());
        } else {
          currentSection.getSections().add(newSection);
          logger.trace("Added a new Section. SectionName={} Arguments={}", newSection.getName(), newSection.getArguments());
        }
        // Call this method recursively with the new Section as the current context
        buildHttpdConfigSection(lineIterator, newSection);
      }
      else {
        // This is a regular directive. Get an instance of it from the factory
        Directive newDirective = DirectiveFactory.getDirectiveInstance(line);
        currentSection.getDirectives().add(newDirective);
        logger.trace("Added a new Directive. DirectiveName={} Arguments={}", newDirective.getName(), newDirective.getArguments());
      }
    }
  }

  /**
   * Determine if this is a conditional directive (<If>, <IfModule>, etc.).
   * @param line - the ConfigurationLine in question
   * @return true if and only if this line is the beginning of a conditional directive
   */
  private boolean isConditionalDirective(ConfigurationLine line) {
    return line != null && line.getContents() != null && line.getContents().startsWith("<If");
  }

  /**
   * Determine if this line indicates the end of the current section.
   * @param line - the ConfigurationLine in question
   * @return true if and only if this line marks the end of the current section
   */
  private boolean isEndOfCurrentSection(ConfigurationLine line) {
    return line != null && line.getContents() != null && line.getContents().startsWith("</");
  }

  private boolean isSectionDirective(ConfigurationLine line) {
    // Section directives are wrapped in angle braces
    return line != null && line.getContents() != null &&
            line.getContents().startsWith("<") && line.getContents().endsWith(">");
  }

  private List<File> getFilesToInclude(String pattern, String basePath) {
    FileResolver fileResolver = new FileResolver(basePath, true);
    return fileResolver.resolveFiles(pattern);
  }

  /**
   * Process any "Include" directive.
   * @param directiveLine The line containing the Include directive.  It is assume the line will begin with "Include"
   *                      and getPatternFromIncludeDirective will not be called unless it does.
   * @return the file path to "Include"
   */
  private String getPatternFromIncludeDirective(String directiveLine) {
    HttpdConfigurationScanner scanner = new HttpdConfigurationScanner(directiveLine);

    // Read the "Include" or "IncludeOptional" token.
    scanner.next();

    // Return the pattern, which will be the next token
    if (scanner.hasNext()) {
      return scanner.next();
    } else {
      // TODO throw exception instead? If we end up here, there is an Include or IncludeOptional directive with no pattern
      return null;
    }
  }

  /**
   * Lines in httpd.conf can be continued onto the next line by ending a line with "\". This method detects this char
   * and produces a complete line by appending multiple lines together.
   * @param scanner The Apache scanner
   * @param currentLineNumber The current line being scanned
   * @return The next complete configuration line.
   */
  private ConfigFileEntryReadResult getNextCompleteConfigurationLine(Scanner scanner, int currentLineNumber) {
    String completeLine = scanner.nextLine().trim();
    // We've read one line: add 1 to currentLineNumber
    int startLineNumber = currentLineNumber + 1;
    int numberOfLines = 1;

    // Handle lines which end in \
    while (completeLine.endsWith("\\") && scanner.hasNextLine()) {
      // Remove last character, and append the (trimmed) next line
      String lineWithBackslashRemoved = completeLine.substring(0, completeLine.length() - 1);
      // Add a space separator only if the existing line contents does not already end with one
      completeLine = lineWithBackslashRemoved + (lineWithBackslashRemoved.endsWith(" ") ? "" : " ") +
              scanner.nextLine().trim();
      numberOfLines++;
    }

    return new ConfigFileEntryReadResult(completeLine, startLineNumber, numberOfLines);
  }

  private String getRelativeFilePath(String absolutePath, String basePath) {
    if (StringUtils.isEmpty(basePath)) {
      return absolutePath;
    }

    if (!basePath.endsWith(File.separator)) {
      basePath += File.separator;
    }
    int indexOfBasePathInAbsolute = absolutePath.indexOf(basePath);
    if (indexOfBasePathInAbsolute == -1) {
      // The absolutePath did not contain basePath
      return absolutePath;
    }
    // Return a substring of absolutePath beginning at the end of basePath
    return absolutePath.substring(indexOfBasePathInAbsolute + basePath.length());
  }

  /**
   * Validate the size and state of the loading of the configuration to ensure the load is not surpassing the
   * logical maximum sizes.
   * @throws IOException - throws this exception if maximum values are surpassed.
   */
  private void checkForRunAwayProcessing(long includeDepthCount, int configurationLines) throws IOException {
    // Monitor the depth level of the include (stack overflow).
    long maximumIncludes = PropertiesUtil.getLongProperty(PropertiesUtil.MAX_INCLUDE_DEPTH_PROP, 50);
    if (includeDepthCount >= maximumIncludes) {
      String message = MessageFormat.format(
              "Maximum number of Httpd includes ({0}) encountered.  Check for circular file includes or raise the \"{1}\" property.",
              maximumIncludes, PropertiesUtil.MAX_INCLUDE_DEPTH_PROP);
      logger.error(message);
      throw new IOException(message);
    }

    // Monitor the number of configurations lines.
    long maximumLines = PropertiesUtil.getLongProperty(PropertiesUtil.MAX_LINES_PROP, 1000000);
    if (configurationLines > maximumLines) {
      String message = MessageFormat.format(
              "Maximum number of Httpd configuration lines ({0}) encountered.  Check for circular file includes or raise the \"{1}\" property.",
              maximumLines, PropertiesUtil.MAX_LINES_PROP);
      logger.error(message);
      throw new IOException(message);
    }
  }

  private void verifyConfigFile(File configFile, boolean optional) throws FileNotFoundException {
    if (!configFile.exists() || configFile.isDirectory()) {
      // Strange exception might slip in where the file does not exist.  Report it here.
      if (configFile.getPath().contains("$")) {
        logger.error("Configuration file was not found. Appears to contain a misrepresented or unset environment variable. File=\"{}\"",
                configFile.getPath());
      } else {
        logger.error("Configuration file was not found.  File=\"{}\"", configFile.getPath());
      }

      if (!optional) {
        throw new FileNotFoundException(
                MessageFormat.format("Configuration file was not found.  File=\"{0}\"", configFile.getPath()));
      }
    }
  }

  private List<ConfigurationLine> handleIncorrectSymLink(File configFile, String line, String basePath, boolean optional)
          throws IOException, FileNotFoundException {
    // File started with a string which appears to be a relative path.  This usually indicates a poorly crafted
    // Symbolic Link, and the path points to the file that the link should resolve to.  Make an effort to
    // resolve the path and continue.
    String newPath = configFile.getParentFile().getParent();
    newPath += line.substring(2);
    newPath = FilenameUtils.separatorsToSystem(newPath)
                      .replace("\\.\\", "\\")
                      .replace("/./", "/");
    logger.info("WINDOWS: Sym Link \"{}\" did not resolve correctly. Trying \"{}\"", configFile.getPath(), newPath);

    File symFile = new File(newPath);
    if (symFile.exists() && !symFile.isDirectory()) {
      List<ConfigurationLine> windowsSymLinkLines = normalizeConfig(new File(newPath), basePath, optional);
      includeDepthCount--;
      return windowsSymLinkLines;
    }

    logger.error("WINDOWS: Sym Link could not be resolved. Verify the creation of symbolic links. Symlink=\"{}\" Line=\"{}\"",
            configFile.getName(), line);
    throw new FileNotFoundException(
            MessageFormat.format("Sym Link File could not be followed. Symlink=\"{0}\"  Line=\"{1}\"",
                    configFile.getPath(), line));
  }

  private List<ConfigurationLine> includeConfigurationFiles(List<File> files, int configurationSize,
          HttpdIncludeType includeType, String basePath, File configFile, int currentLineNumber)
          throws IOException {
    List<ConfigurationLine> includedLines = new ArrayList<>();

    for (File includeFile : files) {
      logger.trace("Including {}", includeFile.getPath());

      // Check if processing is increasing passed logical maximum values.
      checkForRunAwayProcessing(includeDepthCount, configurationSize);

      try {
        // Add contents of each file to our list of configurationLines
        List<ConfigurationLine> includeFileContents = this.normalizeConfig(includeFile, basePath,
                includeType == HttpdIncludeType.OPTIONAL);
        includedLines.addAll(includeFileContents);
      } catch (FileNotFoundException fnfEx) {
        if (includeType == HttpdIncludeType.INCLUDE) {
          FeedbackProcessor.error(logger,
                  "Include directive must include existing files.  Check path, or use IncludeOptional.",
                  "", new ConfigurationSource(configFile.getPath(), currentLineNumber),
                  includeFile.getPath().contains("${") ? null : Severity.MAJOR);   // Not a violation if path has EnvVar.
        }
      }
    }

    return includedLines;
  }
}
