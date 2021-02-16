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

package com.adobe.aem.dot.dispatcher.core;

import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.common.parser.ConfigurationParseResults;
import com.adobe.aem.dot.dispatcher.core.model.Cache;
import com.adobe.aem.dot.dispatcher.core.model.ConfigurationValue;
import com.adobe.aem.dot.dispatcher.core.model.DispatcherConfiguration;
import com.adobe.aem.dot.dispatcher.core.model.Farm;
import com.adobe.aem.dot.dispatcher.core.model.Filter;
import com.adobe.aem.dot.dispatcher.core.model.Render;
import com.adobe.aem.dot.dispatcher.core.model.Rule;
import com.adobe.aem.dot.dispatcher.core.model.RuleType;
import com.adobe.aem.dot.dispatcher.core.model.Statistics;
import com.adobe.aem.dot.dispatcher.core.model.StatisticsRule;
import com.adobe.aem.dot.dispatcher.core.model.VanityUrls;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationParser;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationParserTest {

  private final static ConfigurationParser parser = new ConfigurationParser();
  private static DispatcherConfiguration basicConfig;

  @BeforeClass
  public static void before() throws IOException, ConfigurationSyntaxException {
    // Read test config from resources dir
    URL basic = ConfigurationParserTest.class.getResource("basic_config.any");
    String input = IOUtils.toString(basic, StandardCharsets.UTF_8);

    List<ConfigurationLine> lines = new ArrayList<>();
    int i = 0;
    for (String nextLine: input.split(System.lineSeparator())) {
      lines.add(new ConfigurationLine(nextLine, basic.getPath(), i++));
    }
    // Parse the config
    ConfigurationParseResults<DispatcherConfiguration> parseResults = parser.parseConfiguration(lines);
    basicConfig = parseResults.getConfiguration();
  }

  @Test
  public void shouldParseName()  {
    // config name
    assertEquals("Expect name to be parsed", "test config", basicConfig.getName().getValue());
  }

  @Test
  public void shouldParseFarmBasics() {
    Farm farm = basicConfig.getFarms().get(0).getValue();

    // Farm name
    assertEquals("Expect farm name to be parsed", "publish", farm.getLabel());

    List<ConfigurationValue<String>> ch = farm.getClientHeaders();

    // client headers
    assertEquals("Expect 1st client headers to be *", "*", ch.get(0).getValue());
    assertEquals("Expect 2nd client headers to be Cache-Control", "Cache-Control",
        ch.get(1).getValue());

    // virtual hosts
    assertEquals("Expect 1st virtual host to be parsed", "aem-publish.local",
        farm.getVirtualHosts().get(0).getValue());
  }

  @Test
  public void shouldParseRenderBasics() {
    Render render1 = basicConfig.getFarms().get(0).getValue().getRenders().getValue().get(0);
    assertEquals("Expect name to be parsed", "rend01", render1.getLabel());

    // hostname
    assertEquals("Expect hostname to be parsed", "123.45.67.89", render1.getHostname().getValue());

    // port
    assertEquals("Expect port to be parsed", "45034", render1.getPort().getValue());
    // timeout
    assertEquals("Expect timeout to be parsed",  Integer.valueOf(1), render1.getTimeout().getValue());

    Render render2 = basicConfig.getFarms().get(0).getValue().getRenders().getValue().get(1);
    assertEquals("Expect name to be parsed", "rend02", render2.getLabel());

    // hostname
    assertEquals("Expect hostname to be parsed", "146.45.67.89", render2.getHostname().getValue());

    // port
    assertEquals("Expect port to be parsed", "4503", render2.getPort().getValue());
  }

  @Test
  public void shouldParseFilterRules() {
    Filter filter1 = basicConfig.getFarms().get(0).getValue().getFilter().getValue().get(0);
    assertEquals("Expect name to be parsed", "0001", filter1.getLabel());

    // type
    assertEquals("Expect type to be parsed", RuleType.DENY, filter1.getType());
    // url
    assertEquals("Expect url to be parsed", "*", filter1.getUrl());

    Filter filter2 = basicConfig.getFarms().get(0).getValue().getFilter().getValue().get(1);
    assertEquals("Expect name to be parsed", "0002", filter2.getLabel());

    // type
    assertEquals("Expect extension to be parsed",
            "(css|eot|gif|ico|jpeg|jpg|js|gif|pdf|png|svg|swf|ttf|woff|woff2|html)",
            filter2.getExtension());

    // path
    assertEquals("Expect path to be parsed", "/content/dispatchertester", filter2.getPath());

    Filter filter3 = basicConfig.getFarms().get(0).getValue().getFilter().getValue().get(2);
    assertEquals("Expect suffix to be parsed", "*", filter3.getSuffix());
  }

  @Test
  public void shouldParseCache() {
    Cache cache = basicConfig.getFarms().get(0).getValue().getCache().getValue();
    assertEquals("Expect cache docroot to be parsed", "/Library/WebServer/docroot/publish",
        cache.getDocroot().getValue());
    assertEquals("Expect statfileslevel to be parsed", (Integer) 2, cache.getStatfileslevel().getValue());
    assertEquals("Expect allowAuthorized to be parsed", false,
        cache.getAllowAuthorized().getValue());
    assertEquals("Expect serveStaleOnError to be parsed", true,
        cache.getServeStaleOnError().getValue());
    assertEquals("Expect gracePeriod to be parsed", (Integer) 2, cache.getGracePeriod().getValue());
    assertEquals("Expect enableTTL to be parsed", true, cache.getEnableTTL().getValue());
  }

  @Test
  public void shouldParseCacheRules() {
    Cache cache = basicConfig.getFarms().get(0).getValue().getCache().getValue();
    // Check source
    Rule rule1 = cache.getRules().getValue().get(0);
    assertEquals("Expect rule glob to be parsed", "*", rule1.getGlob());
    assertEquals("Expect rule type to be parsed", RuleType.DENY, rule1.getType());

    Rule rule2 = cache.getRules().getValue().get(1);
    assertEquals("Expect rule glob to be parsed", "/content/*", rule2.getGlob());
    assertEquals("Expect rule type to be parsed", RuleType.ALLOW, rule2.getType());
  }

  @Test
  public void shouldParseCacheInvalidateRules() {
    Cache cache = basicConfig.getFarms().get(0).getValue().getCache().getValue();
    Rule rule1 = cache.getInvalidate().getValue().get(0);
    assertEquals("Expect rule name to be parsed", "0000", rule1.getLabel());
    assertEquals("Expect rule glob to be parsed", "*", rule1.getGlob());
    assertEquals("Expect rule type to be parsed", RuleType.DENY, rule1.getType());


    Rule rule3 = cache.getInvalidate().getValue().get(2);
    assertEquals("Expect rule name to be parsed", "0002", rule3.getLabel());
    assertEquals("Expect rule glob to be parsed", "/etc/segmentation.segment.js", rule3.getGlob());
    assertEquals("Expect rule type to be parsed", RuleType.ALLOW, rule3.getType());
  }

  @Test
  public void shouldParseCacheAllowedClientsRules() {
    Cache cache = basicConfig.getFarms().get(0).getValue().getCache().getValue();
    Rule rule1 = cache.getAllowedClients().getValue().get(0);
    assertEquals("Expect rule name to be parsed", "0000", rule1.getLabel());
    assertEquals("Expect rule glob to be parsed", "*", rule1.getGlob());
    assertEquals("Expect rule type to be parsed", RuleType.DENY, rule1.getType());


    Rule rule2 = cache.getAllowedClients().getValue().get(1);
    assertEquals("Expect rule name to be parsed", "0001", rule2.getLabel());
    assertEquals("Expect rule glob to be parsed", "127.0.0.1", rule2.getGlob());
    assertEquals("Expect rule type to be parsed", RuleType.ALLOW, rule2.getType());
  }

  @Test
  public void shouldParseCacheIgnoreUrlParamsRules() {
    Cache cache = basicConfig.getFarms().get(0).getValue().getCache().getValue();
    Rule rule1 = cache.getIgnoreUrlParams().getValue().get(0);
    assertEquals("Expect rule name to be parsed", "0101", rule1.getLabel());
    assertEquals("Expect rule glob to be parsed", "*", rule1.getGlob());
    assertEquals("Expect rule type to be parsed", RuleType.ALLOW, rule1.getType());


    Rule rule2 = cache.getIgnoreUrlParams().getValue().get(1);
    assertEquals("Expect rule name to be parsed", "0202", rule2.getLabel());
    assertEquals("Expect rule glob to be parsed", "q", rule2.getGlob());
    assertEquals("Expect rule type to be parsed", RuleType.DENY, rule2.getType());
  }

  @Test
  public void shouldParseCacheHeaders() {
    Cache cache = basicConfig.getFarms().get(0).getValue().getCache().getValue();
    List<ConfigurationValue<String>> headers = cache.getHeaders();

    assertEquals("Expect header 2 to be parsed", "Content-Disposition", headers.get(1).getValue());
    assertEquals("Expect header 4 to be parsed", "Expires", headers.get(3).getValue());
    assertEquals("Expect header 6 to be parsed", "X-Content-Type-Options", headers.get(5).getValue());
  }

  @Test
  public void shouldParseVanityUrlsDetails() {
    VanityUrls vanityUrls = basicConfig.getFarms().get(0).getValue().getVanityUrls().getValue();

    assertEquals("Expect file to be parsed", "/tmp/vanity_urls", vanityUrls.getFile().getValue());
    assertEquals("Expect url to be parsed", "/libs/granite/dispatcher/content/vanityUrls.html",
            vanityUrls.getUrl().getValue());
    assertEquals("Expect delay to be parsed", (Integer) 300, vanityUrls.getDelay().getValue());
  }

  @Test
  public void shouldDetectFarmType() {
    Farm farm = basicConfig.getFarms().get(0).getValue();

    assertFalse("Expect isAuthorFarm to be false", farm.isAuthorFarm());
    assertTrue("Expect isPublishFarm to be true", farm.isPublishFarm());
  }

  @Test
  public void shouldDetectPropagateSyndPost() {
    Farm farm = basicConfig.getFarms().get(0).getValue();

    assertTrue("Expect propagateSyndPost to be true", farm.getPropagateSyndPost().getValue());
  }

  @Test
  public void shouldParseStatistics() {
    Statistics stats = basicConfig.getFarms().get(0).getValue().getStatistics().getValue();
    StatisticsRule rule1 = stats.getCategories().getRules().get(0);
    StatisticsRule rule2 = stats.getCategories().getRules().get(1);

    assertEquals("Expect first stats rule name to be parsed", "html", rule1.getLabel());
    assertEquals("Expect first stats rule glob to be parsed", "*.html", rule1.getGlob().getValue());

    assertEquals("Expect second stats rule name to be parsed", "others", rule2.getLabel());
    assertEquals("Expect second stats rule glob to be parsed", "*", rule2.getGlob().getValue());
  }
}
