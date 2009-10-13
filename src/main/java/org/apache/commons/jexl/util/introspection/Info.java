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
package org.apache.commons.jexl.util.introspection;

/**
 * Little class to carry in info such as a file or template name, line and column for
 * information error reporting from the uberspector implementations.
 * <p>
 * Originally taken from Velocity for self-sufficiency.
 * </p>
 * @since 1.0
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public class Info implements DebugInfo {
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
    public Info(String tn, int l, int c) {
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

    /**
     * Gets the file/script/url name.
     * @return template name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the template name.
     * @return template name
     * @deprecated Use {@link #getName()} instead
     */
    @Deprecated
    public String getTemplateName() {
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
