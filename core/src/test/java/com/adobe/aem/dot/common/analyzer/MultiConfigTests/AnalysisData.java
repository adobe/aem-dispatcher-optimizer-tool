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

package com.adobe.aem.dot.common.analyzer.MultiConfigTests;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * A class to encapsulate a configuration's count of the various levels of Violation and other related data.
 */
@Getter
public class AnalysisData implements Comparable<AnalysisData> {
  private final String configurationDirName;
  private final String reportName;
  private final int[] counts = new int[5];

  public AnalysisData(String configDirName, int blockers, int criticals, int majors, int minors, int infos,
                      String reportName) {
    this.configurationDirName = configDirName;
    this.reportName = reportName;
    counts[0] = blockers;
    counts[1] = criticals;
    counts[2] = majors;
    counts[3] = minors;
    counts[4] = infos;
  }

  public int get(int index) {
    return counts[index];
  }

  public int compareTo(@NotNull AnalysisData counts) {
    int i;
    for (i = 0; i < 4; i++) {
      if (this.counts[i] != counts.get(i)) {
        return this.counts[i] - counts.get(i);
      }
    }

    return this.configurationDirName.compareTo(counts.configurationDirName);
  }

  public String toString() {
    return configurationDirName + ": Blockers:" + counts[0] + ", Criticals:" + counts[1] + ", Majors:" + counts[2] + ", Minors:" + counts[3];
  }

}
