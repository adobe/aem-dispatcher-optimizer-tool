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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class MatchesBuilder {

    private static final Logger logger = LoggerFactory.getLogger(MatchesBuilder.class.getName());
    private static final String REGEX_MARKER = "regex(";

    @Getter
    @Setter(AccessLevel.PRIVATE)
    class MatchesPair {
        private String match;
        private String value;

        MatchesPair(String match, String value) {
            setMatch(match == null ? null : match.trim());
            setValue(value == null ? null : value.trim());
        }

        /**
         * Return true if the strings are an exact match, or if the value 'matches' the match regex.
         * @return whether the two strings match
         */
        boolean matches() {
            if (getMatch() == null) {
                return true;
            }

            if (getValue() == null) {
                return false;
            }

            boolean isRegex = getMatch().startsWith(REGEX_MARKER);
            if (isRegex && !getMatch().endsWith(")")) {
                logger.error("Invalid regex expression.  Error: regex() not closed correctly.  Expression=\"{}\"",
                        getMatch());
            } else if (isRegex) {
                try {
                    String matchExpr = getMatch().substring(REGEX_MARKER.length(), getMatch().length() - 1);
                    return value.matches(matchExpr);
                } catch(PatternSyntaxException | IndexOutOfBoundsException ex) {
                    logger.error("Invalid regex expression.  Expression=\"{}\"  Error=\"{}\"",
                            getMatch(), ex.getLocalizedMessage());
                }
            } else {
                return value.equals(getMatch());
            }

            return false;
        }
    }

    private final List<MatchesPair> comparisons;

    public MatchesBuilder() {
        comparisons = new ArrayList<>();
    }

    public MatchesBuilder append(Object match, Object value) {
        comparisons.add(new MatchesPair(
                match == null ? null : match.toString(),
                value == null ? null : value.toString()));
        return this;
    }

    MatchesPair get(int index) {
        if (index < 0 || index >= comparisons.size()) {
            return null;
        }
        return comparisons.get(index);
    }

    public boolean isEquals() {
        boolean equals = true;
        for (MatchesPair nextPair: comparisons) {
            equals = nextPair.matches() && equals;
        }

        return equals;
    }
}

