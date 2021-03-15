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

package com.adobe.aem.dot.common.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Paths;

/**
 * A set of utilities to help and centralize handling paths on different operating systems.
 */
public class PathUtil {

  /**
   * Return whether the path is an absolute path.
   * @param path A system path
   * @return True if path is absolute
   */
  public static boolean isAbsolute(String path) {
    if (StringUtils.isEmpty(path)) {
      return false;
    }

    return (path.startsWith("/") || path.startsWith("\\"));
  }

  /**
   * Return the index of the last separator (slash) of the path.
   * @param path A system path
   * @return Return the index of the last separator of the path, or -1 if it does not exist.
   */
  public static int getLastSeparatorIndex(String path) {
    return Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
  }

  /**
   * Return whether the path starts with the ".." string, indicating the parent directory.
   * @param path A system path
   * @return Return the index of the last separator of the path, or -1 if it does not exist.
   */
  public static boolean startsWithParentFolder(String path) {
    return path != null && (path.startsWith("../") || path.startsWith("..\\"));
  }

  public static String[] split(String path) {
    String newPath = path.replaceAll("\\\\", "/");
    String[] splitUp = newPath.split("/");
    // Do not return the first empty element on absolute paths.
    if (StringUtils.isEmpty(splitUp[0])) {
        splitUp = ArrayUtils.removeElement(splitUp, splitUp[0]);
    }

    return splitUp;
  }

  /**
   * Return the path without the final folder.
   * @param path A path
   * @return path Path without the final folder. If not a path (no slashes), returns original string.
   */
  public static String stripLastPathElement(String path) {
    int lastSlash = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
    if (lastSlash >= 0) {
      return path.substring(0, lastSlash);
    }

    return "";
  }

  /**
   * Determine if a path (descendant) is a sub-folder of another path (ancestor).  Drive letters are ignored.
   * @param descendantPath The path further down in the path hierarchy.
   * @param ancestorPath The path higher up in the path hierarchy.
   * @return True if descendantPath is a subdirectory (with any level) of ancestorPath.
   */
  public static boolean isDescendantOf(String descendantPath, String ancestorPath) {
    descendantPath = removeDriveLetterPrefix(descendantPath);
    ancestorPath = removeDriveLetterPrefix(ancestorPath);
    if (!ancestorPath.endsWith(String.valueOf(File.separatorChar))) {
      ancestorPath += File.separatorChar;
    }
    return descendantPath.startsWith(ancestorPath);
  }

  /**
   * Remove
   * If the provided path (descendant) is a sub-folder of another path (ancestor).  Drive letters are ignored.
   * @param descendantPath The path further down in the path hierarchy.
   * @param ancestorPath The path higher up in the path hierarchy.
   * @return True if descendantPath is a subdirectory (with any level) of ancestorPath.
   */
  public static String removeAncestorPath(String descendantPath, String ancestorPath) {
    descendantPath = removeDriveLetterPrefix(descendantPath);
    ancestorPath = removeDriveLetterPrefix(ancestorPath);
    String subPath = descendantPath;
    if (isDescendantOf(descendantPath, ancestorPath)) {
      subPath = descendantPath.substring(ancestorPath.length());
      if (subPath.startsWith(String.valueOf(File.separatorChar))) {
        subPath = subPath.substring(1);
      }
    }
    return subPath;
  }

  /**
   * For a Windows' path, strip off the drive (ie "C:") from the path.  Assumes only one colon in the path, which is
   * part of the drive (i.e. "C:\...")
   * @param path A path
   * @return path Path without drive.
   */
  public static String removeDriveLetterPrefix(String path) {
    if (StringUtils.isNotEmpty(path) && path.contains(":")) {
      String[] split = path.split(":");
      path = split[1];
    }

    return path;
  }

  /**
   * Return the name of the final folder or filename.
   * @param path A path
   * @return path Path without the final folder. If not a path (no slashes), returns original string.
   */
  public static String getLastPathElement(String path) {
    path = removeDriveLetterPrefix(path);
    return Paths.get(path).getFileName().toString();
  }

/**
 * Return the path without the first folder.
 * @param path A path
 * @return path Path without the final folder. If not a path (no slashes), returns original string.
 */
  public static String stripFirstPathElement(String path) {
    // If absolute file, skip the first slash.
    if (path.startsWith("/") || path.startsWith("\\")) {
      path = path.substring(1);
    }
    int firstSlash = Math.max(path.indexOf("/"), path.indexOf("\\"));
    if (firstSlash >= 0) {
      return path.substring(firstSlash);
    }

    return "";
  }

  /**
   * Append a path to a base path, respecting the file system's separator char.
   * @param base The path to be appended to.
   * @param append The path to append to the base.
   * @return The concatenated path.
   */
  public static String appendPaths(String base, String append) {
    if (StringUtils.isEmpty(append)) {
      return base;
    }

    String appendedPath = FilenameUtils.separatorsToSystem(
            new StringBuilder().append(base).append(File.separatorChar).append(append).toString());
    if (appendedPath.contains("\\\\")) {
      appendedPath = appendedPath.replaceAll("\\\\\\\\", "\\\\");
    }
    if (appendedPath.contains("//")) {
      appendedPath = appendedPath.replaceAll("//", "/");
    }

    return FilenameUtils.separatorsToSystem(appendedPath);
  }

  /**
   * Return whether the path references a directory.
   * @param path A path
   * @return Whether the path references a directory.
   */
  public static boolean isDir(String path) {
    File test = new File(FilenameUtils.separatorsToSystem(path));
    return test.exists() && test.isDirectory();
  }
}
