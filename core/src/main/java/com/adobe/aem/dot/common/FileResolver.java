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

import com.adobe.aem.dot.common.util.PathUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Determine which file(s) match a particular path or pattern.
 */
public class FileResolver {

  private final String basePath;
  private final boolean allowDirectoryPath;

  private static final Logger logger = LoggerFactory.getLogger(FileResolver.class);

  private final static Map<String, String> cachedIncludeFiles = new HashMap<>();

  private static final String missingEnvVarStart = "_ENV___";
  private static final String missingEnvVarEnd = "__";

  /**
   * Instantiate a new FileResolver.
   * @param basePath - the starting point to handle relative path includes from
   * @param allowDirectoryPath - whether to allow a directory to be included, without wildcards
   */
  public FileResolver(String basePath, boolean allowDirectoryPath) {
    this.basePath = basePath;
    this.allowDirectoryPath = allowDirectoryPath;
  }

  /**
   * Return a list of File objects that get resolved based on the filePath. The filePath
   * can point to a single file, or can optionally include a glob reference.
   * @param filePath - the path to the file to include. May optionally contain a * (glob) character
   * @return List of file or files which get resolved from the filePath
   */
  public List<File> resolveFiles(String filePath) {
    return this.resolveFiles(filePath, this.basePath);
  }

  /**
   * resolveAbsoluteFilePath - return a list of File objects that get resolved based on the filePath.  The filePath
   * can point to a single file, or maybe a glob reference.
   *
   * FilePath could be a
   * - simple path, straight to the file to include (/base/folder/hello/config.conf or config.any)
   * - a relative path, including "../" characters starting from the base folder or the file that has the include line
   * - an absolute path that doesn't exists (/etc/httpd/conf.dispatcher.d/filters/ams_publish_filters.any)
   * - a path with a "any" wildcard (/base/folder/hello/*.conf)
   * - a path with an environment variable (env/${ENV}.conf)
   * - a path with an option wildcard (/base/folder/hello/maybe.con[f]) (both maybe.com or maybe.conf)
   * - a path with a mix:
   *     - /home/users/* /profile/querybuilder/savedsearch*
   *     - conf.client.d/* /flushPathMappings.con[f]
   *
   * 'resolveFiles' attempts to find the file by matching the CWD and the filePath to the existing file.
   *
   * @param filePath - the path to the file to include. May optionally contain a * (glob) character
   * @param currentWorkingDirectory - the folder where the file that is including another file resides
   * @return List of file or files which get resolved from the filePath.  They will exist, but are not guaranteed to
   * be files, readable, etc.
   */
  public List<File> resolveFiles(String filePath, String currentWorkingDirectory) {
    String resolvedPath = resolveEnvironmentVariables(filePath);
    if (resolvedPath.contains("${") && resolvedPath.contains("}")) {
      logger.error("Skipping file with unresolved environment variable.  Path=\"{}\"", resolvedPath);
      return new ArrayList<>();
    }

    // Resolve CWD and filepath, finding a common base.
    resolvedPath = getBaseCombinedPath(filePath, currentWorkingDirectory);
    if (resolvedPath == null) {
      logger.error("Skipping include with unresolvable path with respect to current environment.  Path=\"{}\"",
              filePath);
      return new ArrayList<>();
    }

    // If we have a * then we have a glob and need to collect the files (assumption is *'s come before [x]'s).
    if (resolvedPath.contains("*")) {
      logger.trace("Found wildcard (*). Path=\"{}\"", filePath);
      File globFile = new File(resolvedPath);
      File directory = globFile.getParentFile();
      FileFilter fileFilter = new WildcardFileFilter(globFile.getName());
      File[] files = directory.listFiles(fileFilter);

      if (files != null && files.length > 0) {
        // If directories are not allowed, then simply return our list instead of processing subdirectories.
        if (!allowDirectoryPath) {
          return Arrays.asList(files);
        }

        List<File> directoryFiles = new ArrayList<>();
        for (File nextFile: files) {
          String name = PathUtil.removeDriveLetterPrefix(nextFile.getPath());
          if (PathUtil.isDescendantOf(name, currentWorkingDirectory)) {
            name = PathUtil.removeAncestorPath(name, currentWorkingDirectory);
          } else {
            String target = PathUtil.stripLastPathElement(filePath);
            name = PathUtil.appendPaths(target, nextFile.getName());
          }
          List<File> nextFiles = resolveFiles(name, currentWorkingDirectory);
          if (!nextFiles.isEmpty()) {
            directoryFiles.addAll(nextFiles);
          }
        }
        return directoryFiles;
      } else {
        logger.info("No files included with wildcard include. Path=\"{}\"", resolvedPath);
        return Collections.emptyList();
      }
    } else if (resolvedPath.contains("[") && resolvedPath.contains("]")) {
      logger.trace("Found wildcard ([). Path=\"{}\"", filePath);
      List<File> files = new ArrayList<>();
      Pattern p = Pattern.compile("\\[(.*?)\\]");
      Matcher m = p.matcher(filePath);
      boolean foundOne = false;
      if (m.find()) {
        String inner = m.group(1);
        String wildcard = "\\[" + inner + "\\]";

        // Check for file without the [x] in the path.
        String path = FilenameUtils.separatorsToSystem(filePath.replaceAll(wildcard, ""));
        List<File> without = resolveFiles(path, currentWorkingDirectory);
        if (!without.isEmpty()) {
          files.addAll(without);
          foundOne = true;
        }

        // Check for file with the [x] in the path.
        path = FilenameUtils.separatorsToSystem(filePath.replaceAll(wildcard, inner));
        List<File> with = resolveFiles(path, currentWorkingDirectory);
        if (!with.isEmpty()) {
          files.addAll(with);
          foundOne = true;
        }
      }

      // [ ]  wildcards are supposed to be quietly resolved, without error if they don't exist.
      if (!foundOne) {
        logger.warn("Path with wildcard was not resolved. Path=\"{}\"", filePath);
      }

      return files;
    } else {
      File resolvedFile = new File(resolvedPath);
      if (resolvedFile.exists()) {
        if (resolvedFile.isDirectory()) {
          if (!allowDirectoryPath) {
            logger.error("Cannot include a directory.  Use wildcards to include the contents.  Path=\"{}\"", resolvedPath);
            return Collections.emptyList();
          }

          logger.warn("Including a directory is not recommended.  Instead, use wildcards.  Path=\"{}\"", resolvedPath);
          return resolveFiles(PathUtil.appendPaths(filePath, "*"), currentWorkingDirectory);
        }

        // It is a existing, non-directory file.
        return Collections.singletonList(resolvedFile);
      } else {
        return Collections.emptyList();
      }
    }
  }

