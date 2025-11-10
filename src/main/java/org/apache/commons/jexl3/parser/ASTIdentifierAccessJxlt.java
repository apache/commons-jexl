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

package org.apache.commons.jexl3.parser;

import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.jexl3.internal.Scope;
import org.apache.commons.jexl3.internal.TemplateEngine;

/**
 * x.`expr`.
 */
public class ASTIdentifierAccessJxlt extends ASTIdentifierAccess implements JexlNode.JxltHandle {
    protected transient JxltEngine.Expression jxltExpression;

    ASTIdentifierAccessJxlt(final int id) {
        super(id);
    }

    @Override
    public String getExpressionSource() {
        return getName();
    }

    @Override
    public JxltEngine.Expression getExpression() {
        return jxltExpression;
    }

    @Override
    public boolean isExpression() {
        return true;
    }

    @Override
    public void setExpression(final JxltEngine.Expression tp) {
        jxltExpression = tp;
    }

    public void setIdentifier(final String src, final Scope scope) {
        super.setIdentifier(src);
        this.jxltExpression = JexlParser.parseInterpolation(jexlInfo(), src, scope);
    }
}
