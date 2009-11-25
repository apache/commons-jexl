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
package org.apache.commons.jexl2.parser;

/**
 * Common constant strings utilities.
 * <p>
 * This package methods read JEXL string literals and handle escaping through the
 * 'backslash' (ie: \) character. Escaping is used to neutralize string delimiters (the single
 * and double quotes) and read Unicode hexadecimal encoded characters.
 * </p>
 * <p>
 * The only escapable characters are the single and double quotes - ''' and '"' -,
 * a Unicode sequence starting with 'u' followed by 4 hexadecimals and
 * the backslash character - '\' - itself.
 * </p>
 * <p>
 * A sequence where '\' occurs before any non-escapable character or sequence has no effect, the
 * sequence output being the same as the input.
 * </p>
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
                if (c == 'u' && (index + 4) < end && readUnicodeChar(strb, str, index + 1) > 0) {
                    index += 4;
                }
                else {
                    // if c is not an escapable character, re-emmit the backslash before it
                    boolean notSeparator = sep == 0? c != '\'' && c != '"' : c != sep;
                    if (notSeparator && c != '\\' ) {
                        strb.append('\\');
                    }
                    strb.append(c);
                }
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

    /**
     * Reads a Unicode escape character.
     * @param strb the builder to write the character to
     * @param str the sequence
     * @param begin the begin offset in sequence (after the '\\u')
     * @return 0 if char could not be read, 4 otherwise
     */
    private static final int readUnicodeChar(StringBuilder strb, CharSequence str, int begin) {
        char xc = 0;
        int bits = 12;
        int value = 0;
        for(int offset = 0; offset < 4; ++offset) {
            char c = str.charAt(begin + offset);
            if (c >= '0' && c <= '9') {
                value = (c - '0');
            }
            else if (c >= 'a' && c <= 'h') {
               value = (c - 'a' + 10);
            }
            else if (c >= 'A' && c <= 'H') {
                value = (c - 'A' + 10);
            }
            else {
                return 0;
            }
            xc |= value << bits;
            bits -= 4;
        }
        strb.append(xc);
        return 4;
    }
}