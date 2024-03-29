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

package com.adobe.aem.dot.dispatcher.core.model;

import com.adobe.aem.dot.common.ConfigurationSource;
import com.adobe.aem.dot.common.analyzer.Severity;
import com.adobe.aem.dot.common.util.FeedbackProcessor;
import com.adobe.aem.dot.common.util.PathUtil;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationReader;
import com.adobe.aem.dot.dispatcher.core.parser.ConfigurationSyntaxException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_BOOLEAN_FALSE;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_INT_ONE;
import static com.adobe.aem.dot.dispatcher.core.model.ConfigurationValueDefaults.DEFAULT_VALUE_FILE_NAME;

/**
 * Represents an AEM dispatcher Farm entry. Each dispatcher configuration can have
 * many Farms. Often, there is 1 author and 1 publish farm.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class Farm extends LabeledConfigurationValue {
  private List<ConfigurationValue<String>> clientHeaders;
  private List<ConfigurationValue<String>> virtualHosts;
  private ConfigurationValue<List<Render>> renders;
  private ConfigurationValue<List<FilterRule>> filter;
  private ConfigurationValue<Cache> cache;
  private ConfigurationValue<Statistics> statistics;
  private ConfigurationValue<VanityUrls> vanityUrls;
  private ConfigurationValue<Boolean> propagateSyndPost = DEFAULT_BOOLEAN_FALSE;
  private ConfigurationValue<String> homePage;
  private ConfigurationValue<SessionManagement> sessionManagement;
  private ConfigurationValue<StickyConnectionsFor> stickyConnectionsFor;
  private ConfigurationValue<HealthCheck> healthCheck;
  private ConfigurationValue<Integer> retryDelay = DEFAULT_INT_ONE;
  private ConfigurationValue<Integer> numberOfRetries = new ConfigurationValue<>(5, DEFAULT_VALUE_FILE_NAME, 0);
  private ConfigurationValue<Integer> unavailablePenalty = DEFAULT_INT_ONE;
  private ConfigurationValue<Boolean> failOver = DEFAULT_BOOLEAN_FALSE;
  private ConfigurationValue<AuthChecker> authChecker;

  private static final String AUTHOR = "AUTHOR";
  private static final String AUTHOR_PORT = "4502";

  private static final Logger logger = LoggerFactory.getLogger(Farm.class);

  Logger getLogger() {
    return logger;
  }

  String getSimpleClassName() {
    return getClass().getSimpleName();
  }

  /**
   * Is this farm an author farm? The dispatcher module itself doesn't differentiate between farm types, so we need to
   * use the contents of the configuration to make an educated guess.
   * @return true if, by our best estimation, this is an author farm.
   */
  public boolean isAuthorFarm() {
    // Does the label contain "author"?
    // By convention, the author farm often (but not always) contains the word "author". If it does, we can be
    // reasonably sure this is an author farm.
    boolean labelContainsAuthor = this.getLabel().toUpperCase().contains(AUTHOR);
    if (labelContainsAuthor) {
      return true;
    }

    // Does the filename contain "author"?
    // If the label itself does not contain "author", sometimes the actual farm file in the dispatcher config does. We
    // can inspect this filename via the getLabelData() accessor.
    boolean filenameContainsAuthor = false;
    if (this.getLabelData() != null && this.getLabelData().getFileName() != null) {
      String[] pathParts = PathUtil.split(this.getLabelData().getFileName());
      String fileName = pathParts[pathParts.length - 1];
      filenameContainsAuthor = fileName.toUpperCase().contains(AUTHOR);
      if (filenameContainsAuthor) {
        return true;
      }
    }

    // Does the first render look like an author render?
    // If neither the label or farm filename reveal the type, we can inspect the farm's render for hints.
    boolean renderUsesAuthorPort = false;
    boolean renderUsesAuthorHost = false;
    if (this.getRenders() != null && this.getRenders().getValue().size() > 0) {
      Render firstRender = this.getRenders().getValue().get(0);
      // The render's `/port` property is often set to "${AUTHOR_PORT}" or "4502" for author instances
      String renderPort = firstRender.getPort() != null ? firstRender.getPort().getValue() : "";
      renderUsesAuthorPort = renderPort.toUpperCase().contains(AUTHOR) || renderPort.contains(AUTHOR_PORT);
      if (renderUsesAuthorPort) {
        return true;
      }

      // The render's `/hostname` property is often set to "${AUTHOR_IP}" for author instances
      String hostname = firstRender.getHostname() != null ? firstRender.getHostname().getValue() : "";
      renderUsesAuthorHost = hostname.toUpperCase().contains(AUTHOR);
      if (renderUsesAuthorHost) {
        return true;
      }
    }

    return false;
  }

  /**
   * Is this farm a publish farm? The dispatcher module itself doesn't differentiate between farm types, so we need to
   * use the contents of the configuration to make an educated guess.
   * @return true if it is NOT an author farm.
   */
  public boolean isPublishFarm() {
    return !this.isAuthorFarm();
  }

  public static List<ConfigurationValue<Farm>> parseFarms(ConfigurationReader reader) throws ConfigurationSyntaxException {
    List<ConfigurationValue<Farm>> farms = new ArrayList<>();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /farms block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MAJOR);
      return new ArrayList<>();
    }

    reader.next(); // Advance the reader's pointer passed the "{" marker.
    boolean hasMoreFarms = true;
    while (reader.hasNext() && hasMoreFarms) {
      ConfigurationValue<String> nextFarmLabel = reader.nextName();
      if (nextFarmLabel == null || nextFarmLabel.getValue().equals("}")) {
        // We've reached the end of the farms section
        hasMoreFarms = false;
      } else {
        logger.info("*** Processing Farm=\"{}\"...", nextFarmLabel.getFileName());
        Farm farm = Farm.parseFarm(reader);
        farm.setLabel(nextFarmLabel);
        farms.add(new ConfigurationValue<>(farm,
                nextFarmLabel.getFileName(), nextFarmLabel.getLineNumber(), nextFarmLabel.getIncludedFrom()));
      }
    }

    return farms;
  }

  private static Farm parseFarm(ConfigurationReader reader) throws ConfigurationSyntaxException {
    Farm farm = new Farm();

    // Expect { to begin the block
    if (!reader.isNextChar('{', false)) {
      FeedbackProcessor.error(logger,"Each /farm block must begin with a '{' character.",
              reader.getCurrentConfigurationValue(), Severity.MAJOR);
      return new Farm();
    }

    // Trace the braces encountered so we do not exit the "/Farms" block too soon.
    int braceDepth = 0;

    ConfigurationValue<String> parentToken = reader.next(); // Advance the reader's pointer passed the "{" marker.
    boolean hasMoreFarmDetailsToParse = true;
    while (reader.hasNext() && hasMoreFarmDetailsToParse) {
      ConfigurationValue<String> currentToken = reader.next();
      switch(currentToken.getValue()) {
        case "/clientheaders":
          logger.trace("farm > clientheaders");
          List<ConfigurationValue<String>> clientHeaders = reader.nextStringList();
          farm.setClientHeaders(clientHeaders);
          break;
        case "/virtualhosts":
          logger.trace("farm > virtualhosts");
          List<ConfigurationValue<String>> virtualHosts = reader.nextStringList();
          farm.setVirtualHosts(virtualHosts);
          break;
        case "/render":
        case "/renders":
          logger.trace("farm > renders");
          List<Render> renders = Render.parseRenders(reader);
          farm.setRenders(new ConfigurationValue<>(renders, currentToken));
          break;
        case "/filter":
          logger.trace("farm > filter");
          ConfigurationValue<List<FilterRule>> filters = FilterRule.parseFilters(reader);
          farm.setFilter(filters);
          break;
        case "/cache":
          logger.trace("farm > cache");
          Cache cache = Cache.parseCache(reader);
          farm.setCache(new ConfigurationValue<>(cache, currentToken));
          break;
        case "/statistics":
          logger.trace("farm > statistics");
          Statistics statistics = Statistics.parseStatistics(reader);
          farm.setStatistics(new ConfigurationValue<>(statistics, currentToken));
          break;
        case "/vanity_urls":
          logger.trace("farm > vanity_urls");
          VanityUrls vanityUrls = VanityUrls.parseVanityUrls(reader);
          farm.setVanityUrls(new ConfigurationValue<>(vanityUrls, currentToken));
          break;
        case "/propagateSyndPost":
          logger.trace("farm > propagateSyndPost");
          ConfigurationValue<Boolean> propagateSyndPost = reader.nextBoolean();
          farm.setPropagateSyndPost(propagateSyndPost);
          break;
        case "/homepage":
          FeedbackProcessor.error(logger, "/homepage is deprecated. Use IIS URL Rewrite Module.", currentToken,
                  Severity.MAJOR);
          farm.setHomePage(reader.next(false));
          break;
        case "/sessionmanagement":
          SessionManagement sessionManagement = SessionManagement.parseSessionManagement(reader);
          farm.setSessionManagement(new ConfigurationValue<>(sessionManagement, currentToken));
          break;
        case "/stickyConnectionsFor":
        case "/stickyConnections":
          // stickyConnectionsFor has 1 path as a string
          // stickyConnections has paths as a string list & other settings
          StickyConnectionsFor stickyFor = StickyConnectionsFor.parseStickyConnectionsFor(reader);
          if (farm.getStickyConnectionsFor() == null) {
            farm.setStickyConnectionsFor(new ConfigurationValue<>(stickyFor, currentToken));
          } else {
            // Append the paths, keeping the source information intact.
            List<ConfigurationValue<String>> joinedPaths =
                    Stream.concat(farm.getStickyConnectionsFor().getValue().getPaths().stream(),
                            stickyFor.getPaths().stream())
                            .collect(Collectors.toList());
            StickyConnectionsFor newSticky = new StickyConnectionsFor(joinedPaths, stickyFor.getHttpOnly(),
                    stickyFor.getSecure());
            farm.setStickyConnectionsFor(new ConfigurationValue<>(newSticky, farm.getStickyConnectionsFor()));
          }
          break;
        case "/health_check":
          logger.trace("farm > health_check");
          HealthCheck healthCheck = HealthCheck.parseHealthCheck(reader);
          farm.setHealthCheck(new ConfigurationValue<>(healthCheck, currentToken));
          break;
        case "/retryDelay":
          logger.trace("farm > retryDelay");
          farm.setRetryDelay(reader.nextInteger(1));
          // Is a number, default 1
          break;
        case "/numberOfRetries":
          logger.trace("farm > numberOfRetries");
          farm.setNumberOfRetries(reader.nextInteger(5));
          break;
        case "/unavailablePenalty":
          logger.trace("farm > unavailablePenalty");
          farm.setUnavailablePenalty(reader.nextInteger(1));  // unit 10th of seconds
          break;
        case "/failover":
          logger.trace("farm > failover");
          ConfigurationValue<Boolean> failover = reader.nextBoolean();
          farm.setFailOver(failover);
          break;
        case "/auth_checker":
          logger.trace("farm > auth_checker");
          AuthChecker authChecker = AuthChecker.parseAuthChecker(reader);
          farm.setAuthChecker(new ConfigurationValue<>(authChecker, currentToken));
          break;
        case "{":
          logger.trace("entered unknown section");
          braceDepth++;
          break;
        case "}":
          if (braceDepth == 0) {
            logger.trace("end farm section");
            hasMoreFarmDetailsToParse = false;
            Farm.processDefaultValues(farm, parentToken);
          } else {
            logger.trace("end of unknown section");
            braceDepth--;
          }
          break;
        default:
          FeedbackProcessor.error(logger, "Skipping unknown /farm level token. Token=\"{}\".", currentToken,
                  Severity.MAJOR);
          reader.advancePastThisElement();
      }
    }

    return farm;
  }

  /**
   * Process default values and set the source information to its parent's values
   * @param farm The parent Farm.
   */
  private static void processDefaultValues(Farm farm, final ConfigurationSource parentSource) {
    if (farm.getPropagateSyndPost().isUsingDefault()) {
      farm.setPropagateSyndPost(new ConfigurationValue<>(farm.getPropagateSyndPost().getValue(), parentSource));
    }
    if (farm.getRetryDelay().isUsingDefault()) {
      farm.setRetryDelay(new ConfigurationValue<>(farm.getRetryDelay().getValue(), parentSource));
    }
    if (farm.getNumberOfRetries().isUsingDefault()) {
      farm.setNumberOfRetries(new ConfigurationValue<>(farm.getNumberOfRetries().getValue(), parentSource));
    }
    if (farm.getUnavailablePenalty().isUsingDefault()) {
      farm.setUnavailablePenalty(new ConfigurationValue<>(farm.getUnavailablePenalty().getValue(), parentSource));
    }
    if (farm.getFailOver().isUsingDefault()) {
      farm.setFailOver(new ConfigurationValue<>(farm.getFailOver().getValue(), parentSource));
    }
  }
}
