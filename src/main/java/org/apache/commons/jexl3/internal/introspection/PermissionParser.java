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

package org.apache.commons.jexl3.internal.introspection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses permissions akin to NoJexl annotations.
 * The syntax only recognizes packages, classes (and inner classes), methods and fields.
 *
 *  my.package {
 *   class0 {...
 *     class1 {...}
 *     class2 {
 *        ...
 *         class3 {}
 *     }
 *     # and eol comment
 *     class0(); // constructors
 *     method(); // method
 *     field; // field
 *   } // end class0
 * } // end package my.package
 *
 */
public class PermissionParser {
    /** The source. */
    private String src;
    /** The source size. */
    private int size;
    /** The @NoJexl execution-time map. */
    private Map<String, Permissions.NoJexlPackage> packages;

    /**
     * Basic ctor.
     */
    PermissionParser() {
    }

    void clear() {
        src = null; size = 0; packages = null;
    }

    /**
     * Parses permissions from a source.
     * @param src the source
     * @return the permissions map
     */
    Map<String, Permissions.NoJexlPackage> parse(String src) {
        packages = new ConcurrentHashMap<>();
        this.src = src;
        this.size = src.length();
        readPackages();
        Map<String, Permissions.NoJexlPackage> map = packages;
        clear();
        return map;
    }

    /**
     * Compose a parsing error message.
     * @param c the offending character
     * @param i the offset position
     * @return the error message
     */
    private String unexpected(char c, int i) {
        return "unexpected '" + c + "'" + "@" + i;
    }

    /**
     * Reads a comment till end-of-line.
     * @param offset initial position
     * @return position after comment
     */
    private int readEol(int offset) {
        int i = offset;
        while (i < size) {
            char c = src.charAt(i);
            if (c == '\n') {
                break;
            }
            i += 1;
        }
        return offset;
    }

    /**
     * Reads spaces.
     * @param offset
     * @param offset initial position
     * @return position after spaces
     */
    private int readSpaces(int offset) {
        int i = offset;
        while (i < size) {
            char c = src.charAt(i);
            if (!Character.isSpaceChar(c)) {
                break;
            }
            i += 1;
        }
        return offset;
    }

    /**
     * Reads an identifier (optionally dot-separated).
     * @param id the builder to fill the identifier character with
     * @param offset the initial reading position
     * @param dot whether dots (.) are allowed
     * @return the position after the identifier
     */
    private int readIdentifier(StringBuilder id, int offset, boolean dot) {
        int begin = -1;
        int i = offset;
        char c = 0;
        while (i < size) {
            c = src.charAt(i);
            // accumulate identifier characters
            if (Character.isJavaIdentifierStart(c) && begin < 0) {
                begin = i;
                id.append(c);
            } else if (Character.isJavaIdentifierPart(c) && begin >= 0) {
                id.append(c);
            } else if (dot && c == '.') {
                if (src.charAt(i - 1) == '.') {
                    throw new IllegalStateException(unexpected(c, i));
                }
                id.append(c);
                begin = -1;
            } else {
                break;
            }
            i += 1;
        }
        // cant end with a dot
        if (dot && c == '.') {
            throw new IllegalStateException(unexpected(c, i));
        }
        return i;
    }

    /**
     * Reads a package permission.
     */
    private void readPackages() {
        StringBuilder temp = new StringBuilder();
        Permissions.NoJexlPackage njpackage = null;
        int i = 0;
        int j = -1;
        String pname = null;
        for (; i < size; ) {
            char c = src.charAt(i);
            // if no parsing progress can be made, we are in error
            if (j < i) {
                j = i;
            } else {
                throw new IllegalStateException(unexpected(c, i));
            }
            // get rid of space
            if (Character.isSpaceChar(c)) {
                i = readSpaces(i + 1);
                continue;
            }
            // eol comment
            if (c == '#') {
                i = readEol(i + 1);
                continue;
            }
            // read the package qualified name
            if (pname == null) {
                int next = readIdentifier(temp, i, true);
                if (i != next) {
                    pname = temp.toString();
                    temp.setLength(0);
                    i = next;
                    continue;
                }
            }
            // package mode
            if (njpackage == null) {
                if (c == '{') {
                    njpackage = new Permissions.NoJexlPackage();
                    packages.put(pname, njpackage);
                    i += 1;
                }
            } else {
                if (c == '}') {
                    njpackage = null; // can restart anew
                    pname = null;
                    i += 1;
                } else {
                    i = readClass(njpackage, null, null, i);
                }
            }
        }
    }

    /**
     * Reads a class permission.
     * @param njpackage the owning package
     * @param outer the outer class (if any)
     * @param inner the inner class name (if any)
     * @param offset the initial parsing position in the source
     * @return the new parsing position
     */
    private int readClass(Permissions.NoJexlPackage njpackage, String outer, String inner, int offset) {
        StringBuilder temp = new StringBuilder();
        Permissions.NoJexlClass njclass = null;
        String njname = null;
        String identifier = inner;
        int i = offset;
        int j = -1;
        boolean isMethod = false;
        while(i < size) {
            char c = src.charAt(i);
            // if no parsing progress can be made, we are in error
            if (j < i) {
                j = i;
            } else {
                throw new IllegalStateException(unexpected(c, i));
            }
            // get rid of space
            if (Character.isSpaceChar(c)) {
                i = readSpaces(i + 1);
                continue;
            }
            // eol comment
            if (c == '#') {
                i = readEol(i + 1);
                continue;
            }
            // read an identifier, the class name
            if (identifier == null) {
                int next = readIdentifier(temp, i, false);
                if (i != next) {
                    identifier = temp.toString();
                    temp.setLength(0);
                    i = next;
                    continue;
                } else if (c == '}') {
                    i += 1;
                    break;
                }
            }
            // parse a class:
            if (njclass == null) {
                // we must have read the class ('identifier {'...)
                if (c == '{') {
                    // if we have a class, it has a name
                    njclass = new Permissions.NoJexlClass();
                    njname = outer != null ? outer + "$" + identifier : identifier;
                    njpackage.nojexl.put(njname, njclass);
                    identifier = null;
                    i += 1;
                } else {
                    throw new IllegalStateException(unexpected(c, i));
                }
            } else if (identifier != null)  {
                // class member mode
                if (c == '{') {
                    // inner class
                    i = readClass(njpackage, njname, identifier, i - 1);
                } else if (c == ';') {
                    // field or method?
                    if (isMethod) {
                        njclass.methodNames.add(identifier);
                        isMethod = false;
                    } else {
                        njclass.fieldNames.add(identifier);
                    }
                    isMethod = false;
                    identifier = null;
                    i += 1;
                } else if (c == '(' && !isMethod) {
                    // method; only one opening parenthesis allowed
                    isMethod = true;
                    i += 1;
                } else if (c == ')' && src.charAt(i - 1) == '(') {
                    i += 1;
                } else if (c == '}') {
                    i += 1;
                    break;
                } else {
                    throw new IllegalStateException(unexpected(c, i));
                }
            } else {
                i += 1;
            }
        }
        return i;
    }
}
