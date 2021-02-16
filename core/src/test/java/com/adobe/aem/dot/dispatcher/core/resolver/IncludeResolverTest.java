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

package com.adobe.aem.dot.dispatcher.core.resolver;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.aem.dot.common.ConfigurationException;
import com.adobe.aem.dot.common.helpers.AssertHelper;
import com.adobe.aem.dot.common.helpers.DispatcherConfigTestHelper;
import com.adobe.aem.dot.dispatcher.core.DispatcherConstants;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IncludeResolverTest {
  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void before() {
    // Get Logback Logger: create and start a ListAppender
    listAppender = AssertHelper.getLogAppender(IncludeResolver.class);
  }

  @Test
  public void getFilePath() throws ConfigurationSyntaxException {
    IncludeResolver resolver = new IncludeResolver("", "", "");
    String result = resolver.getFilePathFromInclude("dispatcher/vhosts/layer-1.any", "IncludeResolverTest.any");
    assertEquals("Expect path to be", "dispatcher/vhosts/layer-1.any", result);
  }

  @Test
  public void getFilePathInclude() throws ConfigurationSyntaxException {
    IncludeResolver resolver = new IncludeResolver("", "", "");
    String result = resolver.getFilePathFromInclude("$include \"enabled_farms/000_skylab_author_farm.any\"", "IncludeResolverTest.any");
    assertEquals("Expect path to be", "enabled_farms/000_skylab_author_farm.any", result);
  }

  @Test
  public void getFilePathIncludeMissingQuote() throws ConfigurationSyntaxException {
    IncludeResolver resolver = new IncludeResolver("", "", "");
    String result = resolver.getFilePathFromInclude("$include \"enabled_farms/000_skylab_author_farm.any", "IncludeResolverTest.any");
    assertEquals("Expect path to be", "enabled_farms/000_skylab_author_farm.any", result);
  }

  @Test
  public void getFilePathFromIncludeQuoteTest() throws ConfigurationSyntaxException {
    IncludeResolver resolver = new IncludeResolver("", "", "");
    String includePath1 = resolver.getFilePathFromInclude("$include \"somefile.txt", "Included_From.txt");
    assertEquals("Expect missing quote to be handled", "somefile.txt", includePath1);

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Include error should have been logged.",
            "Unmatched double-quoted string encountered.  Token=\"{}\", File=\"{}\", Line={}",
            logsList.get(0).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());
  }

  @Test(expected = ConfigurationSyntaxException.class)
  public void getFilePathFromIncludeMissingFileTest() throws ConfigurationSyntaxException {
    IncludeResolver resolver = new IncludeResolver("", "", "");
    resolver.getFilePathFromInclude("$include", "Included_From.txt");
  }

  @Test
  public void loadInfiniteIncludeConfigTest() {
    DispatcherConfigTestHelper helper = new DispatcherConfigTestHelper();
    String absPath = DispatcherConfigTestHelper.getConfigFileAbsolutePath(this.getClass(),
            DispatcherConstants.DISPATCHER_ANY);
    try {
      helper.loadDispatcherConfiguration(absPath);
      fail("Exception should have been thrown.");
    } catch(ConfigurationException ignore) {
    }

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Include error should have been logged.",
            "Maximum number of Dispatcher includes (50) encountered.  Check for circular file includes or raise the \"dot.maximum.configuration.include.depth\" property.",
            logsList.get(0).getMessage());
    assertEquals("Severity should be ERROR.", Level.ERROR, logsList.get(0).getLevel());
  }
}
