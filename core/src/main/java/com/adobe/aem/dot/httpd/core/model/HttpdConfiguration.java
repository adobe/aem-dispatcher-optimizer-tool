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

import com.adobe.aem.dot.common.Configuration;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * This model object represents an Apache Httpd configuration. It is not a complete representation, but instead focuses
 * on aspects of the configuration which are relevant to AEM.
 * Config spec: http://httpd.apache.org/docs/2.4/configuring.html
 */
@Getter
@Setter
public class HttpdConfiguration extends Section implements Configuration {

  private List<VirtualHost> virtualHosts;

  public HttpdConfiguration() {
    super();
    this.virtualHosts = new ArrayList<>();
  }

  @Override
  public SectionType getType() {
    return SectionType.TOP_LEVEL;
  }
}
