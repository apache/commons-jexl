/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.jexl3.JexlFeatures;

/**
 * Maintains the set of allowed features associated with a script/expression source.
 * <p>This is meant for caching scripts using their 'source' as key but still distinguishing
 * scripts with different features and prevent false sharing.
 */
public final class Source implements Comparable<Source> {

    /** The hash code, pre-computed for fast op. */
    private final int hashCode;

    /** The set of features. */
    private final JexlFeatures features;

    /** The local symbols, if any. */
    private final Map<String, Integer> symbols;

    /** The actual source script/expression. */
    private final String str;

    /**
     * Default constructor.
     *
     * @param theFeatures the features
     * @param theSymbols the map of variable name to symbol offset in evaluation frame
     * @param theStr the script source
     */
    Source(final JexlFeatures theFeatures, final Map<String, Integer> theSymbols,  final String theStr) {
        this.features = theFeatures;
        this.symbols = theSymbols == null ? Collections.emptyMap() : theSymbols;
        this.str = theStr;
        this.hashCode = Objects.hash(features, symbols, str);
    }

    @Override
    public int compareTo(final Source s) {
        int cmp = str.compareTo(s.str);
        if (cmp == 0) {
            cmp = Integer.compare(features.hashCode(), s.features.hashCode());
            if (cmp == 0) {
                cmp = Integer.compare(symbols.hashCode(), s.symbols.hashCode());
                if (cmp == 0) {
                    if (Objects.equals(features, s.features)) {
                        if (Objects.equals(symbols, s.symbols)) {
                            return 0;
                        }
                       return -1; // Same features, different symbols
                    }
                    return +1; // Different features
                }
            }
        }
        return cmp;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Source other = (Source) obj;
        if (!Objects.equals(this.features, other.features)) {
            return false;
        }
        if (!Objects.equals(this.symbols, other.symbols)) {
            return false;
        }
        if (!Objects.equals(this.str, other.str)) {
            return false;
        }
        return true;
    }

    /**
     * @return the features associated with the source
     */
    public JexlFeatures getFeatures() {
        return features;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * @return the length of the script source
     */
    int length() {
        return str.length();
    }

    @Override
    public String toString() {
        return str;
    }

}
