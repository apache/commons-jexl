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

import org.apache.commons.jexl2.JexlInfo;

/**
 * Base class for parser nodes - holds an 'image' of the token for later use.
 *
 * @since 2.0
 */
public abstract class JexlNode extends SimpleNode implements JexlInfo {
    /** token value. */
    public String image;

    public JexlNode(int id) {
        super(id);
    }

    public JexlNode(Parser p, int id) {
        super(p, id);
    }

    public JexlInfo getInfo() {
        JexlNode node = this;
        while (node != null) {
            if (node.value instanceof JexlInfo) {
                return (JexlInfo) node.value;
            }
            node = node.jjtGetParent();
        }
        return null;
    }
    
    /** {@inheritDoc} */
    public String debugString() {
        JexlInfo info = getInfo();
        return info != null? info.debugString() : "";
    }
}
