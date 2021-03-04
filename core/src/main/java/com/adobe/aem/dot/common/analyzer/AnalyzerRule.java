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

package com.adobe.aem.dot.common.analyzer;

import com.adobe.aem.dot.common.util.GoUrlUtil;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.analyzer.FarmType;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.Farm;
import com.adobe.aem.dot.httpd.core.HttpdConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Represents a Rule that a DispatcherConfiguration must conform to in order
 * to be considered optimally configured.
 */
@Getter
@Setter
public class AnalyzerRule {
  private String id;
  private String description;
  private Severity severity;
  private List<FarmType> farmTypeList;
  private String element;
  boolean enabled = true;
  private List<Check> checks;
  private String origin;
  private String type;
  private List<String> tags;
  private String effort;      // integer + unit (i.e. "15min")

  protected static final String DOC_URL_DEFAULT = "https://experienceleague.adobe.com/docs/experience-manager-dispatcher/using/configuring/dispatcher-configuration.html";

  private static final Logger logger = LoggerFactory.getLogger(AnalyzerRule.class);

  /**
   * Based on the element, determine which object should be the target of this rule's Check(s).
   * @param farm - the <code>Farm</code> to get the target object from
   * @return the object to check as part of this <code>AnalyzerRule</code>
   */
  @JsonIgnore
  public Object determineCheckTarget(Farm farm) {
    // Determine the configuration element to check
    if (StringUtils.isEmpty(this.getElement())) {
      logger.error("Each rule must contain an `element` string, such as \"farm.filter\" or \"farm.cache.statfileslevel\".");
      return null;
    }

    String[] elementTokens = this.getElement().split("\\.");

    if (elementTokens.length < 2) {
      logger.error("Each rule's `element` string must contain at least 2 tokens separated by \".\", such as \"farm.filter\" or \"farm.cache.statfileslevel\". ProvidedElement=\"{}\"",
              this.getElement());
      return null;
    }

    String topLevelElementName = elementTokens[0].toLowerCase();
    Object objectToCheck;
    String fieldNameToCheck;

    // Determine the property to check
    logger.debug("Processing rule element=\"{}\" from RuleId=\"{}\"", this.getElement(), this.getId());
    if (!topLevelElementName.equals(DispatcherConstants.FARM)) {
      logger.error("Do not know how to handle this rule's element path: ProvidedElement=\"{}\" failed on property=\"{}\"",
              this.getElement(), topLevelElementName);
      return null;
    }

    Object configValueToCheck = null;
    logger.trace("topLevelElement: farm");

    String secondLevelElementName = elementTokens[1].toLowerCase();
    switch (secondLevelElementName) {
      case "cache":
        logger.trace("secondLevelElement: cache");

        if (elementTokens.length == 3) {
          // There's one final token: the field name
          fieldNameToCheck = elementTokens[2];
          objectToCheck = callGetter(farm, secondLevelElementName);
          configValueToCheck = callGetter(objectToCheck, fieldNameToCheck);
          logger.trace("Property on object=\"{}\" named=\"{}\" has value=\"{}\"", secondLevelElementName,
                  fieldNameToCheck, configValueToCheck);
        } else {
          // This method does not know how to handle this particular rule element
          logger.error("Unhandled rule element. ProvidedElement=\"{}\"", this.getElement());
        }
        break;
      case "filter":
        logger.trace("secondLevelElement: filter");

        configValueToCheck = callGetter(farm, secondLevelElementName);
        logger.trace("Property on object=\"{}\" named=\"{}\" has value=\"{}\"", topLevelElementName,
                secondLevelElementName, configValueToCheck);
        break;
      default:
        // TODO: handle others
        logger.error("Do not know how to handle this rule's element path: ProvidedElement=\"{}\" failed on property=\"{}\"",
                this.getElement(), secondLevelElementName);
    }

    return configValueToCheck;
  }

  /**
   * Is this a rule that should be applied to the dispatcher configuration?
   * @return true if this rule is for the dispatcher
   */
  @JsonIgnore
  public boolean isDispatcherRule() {
    return this.element != null && this.element.startsWith(DispatcherConstants.FARM);
  }

  /**
   * Is this a rule that should be applied to the Apache Httpd configuration?
   * @return true if this rule is for Apache Httpd
   */
  @JsonIgnore
  public boolean isApacheHttpdRule() {
    return this.element != null && this.element.startsWith(HttpdConstants.HTTPD);
  }

  /**
   * Is this rule a multi <code>Farm</code> rule? Multi Farm rules need knowledge of more than 1 Farm to determine if
   * the provided configuration is optimal.
   * @return true if and only if this rule is a multi farm rule
   */
  @JsonIgnore
  public boolean isMultiFarmRule() {
    return DispatcherConstants.FARM.equals(this.getElement());
  }

  /**
   * Is this rule a single <code>Farm</code> rule?
   * @return true if and only if this rule is a multi farm rule
   */
  @JsonIgnore
  public boolean isSingleFarmRule() {
    return this.isDispatcherRule() && !this.isMultiFarmRule();
  }

  /**
   * Format the Go URL from the rule's id.
   * @return The Go URL to this rule's documentation.
   */
  @JsonInclude
  public String getDocumentationURL() {
    return GoUrlUtil.getDocumentationGoURL(this.getId(), DOC_URL_DEFAULT);
  }

  private Object callGetter(Object obj, String fieldName){
    if (obj == null) {
      logger.error("Could not read property=\"{}\" from a null object", fieldName);
      return null;
    }

    Object valueObj = obj;
    // Cover case where the object is a configuration value - a 'getValue()' must be called on it.
    if ((obj instanceof ConfigurationValue)) {
      valueObj = ((ConfigurationValue<?>)obj).getValue();
    }

    PropertyDescriptor propertyDescriptor;
    try {
      propertyDescriptor = new PropertyDescriptor(fieldName, valueObj.getClass(),
              "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1),
              null);
      Object result = propertyDescriptor.getReadMethod().invoke(valueObj);
      logger.trace("callGetter found result for property=\"{}\" value=\"{}\"", fieldName,
              result != null ? result.toString() : "null");
      return result;
    } catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      logger.error("Error reading property=\"{}\" from value=\"{}\"", fieldName, valueObj, e);
      return null;
    }
  }
}
