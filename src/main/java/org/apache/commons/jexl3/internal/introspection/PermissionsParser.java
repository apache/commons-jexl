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

package org.apache.commons.jexl3.internal.introspection;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A crude parser to configure permissions akin to NoJexl annotations.
 * The syntax recognizes 2 types of permissions:
 * <ul>
 * <li>restricting access to packages, classes (and inner classes), methods and fields</li>
 * <li>allowing access to a wildcard restricted set of packages</li>
 * </ul>
 * <p>
 *  Example:
 * </p>
 * <pre>
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
 *     method(); # method is not allowed
 *     field; # field
 *   } # end class0
 *   +class1 {
 *     method(); // only allowed method of class1
 *   }
 * } # end package my.package
 * </pre>
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
        // nothing besides default member initialization
    }

    /**
     * Clears this parser internals.
     */
    private void clear() {
        src = null; size = 0; packages = null; wildcards = null;
    }

    /**
     * Parses permissions from a source.
     * @param wildcards the set of allowed packages
     * @param packages the map of restricted elements
     * @param srcs the sources
     * @return the permissions map
     */
    synchronized Permissions parse(final Set<String> wildcards, final Map<String, Permissions.NoJexlPackage> packages,
            final String... srcs) {
        try {
            if (srcs == null || srcs.length == 0) {
                return Permissions.UNRESTRICTED;
            }
            this.packages = packages;
            this.wildcards = wildcards;
            for (final String source : srcs) {
                this.src = source;
                this.size = source.length();
                readPackages();
            }
            return new Permissions(wildcards, packages);
        } finally {
            clear();
        }
    }

    /**
     * Parses permissions from a source.
     * @param srcs the sources
     * @return the permissions map
     */
    public Permissions parse(final String... srcs) {
        return parse(new LinkedHashSet<>(), new ConcurrentHashMap<>(), srcs);
    }

    /**
     * Reads a class permission.
     * @param njpackage the owning package
     * @param nojexl whether the restriction is explicitly denying (true) or allowing (false) members
     * @param outer the outer class (if any)
     * @param inner the inner class name (if any)
     * @param offset the initial parsing position in the source
     * @return the new parsing position
     */
    private int readClass(final Permissions.NoJexlPackage njpackage, final boolean nojexl, final String outer, final String inner, final int offset) {
        final StringBuilder temp = new StringBuilder();
        Permissions.NoJexlClass njclass = null;
        String njname = null;
        String identifier = inner;
        boolean deny = nojexl;
        int i = offset;
        int j = -1;
        boolean isMethod = false;
        while(i < size) {
            final char c = src.charAt(i);
            // if no parsing progress can be made, we are in error
            if (j >= i) {
                throw new IllegalStateException(unexpected(c, i));
            }
            j = i;
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
                i += 1;
                break;
            }
            // read an identifier, the class name
            if (identifier == null) {
                // negative or positive set ?
                if (c == '-') {
                    i += 1;
                } else if (c == '+') {
                    deny = false;
                    i += 1;
                }
                final int next = readIdentifier(temp, i);
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
                if (identifier == null || c != '{') {
                    throw new IllegalStateException(unexpected(c, i));
                }
                // if we have a class, it has a name
                njclass = deny ? new Permissions.NoJexlClass() : new Permissions.JexlClass();
                njname = outer != null ? outer + "$" + identifier : identifier;
                njpackage.addNoJexl(njname, njclass);
                identifier = null;
            } else if (identifier != null)  {
                // class member mode
                if (c == '{') {
                    // inner class
                    i = readClass(njpackage, deny, njname, identifier, i - 1);
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
        // empty class means allow or deny all
        if (njname != null && njclass.isEmpty()) {
            njpackage.addNoJexl(njname, njclass instanceof Permissions.JexlClass
                ? Permissions.JEXL_CLASS
                : Permissions.NOJEXL_CLASS);

        }
        return i;
    }

    /**
     * Reads a comment till end-of-line.
     * @param offset initial position
     * @return position after comment
     */
    private int readEol(final int offset) {
        int i = offset;
        while (i < size) {
            final char c = src.charAt(i);
            if (c == '\n') {
                break;
            }
            i += 1;
        }
        return i;
    }

    /**
     * Reads an identifier (optionally dot-separated).
     * @param id the builder to fill the identifier character with
     * @param offset the initial reading position
     * @return the position after the identifier
     */
    private int readIdentifier(final StringBuilder id, final int offset) {
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
    private int readIdentifier(final StringBuilder id, final int offset, final boolean dot, final boolean star) {
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
        final StringBuilder temp = new StringBuilder();
        Permissions.NoJexlPackage njpackage = null;
        int i = 0;
        int j = -1;
        String pname = null;
        while (i < size) {
            final char c = src.charAt(i);
            // if no parsing progress can be made, we are in error
            if (j >= i) {
                throw new IllegalStateException(unexpected(c, i));
            }
            j = i;
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
                final int next = readIdentifier(temp, i, true, true);
                if (i != next) {
                    pname = temp.toString();
                    temp.setLength(0);
                    i = next;
                    // consume it if it is a wildcard declaration
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
                    njpackage = packages.compute(pname,
                        (n, p) -> new Permissions.NoJexlPackage(p == null? null : p.nojexl)
                    );
                    i += 1;
                }
            } else if (c == '}') {
                // empty means whole package
                if (njpackage.isEmpty()) {
                    packages.put(pname, Permissions.NOJEXL_PACKAGE);
                }
                njpackage = null; // can restart anew
                pname = null;
                i += 1;
            } else {
                i = readClass(njpackage, true,null, null, i);
            }
        }
    }

    /**
     * Reads spaces.
     * @param offset initial position
     * @return position after spaces
     */
    private int readSpaces(final int offset) {
        int i = offset;
        while (i < size) {
            final char c = src.charAt(i);
            if (!Character.isWhitespace(c)) {
                break;
            }
            i += 1;
        }
        return offset;
    }

    /**
     * Compose a parsing error message.
     * @param c the offending character
     * @param i the offset position
     * @return the error message
     */
    private String unexpected(final char c, final int i) {
        return "unexpected '" + c + "'" + "@" + i;
    }
}
