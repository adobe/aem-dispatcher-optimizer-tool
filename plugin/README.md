# AEM Dispatcher Optimizer Tool Maven Plugin

Maven plugin implementation of the Dispatcher Optimizer Tool.

## Build and install locally

```
mvn clean install
```

## Use from an AEM archetype based project

Include the following in the project's pom.xml, build.plugins section:

```
<!-- Dispatcher Optimizer Plugin -->
<plugin>
    <groupId>com.adobe.aem.dot</groupId>
    <artifactId>dispatcher-optimizer-maven-plugin</artifactId>
    <version>0.2.3-SNAPSHOT</version>
    <configuration>
        <optimizerRulesPath></optimizerRulesPath>
        <reportVerbosity>PARTIAL</reportVerbosity>
        <!-- Specify any test parameters here -->
    </configuration>
</plugin>
```

#### CONFIGURATION

The `<configuration>` settings are as follows:
* _optimizerRulesPath_: specify a path to a directory containing addition rule files to extend the core rules to
  validate against the configuration files.
* _reportVerbosity_: Specify the level of verbosity of the final violation report.
  * **FULL**:  All violations will be reported.
  * **PARTIAL**: A violation can occur in a configuration file that is included many times, producing many, 
    near-identical violations.
    A PARTIAL value will collapse all these into 1 line, indicating how many times the violation was encountered.
  * **MINIMIZED**: In addition to the PARTIAL collapsing of violations, MINIMIZED will only report each distinct rule
    or parsing violation only once, indicating how many times it was encountered.

## Run it

Run the following goal:

```
mvn dispatcher-optimizer:analyze
```

Optionally, check _target/dispatcher-optimizer-tool/results.csv_, the location of the final DOT report.

## Release

From the `plugin/` root:

```
mvn release:prepare
mvn release:perform
```
