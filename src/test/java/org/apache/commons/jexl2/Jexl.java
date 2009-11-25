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

import java.util.Map;

/**
 * @author Dion Gillard
 * @since 1.0
 * Command line interface for Jexl for use in testing
 */
public class Jexl {

    public static void main(String[] args) {
        final JexlEngine JEXL = new JexlEngine();
        // dummy context to get variables
        JexlContext context = new JexlContext() {
            @SuppressWarnings("unchecked")
            public Map getVars() { return System.getProperties(); }
            public void setVars(Map<String,Object> map) { }
        };
        try {
            for (int i = 0; i < args.length; i++) {
                Expression e = JEXL.createExpression(args[i]);
                System.out.println("evaluate(" + args[i] + ") = '" + e.evaluate(context) + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
