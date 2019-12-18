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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages variables which can be referenced in a JEXL expression.
 *
 * <p>JEXL variable names in their simplest form are 'java-like' identifiers.
 * JEXL also considers 'ant' inspired variables expressions as valid.
 * For instance, the expression 'x.y.z' is an 'antish' variable and will be resolved as a whole by the context,
 * i.e. using the key "x.y.z". This proves to be useful to solve "fully qualified class names".</p>
 *
 * <p>The interpreter variable resolution algorithm will try the different sequences of identifiers till it finds
 * one that exists in the context; if "x" is an object known in the context (JexlContext.has("x") returns true),
 * "x.y" will <em>not</em> be looked up in the context but will most likely refer to "x.getY()".</p>
 *
 * <p>Note that JEXL may use '$jexl' and '$ujexl' variables for internal purpose; setting or getting those
 * variables may lead to unexpected results unless specified otherwise.</p>
 *
 * @since 1.0
 */
public interface JexlContext {

    /**
     * Gets the value of a variable.
     *
     * @param name the variable's name
     * @return the value
     */
    Object get(String name);

    /**
     * Sets the value of a variable.
     *
     * @param name the variable's name
     * @param value the variable's value
     */
    void set(String name, Object value);

    /**
     * Checks whether a variable is defined in this context.
     *
     * <p>A variable may be defined with a null value; this method checks whether the
     * value is null or if the variable is undefined.</p>
     *
     * @param name the variable's name
     * @return true if it exists, false otherwise
     */
    boolean has(String name);

    /**
     * A marker interface of the JexlContext that declares how to resolve a namespace from its name;
     * it is used by the interpreter during evaluation.
     *
     * <p>In JEXL, a namespace is an object that serves the purpose of encapsulating functions; for instance,
     * the "math" namespace would be the proper object to expose functions like "log(...)", "sinus(...)", etc.</p>
     *
     * In expressions like "ns:function(...)", the resolver is called with resolveNamespace("ns").
     *
     * <p>JEXL itself reserves 'jexl' and 'ujexl' as namespaces for internal purpose; resolving those may lead to
     * unexpected results.</p>
     *
     * @since 3.0
     */
    interface NamespaceResolver {

        /**
         * Resolves a namespace by its name.
         * @param name the name
         * @return the namespace object
         */
        Object resolveNamespace(String name);
    }

    /**
     * A marker interface of the JexlContext, NamespaceFunctor allows creating an instance
     * to delegate namespace methods calls to.
     *
     * <p>The functor is created once during the lifetime of a script evaluation.</p>
     */
    interface NamespaceFunctor {
        /**
         * Creates the functor object that will be used instead of the namespace.
         * @param context the context
         * @return the namespace functor instance
         */
        Object createFunctor(JexlContext context);
    }

    /**
     * A marker interface  of the JexlContext that indicates the interpreter to put this context
     * in the JexlEngine thread local context instance during evaluation.
     * This allows user functions or methods to access the context during a call.
     * Note that the usual caveats wrt using thread local apply (caching/leaking references, etc.); in particular,
     * keeping a reference to such a context is to be considered with great care and caution.
     * It should also be noted that sharing such a context between threads should implicate synchronizing variable
     * accessing the implementation class.
     *
     * @see JexlEngine#setThreadContext(JexlContext.ThreadLocal)
     * @see JexlEngine#getThreadContext()
     */
    interface ThreadLocal extends JexlContext {
        // no specific method
    }

    /**
     * A marker interface of the JexlContext that processes annotations.
     * It is used by the interpreter during evaluation to execute annotation evaluations.
     * <p>If the JexlContext is not an instance of an AnnotationProcessor, encountering an annotation will generate
     * an error or a warning depending on the engine strictness.
     * @since 3.1
     */
    interface AnnotationProcessor {
        /**
         * Processes an annotation.
         * <p>All annotations are processed through this method; the statement 'call' is to be performed within
         * the processAnnotation method. The implementation <em>must</em> perform the call explicitly.
         * <p>The arguments and the statement <em>must not</em> be referenced or cached for longer than the duration
         * of the processAnnotation call. 
         *
         * @param name the annotation name
         * @param args the arguments of the annotation, evaluated as arguments of this call
         * @param statement the statement that was annotated; the processor should invoke this statement 'call' method
         * @return the result of statement.call()
         * @throws Exception if annotation processing fails
         */
        Object processAnnotation(String name, Object[] args, Callable<Object> statement) throws Exception;
    }
    
    /**
     * A marker interface of the JexlContext that exposes runtime evaluation options.
     * @since 3.2
     */
    interface OptionsHandle {
        /**
         * Retrieves the current set of options though the context.
         * <p>
         * This method will be called once at beginning of evaluation and an interpreter private copy
         * of the context handled JexlOptions instance used for the duration of the execution;
         * the context handled JexlOptions instance being only used as the source of that copy,
         * it can safely alter its boolean flags during execution with no effect, avoiding any behavior ambiguity.
         * @return the engine options
         */
        JexlOptions getEngineOptions();
    }
     
    /**
     * A marker interface of the JexlContext that processes pragmas.
     * It is called by the engine before interpreter creation; as a marker of
     * JexlContext, it is expected to have access and interact with the context
     * instance.
     * @since 3.2
     */
    interface PragmaProcessor {
        /**
         * Process one pragma.
         * @param key the key
         * @param value the value
         */
        void processPragma(String key, Object value);
    }
    
    /**
     * A marker interface of the JexlContext sharing a cancelling flag.
     * <p>A script running in a thread can thus be notified through this reference
     * of its cancellation through the context. It uses the same interpreter logic
     * that reacts to cancellation and is an alternative to using callable() and/or
     * interrupting script interpreter threads.
     * @since 3.2
     */
    interface CancellationHandle {
        /**
         * @return a cancelable boolean used by the interpreter 
         */
        AtomicBoolean getCancellation();
    }
}
