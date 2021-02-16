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
import com.adobe.aem.dot.httpd.core.helpers.HttpdConfigurationTestHelper;
import com.adobe.aem.dot.httpd.core.model.Directive;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.adobe.aem.dot.httpd.core.model.Section;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HasDirectiveCheckTest {

  @Test
  public void testPerformCheck() {
    // Set up the HttpdConfiguration model
    HttpdConfiguration configuration = HttpdConfigurationTestHelper.getBasicHttpdConfiguration();

    Directive requireAllGranted = new Directive("Require", Arrays.asList("all", "granted"));

    Section directory = configuration.getVirtualHosts().get(0).getSections().get(0);
    directory.setDirectives(Collections.singletonList(requireAllGranted));

    // Set up the Check
    Check check = new HasDirectiveCheck();
    Directive directiveToCheck = new Directive("Require", Arrays.asList("all", "granted"));
    check.setDirectiveValue(directiveToCheck);
    check.setFailIf(true);

    // Run the Check
    CheckResult result = check.performCheck(directory);
    assertFalse("CheckResult should be false, since this check fails if true (and the result of the Check should be true)", result.isPassed());
    assertEquals("Directive(name=Require, arguments=[all, granted])", directiveToCheck.toString());

    // ----------
    // Check with another directive

    Directive otherDirectiveToCheck = new Directive("Require", Arrays.asList("all", "denied"));
    check.setDirectiveValue(otherDirectiveToCheck);
    check.setFailIf(true);

    // Run the Check
    CheckResult result2 = check.performCheck(directory);
    assertTrue("CheckResult should be true, since this check fails if true (and the result of the Check should be false)", result2.isPassed());
  }

  @Test
  public void testPerformCheckBadArguments() {
    // Set up the HttpdConfiguration model
    HttpdConfiguration configuration = new HttpdConfiguration();

    Directive requireAllGranted = new Directive("Require", Arrays.asList("all", "granted"));

    configuration.setDirectives(Collections.singletonList(requireAllGranted));

    // Set up the Check
    Check check = new HasDirectiveCheck();
    Directive directiveToCheck = new Directive("Require", Arrays.asList("all", "granted"));
    check.setDirectiveValue(directiveToCheck);
    check.setFailIf(true);

    // Run the Check
    CheckResult result = check.performCheck(new Directive());
    assertTrue("CheckResult should be true, since this check fails if true (and the result of the Check should be false given that the provided Directive is not an instance of Section)", result.isPassed());

    // ----------
    // Try again with a null directiveToCheck on the Check
    check.setDirectiveValue(null);
    result = check.performCheck(configuration);
    assertTrue("CheckResult should be true, since this check fails if true (and the result of the Check should be false given that the Check's directive is null)", result.isPassed());

  }
}