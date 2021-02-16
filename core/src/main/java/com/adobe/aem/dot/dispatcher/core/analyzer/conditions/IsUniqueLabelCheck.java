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

package com.adobe.aem.dot.dispatcher.core.analyzer.conditions;

import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.common.analyzer.Check;
import com.adobe.aem.dot.common.analyzer.CheckResult;
import com.adobe.aem.dot.common.analyzer.Condition;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.LabeledItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract class to facilitate checks involving boolean values.
 */
public class IsUniqueLabelCheck extends Check {
  private static final Logger logger = LoggerFactory.getLogger(IsUniqueLabelCheck.class);

  @Override
  public Condition getCondition() {
    return Condition.IS_UNIQUE_LABEL;
  }

  @SuppressWarnings("unchecked")
  @Override
  public CheckResult performCheck(Object configurationValue) {
    // Look for any duplicate labels
    List<ConfigurationValue<LabeledItem>> listOfLabeledItems;
    try {
      listOfLabeledItems = (List<ConfigurationValue<LabeledItem>>) configurationValue;
    } catch(ClassCastException ccEx) {
      logger.error("Value could not be cast to a labeled list. Value=\"{}\"", configurationValue.toString());
      listOfLabeledItems = null;
    }

    if (listOfLabeledItems == null) {
      // Returning true, as there cannot be duplicates in an empty/null list.
      return new CheckResult(this.processFailIf(true));
    }

    Set<String> uniqueLabels = new HashSet<>();
    List<String> duplicateLabels = new ArrayList<>();
    ConfigurationSource duplicateItemSource = null;

    for (ConfigurationValue<LabeledItem> item : listOfLabeledItems) {
      String currentLabel = item.getValue().getLabel();
      // .add returns false if the item already exists in the Set
      boolean isUnique = uniqueLabels.add(currentLabel);
      if (!isUnique && !duplicateLabels.contains(currentLabel)) {
        duplicateLabels.add(currentLabel);
        // Grab the configurationSource of this item.
        // The current Violation reporting format only allows a single pointer to the source to be returned, so we'll
        // reference the last duplicate encountered.
        duplicateItemSource = item.getConfigurationSource();
      }
    }

    return new CheckResult(this.processFailIf(duplicateLabels.isEmpty()), duplicateLabels, duplicateItemSource);
  }
}
