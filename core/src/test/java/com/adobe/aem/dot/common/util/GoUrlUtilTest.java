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

package com.adobe.aem.dot.common.util;

import com.adobe.aem.dot.common.analyzer.AnalyzerRule;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleList;
import com.adobe.aem.dot.common.analyzer.rules.AnalyzerRuleListFactory;
import com.adobe.aem.dot.common.parser.ConfigurationViolations;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class GoUrlUtilTest {
  // List of all rule/violation ids, and the expected documentation url.  Order is not important.
  private final Map<String, String> goUrls = Stream.of(new String[][] {
          { "DOTRules:Disp-1---ignoreUrlParams-allow-list", "https://www.adobe.com/go/aem_cmcq_disp-1---ignoreurlpa_en" },
          { "DOTRules:Disp-2---statfileslevel", "https://www.adobe.com/go/aem_cmcq_disp-2---statfilesle_en" },
          { "DOTRules:Disp-3---gracePeriod", "https://www.adobe.com/go/aem_cmcq_disp-3---graceperiod_en" },
          { "DOTRules:Disp-4---default-filter-deny-rules", "https://www.adobe.com/go/aem_cmcq_disp-4---default-fil_en" },
          { "DOTRules:Disp-5---serveStaleOnError", "https://www.adobe.com/go/aem_cmcq_disp-5---servestaleo_en" },
          { "DOTRules:Disp-6---suffix-allow-list", "https://www.adobe.com/go/aem_cmcq_disp-6---suffix-allo_en" },
          { "DOTRules:Disp-7---selector-allow-list", "https://www.adobe.com/go/aem_cmcq_disp-7---selector-al_en" },
          { "DOTRules:Disp-8---unique-farm-name", "https://www.adobe.com/go/aem_cmcq_disp-8---unique-farm_en" },
          { "DOTRules:Httpd-1---require-all-granted", "https://www.adobe.com/go/aem_cmcq_httpd-1---require-al_en" },
          { "DOTRules:Syntax0---syntax-violation", "https://www.adobe.com/go/aem_cmcq_syntax0---syntax-vio_en" },
          { "DOTRules:Disp-S4---brace-unclosed", "https://www.adobe.com/go/aem_cmcq_disp-s4---brace-uncl_en" },
          { "DOTRules:Httpd-S1---include-failed", "https://www.adobe.com/go/aem_cmcq_httpd-s1---include-f_en" },
          { "DOTRules:Disp-S6---property-deprecated", "https://www.adobe.com/go/aem_cmcq_disp-s6---property-d_en" },
          { "DOTRules:Disp-S3---quote-unmatched", "https://www.adobe.com/go/aem_cmcq_disp-s3---quote-unma_en" },
          { "DOTRules:Disp-S2---token-unexpected", "https://www.adobe.com/go/aem_cmcq_disp-s2---token-unex_en" },
          { "DOTRules:Disp-S1---brace-missing", "https://www.adobe.com/go/aem_cmcq_disp-s1---brace-miss_en" },
          { "DOTRules:Disp-S5---mandatory-missing", "https://www.adobe.com/go/aem_cmcq_disp-s5---mandatory-_en" },
          { "DOTRules:Disp-S7---no-dispatcher-config", "https://www.adobe.com/go/aem_cmcq_disp-s7---no-dispatc_en" }
  }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

  @Test
  public void testGoURLs() throws IOException {
    AnalyzerRuleList ruleList = AnalyzerRuleListFactory.getAnalyzerRuleList();
    List<String> ids = ruleList.getRules().stream().map(AnalyzerRule::getId)
                               .collect(Collectors.toList());
    ids.add(ConfigurationViolations.UNKNOWN_VIOLATION_ID);
    ids.addAll(ConfigurationViolations.parsingRuleMap.keySet());

    for (String id: ids) {
      // Uncomment this line to output the generated array for `goUrls` above.
      //System.out.println("{ \"" + id + "\", \"" + GoUrlUtil.getDocumentationGoURL(id, "") + "\" },");
      assertEquals(goUrls.get(id), GoUrlUtil.getDocumentationGoURL(id, "bad_default"));
    }
    // If this fails, a rule has been added or removed from the core rules or parsing violations without updating
    // the appropriate list (goUrls, etc.)
    assertEquals(goUrls.size(), ids.size());
  }
}
