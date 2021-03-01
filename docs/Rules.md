# Dispatcher Optimizer Tool (DOT) Core Rules

Below are details on each of the rules that the DOT includes out-of-the-box. These are known as the core rules.

- [DOT - Parsing Violation - Dispatcher Configuration Unexpected Tokens](#dot---parsing-violation---dispatcher-configuration-unexpected-tokens)
- [DOT - Parsing Violation - Dispatcher Configuration Unmatched Quote](#dot---parsing-violation---dispatcher-configuration-unmatched-quote)
- [DOT - Parsing Violation - Dispatcher Configuration Missing Brace](#dot---parsing-violation---dispatcher-configuration-missing-brace)
- [DOT - Parsing Violation - Dispatcher Configuration Extra Brace](#dot---parsing-violation---dispatcher-configuration-extra-brace)
- [DOT - Parsing Violation - Dispatcher Configuration Missing Mandatory Property](#dot---parsing-violation---dispatcher-configuration-missing-mandatory-property)
- [DOT - Parsing Violation - Dispatcher Configuration Deprecated Property](#dot---parsing-violation---dispatcher-configuration-deprecated-property)
- [DOT - Parsing Violation - Dispatcher Configuration Not Found](#dot---parsing-violation---dispatcher-configuration-not-found)
- [DOT - Parsing Violation - Httpd Configuration Include file not found](#dot---parsing-violation---httpd-configuration-include-file-not-found)
- [DOT - Parsing Violation - Dispatcher Configuration General](#dot---parsing-violation---dispatcher-configuration-general)
- [DOT - The Dispatcher publish farm cache should have serveStaleOnError enabled](#dot---the-dispatcher-publish-farm-cache-should-have-servestaleonerror-enabled)
- [DOT - The Dispatcher publish farm filters should contain the default `deny` rules from the 6.x.x version of the AEM archetype](#dot---the-dispatcher-publish-farm-filters-should-contain-the-default-deny-rules-from-the-6xx-version-of-the-aem-archetype)
- [DOT - The Dispatcher publish farm cache statfileslevel property should be >= 2](#dot---the-dispatcher-publish-farm-cache-statfileslevel-property-should-be--2)
- [DOT - The Dispatcher publish farm gracePeriod property should be >= 2](#dot---the-dispatcher-publish-farm-graceperiod-property-should-be--2)
- [DOT - Each Dispatcher farm should have a unique name](#dot---each-dispatcher-farm-should-have-a-unique-name)
- [DOT - The Dispatcher publish farm cache should have its ignoreUrlParams rules configured in an allow list manner](#dot---the-dispatcher-publish-farm-cache-should-have-its-ignoreurlparams-rules-configured-in-an-allow-list-manner)
- [DOT - The Dispatcher publish farm filters should specify the allowed Sling selectors in an allow list manner](#dot---the-dispatcher-publish-farm-filters-should-specify-the-allowed-sling-selectors-in-an-allow-list-manner)
- [DOT - The Dispatcher publish farm filters should specify the allowed Sling suffix patterns in an allow list manner](#dot---the-dispatcher-publish-farm-filters-should-specify-the-allowed-sling-suffix-patterns-in-an-allow-list-manner)
- [DOT - The 'Require all granted' directive should not be used in a VirtualHost Directory section with a root directory-path](#dot---the-require-all-granted-directive-should-not-be-used-in-a-virtualhost-directory-section-with-a-root-directory-path)

## DOT - Parsing Violation - Dispatcher Configuration Unexpected Tokens

Key: `DOTRules:Disp-S2---token-unexpected`

Type: Code Smell

Severity: Major

Since: TBD

The Dispatcher configuration has a well defined structure. Reserved words and hierarchies are used to configure specific aspects of the module. When these reserved tokens are used out of place, or some unknown token is used, the intent will not be achieved. In fact, they might have unattended side-effects which are not immediately evident.

This parsing violation will identify unexpected tokens and indicate the file and line number where the issue was encountered. It is also recommended to review the logs as they contain more details than the reported violations including whether a known, reserved property name is being used in an incorrect location.

#### Non-Compliant Code

```
/filter {
  /statfileslevel "2"
}
```

#### Compliant Code

```
/cache {
  /statfileslevel "2"
}
```

## DOT - Parsing Violation - Dispatcher Configuration Unmatched Quote

Key: `DOTRules:Disp-S3---quote-unmatched`

Type: Code Smell

Severity: Major

Since: TBD

Many Dispatcher configuration string values are expected to be contained within quotes, either double or single quotes. At times, quotes can be misplaced, inserted incorrectly, accidentally pasted or deleted, etc. and many unattended side-effects can occur which are not immediately evident. These missing or additional quotes can be hard to find in the set of Dispatcher configuration files.

This violation will identify missing or extra quotes and indicate the file and line number where the issue was encountered. It is also recommended to review the logs as they contain more details than the reported violations.

#### Non-Compliant Code

```
"cq-handle'
```

#### Compliant Code

```
"cq-handle"
```

## DOT - Parsing Violation - Dispatcher Configuration Missing Brace

Key: `DOTRules:Disp-S1---brace-missing`

Type: Code Smell

Severity: Major

Since: TBD

Most multi-valued properties in the Dispatcher configuration group values within curly braces. At times, braces can be misplaced, inserted incorrectly, accidentally pasted or deleted, etc. and many unpredictable configurations can result. These missing or additional braces can be difficult to locate in the set of Dispatcher configuration files.

This violation will identify missing braces and indicate the file and line number where the issue was encountered. It is also recommended to review the logs as they contain more context than the reported violation.

#### Non-Compliant Code

```
/renders {
  /0
    /hostname "127.0.0.1"
    /port "4502"
}
```

#### Compliant Code

```
/renders {
  /0 {
    /hostname "127.0.0.1"
    /port "4502"
  }
}
```

## DOT - Parsing Violation - Dispatcher Configuration Extra Brace

Key: `DOTRules:Disp-S4---brace-unclosed`

Type: Code Smell

Severity: Major

Since: TBD

Most multi-valued properties in the Dispatcher configuration group values within curly braces. At times, braces can be misplaced, inserted incorrectly, accidentally pasted or deleted, etc. and many unpredictable configurations can result. These missing or additional braces can be difficult to locate in the set of Dispatcher configuration files.

This violation will identify when an unexpected token starts with a brace, but does not have a matching closing brace. This is a similar case to `DOTRules:Disp-S1---brace-missing` but is reported for unexpected tokens. It is usually an indicator that the configuration is not set up in an understandable way. The log will contain more information and will indicate where the parsing started to fail.

#### Non-Compliant Code

```
/unexpectedToken {
  /property "value"
# note missing '}'
```

#### Compliant Code

```
/unexpectedToken {
  /property "value"
}
```

## DOT - Parsing Violation - Dispatcher Configuration Missing Mandatory Property

Key: `DOTRules:Disp-S5---mandatory-missing`

Type: Code Smell

Severity: Major

Since: TBD

The Dispatcher configuration has a well defined structure. Some multi-valued properties have values that are mandatory.
This parsing violation will identify missing values that are mandatory for the multi-valued property. It is also recommended to review the logs as they contain more details than the reported violations.

#### Non-Compliant Code

```
/sessionmanagement {}
```

#### Compliant Code

```
/sessionmanagement {
  /directory "/usr/local/apache/.sessions"
}
```

## DOT - Parsing Violation - Dispatcher Configuration Deprecated Property

Key: `DOTRules:Disp-S6---property-deprecated`

Type: Code Smell

Severity: Major

Since: TBD

The Dispatcher configuration has a well defined structure which has evolved over time. As technology changes, some values become deprecated.

This parsing violation will identify the use of deprecated values, which should be reviewed and updated. It is also recommended to review the logs as they contain more details than the reported violations.

#### Non-Compliant Code

```
/publishfarm {
  /homepage "deprecated"
  # other farm configurations
}
```

#### Compliant Code

```
/publishfarm {
  # other farm configurations
}
```

## DOT - Parsing Violation - Dispatcher Configuration Not Found

Key: `DOTRules:Disp-S7---no-dispatcher-config`

Type: Code Smell

Severity: Major

Since: TBD

A "dispatcher.any" configuration file was not found so the configuration parsing and analysis could not be performed.  This violation is mostly to ensure the user is not given the impression that their configuration was free of violations.  Instead it announces that no Dispatcher configuration was analyzed at all.

## DOT - Parsing Violation - Httpd Configuration Include file not found

Key: `DOTRules:Httpd-S1---include-failed`

Type: Code Smell

Severity: Major

Since: TBD

The Apache Httpd configuration spec contains an `Include` directive. This indicates that its argument should be included as part of the configuration. It also contains a `IncludeOptional` directive. The difference between the two is that the former reports an error if the included file is not found. The latter does not report an error.

This violation is to alert the user that a mandatory file was not included successfully.

#### Non-Compliant Code

```
Include conf.d/not_a_real_file.conf
```

#### Compliant Code

```
Include conf.modules.d/*.conf
```

## DOT - Parsing Violation - Dispatcher Configuration General

Key: `DOTRules:Syntax0---syntax-violation`

Type: Code Smell

Severity: Major

Since: TBD

This violation will be raised if a parsing-related problem occurs that does not fit into the foreseen categories (above). The developer should review the logs to determine the details of this general violation.

## DOT - The Dispatcher publish farm cache should have serveStaleOnError enabled

Key: `DOTRules:Disp-5---serveStaleOnError`

Type: Code Smell

Severity: Major

Since: TBD

The serveStaleOnError flag may be considered to be set to "1" to enable invalidated pages to be served from the cache when an error response is received from the AEM publish tier, if this is acceptable with your customization requirement.

For additional detail on this setting: https://helpx.adobe.com/experience-manager/kb/ServeStaleContentOnError.html

#### Non-Compliant Code

```
/cache {
  # serveStaleOnError not set
```

#### Compliant Code

```
/cache {
  /serveStaleOnError "1"
```

## DOT - The Dispatcher publish farm filters should contain the default `deny` rules from the 6.x.x version of the AEM archetype

Key: `DOTRules:Disp-4---default-filter-deny-rules`

Type: Code Smell

Severity: Major

Since: TBD

The default deny rules from the AMS flavour of the AEM archetype should be left in-place and extended as needed.

The AEM archetype's dispatcher.ams configuration contains a set of publish farm rules which are intended to be left as-is for security and performance reasons: conf.dispatcher.d/filters/ams_publish_filters.any.

As additional filter rules are needed, they should be added in a separate file next to ams_publish_filters.any. The publish farm file (999_ams_publish_farm.any for example) would then be amended with an additional $include to pull in the site-specific filter rule file.

With this approach, any upstream changes to ams_publish_filters.any (for example, blocking of newly discovered exploit vectors) can be easily integrated by replacing this file.

#### Non-Compliant Code

```
/0001 { /type "allow"  /url "*" }
```

#### Compliant Code

```
# filter set for a publish farm should start with a "deny" of "*"
/0001 { /type "deny"  /url "*" }
 
# the following filter "deny" entries should also be present in the filter set
/0100 { /type "deny" /selectors '(feed|rss|pages|languages|blueprint|infinity|tidy|sysview|docview|query|[0-9-]+|jcr:content)' /extension '(json|xml|html|feed)' }
/0101 { /type "deny" /method "GET" /query "debug=*" }
/0102 { /type "deny" /method "GET" /query "wcmmode=*" }
/0103 { /type "deny" /path "/content/ams/healthcheck/*" }
/0104 { /type "deny" /url "/content/regent.html" }
```

## DOT - The Dispatcher publish farm cache statfileslevel property should be >= 2

Key: `DOTRules:Disp-2---statfileslevel`

Type: Code Smell

Severity: Major

Since: TBD

A statfileslevel set to 0 or 1 can cause issues where cache invalidations affect vast (and unrelated) parts of the cache, causing unnecessary stress on the publish tier whenever activations happen.

When a publish farm's statfileslevel is set to 2 or greater, it means that .stat files can be placed deeper into the cache directories (1 = /content, 2 = /content/we-retail, etc.). When set optimally, publishing changes to a page will only invalidate related portions of the cache where this page may be referenced. For example, changing a page in the the us/en language hierarchy of the site should only invalidate us/en, while sparing ca/fr from invalidation.

A statfileslevel of 0 means that there is only one .stat file present in the cache: at the root. Whenever anything is activated this .stat file is touched, and the entire cache is considered invalid (the exact files that are considered invalid are defined by the /invalidate rules). When an increased volume of activation events occurs, this setting can cause the publish tier to repeatedly render the same pages over and over each time they become invalidated in the cache. Given enough traffic, this can cause stress on the publish tier and result in performance degredation. At best, it reduces the end user's experience of the site, as rendering a page takes much longer than serving it directly from the cache.

If you'd like to experiment with this setting, consider trying the AEM dispatcher experiments: [Effect of a statfileslevel greater than 0](https://github.com/adobe/aem-dispatcher-experiments/blob/main/experiments/statfileslevel)

#### Non-Compliant Code

```
/cache {
  /statfileslevel "0"
```

#### Compliant Code

```
/cache {
  /statfileslevel "2"
```

## DOT - The Dispatcher publish farm gracePeriod property should be >= 2

Key: `DOTRules:Disp-3---gracePeriod`

Type: Code Smell

Severity: Major

Since: TBD

Setting gracePeriod defines the number of seconds a stale, auto-invalidated resource may still be served from the cache after the last activation occurring. This can shield the publish tier from spikes in load when a number of cache invalidation events occur in quick succession. Please evaluate whether this can work with your site requirements.

If you'd like to experiment with this setting, consider trying the AEM dispatcher experiments: Effect of the gracePeriod setting

#### Non-Compliant Code

```
/cache {
  # gracePeriod unset, which defaults to "0"
```

#### Compliant Code

```
/cache {
  /gracePeriod "2"
```

## DOT - Each Dispatcher farm should have a unique name

Key: `DOTRules:Disp-8---unique-farm-name`

Type: Code Smell

Severity: Major

Since: TBD

All enabled farms referenced from dispatcher.any should have a unique name.

#### Non-Compliant Code

```
/farms
  {
  # author farm
  /examplesite { ... }
 
  # publish farm
  /examplesite { ... }
  }
```

#### Compliant Code

```
/farms
  {
  /examplepublish { ... }
 
  /exampleauthor { ... }
  }
```

## DOT - The Dispatcher publish farm cache should have its ignoreUrlParams rules configured in an allow list manner

Key: `DOTRules:Disp-1---ignoreUrlParams-allow-list`

Type: Code Smell

Severity: Major

Since: TBD

For a publish farm's cache config, the ignoreUrlParams setting should be configured so that all query parameters are ignored, and only known/expected query parameters are exempt ("denied") from being ignored. This means that a request containing an unexpected query param (for example, en.html?utm_source=email) will still be handled by the dispatcher as a request for en.html, and can be served from the cache (if present and valid).

If you'd like to experiment with this setting, consider trying the AEM dispatcher experiments: [Effect of an ignoreUrlParams allow list](https://github.com/adobe/aem-dispatcher-experiments/blob/main/experiments/ignoreUrlParams)

#### Non-Compliant Code

```
/ignoreUrlParams {
  /0001 { /glob "*" /type "deny" }              # all query params "denied" from being ignored
  /0002 { /glob "utm_campaign" /type "allow" }  # only utm_campaign is ignored
}
```

#### Compliant Code

If there are query parameters which are needed by the server side code, then they should explicitly be "denied" from being ignored. For example, consider a search term query param which is used by a server side search method. When a request for en.html?search=cycling is received, it should be handled by the publish tier and always count as a cache miss.

In this case, "search" would be added to the allow list:

```
/ignoreUrlParams {
  /0001 { /glob "*" /type "allow" }
  /0002 { /glob "search" /type "deny" }
}
```

## DOT - The Dispatcher publish farm filters should specify the allowed Sling selectors in an allow list manner

Key: `DOTRules:Disp-7---selector-allow-list`

Type: Code Smell

Severity: Major

Since: TBD

For a publish farm config, all Sling selectors should be denied by a filter rule to prevent a malicious user from crafting requests that could overwhelm both the publish tier and consume disk space on the dispatcher. Individual selectors which are needed on the server side can be added in an allow list manner.

For more background on this recommended approach, please refer to the Security Checklist: [Mitigate Denial of Service (DoS) Attacks](https://helpx.adobe.com/ca/experience-manager/6-3/sites/administering/using/security-checklist.html#FurtherReadings)

#### Non-Compliant Code

```
/publishfarm {
  /filter {
    # no restriction on which selectors can be used
```

#### Compliant Code

```
/publishfarm {
  /filter {
    # filter set from AMS-style AEM archetype
     
    # Block use of all selectors on any resource in /content
    /0150 { /type "deny" /url "/content*" /selectors "*" }
 
    # Individual selectors which are needed on the server side can be added in an allow list manner
    /0151 { /type "allow" /url "/content*" /selectors '(legit-selector|other-fine-selector)' /method "GET" }
```

## DOT - The Dispatcher publish farm filters should specify the allowed Sling suffix patterns in an allow list manner

Key: `DOTRules:Disp-6---suffix-allow-list`

Type: Code Smell

Severity: Major

Since: TBD

For a publish farm config, all Sling suffixes should be denied by a filter rule to prevent a malicious user from crafting requests that could overwhelm both the publish tier and consume disk space on the dispatcher. Suffix patterns which are needed on the server side can be added in an allow list manner.

For more background on this recommended approach, please refer to the Security Checklist: [Mitigate Denial of Service (DoS) Attacks](https://helpx.adobe.com/ca/experience-manager/6-3/sites/administering/using/security-checklist.html#FurtherReadings)

#### Non-Compliant Code

```
/publishfarm {
  /filter {
    # no restriction on which suffixes can be used
```

#### Compliant Code

```
/publishfarm {
  /filter {
    # filter set from AMS-style AEM archetype
     
    # Block use of all suffixes on any resource in /content
    /0160 { /type "deny" /url "/content*" /suffix "*" }
 
    # Suffix patterns which are needed on the server side can be added in an allow list manner
    /0161 { /type "allow" /url "/content/we-retail/us/en/equipment/*" /suffix "/content/we-retail/*" /method "GET" }
```

## DOT - The 'Require all granted' directive should not be used in a VirtualHost Directory section with a root directory-path

Key: `DOTRules:Httpd-1---require-all-granted`

Type: Code Smell

Severity: Major

Since: TBD

When a VirtualHost's Directory section is configured improperly it can allow access to files that exist outside of the Apache document root. Setting "Require all granted" on the root can expose the filesystem to anonymous access.

The AEM archetype should be used as a reference when resolving violations of this rule. Note how the top level `<Directory />` section has Require all denied set. The `<Directory />` sections in the individual .vhost files do not include `Require all granted` (i.e. aem_publish.vhost).

#### Non-Compliant Code

```
<VirtualHost *:80>
    <Directory />
        Require all granted
    </Directory>
</VirtualHost>
```

#### Compliant Code

```
<VirtualHost *:80>
    <Directory "${PUBLISH_DOCROOT}">
        Require all granted
    </Directory>
</VirtualHost>
```
