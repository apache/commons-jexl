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

package org.apache.commons.jexl3;

import org.apache.commons.jexl3.internal.Script;

/**
 * Helper class to carry information such as a url/file name, line and column for
 * debugging information reporting.
 */
public class JexlInfo {

    /**
     * Describes errors more precisely.
     */
    public interface Detail {
        /**
         * Gets the end column on the line that triggered the error
         * @return the end column on the line that triggered the error
         */
        int end();

        /**
         * Gets the start column on the line that triggered the error
         * @return the start column on the line that triggered the error
         */
        int start();

        /**
         * Gets the code that triggered the error
         * @return the actual part of code that triggered the error
         */
        @Override
        String toString();
    }

    /**
     * Gets the info from a script.
     * @param script the script
     * @return the info
     */
    public static JexlInfo from(final JexlScript script) {
        return script instanceof Script? ((Script) script).getInfo() :  null;
    }

    /** Line number. */
    private final int line;

    /** Column number. */
    private final int column;

    /** Name. */
    private final String name;

    /**
     * Create an information structure for dynamic set/get/invoke/new.
     * <p>This gathers the class, method and line number of the first calling method
     * outside of o.a.c.jexl3.</p>
     */
    public JexlInfo() {
        final StackTraceElement[] stack = new Throwable().getStackTrace();
        String cname = getClass().getName();
        final String pkgname = getClass().getPackage().getName();
        StackTraceElement se = null;
        for (int s = 1; s < stack.length; ++s) {
            se = stack[s];
            final String className = se.getClassName();
            if (!className.equals(cname)) {
                // go deeper if called from jexl implementation classes
                if (!className.startsWith(pkgname + ".internal.") && !className.startsWith(pkgname + ".Jexl")
                    && !className.startsWith(pkgname + ".parser")) {
                    break;
                }
                cname = className;
            }
        }
        this.name = se != null ? se.getClassName() + "." + se.getMethodName() + ":" + se.getLineNumber() : "?";
        this.line = 1;
        this.column = 1;
    }

    /**
     * The copy constructor.
     *
     * @param copy the instance to copy
     */
    protected JexlInfo(final JexlInfo copy) {
        this(copy.getName(), copy.getLine(), copy.getColumn());
    }

    /**
     * Create info.
     *
     * @param source source name
     * @param l line number
     * @param c column number
     */
    public JexlInfo(final String source, final int l, final int c) {
        name = source;
        line = l <= 0? 1: l;
        column = c <= 0? 1 : c;
    }

    /**
     * Creates info reusing the name.
     *
     * @param l the line
     * @param c the column
     * @return a new info instance
     */
    public JexlInfo at(final int l, final int c) {
        return new JexlInfo(name, l, c);
    }

    /**
     * Gets this instance or a copy without any decorations
     * @return this instance or a copy without any decorations
     */
    public JexlInfo detach() {
        return this;
    }

    /**
     * Gets the column number.
     *
     * @return the column.
     */
    public final int getColumn() {
        return column;
    }

    /**
     * Gets error detail
     * @return the detailed information in case of an error
     */
    public Detail getDetail() {
        return null;
    }

    /**
     * Gets the line number.
     *
     * @return line number.
     */
    public final int getLine() {
        return line;
    }

    /**
     * Gets the file/script/url name.
     *
     * @return template name
     */
    public final String getName() {
        return name;
    }

    /**
     * Formats this info in the form 'name&#064;line:column'.
     *
     * @return the formatted info
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(name != null ? name : "");
        sb.append("@");
        sb.append(line);
        sb.append(":");
        sb.append(column);
        final JexlInfo.Detail dbg = getDetail();
        if (dbg!= null) {
            sb.append("![");
            sb.append(dbg.start());
            sb.append(",");
            sb.append(dbg.end());
            sb.append("]: '");
            sb.append(dbg.toString());
            sb.append("'");
        }
        return sb.toString();
    }
}