  /**
   * Find environment variable indicators ("${...}") in the path, and replace them with the environment variable value.
   * @param line The line which contains environment variables to replace.
   * @param replaceMarkers Replace the env var markers, ${ and }, with underscores if the env var is not defined.
   *                       (This will allow the parser to read the value as one token.)
   * @param variablesNotResolved A <code>Set</code> of strings to contain the variables that could not be resolved.
   *                             If null, those variables will be logged as a warning.
   * @return The original path with env variables replaced, if possible.
   */
  public static String resolveEnvironmentVariables(String line, boolean replaceMarkers, Set<String> variablesNotResolved) {
    int index = line.indexOf("${");
    while (index >= 0) {
      int endIndex = line.indexOf("}", index);
      if (endIndex > index) {
        String name = line.substring(index + 2, endIndex);
        String envValue = System.getenv(name);
        String replace = "\\$\\{" + name + "}";
        if (StringUtils.isNotEmpty(envValue)) {
          line = line.replaceAll(replace, envValue);
          logger.trace("Replacing environment variable.. EnvVar=\"${{}}\" Value=\"{}\"", name, envValue);
        } else {
          if (variablesNotResolved != null) {
            variablesNotResolved.add(name);
          } else {
            logger.warn("Environment variable is not set.  EnvVar=\"{}\"", name);
          }
          if (replaceMarkers) {
            line = line.replaceAll(replace, missingEnvVarStart + name + missingEnvVarEnd);
          }
        }
      } else {
        logger.warn("Path variable contains unclosed environment variable placeholder (i.e. \"${...\").  Line=\"{}\"",
                line);
      }
      index = line.indexOf("${", index + 1);
    }

    return line;
  }

  /**
   * Find environment variable indicators ("${...}") in the path, and replace them with the environment variable value.
   * @param line The line which contains environment variables to replace.
   * @return The original path with env variables replaced, if possible.
   */
  private static String resolveEnvironmentVariables(String line) {
    return resolveEnvironmentVariables(line, false, null);
  }

