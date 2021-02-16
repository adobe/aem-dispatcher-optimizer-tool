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

package com.adobe.aem.dot.httpd.core.model;

import com.adobe.aem.dot.common.ConfigurationSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a Section of an Apache Httpd config file, sometimes referred to as a container.
 * For additional detail on the available sections: http://httpd.apache.org/docs/2.4/configuring.html
 */
@Getter
@Setter
public class Section extends Directive {

  private List<Directive> directives;
  private List<Section> sections;

  @Setter(AccessLevel.NONE)
  private Section parent;

  public Section() {
    super();

    this.directives = new ArrayList<>();
    this.sections = new ArrayList<>();
  }

  public Section(String name, List<String> arguments, ConfigurationSource configurationSource, Section parent) {
    super(name, arguments, configurationSource);

    this.parent = parent;
    this.directives = new ArrayList<>();
    this.sections = new ArrayList<>();
  }

  /**
   * Get the named effective Directive, as computed by searching up the configuration tree structure for a match.
   * @param directiveName - name of the directive to locate
   * @return the nearest instance of the named Directive to the current section, if found. null otherwise.
   */
  public Directive getEffectiveDirective(String directiveName) {
    for (Directive currentDirective : this.getDirectives()) {
      if (directiveName.equals(currentDirective.getName())) {
        return currentDirective;
      }
    }
    // Did not find the requested directive. Continue searching in this Section's parent.
    if (parent != null) {
      return parent.getEffectiveDirective(directiveName);
    }
    // Finally, reached the top of the config tree (parent == null) and the directive in question was not found.
    return null;
  }

  /**
   * Does this section have the provided Directive set?
   * @param directive - the Directive to check for
   * @return true if this Directive is set on the current Section or any of it's ancestor Sections
   */
  public boolean hasDirective(Directive directive) {
    for (Directive currentDirective : this.getDirectives()) {
      if (currentDirective.equals(directive)) {
        return true;
      }
    }

    // Did not find the requested directive. Continue searching in this Section's parent.
    if (parent != null) {
      return parent.hasDirective(directive);
    }

    return false;
  }

  /**
   * Type of the section.
   * @return a value of the SectionType enum
   */
  public SectionType getType() {
    return SectionType.UNKNOWN;
  }

  @Override
  public String toString() {
    return "SectionType=" + this.getType() + (this.getName() != null ? " Name=" + this.getName() : "");
  }

  /**
   * Get the subset of this objects sections which are Directory type sections.
   * @return a list of Directory type sections
   */
  public List<Section> getDirectorySections() {
    if (this.getSections() == null) {
      return null;
    }

    return this.getSections().stream()
            .filter(section -> section instanceof DirectorySection)
            .collect(Collectors.toList());
  }
}
