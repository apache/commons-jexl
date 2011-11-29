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

/**
 * 
 * This interface declares how to resolve a namespace from its name; it is used by the interpreter during evalutation.
 * <p>
 * In JEXL, a namespace is an object that serves the purpose of encapsulating functions; for instance,
 * the "math" namespace would be the proper object to expose functions like "log(...)", "sinus(...)", etc.
 * </p>
 * In expressions like "ns:function(...)", the resolver is called with resolveNamespace("ns").
 * <p>
 * JEXL itself reserves 'jexl' and 'ujexl' as namespaces for internal purpose; resolving those may lead to unexpected
 * results.
 * </p>
 * @since 3.0
 */
public interface NamespaceResolver {
    /**
     * Resolves a namespace by its name.
     * @param name the name
     * @return the namespace object
     */
    Object resolveNamespace(String name);
}
