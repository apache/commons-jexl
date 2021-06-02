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
package org.apache.commons.jexl3.parser;

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
    /** Default constructor.  */
    public StringParser() {
    }

    /**
     * Builds a string, handles escaping through '\' syntax.
     * @param str the string to build from
     * @param eatsep whether the separator, the first character, should be considered
     * @return the built string
     */
    public static String buildString(final CharSequence str, final boolean eatsep) {
        return buildString(str, eatsep, true);
    }

    /**
     * Builds a template, does not escape characters.
     * @param str the string to build from
     * @param eatsep whether the separator, the first character, should be considered
     * @return the built string
     */
    public static String buildTemplate(final CharSequence str, final boolean eatsep) {
        return buildString(str, eatsep, false);
    }

    /**
     * Builds a string, handles escaping through '\' syntax.
     * @param str the string to build from
     * @param eatsep whether the separator, the first character, should be considered
     * @param esc whether escape characters are interpreted or escaped
     * @return the built string
     */
    private static String buildString(final CharSequence str, final boolean eatsep, final boolean esc) {
        final StringBuilder strb = new StringBuilder(str.length());
        final char sep = eatsep ? str.charAt(0) : 0;
        final int end = str.length() - (eatsep ? 1 : 0);
        final int begin = (eatsep ? 1 : 0);
        read(strb, str, begin, end, sep, esc);
        return strb.toString();
    }

    /**
     * Builds a regex pattern string, handles escaping '/' through '\/' syntax.
     * @param str the string to build from
     * @return the built string
     */
    public static String buildRegex(final CharSequence str) {
        return buildString(str.subSequence(1, str.length()), true);
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
    public static int readString(final StringBuilder strb, final CharSequence str, final int index, final char sep) {
        return read(strb, str, index, str.length(), sep, true);
    }
    /** The length of an escaped unicode sequence. */
    private static final int UCHAR_LEN = 4;

    /**
     * Read the remainder of a string till a given separator,
     * handles escaping through '\' syntax.
     * @param strb the destination buffer to copy characters into
     * @param str the origin
     * @param begin the relative offset in str to begin reading
     * @param end the relative offset in str to end reading
     * @param sep the separator, single or double quote, marking end of string
     * @param esc whether escape characters are interpreted or escaped
     * @return the last character offset handled in origin
     */
    private static int read(final StringBuilder strb, final CharSequence str, final int begin, final int end, final char sep, final boolean esc) {
        boolean escape = false;
        int index = begin;
        for (; index < end; ++index) {
            final char c = str.charAt(index);
            if (escape) {
                if (c == 'u' && (index + UCHAR_LEN) < end && readUnicodeChar(strb, str, index + 1) > 0) {
                    index += UCHAR_LEN;
                } else {
                    // if c is not an escapable character, re-emmit the backslash before it
                    final boolean notSeparator = sep == 0 ? c != '\'' && c != '"' : c != sep;
                    if (notSeparator && c != '\\') {
                        if (!esc) {
                            strb.append('\\').append(c);
                        } else {
                            switch (c) {
                                // http://es5.github.io/x7.html#x7.8.4
                                case 'b':
                                    strb.append('\b');
                                    break; // backspace \u0008
                                case 't':
                                    strb.append('\t');
                                    break; // horizontal tab \u0009
                                case 'n':
                                    strb.append('\n');
                                    break; // line feed \u000A
                                // We don't support vertical tab. If needed, the unicode (\u000B) should be used instead
                                case 'f':
                                    strb.append('\f');
                                    break; // form feed \u000C
                                case 'r':
                                    strb.append('\r');
                                    break; // carriage return \u000D
                                default:
                                    strb.append('\\').append(c);
                            }
                        }
                    } else {
                        strb.append(c);
                    }
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
    /** Initial shift value for composing a Unicode char from 4 nibbles (16 - 4). */
    private static final int SHIFT = 12;
    /** The base 10 offset used to convert hexa characters to decimal. */
    private static final int BASE10 = 10;

    /**
     * Reads a Unicode escape character.
     * @param strb the builder to write the character to
     * @param str the sequence
     * @param begin the begin offset in sequence (after the '\\u')
     * @return 0 if char could not be read, 4 otherwise
     */
    private static int readUnicodeChar(final StringBuilder strb, final CharSequence str, final int begin) {
        char xc = 0;
        int bits = SHIFT;
        int value = 0;
        for (int offset = 0; offset < UCHAR_LEN; ++offset) {
            final char c = str.charAt(begin + offset);
            if (c >= '0' && c <= '9') {
                value = (c - '0');
            } else if (c >= 'a' && c <= 'h') {
                value = (c - 'a' + BASE10);
            } else if (c >= 'A' && c <= 'H') {
                value = (c - 'A' + BASE10);
            } else {
                return 0;
            }
            xc |= value << bits;
            bits -= UCHAR_LEN;
        }
        strb.append(xc);
        return UCHAR_LEN;
    }
    /** The last 7bits ascii character. */
    private static final char LAST_ASCII = 127;
    /** The first printable 7bits ascii character. */
    private static final char FIRST_ASCII = 32;

    /**
     * Escapes a String representation, expand non-ASCII characters as Unicode escape sequence.
     * @param delim the delimiter character
     * @param str the string to escape
     * @return the escaped representation
     */
    public static String escapeString(final String str, final char delim) {
        if (str == null) {
            return null;
        }
        final int length = str.length();
        final StringBuilder strb = new StringBuilder(length + 2);
        strb.append(delim);
        for (int i = 0; i < length; ++i) {
            final char c = str.charAt(i);
            switch (c) {
                case 0:
                    continue;
                case '\b':
                    strb.append("\\b");
                    break;
                case '\t':
                    strb.append("\\t");
                    break;
                case '\n':
                    strb.append("\\n");
                    break;
                case '\f':
                    strb.append("\\f");
                    break;
                case '\r':
                    strb.append("\\r");
                    break;
                case '\"':
                    strb.append("\\\"");
                    break;
                case '\'':
                    strb.append("\\\'");
                    break;
                case '\\':
                    strb.append("\\\\");
                    break;
                default:
                    if (c >= FIRST_ASCII && c <= LAST_ASCII) {
                        strb.append(c);
                    } else {
                        // convert to Unicode escape sequence
                        strb.append('\\');
                        strb.append('u');
                        final String hex = Integer.toHexString(c);
                        for (int h = hex.length(); h < UCHAR_LEN; ++h) {
                            strb.append('0');
                        }
                        strb.append(hex);
                    }
            }
        }
        strb.append(delim);
        return strb.toString();
    }

    /**
     * Remove escape char ('\') from an identifier.
     * @param str the identifier escaped string, ie with a backslash before space, quote, double-quote and backslash
     * @return the string with no '\\' character
     */
    public static String unescapeIdentifier(final String str) {
        StringBuilder strb = null;
        if (str != null) {
            int n = 0;
            final int last = str.length();
            while (n < last) {
                final char c = str.charAt(n);
                if (c == '\\') {
                    if (strb == null) {
                        strb = new StringBuilder(last);
                        strb.append(str.substring(0, n));
                    }
                } else if (strb != null) {
                    strb.append(c);
                }
                n += 1;
            }
        }
        return strb == null ? str : strb.toString();
    }

    /**
     * Adds a escape char ('\') where needed in a string form of an ide
     * @param str the identifier un-escaped string
     * @return the string with added  backslash character before space, quote, double-quote and backslash
     */
    public static String escapeIdentifier(final String str) {
        StringBuilder strb = null;
        if (str != null) {
            int n = 0;
            final int last = str.length();
            while (n < last) {
                final char c = str.charAt(n);
                switch (c) {
                    case ' ':
                    case '\'':
                    case '"':
                    case '\\': {
                        if (strb == null) {
                            strb = new StringBuilder(last);
                            strb.append(str.substring(0, n));
                        }
                        strb.append('\\');
                        strb.append(c);
                        break;
                    }
                    default:
                        if (strb != null) {
                            strb.append(c);
                        }
                }
                n += 1;
            }
        }
        return strb == null ? str : strb.toString();
    }
}
