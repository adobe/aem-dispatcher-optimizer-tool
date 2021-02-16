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

import com.adobe.aem.dot.common.ConfigurationSource;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * A violation is a record of where a loaded configuration does not follow an AnalyzerRule.
 */
@Getter
public class Violation implements Comparable<Violation> {

  private final AnalyzerRule analyzerRule;
  private final String context;
  private final ConfigurationSource configurationSource;

  public Violation(AnalyzerRule analyzerRule, String context, ConfigurationSource configurationSource) {
    this.analyzerRule = analyzerRule;
    this.context = context;
    this.configurationSource = configurationSource;
  }

  public int compareTo(@NotNull Violation v) {
    int compare;
    if (this == v) {
      compare = 0;
    } else if (this.analyzerRule.getSeverity() == v.getAnalyzerRule().getSeverity()) {
      AnalyzerRule thisRule = this.getAnalyzerRule();
      AnalyzerRule thatRule = v.getAnalyzerRule();
      String thisSource = this.getConfigurationSource().getFileName() + this.getConfigurationSource().getLineNumber();
      String thatSource = v.getConfigurationSource().getFileName() + v.getConfigurationSource().getLineNumber();
      if (thisRule.getId().equals(thatRule.getId()) && thisSource.equals(thatSource)) {
        compare = 0;  // If the Rule Id & File/LineNumber are the same, consider the violation the same.
      } else {
        // Sort by the violated rule Id & where the violation was found.
        String thisId = thisRule.getId() + thisSource;
        String thatId = thatRule.getId() + thatSource;
        compare = thisId.compareTo(thatId);
      }
    } else {
      compare = this.analyzerRule.getSeverity().compareTo(v.getAnalyzerRule().getSeverity());
    }

    return compare;
  }

  @Override
  public String toString() {
    return "Violation { " +
            "severity=" + analyzerRule.getSeverity() +
            ", description='" + analyzerRule.getDescription() + '\'' +
            ", context='" + context + '\'' +
            " }";
  }
}
