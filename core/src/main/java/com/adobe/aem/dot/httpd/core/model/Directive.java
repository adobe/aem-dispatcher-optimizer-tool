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
import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an individual Apache Httpd directive. Directives are the building blocks of an Apache Httpd configuration
 * file. For more detail: http://httpd.apache.org/docs/2.4/configuring.html
 */
@Getter
@Setter(AccessLevel.PRIVATE)
@ToString
public class Directive {
  private String name;
  private List<String> arguments;
  @ToString.Exclude
  private ConfigurationSource configurationSource;


  public Directive() {
    this.arguments = new ArrayList<>();
  }

  public Directive(String name, List<String> arguments) {
    this.name = name;
    this.arguments = arguments;
  }

  public Directive(String name, List<String> arguments, ConfigurationSource configurationSource) {
    this.name = name;
    this.arguments = arguments;
    this.configurationSource = configurationSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    Directive directive = (Directive) o;

    return new EqualsBuilder()
            .append(getName(), directive.getName())
            .append(getArguments(), directive.getArguments())
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(47, 123)
            .append(getName())
            .append(getArguments())
            .toHashCode();
  }
}
