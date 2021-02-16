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

package com.adobe.aem.dot.dispatcher.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReservedTokensUtil {

  private final static List<String> reservedTokens = new ArrayList <>(Arrays.asList(
          "/allowAuthorized",
          "/allowedClients",
          "/auth_checker",
          "/cache",
          "/categories",
          "/clientheaders",
          "/delay",
          "/directory",
          "/docroot",
          "/enableTTL",
          "/encode",
          "/extension",
          "/failover",
          "/farm",
          "/farms",
          "/file",
          "/filter",
          "/glob",
          "/gracePeriod",
          "/header",
          "/headers",
          "/headers",
          "/health_check",
          "/homepage",
          "/hostname",
          "/ignoreUrlParams",
          "/invalidate",
          "/method",
          "/name",
          "/numberOfRetries",
          "/path",
          "/paths",
          "/port",
          "/propagateSyndPost",
          "/query",
          "/renders",
          "/retryDelay",
          "/rules",
          "/secure",
          "/selectors",
          "/serveStaleOnError",
          "/sessionmanagement",
          "/statfileslevel",
          "/statistics",
          "/stickyConnectionsFor",
          "/suffix",
          "/timeout",
          "/timeout",
          "/type",
          "/unavailablePenalty",
          "/url",
          "/vanity_urls",
          "/virtualhosts"
  ));

  public static boolean isReservedToken(String token) {
    return (ReservedTokensUtil.reservedTokens.contains(token) ||
            ReservedTokensUtil.reservedTokens.contains("/" + token));
  }
}
