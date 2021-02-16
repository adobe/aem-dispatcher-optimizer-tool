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

import com.adobe.aem.dot.common.helpers.PathEncodingHelper;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PathUtilTest {

  @Test
  public void isDirTest() {
    PathUtil pathUtil = new PathUtil();
    assertTrue(pathUtil.isDir("."));  // Intentional static usage here.
    assertFalse(PathUtil.isDir("/here"));

    String classPath = PathEncodingHelper.getDecodedClassPath(this.getClass());
    assertTrue(PathUtil.isDir(classPath));
    assertFalse(PathUtil.isDir(classPath + File.separator + "PathUtilTest.class"));
  }

  @Test
  public void isAbsoluteTest() {
    assertTrue(PathUtil.isAbsolute("/hello"));
    assertTrue(PathUtil.isAbsolute("\\hello"));
    assertFalse(PathUtil.isAbsolute("hello"));
    assertFalse(PathUtil.isAbsolute(""));
    assertFalse(PathUtil.isAbsolute(null));
  }

  @Test
  public void stripFirstPathElementTest() {
    assertTrue(PathUtil.stripFirstPathElement("hello").isEmpty());
    assertEquals("/there", PathUtil.stripFirstPathElement("/hello/there"));
    assertEquals("\\there", PathUtil.stripFirstPathElement("\\hello\\there"));
  }

  @Test
  public void getLastPathElementTest() {
    assertEquals("hello", PathUtil.getLastPathElement("hello"));
    assertEquals("there", PathUtil.getLastPathElement(FilenameUtils.separatorsToSystem("/hello/there")));
    assertEquals("there", PathUtil.getLastPathElement(FilenameUtils.separatorsToSystem("/hello/there/")));
  }

  @Test
  public void appendPathsTest() {
    assertEquals("hello", PathUtil.appendPaths("hello", null));
    assertEquals("hello2", PathUtil.appendPaths("hello2", ""));

    String appendedPaths = PathUtil.appendPaths("hello2", "hi\\\\hello");
    boolean osCheck = (appendedPaths.equals("hello2/hi/hello") || appendedPaths.equals("hello2\\hi\\hello"));
    assertTrue("Escaping is different on different OS", osCheck);
  }

  @Test
  public void startsWithParentFolderTest() {
    assertTrue(PathUtil.startsWithParentFolder("../hello2"));
    assertTrue(PathUtil.startsWithParentFolder("..\\hello2"));
    assertFalse(PathUtil.startsWithParentFolder("hi\\\\hello"));
    assertFalse(PathUtil.startsWithParentFolder("\\hi\\hello"));
    assertFalse(PathUtil.startsWithParentFolder("/hi\\hello"));
  }
}
