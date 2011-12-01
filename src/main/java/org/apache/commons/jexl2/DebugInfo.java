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
package org.apache.commons.jexl2;


/**
 * Helper class to carry in info such as a url/file name, line and column for
 * debugging information reporting.
 */
public class DebugInfo implements JexlInfo {
    /** line number. */
    private final int line;
    /** column number. */
    private final int column;
    /** name. */
    private final String name;
    /** 
     * Create info.
     * @param tn template name
     * @param l line number
     * @param c column
     */
    public DebugInfo(String tn, int l, int c) {
        name = tn;
        line = l;
        column = c;
    }

    /**
     * Formats this info in the form 'name&#064;line:column'.
     * @return the formatted info
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name != null? name : "");
        if (line > 0) {
            sb.append("@");
            sb.append(line);
            if (column > 0) {
                sb.append(":");
                sb.append(column);
            }
        }
        return sb.toString();
    }
    
    /** {@inheritDoc} */
    public String debugString() {
        return toString();
    }
    
    /** {@inheritDoc} 
     * @since 2.1 
     */
    public DebugInfo debugInfo() {
        return this;
    }

    /**
     * Gets the file/script/url name.
     * @return template name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the line number.
     * @return line number.
     */
    public int getLine() {
        return line;
    }

    /**
     * Gets the column number.
     * @return the column.
     */
    public int getColumn() {
        return column;
    }
}
