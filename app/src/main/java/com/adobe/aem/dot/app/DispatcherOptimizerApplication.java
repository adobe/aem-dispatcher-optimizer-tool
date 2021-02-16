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

package com.adobe.aem.dot.app;

import com.adobe.aem.dot.app.service.ConfigurationOptimizerService;
import com.adobe.aem.dot.common.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Spring Boot app implementation of the Dispatcher Optimizer.
 */
@SpringBootApplication
public class DispatcherOptimizerApplication implements ApplicationRunner {

  private final Logger logger = LoggerFactory.getLogger(DispatcherOptimizerApplication.class);

  private final ConfigurationOptimizerService configurationOptimizerService;

  @Autowired
  public DispatcherOptimizerApplication(ConfigurationOptimizerService configurationOptimizerService) {
    this.configurationOptimizerService = configurationOptimizerService;
  }

  public static void main(String[] args) {
    SpringApplication.run(DispatcherOptimizerApplication.class, args);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    try {
      configurationOptimizerService.run();
    } catch (ConfigurationException e) {
      logger.error("General error running dispatcherConfigService.", e);
    }
  }
}
