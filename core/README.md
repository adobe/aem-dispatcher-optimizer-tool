# AEM Dispatcher Optimizer Tool Core

The core implementation of the Dispatcher Optimizer Tool. Includes Dispatcher & Apache Httpd configuration parsing, 
an extensible rules engine, and reporting features.

## Build

```
mvn clean install
```

## Release

From the `core/` root:

```
mvn release:prepare
mvn release:perform
```

## Rules

The core rules used by the Dispatcher Optimizer Tool are read from a JSON file, [core-rules.json](src/main/resources/core-rules.json).

### Rules JSON specification

Example rules file:

```
{
  "rules": [
    {
      "id": "DOTRules:Disp-1",
      "description": "ignoreUrlParams should be configured in an allow list manner...",
      "severity": "INFO",
      "farmTypeList": ["PUBLISH"],
      "element": "farm.cache.ignoreUrlParams",
      "type": "Code Smell",
      "tags": ["beta","dispatcher"],
      "enabled": true,
      "checks": [
        {
          "condition": "RULE_LIST_STARTS_WITH",
          "ruleValue": {
            "glob": "*",
            "type": "DENY"
          }
        }
      ]
    }
  ]
}
```

#### Top level object

| Property | Description | Optional? |
| -------- | ----------- | --------- |
| `rules` | An array of [AnalyzerRule](src/main/java/com/adobe/aem/dot/common/analyzer/AnalyzerRule.java) objects. This array of rules determines how the dispatcher configuration will be analyzed. | no |
| `mergeMode` | If there are existing rules present, how should this set of rules be incorporated? Supports `EXTEND` or `REPLACE` | yes, defaults to `EXTEND` |

#### [AnalyzerRule](src/main/java/com/adobe/aem/dot/common/analyzer/AnalyzerRule.java) items

| Property | Description | Optional? |
| -------- | ----------- | --------- |
| `element` | A dot separated string which describes the path to the configuration element(s) to check. For example: `farm.filter`, `farm.cache.ignoreUrlParams`, `farm.cache.gracePeriod`, or `httpd.vhost.directory`. Element strings beginning with `farm` will be applied to the dispatcher farm config, and element strings beginning with `httpd` will be applied to the Apache Httpd config. | no |
| `id` | A string value defining a unique identifier for this rule. | no |
| `description` | Details about the rule. This value will be included in the report presented to the end user, if the provided configuration fails this rule. | no |
| `documentationURL` | URL of a page containing details about the rule. | no |
| `severity` | A value indicating the importance of this rule. It's available values are defined in [Severity](src/main/java/com/adobe/aem/dot/common/analyzer/Severity.java), and it will be used to sort how the violations are listed in the report. | no |
| `farmTypeList` | Array of [FarmType](src/main/java/com/adobe/aem/dot/dispatcher/core/analyzer/FarmType.java). Used to determine which type of farm (`AUTHOR`, `PUBLISH`, or both) this rule should apply to. If a farm's name includes the string "author" it is considered an Author farm, otherwise it is considered a Publish farm. | no |
| `type` | A string describing the type of issue the rule detects, such as "Code Smell" or "Vulnerability". | yes |
| `tags` | A list of strings that categorize the rule. | yes |
| `enabled` | If false, this rule will be skipped. | yes, defaults to `true` |
| `checks` | An array of [Check](src/main/java/com/adobe/aem/dot/common/analyzer/Check.java) objects. This defines the expected value to use in the check, and the condition to use when comparing the provided value. If there are multiple checks, all checks will need to "pass" in order for the AnalyzerRule to pass. In other words, the checks should be considered to be combined with `AND` operators. | no |

#### [Check](src/main/java/com/adobe/aem/dot/common/analyzer/Check.java) items

Note: non-applicable element & condition combinations will be ignored. For example, using the `HAS_DIRECTIVE` condition 
will have no effect with a dispatcher element such as `farm.filter`. 

| Property | Description | Optional? | Dispatcher only? | Apache Httpd only? |
| -------- | ----------- | --------- | :--------------: | :----------------: |
| `condition` | Enum, maps to the values defined in [Condition](src/main/java/com/adobe/aem/dot/common/analyzer/Condition.java). Defines how the configuration value should be compared to the value in the Check. | no |  |  |
| `value` | Can be used to provide string or integer values to test. | yes, defaults to `null` | ☑️ |  |
| `ruleValue` | Can be used to provide [Rule](src/main/java/com/adobe/aem/dot/dispatcher/core/model/Rule.java) objects to test. Rule type values are used for `/ignoreUrlParas`, `/invalidate`, `/allowedClients` and [Cache](src/main/java/com/adobe/aem/dot/dispatcher/core/model/Cache.java) `/rules`.  Its fields may specify regex values by placing the string in regex specifier: `regex(string)` | yes, defaults to `null` | ☑️ |  |
| `filterValue` | Can be used to provide [Filter](src/main/java/com/adobe/aem/dot/dispatcher/core/model/Filter.java) objects to test. These are used by the [Farm](src/main/java/com/adobe/aem/dot/dispatcher/core/model/Farm.java)'s `/filter` configuration.  Its fields may specify regex values by placing the string in regex specifier: `regex(string)` | yes, defaults to `null` | ☑️ |  |
| `directiveValue` | Can be used to provide [Directive](src/main/java/com/adobe/aem/dot/httpd/core/model/Directive.java) objects to test. These are used by the Apache Httpd configuration. | yes, defaults to `null` |  | ☑️ |
| `context` | Details about this check. Will be included in the report. | yes |  |  |
| `failIf` | If set to `true`, a positive result found when processing this check will count as a rule failure | yes, defaults to `false` |  |  |

