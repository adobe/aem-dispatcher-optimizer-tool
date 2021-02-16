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

package com.adobe.aem.dot.httpd.core.model;

import com.adobe.aem.dot.common.ConfigurationLine;
import com.adobe.aem.dot.httpd.core.parser.DirectiveFactory;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HttpdConfigurationTest {

  @Test
  public void shouldResolveEffectiveDirectives() {
    // Set up config tree
    // Top level config
    HttpdConfiguration config = new HttpdConfiguration();
    Directive docRoot = DirectiveFactory.getDirectiveInstance(new ConfigurationLine("DocumentRoot \"/var/www/html\"", "", 0));
    Directive serverAdmin  = DirectiveFactory.getDirectiveInstance(new ConfigurationLine("ServerAdmin root@localhost", "", 1));
    config.setDirectives(Arrays.asList(docRoot, serverAdmin));

    // Vhost
    VirtualHost pubVhost = (VirtualHost) DirectiveFactory.getSectionInstance(new ConfigurationLine("<VirtualHost *:80>", "", 3), config);
    Directive vhostDocRoot = DirectiveFactory.getDirectiveInstance(new ConfigurationLine("DocumentRoot\t${PUBLISH_DOCROOT}", "", 1));
    pubVhost.setDirectives(Collections.singletonList(vhostDocRoot));
    config.setVirtualHosts(Collections.singletonList(pubVhost));

    // Directory 1
    Section vhostDir = DirectiveFactory.getSectionInstance(new ConfigurationLine("<Directory />", "", 3), pubVhost);
    Directive requireAllGranted = DirectiveFactory.getDirectiveInstance(new ConfigurationLine("Require all granted", "", 1));
    vhostDir.setDirectives(Collections.singletonList(requireAllGranted));
    // Directory 2
    Section vhostDirPub = DirectiveFactory.getSectionInstance(new ConfigurationLine("<Directory \"${PUBLISH_DOCROOT}\">", "", 3), pubVhost);
    Directive allowOverrideDir = DirectiveFactory.getDirectiveInstance(new ConfigurationLine("AllowOverride None", "", 1));
    vhostDirPub.setDirectives(Collections.singletonList(allowOverrideDir));
    config.setSections(Arrays.asList(vhostDir, vhostDirPub));

    // Query directives at different layers

    assertNull("Expect top level config to not know about AllowOverride", config.getEffectiveDirective("AllowOverride"));
    assertNull("Expect vhostDir to not know about AllowOverride", vhostDir.getEffectiveDirective("AllowOverride"));
    assertEquals("Expect vhostDirPub to know about AllowOverride", "None", vhostDirPub.getEffectiveDirective("AllowOverride").getArguments().get(0));

    assertEquals("Expect vhostDirPub to know about DocumentRoot", "${PUBLISH_DOCROOT}", vhostDirPub.getEffectiveDirective("DocumentRoot").getArguments().get(0));
    assertEquals("Expect config to have a different argument for DocumentRoot", "/var/www/html", config.getEffectiveDirective("DocumentRoot").getArguments().get(0));

    assertEquals("Expect pubVhost to know about ServerAdmin", "root@localhost", pubVhost.getEffectiveDirective("ServerAdmin").getArguments().get(0));
  }

  @Test
  public void typeTest() {
    Section section = new Section();
    assertEquals("Should be unknown", SectionType.UNKNOWN, section.getType());
  }

  @Test
  public void toStringTest() {
    Section section1 = new Section();
    assertEquals("Default String", "SectionType=UNKNOWN", section1.toString());

    Section section2 = new Section("SectionName", null, null, null);
    assertEquals("Named String", "SectionType=UNKNOWN Name=SectionName", section2.toString());
  }

  @Test
  public void parentTest() {
    Section section = new Section();
    assertNull("No Parent", section.getParent());
  }
}
