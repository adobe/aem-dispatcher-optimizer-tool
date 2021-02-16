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

package com.adobe.aem.dot.httpd.core.analyzer;

import com.adobe.aem.dot.httpd.core.HttpdConstants;
import com.adobe.aem.dot.httpd.core.helpers.HttpdConfigurationTestHelper;
import com.adobe.aem.dot.httpd.core.model.DirectorySection;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.adobe.aem.dot.httpd.core.model.Section;
import com.adobe.aem.dot.httpd.core.model.VirtualHost;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HttpdCheckTargetLocatorTest {

  @Test(expected = IllegalArgumentException.class)
  public void determineCheckTargetsNullElement() {
    // Null element string
    HttpdConfiguration configuration = HttpdConfigurationTestHelper.getBasicHttpdConfiguration();
    HttpdCheckTargetLocator locator = new HttpdCheckTargetLocator(null);
    locator.determineCheckTargets(configuration);
  }

  @Test(expected = IllegalArgumentException.class)
  public void determineCheckTargetsNullConfig() {
    // Try with a null config
    HttpdCheckTargetLocator locator = new HttpdCheckTargetLocator(HttpdConstants.HTTPD + "." +
                                                                          HttpdConstants.VHOST);
    locator.determineCheckTargets(null);
  }

  @Test
  public void determineCheckTargetsEdgeCases() {
    HttpdConfiguration configuration = HttpdConfigurationTestHelper.getBasicHttpdConfiguration();
    // Try with a single token in the element string
    HttpdCheckTargetLocator locator = new HttpdCheckTargetLocator(HttpdConstants.HTTPD);
    assertNull("Should return null for an element with a single token", locator.determineCheckTargets(configuration));

    // Try with a farm element
    locator = new HttpdCheckTargetLocator("farm.filter");
    assertNull("Should return null for a non-httpd element", locator.determineCheckTargets(configuration));
  }

  @Test
  public void determineCheckTargetDirectoryRoot() {
    HttpdConfiguration configuration = HttpdConfigurationTestHelper.getBasicHttpdConfiguration();

    // Locate the target based on a "httpd.vhost.directory.root" element
    HttpdCheckTargetLocator locator = new HttpdCheckTargetLocator(
            HttpdConstants.HTTPD + "." +
                    HttpdConstants.VHOST + "." +
                    HttpdConstants.DIRECTORY + "." +
                    HttpdConstants.ROOT);
    List<Section> foundSections = locator.determineCheckTargets(configuration);

    assertEquals("Should have found 1 section", 1, foundSections.size());
    assertTrue("The target section should be a directory", foundSections.get(0) instanceof DirectorySection);
  }

  @Test(expected = IllegalArgumentException.class)
  public void determineCheckTargetUnknownFourthElement() {
    HttpdConfiguration configuration = HttpdConfigurationTestHelper.getBasicHttpdConfiguration();

    // Locate the target based on a "httpd.vhost.directory.root" element
    HttpdCheckTargetLocator locator = new HttpdCheckTargetLocator(
            HttpdConstants.HTTPD + "." +
                    HttpdConstants.VHOST + "." +
                    HttpdConstants.DIRECTORY + "." +
                    "unknown");
    List<Section> foundSections = locator.determineCheckTargets(configuration);
  }

  @Test
  public void determineCheckTargetVhost() {
    HttpdConfiguration configuration = HttpdConfigurationTestHelper.getBasicHttpdConfiguration();

    // Locate the target based on a "httpd.vhost" element
    HttpdCheckTargetLocator locator = new HttpdCheckTargetLocator(HttpdConstants.HTTPD + "." +
                                                                          HttpdConstants.VHOST);
    List<Section> foundSections = locator.determineCheckTargets(configuration);

    assertEquals("Should have found 1 section", 1, foundSections.size());
    assertTrue("The target section should be a vhost", foundSections.get(0) instanceof VirtualHost);
  }

  @Test
  public void determineCheckTargetMultipleDirectorySections() {
    HttpdConfiguration configuration = HttpdConfigurationTestHelper.getBasicHttpdConfiguration();

    DirectorySection otherDirectory = new DirectorySection("name", null, null, null);
    configuration.getVirtualHosts().get(0).getSections().add(otherDirectory);

    // Locate the target based on a "httpd.vhost.directory" element
    HttpdCheckTargetLocator locator = new HttpdCheckTargetLocator(HttpdConstants.HTTPD + "." +
                                                                          HttpdConstants.VHOST + "." +
                                                                          HttpdConstants.DIRECTORY);
    List<Section> foundSections = locator.determineCheckTargets(configuration);

    assertEquals("Should have found 2 sections", 2, foundSections.size());
    assertTrue("The 1st target section should be a Directory", foundSections.get(0) instanceof DirectorySection);
    assertEquals("The 1st target section should have 1 directive", 1, foundSections.get(0).getDirectives().size());
    assertTrue("The 2nd target section should be a Directory", foundSections.get(1) instanceof DirectorySection);
    assertEquals("The 2nd target section should have no directives", 0, foundSections.get(1).getDirectives().size());
  }
}