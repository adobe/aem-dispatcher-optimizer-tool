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

/**
 * The ViolationVerbosity indicates how the final list of violations should be presented.  For some configurations
 * there can be 1000's of violations which can be overwhelming.  The levels that a caller can decide are as follows:
 * - FULL: do not reduce the list at all but instead, send each violation that is encountered
 * - PARTIAL: some violations occur in configuration files that are included from many other files.  In this case,
 *            the same violation can occur many times.  If PARTIAL is chosen, these violations are collapsed into 1
 *            violation, with the added information of how many violations were collapsed into the one.
 * - MINIMIZED: this level indicates that all effort should be done to reduce the list.  The same steps are done as
 *            in the PARTIAL level, but also collapses all violations of a particular rule into 1 violation, with a
 *            count as to how many times it happens.  In this case, the largest list of violations will be the number
 *            of rules that are used, and a few violations that occur when ingesting the configuration files.
 */
public enum ViolationVerbosity {
  FULL,
  PARTIAL,
  MINIMIZED
}
