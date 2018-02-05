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

import org.apache.commons.jexl3.JxltEngine;

/**
 * x.`expr`.
 */
public class ASTIdentifierAccessJxlt extends ASTIdentifierAccess {
    protected JxltEngine.Expression jxltExpr;

    ASTIdentifierAccessJxlt(int id) {
        super(id);
    }

    ASTIdentifierAccessJxlt(Parser p, int id) {
        super(p, id);
    }

    @Override
    public boolean isExpression() {
        return true;
    }

    public void setExpression(JxltEngine.Expression tp) {
        jxltExpr = tp;
    }

    public JxltEngine.Expression getExpression() {
        return jxltExpr;
    }

}
