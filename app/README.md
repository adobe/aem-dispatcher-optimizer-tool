# AEM Dispatcher Optimizer Tool App

Analyze an AEM dispatcher configuration for issues. Output a CSV report of the issues detected including the rule description, the context in which the violation was detected, and the severity of the issue.

## Build

This project requires Java 8.

```
mvn install
```

## Run

- Set `REPOSITORY_URL` to the path to an AEM project directory (at minimum: a directory containing a dispatcher module)
- Set `ARTIFACTS_DESTINATION_PATH` to the path where you would like the results file (results.csv) written

For example, to test an AEM project in `../test-projects/test-project-all-rules-fail` and have the results written to 
`./results.csv`:

```
REPOSITORY_URL=../test-projects/test-project-all-rules-fail \
ARTIFACTS_DESTINATION_PATH=. \
java -jar target/dispatcher-optimizer-app-*.jar
```

### Run in IntelliJ

Set up a "Spring Boot" run configuration, and set the following 2 Environment Variables to test the included `test-project/test-project-all-rules-fail` config and output results to `results/test-project/`:

```
REPOSITORY_URL=$PROJECT_DIR$/test-project/test-project-all-rules-fail;ARTIFACTS_DESTINATION_PATH=$PROJECT_DIR$/results/test-project
```

## Release

1. Update the `dispatcher-optimizer-core` dependency version in pom.xml to the latest release (ie. `1.0.4`). 
2. Build and test the app with the updated dependency: `mvn clean install`
3. Commit the pom.xml change and push to `main`.
4. Release the app. From the `app/` root:
```
mvn release:prepare
mvn release:perform
```
5. Finally, update the `dispatcher-optimizer-core` dependency version in pom.xml to the latest SNAPSHOT (ie. `1.0.5-SNAPSHOT`).
6. Commit the pom.xml change and push to `main`.

## Docker

Build Docker image:

```
docker build -t aem-skylab/dispatcher-optimizer-tool:v1 .
```

Run Docker image:

Note how we're mounting 2 directories:
- `$PWD` is mounted to `/mnt/git` - this is where the dispatcher config will be read from
- `$PWD/results-container` is mounted to `/mnt/artifacts` - this is where the results will be written

```
cd test-projects/test-project-all-rules-fail
docker run -it --rm -v $PWD:/mnt/git -v $PWD/results-container:/mnt/artifacts aem-skylab/dispatcher-optimizer-tool:v1
```