#### Available [Condition](src/main/java/com/adobe/aem/dot/common/analyzer/Condition.java)s

| Value | Description | Dispatcher only? | Apache Httpd only? |
| ----- | ----------- | :--------------: | :----------------: |
| `INT_GREATER_OR_EQUAL` | Compares a value from the provided dispatcher configuration (identified by the AnalyzerRule's `element`) to see if it is greater than or equal to the provided `value`. | ☑️ |  |
| `INT_EQUALS` | Compares a value from the provided dispatcher configuration (identified by the AnalyzerRule's `element`) to see if it is equal to the provided `value`. | ☑️ |  |
| `BOOLEAN_EQUALS` | Compares a boolean/flag value from the provided dispatcher configuration (identified by the AnalyzerRule's `element`) to see if it is equal to the provided `value`. | ☑️ |  |
| `RULE_LIST_STARTS_WITH` | Inspects a Rule list from the provided dispatcher configuration (identified by the AnalyzerRule's `element`) to see if it starts with the provided `ruleValue`. | ☑️ |  |
| `RULE_LIST_INCLUDES` | Inspects a Rule list from the provided dispatcher configuration (identified by the AnalyzerRule's `element`) to see if it includes the provided `ruleValue`. | ☑️ |  |
| `FILTER_LIST_STARTS_WITH` | Inspects a Filter list from the provided dispatcher configuration (identified by the AnalyzerRule's `element`) to see if it starts with the provided `filterValue`. | ☑️ |  |
| `FILTER_LIST_INCLUDES` | Inspects a Filter list from the provided dispatcher configuration (identified by the AnalyzerRule's `element`) to see if it includes the provided `filterValue`. | ☑️ |  |
| `IS_UNIQUE_LABEL` | Inspects a List of labeled configuration elements (identified by the AnalyzerRule's `element`) to see if it includes any duplicate labels. This can be useful to ensure each Farm has a unique label (`"element": "farm"`), or to check that all Filter rules have unique labels (`"element": "farm.filter"`). | ☑️ |  |
| `HAS_DIRECTIVE` | Inspects the list of [Directive](src/main/java/com/adobe/aem/dot/httpd/core/model/Directive.java)s set for a [Section](src/main/java/com/adobe/aem/dot/httpd/core/model/Section.java) (identified by the AnalyzerRule's `element`) to see if they include the directive specified by the provided `directiveValue`. This can be useful to ensure that certain sections do (or do not, with `"failIf": true`) have specific directives set. |  | ☑️ |


### Extending the core rules

The Dispatcher Optimizer will always come loaded with a core set of rules 
([core-rules.json](src/main/resources/core-rules.json)). As all future scenarios cannot be realistically
foreseen, the rules can be disabled, overwritten and extended as desired.  A user can do this by loading
multiple rule files when the Optimizer begins by specifying an _OPTIMIZER_RULES_FOLDER_ environment
variable.  JSON rule files in that folder will be loaded in alphabetical order.  To learn how to manipulate
the rules, please read the following points.

#### Rules Set: REPLACE
If the rule file that is being loaded has its [mergeMode](#top-level-object) set to **"REPLACE"**, then that
rule set will completely replace what has already been loaded.  All existing rules will no longer exist in the 
Optimizer.

#### Rules Set: EXTEND
If the rule file that is being loaded has its [mergeMode](#top-level-object) set to **"EXTEND"**, then that
rule set will be _added_ to the rules already loaded with one exception.  If any rule being loaded has exactly
the same id as an existing rule, the new rule will completely replace the existing rule.

When extending rules, the optimizer will not attempt to prevent any collision.  For instance, if two loaded
rules insist that a section begin with different values, at least one of those rules will produce a violation.
Some rules may become redundant.  For instance, if one rule specifies a value must be greater than 2 and another
specifies that same value must be greater than 4, the first rule adds no value. 

Some cautions are logged when the rules are being loaded, but it is up to the person setting up the rules to
produce a final set that is logical.

#### Tips

##### Quiet a Rule

Here are some ways to reduce the impact of a rule:
- Copy the rule to a rule file being loaded and change its properties (except `id`) to check for configuration
values more suited to the environment.
- Copy the rule to a rule file being loaded and change the [severity property](#analyzerrule-items) to
**"INFO"**.
- Copy the rule to a rule file being loaded and change the [enabled flag](#analyzerrule-items) to **false**.

When disabling or overwriting a rule, it is advised to understand why that rule is important.  Blindly getting rid of
a rule may cause errors in the future.

##### Add Rules

To add your own rules:
- create a rules file, following the (specification)[#rules-json-specification], with new rules and
load it in when executing the optimizer using the _OPTIMIZER_RULES_PATH_ environment variable.


## Configuration Parsing Issues

The Dispatcher and Apache Httpd configurations can contain syntax issues.  The user is encouraged to check the
logs regularly for details about issues that are encountered.  Some parsing syntax issues will produce violations.
Those violations pertain to the structure of the configuration and not its functionality and will have ids starting
with `DOTRules:Disp-Sn---description` for the Dispatcher and `DOTRules:Httpd-Sn---description` for Apache Httpd
(where `Sn` creates a unique ID for each scenario, briefly described in the `description` - `S` for "syntax").
