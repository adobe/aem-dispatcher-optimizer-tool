# AEM Dispatcher Optimizer Tool

Analyze an AEM dispatcher module for violations of best practices, and report a list of actionable optimizations.

## Goals

The dispatcher is a critical component of a secure, stable, and lightning fast AEM implementation. Unfortunately it is often an afterthought, and its large array of configurations can be overwhelming and easily misconfigured.

This repository offers a tool (implemented as both a Maven Plugin and standalone app) which performs static analysis of the dispatcher module of an [AEM archetype](https://github.com/adobe/aem-project-archetype) based project. It can identify:

- Syntax issues such as misplaced tokens, unmatched quotes, or missing files referenced from `Include` directives
- Violations of best practices including checks for `/filter` deny rules, `/statfileslevel`, `/gracePeriod` and more

Don't agree with one of the rules? Wish there was a check for `/propertyX`? Use the extensible rules engine to augment (or completely replace) the core set of rules to meet your exact needs.

## Non-Goals

This repository does not attempt to suggest or prescribe a one-size-fits-all configuration for the dispatcher. Due to the myriad use cases that AEM supports, it would be impossible to do so. Instead, we attempt to identify syntax issues and known violations of best practices that can, when corrected, make your AEM environment more resilient and performant.

## Modules

- [core](core/) - Core code shared between the Plugin and App
- [app](app/) - Spring Boot app implementation and Dockerfile
- [plugin](plugin/) - Maven plugin implementation
- [plugin-it](plugin-it/) - Maven plugin integration tests

## Build

```
mvn clean install
```

## Use the Maven plugin

Refer to the [Plugin README](plugin/).

## Use the standalone app

Refer to the [App README](app/).

## Reading the code

### Rules

- The "core rules" can be found here: [core-rules.json](core/src/main/resources/core-rules.json)
- The spec of the Rules file can be found here: [Rules](core#rules)

### Plugin

- Entry point for the Maven plugin implementation: [AnalyzerMojo](plugin/src/main/java/com/adobe/aem/dot/dispatcher/plugin/AnalyzerMojo.java)

### App

- Entry point for the Spring Boot app implementation: [DispatcherOptimizerApplication](app/src/main/java/com/adobe/aem/dot/app/DispatcherOptimizerApplication.java)

### Core

- DispatcherConfiguration model class: [DispatcherConfiguration](core/src/main/java/com/adobe/aem/dot/dispatcher/core/model/DispatcherConfiguration.java)
- Test cases which exercise the core Parser code: [ConfigurationParserTest](core/src/test/java/com/adobe/aem/dot/dispatcher/core/ConfigurationParserTest.java)
- Test cases which exercise the core Rule Processor code: [com/adobe/aem/dot/common/analyzer/rules](core/src/test/java/com/adobe/aem/dot/common/analyzer/rules)

### IDE Support

- Install the [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok) for your IDE

## Releasing with Maven

### Releasing only the parent pom

From the project root:

```
mvn -N -Darguments=-N release:prepare
mvn -N -Darguments=-N release:perform
```

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
