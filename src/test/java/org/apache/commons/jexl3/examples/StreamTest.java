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
package org.apache.commons.jexl3.examples;

import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlPermissions.ClassPermissions;
import org.junit.jupiter.api.Test;

/**
 * A test around scripting streams.
 */
public class StreamTest {

    /**
     * A MapContext that can operate on streams and collections.
     */
    public static class CollectionContext extends MapContext {
        /**
         * This allows using a JEXL lambda as a filter.
         * @param collection the collection
         * @param filter the lambda to use as filter
         * @return the filtered result as a list
         */
        public List<?> filter(final Collection<?> collection, final JexlScript filter) {
            return collection.stream()
                .filter(x -> x != null && TRUE.equals(filter.execute(this, x)))
                .collect(Collectors.toList());
        }

        /**
         * This allows using a JEXL lambda as a mapper.
         * @param collection the collection
         * @param mapper the lambda to use as mapper
         * @return the mapped result as a list
         */
        public List<?> map(final Collection<?> collection, final JexlScript mapper) {
            return collection.stream()
                .map(x -> mapper.execute(this, x))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
    }

    /**
     * A MapContext that can operate on streams and collections.
     */
    public static class StreamContext extends MapContext {
        /**
         * This allows using a JEXL lambda as a filter.
         * @param stream the stream
         * @param filter the lambda to use as filter
         * @return the filtered stream
         */
        public Stream<?> filter(final Stream<?> stream, final JexlScript filter) {
            return stream.filter(x -> x != null && TRUE.equals(filter.execute(this, x)));
        }

        /**
         * This allows using a JEXL lambda as a mapper.
         * @param stream the stream
         * @param mapper the lambda to use as mapper
         * @return the mapped stream
         */
        public Stream<?> map(final Stream<?> stream, final JexlScript mapper) {
            return stream.map(x -> mapper.execute(this, x));
        }
    }

    /** Our engine instance. */
    private final JexlEngine jexl;

    public StreamTest() {
        // Restricting features; no loops, no side effects
        final JexlFeatures features = new JexlFeatures()
                .loops(false)
                .sideEffectGlobal(false)
                .sideEffect(false);
        // Restricted permissions to a safe set but with URI allowed
        final JexlPermissions permissions = new ClassPermissions(java.net.URI.class);
        // Create the engine
        jexl = new JexlBuilder()
            .features(features)
            .permissions(permissions)
            .namespaces(Collections.singletonMap("URI", java.net.URI.class))
            .create();
    }

    @Test
    void testURICollection() {
        // A collection map/filter aware context
        final JexlContext sctxt = new CollectionContext();
        // Some uris
        final List<URI> uris = Arrays.asList(
            URI.create("http://user@www.apache.org:8000?qry=true"),
            URI.create("https://commons.apache.org/releases/prepare.html"),
            URI.create("mailto:henrib@apache.org")
        );

        // filter, all results schemes start with 'http'
        final JexlScript filter = jexl.createScript(
            "list.filter(uri -> uri.scheme =^ 'http')",
            "list");
        final Object filtered = filter.execute(sctxt, uris);
        assertInstanceOf(List.class, filtered);
        List<URI> result = (List<URI>) filtered;
        assertEquals(2, result.size());
        for(final URI uri : result) {
            assertTrue(uri.getScheme().startsWith("http"));
        }

        // map, all results scheme now 'https'
        final JexlScript mapper = jexl.createScript(
            "list.map(uri -> uri.scheme =^ 'http'? URI:create(`https://${uri.host}`) : null)",
            "list");
        final Object transformed = mapper.execute(sctxt, uris);
        assertInstanceOf(List.class, transformed);
        result = (List<URI>) transformed;
        assertEquals(2, result.size());
        for(final URI uri : result) {
          assertEquals("https", uri.getScheme());
        }
    }

    @Test
    void testURIStream() {
        // let's assume a collection of uris need to be processed and transformed to be simplified ;
        // we want only http/https ones, only the host part and using a https scheme
        final List<URI> uris = Arrays.asList(
                URI.create("http://user@www.apache.org:8000?qry=true"),
                URI.create("https://commons.apache.org/releases/prepare.html"),
                URI.create("mailto:henrib@apache.org")
        );
        // Create the test control, the expected result of our script evaluation
        final List<?> control =  uris.stream()
                .map(uri -> uri.getScheme().startsWith("http")? "https://" + uri.getHost() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertEquals(2, control.size());

        // Create scripts:
        // uri is the name of the variable used as parameter; the beans are exposed as properties
        // note that it is also used in the backquoted string
        final JexlScript mapper = jexl.createScript("uri.scheme =^ 'http'? `https://${uri.host}` : null", "uri");
        // using the bang-bang / !! - JScript like -  is the way to coerce to boolean in the filter
        final JexlScript transform = jexl.createScript(
                "list.stream().map(mapper).filter(x -> !!x).collect(Collectors.toList())", "list");

        // Execute scripts:
        final JexlContext sctxt = new StreamContext();
        // expose the static methods of Collectors; java.util.* is allowed by permissions
        sctxt.set("Collectors", Collectors.class);
        // expose the mapper script as a global variable in the context
        sctxt.set("mapper", mapper);

        final Object transformed = transform.execute(sctxt, uris);
        assertInstanceOf(List.class, transformed);
        assertEquals(control, transformed);
    }
}
