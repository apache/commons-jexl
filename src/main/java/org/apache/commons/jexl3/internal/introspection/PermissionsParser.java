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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A crude parser to configure permissions akin to NoJexl annotations.
 * The syntax recognizes 2 types of permissions:
 * - restricting access to packages, classes (and inner classes), methods and fields
 * - allowing access to a wildcard restricted set of packages
 *  Example:
 *  my.allowed.packages.*
 *  another.allowed.package.*
 *  # nojexl like restrictions
 *  my.package {
 *   class0 {...
 *     class1 {...}
 *     class2 {
 *        ...
 *         class3 {}
 *     }
 *     # and eol comment
 *     class0(); # constructors
 *     method(); # method
 *     field; # field
 *   } # end class0
 * } # end package my.package
 *
 */
public class PermissionsParser {
    /** The source. */
    private String src;
    /** The source size. */
    private int size;
    /** The @NoJexl execution-time map. */
    private Map<String, Permissions.NoJexlPackage> packages;
    /** The set of wildcard imports. */
    private Set<String> wildcards;

    /**
     * Basic ctor.
     */
    public PermissionsParser() {
    }

    /**
     * Clears this parser internals.
     */
    public void clear() {
        src = null; size = 0; packages = null; wildcards = null;
    }

    /**
     * Parses permissions from a source.
     * @param srcs the sources
     * @return the permissions map
     */
    public Permissions parse(String... srcs) {
        if (srcs == null || srcs.length == 0) {
            return Permissions.UNRESTRICTED;
        }
        packages = new ConcurrentHashMap<>();
        wildcards = new LinkedHashSet<>();
        for(String src : srcs) {
            this.src = src;
            this.size = src.length();
            readPackages();
        }
        Permissions permissions = new Permissions(wildcards, packages);
        clear();
        return permissions;
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
        return i;
    }

    /**
     * Reads spaces.
     * @param offset initial position
     * @return position after spaces
     */
    private int readSpaces(int offset) {
        int i = offset;
        while (i < size) {
            char c = src.charAt(i);
            if (!Character.isWhitespace(c)) {
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
     * @return the position after the identifier
     */
    private int readIdentifier(StringBuilder id, int offset) {
        return readIdentifier(id, offset, false, false);
    }

    /**
     * Reads an identifier (optionally dot-separated).
     * @param id the builder to fill the identifier character with
     * @param offset the initial reading position
     * @param dot whether dots (.) are allowed
     * @param star whether stars (*) are allowed
     * @return the position after the identifier
     */
    private int readIdentifier(StringBuilder id, int offset, boolean dot, boolean star) {
        int begin = -1;
        boolean starf = star;
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
                id.append('.');
                begin = -1;
            } else if (starf && c == '*') {
                id.append('*');
                starf = false; // only one star
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
        while (i < size) {
            char c = src.charAt(i);
            // if no parsing progress can be made, we are in error
            if (j < i) {
                j = i;
            } else {
                throw new IllegalStateException(unexpected(c, i));
            }
            // get rid of space
            if (Character.isWhitespace(c)) {
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
                int next = readIdentifier(temp, i, true, true);
                if (i != next) {
                    pname = temp.toString();
                    temp.setLength(0);
                    i = next;
                    // consume it if it is a wildcard decl
                    if (pname.endsWith(".*")) {
                        wildcards.add(pname);
                        pname = null;
                    }
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
                    // empty means whole package
                    if (njpackage.isEmpty()) {
                        packages.put(pname, Permissions.NOJEXL_PACKAGE);
                    }
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
            if (Character.isWhitespace(c)) {
                i = readSpaces(i + 1);
                continue;
            }
            // eol comment
            if (c == '#') {
                i = readEol(i + 1);
                continue;
            }
            // end of class ?
            if (njclass != null && c == '}') {
                // restrict the whole class
                if (njclass.isEmpty()) {
                    njpackage.addNoJexl(njname, Permissions.NOJEXL_CLASS);
                }
                i += 1;
                break;
            }
            // read an identifier, the class name
            if (identifier == null) {
                int next = readIdentifier(temp, i);
                if (i != next) {
                    identifier = temp.toString();
                    temp.setLength(0);
                    i = next;
                    continue;
                }
            }
            // parse a class:
            if (njclass == null) {
                // we must have read the class ('identifier {'...)
                if (identifier != null && c == '{') {
                    // if we have a class, it has a name
                    njclass = new Permissions.NoJexlClass();
                    njname = outer != null ? outer + "$" + identifier : identifier;
                    njpackage.addNoJexl(njname, njclass);
                    identifier = null;
                } else {
                    throw new IllegalStateException(unexpected(c, i));
                }
            } else if (identifier != null)  {
                // class member mode
                if (c == '{') {
                    // inner class
                    i = readClass(njpackage, njname, identifier, i - 1);
                    identifier = null;
                    continue;
                }
                if (c == ';') {
                    // field or method?
                    if (isMethod) {
                        njclass.methodNames.add(identifier);
                        isMethod = false;
                    } else {
                        njclass.fieldNames.add(identifier);
                    }
                    identifier = null;
                } else if (c == '(' && !isMethod) {
                    // method; only one opening parenthesis allowed
                    isMethod = true;
                } else if (c != ')' || src.charAt(i - 1) != '(') {
                    // closing parenthesis following opening one was expected
                    throw new IllegalStateException(unexpected(c, i));
                }
            }
            i += 1;
        }
        return i;
    }
}
