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

import java.util.List;

/**
 * Represents <Location> and <LocationMatch> sections.
 * For additional detail on these sections: http://httpd.apache.org/docs/2.4/configuring.html
 */
public class LocationSection extends Section {

  public LocationSection(String name, List<String> arguments, ConfigurationSource configurationSource, Section parent) {
    super(name, arguments, configurationSource, parent);
  }

  @Override
  public SectionType getType() {
    return SectionType.LOCATION;
  }
}
