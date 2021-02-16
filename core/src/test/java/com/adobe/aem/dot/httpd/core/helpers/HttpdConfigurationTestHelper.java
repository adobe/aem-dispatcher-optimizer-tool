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

package com.adobe.aem.dot.httpd.core.helpers;

import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.httpd.core.model.Directive;
import com.adobe.aem.dot.httpd.core.model.DirectorySection;
import com.adobe.aem.dot.httpd.core.model.HttpdConfiguration;
import com.adobe.aem.dot.httpd.core.model.Section;
import com.adobe.aem.dot.httpd.core.model.VirtualHost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test helper for Httpd related tests.
 */
public class HttpdConfigurationTestHelper {
  public static HttpdConfiguration getBasicHttpdConfiguration() {
    return getBasicHttpdConfiguration(null);
  }

  /**
   * Create a very basic httpd configuration for testing.
   * @param vHostArguments  The arguments to be used in the the vhost section of the configuration.  It can be be null.
   * @return A basic httpd configuration.
   */
  public static HttpdConfiguration getBasicHttpdConfiguration(List<String> vHostArguments) {
    // Set up the HttpdConfiguration model
    HttpdConfiguration configuration = new HttpdConfiguration();
    int lineNumber = 1;

    Directive serverName = new Directive("ServerName", Collections.singletonList("publish"));
    Directive requireAllGranted = new Directive("Require", Arrays.asList("all", "denied"));

    // Add vHost to configuration
    List<String> arguments = vHostArguments;
    if (arguments == null) {
      arguments = Collections.singletonList("*:80");
    }
    VirtualHost vHost = new VirtualHost("VirtualHosts", arguments, getTestConfigurationSource(lineNumber++),
            configuration);

    List<Directive> directives = new ArrayList<>();
    directives.add(serverName);
    directives.add(new Directive("Options", Collections.singletonList("FollowSymLinks")));
    vHost.setDirectives(directives);

    configuration.setVirtualHosts(Collections.singletonList(vHost));

    // Add directory section to vHost
    Section directory = new DirectorySection("directory", Collections.singletonList("/"),
            getTestConfigurationSource(lineNumber), vHost);
    directory.setDirectives(Collections.singletonList(requireAllGranted));

    ArrayList<Section> vhostSections = new ArrayList<>();
    vhostSections.add(directory);
    vHost.setSections(vhostSections);

    return configuration;
  }

  private static ConfigurationSource getTestConfigurationSource(int lineNumber) {
    return new ConfigurationSource("test_file.conf", lineNumber);
  }
}
