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

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import org.apache.commons.logging.Log;

/**
 * This basic function of this class is to return a Method
 * object for a particular class given the name of a method
 * and the parameters to the method in the form of an Object[]
 * <p>
 * The first time the Introspector sees a
 * class it creates a class method map for the
 * class in question. Basically the class method map
 * is a map where Method objects are keyed by a
 * concatenation of the method name and the names of
 * classes that make up the parameters.
 * </p>
 * <p>
 * For example, a method with the following signature:
 * <code>
 * public void method(String a, StringBuffer b)
 * </code>
 * would be mapped by the key:
 * <code>
 * { "method", {"java.lang.String", "java.lang.StringBuffer" } }
 * </code>
 * </p>
 * <p>
 * This mapping is performed for all the methods in a class
 * and stored for the lifetime of the instance. It is thus advised when possible
 * to share one instance of this class accross Uberspect instances.
 * </p>
 * @since 1.0
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:bob@werken.com">Bob McWhirter</a>
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @author <a href="mailto:paulo.gaspar@krankikom.de">Paulo Gaspar</a>
 * @version $Id$
 */
public final class Introspector extends IntrospectorBase {
    /**
     *  define a public string so that it can be looked for
     *  if interested.
     */
    public static final String CACHEDUMP_MSG =
        "Introspector : detected classloader change. Dumping cache.";

    /**
     *  Creates a new instance.
     *  @param logger a {@link Log}.
     */
    public Introspector(Log logger) {
        super(logger);
    }

    /**
     * Gets the method defined by <code>key</code> for the Class <code>c</code>.
     *
     * @param c Class in which the method search is taking place
     * @param key MethodKey of the method being searched for
     *
     * @return The desired Method object.
     * @throws IllegalArgumentException When the parameters passed in can not be used for introspection.
     * CSOFF: RedundantThrows
     */
    @Override
    public Method getMethod(Class<?> c, MethodKey key) throws IllegalArgumentException {
        //  just delegate to the base class
        try {
            return super.getMethod(c, key);
        } catch (MethodKey.AmbiguousException ae) {
            // whoops.  Ambiguous.  Make a nice log message and return null...
            if (rlog != null) {
                rlog.error("ambiguous method invocation: "
                           + c.getName() + "."
                           + key.debugString());
            }
        }
        return null;
    }

    // CSON: RedundantThrows
    /**
     * Gets the method defined by <code>key</code> for the Class <code>c</code>.
     *
     * @param key MethodKey of the method being searched for
     *
     * @return The desired Method object.
     * @throws IllegalArgumentException When the parameters passed in can not be used for introspection.
     * CSOFF: RedundantThrows
     */
    @Override
    public Constructor<?> getConstructor(MethodKey key) throws IllegalArgumentException {
        //  just delegate to the base class
        try {
            return super.getConstructor(key);
        } catch (MethodKey.AmbiguousException ae) {
            // whoops.  Ambiguous.  Make a nice log message and return null...
            if (rlog != null) {
                rlog.error("ambiguous constructor invocation: new "
                           + key.debugString());
            }
        }
        return null;
    } // CSON: RedundantThrows

    /**
     * Clears the classmap and classname
     * caches, and logs that we did so.
     */
    @Override
    protected void clearCache() {
        super.clearCache();
        rlog.info(CACHEDUMP_MSG);
    }
}
