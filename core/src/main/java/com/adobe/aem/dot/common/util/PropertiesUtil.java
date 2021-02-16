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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

  private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

  private static final Properties properties = new Properties();

  public static final String APP_PROPERTIES_FILE = "application.properties";

  public static final String MAX_LINES_PROP = "dot.maximum.configuration.lines";
  public static final String MAX_INCLUDE_DEPTH_PROP = "dot.maximum.configuration.include.depth";
  public static final String DISP_VIOLATION_ELEMENT_PROP = "dot.parsing.violation.element";

  public static long getLongProperty(String propertyName, long defaultValue) {
    try {
      String prop = getProperty(propertyName);
      if (StringUtils.isEmpty(prop)) {
        logger.warn("Property did not have a valid integer value.  Name=\"{}\"", propertyName);
        return defaultValue;
      }
      return Long.parseLong(prop);
    } catch(Exception ex) {
      logger.warn("Property was not read correctly.  Name=\"{}\"", propertyName, ex);
      return defaultValue;
    }
  }

  public static String getProperty(String propertyName) throws IOException {
    // Return value if we already read it.
    if (properties.containsKey(propertyName)) {
      return properties.getProperty(propertyName);
    }

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    InputStream resourceInputStream = loader.getResourceAsStream(APP_PROPERTIES_FILE);

    if (resourceInputStream != null) {
      properties.load(resourceInputStream);
    } else {
      throw new FileNotFoundException("property file '" + APP_PROPERTIES_FILE + "' not found in the classpath");
    }

    return properties.getProperty(propertyName);
  }
}
