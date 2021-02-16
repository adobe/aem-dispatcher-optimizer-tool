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

package com.adobe.aem.dot.httpd.core.analyzer.conditions;

import com.adobe.aem.dot.common.analyzer.Check;
import com.adobe.aem.dot.common.analyzer.CheckResult;
import com.adobe.aem.dot.common.analyzer.Condition;
import com.adobe.aem.dot.httpd.core.model.Directive;
import com.adobe.aem.dot.httpd.core.model.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check for the presence of a Directive in a given Section.
 */
public class HasDirectiveCheck extends Check {
  private static final Logger logger = LoggerFactory.getLogger(HasDirectiveCheck.class);

  @Override
  public Condition getCondition() {
    return Condition.HAS_DIRECTIVE;
  }

  @Override
  public CheckResult performCheck(Object configurationSection) {
    if (configurationSection instanceof Section && this.getDirectiveValue() != null) {
      Section sectionToCheck = (Section) configurationSection;
      Directive directiveToCheckFor = this.getDirectiveValue();

      boolean hasDirective = sectionToCheck.hasDirective(directiveToCheckFor);

      return new CheckResult(this.processFailIf(hasDirective));
    }
    else {
      // Either the configurationSection is not a Section or there was no target Directive provided to the Check
      logger.error("Failed to process this HAS_DIRECTIVE Check due to invalid parameters: configurationSection=\"{}\" " +
              "directiveValue=\"{}\"", configurationSection, this.getDirectiveValue());

      return new CheckResult(this.processFailIf(false));
    }
  }
}
