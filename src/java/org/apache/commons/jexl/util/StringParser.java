/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl.util;

/**
 * Common constant strings utilities.
 */
public class StringParser {
    /**
     * Builds a string, handles escaping through '\' syntax.
     * @param str the string to build from
     * @param eatsep whether the separator, the first character, should be considered
     * @return the built string
     */
    public static String buildString(CharSequence str, boolean eatsep) {
        StringBuilder strb = new StringBuilder(str.length());
        char sep = eatsep ? str.charAt(0) : 0;
        int end = str.length() - (eatsep ? 1 : 0);
        int begin = (eatsep ? 1 : 0);
        read(strb, str, begin, end, sep);
        return strb.toString();
    }

    /**
     * Read the remainder of a string till a given separator,
     * handles escaping through '\' syntax.
     * @param strb the destination buffer to copy characters into
     * @param str the origin
     * @param index the offset into the origin
     * @param sep the separator, single or double quote, marking end of string
     * @return the offset in origin
     */
    public static int readString(StringBuilder strb, CharSequence str, int index, char sep) {
        return read(strb, str, index, str.length(), sep);
    }
    
    /**
     * Read the remainder of a string till a given separator,
     * handles escaping through '\' syntax.
     * @param strb the destination buffer to copy characters into
     * @param str the origin
     * @param begin the relative offset in str to begin reading
     * @param end the relative offset in str to end reading
     * @param sep the separator, single or double quote, marking end of string
     * @return the last character offset handled in origin
     */
    private static int read(StringBuilder strb, CharSequence str, int begin, int end, char sep) {
        boolean escape = false;
        int index = begin;
        for (; index < end; ++index) {
            char c = str.charAt(index);
            if (escape) {
                strb.append(c);
                if (c == '\\')
                    strb.append('\\');
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            strb.append(c);
            if (c == sep) {
                break;
            }
        }
        return index;
    }
}