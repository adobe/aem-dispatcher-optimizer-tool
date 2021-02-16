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

import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.model.Farm;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationParser;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class IncludeConfigurationTest {

  @Test
  public void basicInclude() throws IOException, ConfigurationSyntaxException {
    StringBuilder sb = new StringBuilder("/name \"basic\"\n");
    sb.append("/farms {\n");
    sb.append("/publish {\n");
    sb.append("/virtualhosts {\n");
    sb.append("$include \"src/test/resources/dispatcher-includes/src/conf.dispatcher.d/vhosts/basic.any\"\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("}\n");

    IncludeResolver includeResolver = new IncludeResolver(sb.toString(), System.getProperty("user.dir"), "");
    List<ConfigurationLine> config = includeResolver.resolve();
    ConfigurationParser parser = new ConfigurationParser();
    ConfigurationParseResults<DispatcherConfiguration> results = parser.parseConfiguration(config);
    DispatcherConfiguration includeConfig = results.getConfiguration();

    Farm farm = includeConfig.getFarms().get(0).getValue();
    assertEquals("Expect 1 virtualhost", farm.getVirtualHosts().size(), 1);
    assertEquals("Expect virtualhost to be parsed", "basic-include-*", farm.getVirtualHosts().get(0).getValue());
  }

  @Test
  public void recursiveInclude() throws IOException, ConfigurationSyntaxException {
    StringBuilder sb = new StringBuilder("/name \"basic\"\n");
    sb.append("/farms {\n");
    sb.append("/publish {\n");
    sb.append("/virtualhosts {\n");
    sb.append("    $include \"src/test/resources/dispatcher-includes/src/conf.dispatcher.d/vhosts/layer-1.any\"\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("}\n");

    IncludeResolver includeResolver = new IncludeResolver(sb.toString(), System.getProperty("user.dir"), "");
    List<ConfigurationLine> config = includeResolver.resolve();
    ConfigurationParser parser = new ConfigurationParser();
    ConfigurationParseResults<DispatcherConfiguration> results = parser.parseConfiguration(config);
    DispatcherConfiguration includeConfig = results.getConfiguration();

    Farm farm = includeConfig.getFarms().get(0).getValue();
    assertEquals("Expect 3 virtualhosts", farm.getVirtualHosts().size(), 3);
    assertEquals("Expect 1st virtualhost to be parsed", "author-*", farm.getVirtualHosts().get(0).getValue());
    assertEquals("Expect 1st virtualhost to be parsed", "publisher-*", farm.getVirtualHosts().get(1).getValue());
    assertEquals("Expect 1st virtualhost to be parsed", "dispatcher-*", farm.getVirtualHosts().get(2).getValue());
  }

  @Test
  public void twoElementIncludes() throws IOException, ConfigurationSyntaxException {
    StringBuilder sb = new StringBuilder("/name \"basic\"\n");
    sb.append("/farms {\n");
    sb.append("/publish {\n");
    sb.append("/virtualhosts {\n");
    sb.append("   $include \"src/test/resources/dispatcher-includes/src/conf.dispatcher.d/vhosts/basic.any\"\n");
    sb.append("}\n");
    sb.append("/filters\n");
    sb.append("   $include \"src/test/resources/dispatcher-includes/src/conf.dispatcher.d/filters/filter.any\"");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("}\n");

    IncludeResolver includeResolver = new IncludeResolver(sb.toString(), System.getProperty("user.dir"), "");
    List<ConfigurationLine> config = includeResolver.resolve();
    ConfigurationParser parser = new ConfigurationParser();
    ConfigurationParseResults<DispatcherConfiguration> results = parser.parseConfiguration(config);
    DispatcherConfiguration includeConfig = results.getConfiguration();

    Farm farm = includeConfig.getFarms().get(0).getValue();
    assertEquals("Expect 1 virtualhost", farm.getVirtualHosts().size(), 1);
    assertEquals("Expect virtualhost to be parsed", "basic-include-*", farm.getVirtualHosts().get(0).getValue());
  }
}
