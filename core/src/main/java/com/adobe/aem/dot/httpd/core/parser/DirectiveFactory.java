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
import com.adobe.aem.dot.httpd.core.model.Directive;
import com.adobe.aem.dot.httpd.core.model.DirectorySection;
import com.adobe.aem.dot.httpd.core.model.FilesSection;
import com.adobe.aem.dot.httpd.core.model.LocationSection;
import com.adobe.aem.dot.httpd.core.model.Section;
import com.adobe.aem.dot.httpd.core.model.VirtualHost;

import java.util.ArrayList;
import java.util.List;

/**
 * Instantiate appropriate Section types based on the contents of the given ConfigurationLine.
 */
public class DirectiveFactory {

  /**
   * Construct an instance of a Section based on the contents of the provided ConfigurationLine.
   * @param line - the ConfigurationLine object to base this section on
   * @param parent - the parent section which will contain this new section
   * @return an instance of Section (or one of its subtypes)
   */
  public static Section getSectionInstance(ConfigurationLine line, Section parent) {
    if (line == null || line.getContents() == null) {
      return null;
    }

    // Remove the "<" prefix and ">" suffix from the section directive line
    String unwrappedContents = line.getContents().substring(1, line.getContents().length() - 1);
    HttpdConfigurationScanner scanner = new HttpdConfigurationScanner(unwrappedContents);

    // First token is the name
    String sectionName = scanner.next();
    List<String> arguments = new ArrayList<>();

    // Any additional tokens are the arguments
    while (scanner.hasNext()) {
      arguments.add(scanner.next());
    }

    Section newSection;

    // Construct the appropriate section subtype
    switch (sectionName) {
      case "VirtualHost":
        newSection = new VirtualHost(sectionName, arguments, line, parent);
        break;
      case "Directory":
      case "DirectoryMatch":
        newSection = new DirectorySection(sectionName, arguments, line, parent);
        break;
      case "Files":
      case "FilesMatch":
        newSection = new FilesSection(sectionName, arguments, line, parent);
        break;
      case "Location":
      case "LocationMatch":
        newSection = new LocationSection(sectionName, arguments, line, parent);
        break;
      default:
        newSection = new Section(sectionName, arguments, line, parent);
        break;
    }

    return newSection;
  }

  /**
   * Construct an instance of a Directive based on the contents of the provided ConfigurationLine.
   * @param line - the ConfigurationLine object to base this directive on
   * @return an instance of Directive
   */
  public static Directive getDirectiveInstance(ConfigurationLine line) {
    if (line == null || line.getContents() == null) {
      return null;
    }

    HttpdConfigurationScanner scanner = new HttpdConfigurationScanner(line.getContents());

    // First token is the name
    String name = scanner.next();

    // Any additional tokens are the arguments
    List<String> arguments = new ArrayList<>();
    while (scanner.hasNext()) {
      arguments.add(scanner.next());
    }

    return new Directive(name, arguments, line);
  }
}
