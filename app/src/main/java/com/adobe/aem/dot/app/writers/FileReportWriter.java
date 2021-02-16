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

package com.adobe.aem.dot.app.writers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Component
public class FileReportWriter implements ReportWriter {

  private Logger logger = LoggerFactory.getLogger(FileReportWriter.class);

  private String pathToReportDestination;

  public FileReportWriter(
          @Value("${ARTIFACTS_DESTINATION_PATH}") String artifactDestinationPath,
          @Value("${REPORT_FILE_NAME}") String reportFileName
  ) {
    boolean pathEndsWithSlash = artifactDestinationPath.endsWith(File.separator);
    this.pathToReportDestination = artifactDestinationPath + (pathEndsWithSlash ? "" : File.separator) + reportFileName;
  }

  @Override
  public void writeReport(String report) throws IOException {
    logger.info("Begin: Writing report.  Destination=\"{}\"", this.pathToReportDestination);

    // Create directories, if needed
    File reportFile = new File(this.pathToReportDestination);
    reportFile.getParentFile().mkdirs();

    // Write report
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.pathToReportDestination))) {
      writer.write(report);
    }

    logger.info("End: Wrote report.  Destination=\"{}\"", this.pathToReportDestination);
  }
}
