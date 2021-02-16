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
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.adobe.aem.dot.httpd.core.model.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class to determine the target object(s) of a Check.
 */
public class HttpdCheckTargetLocator {

  private static final Logger logger = LoggerFactory.getLogger(HttpdCheckTargetLocator.class);

  private String element;

  public HttpdCheckTargetLocator(String element) {
    this.element = element;
  }

  /**
   * Based on the element field, determine which object should be the target of this rule's Check(s).
   * @param config - the <code>HttpdConfiguration</code> to get the target object from
   * @return the list of Sections to check as part of this <code>AnalyzerRule</code>
   */
  public List<Section> determineCheckTargets(HttpdConfiguration config) {
    if (config == null) {
      throw new IllegalArgumentException("The HttpdConfiguration parameter must not be null to continue.");
    }

    if (this.element == null) {
      throw new IllegalArgumentException("The element field must not be null to continue.");
    }

    logger.debug("Determining Check target for element=\"{}\"", this.element);

    String[] elementTokens = this.element.split("\\.");

    if (elementTokens.length < 2) {
      logger.error("Each Httpd rule's `element` string must contain at least 2 tokens separated by \".\", such as " +
              "\"httpd.vhost\" or \"httpd.vhost.directory.root\". ProvidedElement=\"{}\"", this.element);
      return null;
    }

    String topLevelElementName = elementTokens[0].toLowerCase();
    if (!HttpdConstants.HTTPD.equals(topLevelElementName)) {
      logger.error("The HttpdCheckTargetLocator can only locate target objects for httpd related rules (element begins with \"httpd\")");
      return null;
    }

    return this.processElementTokens(Collections.singletonList(config), elementTokens);
  }

  private List<Section> processElementTokens(List<Section> currentSections, String[] elementTokens) {
    if (elementTokens.length == 0) {
      // We've reached the end of the array of tokens
      return currentSections;
    }

    String currentElementToken = elementTokens[0];
    // Create an array of tokens with the current token removed
    String[] remainingElementTokens = Arrays.copyOfRange(elementTokens, 1, elementTokens.length);

    switch (currentElementToken) {
      case HttpdConstants.HTTPD:
        // Discard this token and process the remaining tokens with the current list of Sections
        return this.processElementTokens(currentSections, remainingElementTokens);
      case HttpdConstants.VHOST:
        // Collect all the vhosts from the current list of Sections
        List<Section> allVhosts = new ArrayList<>();
        for (Section section : currentSections) {
          if (section instanceof HttpdConfiguration) {
            HttpdConfiguration config = (HttpdConfiguration) section;
            allVhosts.addAll(config.getVirtualHosts());
          }
        }
        return this.processElementTokens(allVhosts, remainingElementTokens);
      case HttpdConstants.DIRECTORY:
        // Collect all directory sections from the current list of Sections
        List<Section> allDirectorySections = new ArrayList<>();
        for (Section section : currentSections) {
          allDirectorySections.addAll(section.getDirectorySections());
        }
        return this.processElementTokens(allDirectorySections, remainingElementTokens);
      case HttpdConstants.ROOT:
        // Locate all "root" sections (where the argument list is simply ["/"])
        List<Section> allRootSections = currentSections.stream()
                .filter(section -> section.getArguments() != null &&
                        section.getArguments().size() == 1 &&
                        "/".equals(section.getArguments().get(0)))
                .collect(Collectors.toList());
        return this.processElementTokens(allRootSections, remainingElementTokens);
      default:
        throw new IllegalArgumentException("Cannot process this rule's element string: ProvidedElement=\"" +
                this.element + "\" failed on Token=\"" + currentElementToken + "\"");
    }
  }
}
