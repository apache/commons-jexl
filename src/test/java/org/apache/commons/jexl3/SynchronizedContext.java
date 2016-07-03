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
package org.apache.commons.jexl3;

import java.util.concurrent.Callable;

/**
 * Exposes a synchronized call to a script and synchronizes access to get/set methods.
 */
public class SynchronizedContext extends MapContext implements JexlContext.AnnotationProcessor {
    private final JexlContext context;

    public SynchronizedContext(JexlContext ctxt) {
        this.context = ctxt;
    }

    /**
     * Calls a script synchronized by an object monitor.
     * @param var the object used for sync
     * @param script the script
     * @return the script value
     */
    public Object call(Object var, JexlScript script) {
        String[] parms = script.getParameters();
        boolean varisarg = parms != null && parms.length == 1;
        if (var == null) {
            return varisarg ? script.execute(context, var) : script.execute(context);
        } else {
            synchronized (var) {
                return varisarg ? script.execute(context, var) : script.execute(context);
            }
        }
    }

    @Override
    public Object get(String name) {
        synchronized (this) {
            return super.get(name);
        }
    }

    @Override
    public void set(String name, Object value) {
        synchronized (this) {
            super.set(name, value);
        }
    }

    @Override
    public Object processAnnotation(String name, Object[] args, Callable<Object> statement) throws Exception {
        if ("synchronized".equals(name)) {
            final Object arg = args[0];
            synchronized(arg) {
                return statement.call();
            }
        }
        throw new IllegalArgumentException("unknown annotation " + name);
    }

}
