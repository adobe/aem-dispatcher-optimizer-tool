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
 * A Condition represents how each Check will be executed.
 */
public enum Condition {
  // Int value checks
  INT_GREATER_OR_EQUAL,
  INT_EQUALS,

  // Boolean check
  BOOLEAN_EQUALS,

  // Rule checks
  RULE_LIST_STARTS_WITH,
  RULE_LIST_INCLUDES,

  // Filter checks
  FILTER_LIST_STARTS_WITH,
  FILTER_LIST_INCLUDES,

  // Unique label check
  IS_UNIQUE_LABEL,

  // Directive check
  HAS_DIRECTIVE
}