  /**
   * Given a path to include, and the current path, find the intersection of the lowest common folder.
   *
   * includePath could be a
   * - simple path, straight to the file to include (/base/folder/hello/config.conf or config.any)
   * - a relative path, including "../" characters starting from the base folder or the file that has the include line
   * - an absolute path that doesn't exists (/etc/httpd/conf.dispatcher.d/filters/ams_publish_filters.any)
   * - a path with a "any" wildcard (/base/folder/hello/*.conf)
   * - a path with an environment variable (env/${ENV}.conf)
   * - a path with an option wildcard (/base/folder/hello/maybe.con[f]) (both maybe.con and/or maybe.conf)
   * - a path with a mix:
   *     - /home/users/* /profile/querybuilder/savedsearch*
   *     - conf.client.d/* /flushPathMappings.con[f]
   * @param includePath The path to include
   * @param cwd The current path, giving a area to find the intersection of the paths.
   * @return A lowest common denominator of paths, with file or wildcard section of the include path appended.
   */
  private String getBaseCombinedPath(String includePath, String cwd) {
    // Check if this combination has already been determined.  If so, use the cached value.
    String key = PathUtil.appendPaths(cwd, includePath);
    if (cachedIncludeFiles.containsKey(key)) {
      return cachedIncludeFiles.get(key);
    }

    String combinedPath = "";
    String includeSuffix;         // filename, or more, depending on wild cards used.
    String baseIncludePath;       // The path without the includeSuffix.

    // Determine the first wildcard occurrence.
    int wildCharIndex = includePath.indexOf("[");
    int wildcardIndex = includePath.indexOf("*");
    int firstWildIndex = wildCharIndex < 0 && wildcardIndex < 0 ? 0
            : wildCharIndex > 0 && wildcardIndex > 0 ? Math.min(wildCharIndex, wildcardIndex)
            : Math.max(wildCharIndex, wildcardIndex);
    if (firstWildIndex > 0) {
      char charBeforeWild = includePath.charAt(firstWildIndex - 1);
      baseIncludePath = includePath.substring(0, firstWildIndex - 1);
      // Was the wildcard in middle of a file or dir name?  Or right after a path separator?
      if (charBeforeWild != '/' && charBeforeWild != '\\') {
        baseIncludePath = PathUtil.stripLastPathElement(baseIncludePath);
      }
      includeSuffix = includePath.substring(baseIncludePath.length());
    } else {
      baseIncludePath = PathUtil.stripLastPathElement(includePath);
      includeSuffix = includePath.substring(baseIncludePath.length());
    }

    // Check for a file without any path being included.  If so, concatenate and return the value.
    if (StringUtils.isEmpty(baseIncludePath)) {
      cachedIncludeFiles.put(key, key);   // 'key' happens to be our value, cwd + '/' + includePath
      return key;
    }

    boolean isIncludeAbsolute = PathUtil.isAbsolute(includePath);

    String withoutRelativeParent = baseIncludePath;
    while (PathUtil.startsWithParentFolder(withoutRelativeParent)) {
      withoutRelativeParent = withoutRelativeParent.substring(3);
    }

    // Check for an easy overlap
    if (!isIncludeAbsolute && cwd.endsWith(baseIncludePath)) {
      cachedIncludeFiles.put(key, cwd + includeSuffix);
      return cachedIncludeFiles.get(key);
    }

    // See if the baseIncludePath can fit on any section of the cwd.
    String cwdSection = cwd;
    while (StringUtils.isNotEmpty(cwdSection)) {
      String lastBase = cwdSection;
      // If not absolute path, take away folders from the end.
      if (!isIncludeAbsolute) {
        String testPath = PathUtil.appendPaths(lastBase, withoutRelativeParent);
        if (PathUtil.isDir(testPath)) {
          combinedPath = testPath;
          break;
        }
      } else {
        // If absolute path, take away folders from the start.
        String subPath = baseIncludePath;
        while (StringUtils.isNotEmpty(subPath)) {
          String dirCheck = PathUtil.appendPaths(lastBase, subPath);
          if (PathUtil.isDir(dirCheck)) {
            // Double-Check: If no wildcards were found, see if the intended file exists in this location.
            // Helps in cases where a folder is used twice:  /configurations/conf/conf/file.conf
            File foundFile = null;
            if (firstWildIndex == 0) {
              foundFile = new File(PathUtil.appendPaths(dirCheck, includeSuffix));
            }
            if (foundFile == null || foundFile.exists()) {
              lastBase = PathUtil.appendPaths(lastBase, subPath);
              break;
            }
          }
          subPath = PathUtil.stripFirstPathElement(subPath);
        }
      }

      // Found a viable connection
      if (!lastBase.equals(cwdSection)) {
        combinedPath = lastBase;
        break;
      } else {
        cwdSection = PathUtil.stripLastPathElement(cwdSection);
      }
    }

    if (StringUtils.isEmpty(combinedPath)) {
      cachedIncludeFiles.put(key, null);
      return null;
    }

    cachedIncludeFiles.put(key, FilenameUtils.separatorsToSystem(combinedPath + includeSuffix));
    return cachedIncludeFiles.get(key);
  }
}
