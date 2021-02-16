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

import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PathEncodingHelper {

  public static String getDecodedClassPath(Class<?> clazz) {
    return PathEncodingHelper.getDecodedPath(clazz.getResource("").getPath());
  }

  public static String getDecodedPath(String path) {
    while (path.contains(":")) {
      path = path.substring(1);
    }
    try {
      path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException useEX) {
      Assert.fail("Encoding of class path not UTF8 compatible: " + path);
    }

    return path;
  }

  /**
   * Get the absolute path of the test module indicated by testDirectoryPath.
   */
  public static String getPathToTestModule(Class<?> clazz, String testDirectoryPath) {
    String classPath = PathEncodingHelper.getDecodedClassPath(clazz);
    int targetIndex = classPath.indexOf("/core/target/");
    return classPath.substring(0, targetIndex) + File.separatorChar + FilenameUtils.separatorsToSystem(testDirectoryPath);
  }
}
