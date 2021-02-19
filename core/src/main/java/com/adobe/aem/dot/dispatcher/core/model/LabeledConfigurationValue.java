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

import com.adobe.aem.dot.common.util.FeedbackProcessor;
import com.adobe.aem.dot.dispatcher.core.util.ReservedTokensUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

/**
 * The <code>ConfigurationValueSource</code> class encapsulates the filename and the line number of a
 * configuration source.  Other classes can expand it to have that information tagged with its data.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public abstract class LabeledConfigurationValue implements LabeledItem {

    private ConfigurationValue<String> label;

    abstract Logger getLogger();
    abstract String getSimpleClassName();

    @JsonIgnore
    public String getLabel() {
        return this.label == null ? "" : this.label.getValue();
    }
    // This is mainly for testing ConfigurationValue parsing values.
    protected ConfigurationValue<String> getLabelData() {
        return this.label;
    }

    public void setLabel(ConfigurationValue<String> label) {
        if (label != null && label.getValue() != null && !label.getValue().equals("{")) {
            this.label = label;
            if (ReservedTokensUtil.isReservedToken(label.getValue())) {
                FeedbackProcessor.warn(this.getLogger(), "A reserved token was used to label a " +
                        this.getSimpleClassName() +
                        " indicating a section may have been closed incorrectly.  Label=\"{}\"", label, null);
            }
        }
    }
}
