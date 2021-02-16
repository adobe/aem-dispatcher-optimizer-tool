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

package com.adobe.aem.dot.dispatcher.core.model;

import com.adobe.aem.dot.common.Configuration;
import lombok.Getter;

import java.util.List;

import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_VALUE_FILE_NAME;

/**
 * This model object represents an AEM dispatcher configuration in it's entirety,
 * including $includes of other files.
 */
@Getter
public class DispatcherConfiguration implements Configuration {
  private final ConfigurationValue<String> name;
  private final ConfigurationValue<Boolean> ignoreEINTR;
  private final List<ConfigurationValue<Farm>> farms;

  public DispatcherConfiguration(ConfigurationValue<String> name, List<ConfigurationValue<Farm>> farms) {
    this.name = name;
    this.ignoreEINTR = new ConfigurationValue<>(false, DEFAULT_VALUE_FILE_NAME, 0);
    this.farms = farms;
  }

  public DispatcherConfiguration(ConfigurationValue<String> name, ConfigurationValue<Boolean> ignoreEINTR,
                                 List<ConfigurationValue<Farm>> farms) {
    this.name = name;
    this.ignoreEINTR = ignoreEINTR;
    this.farms = farms;
  }
}
