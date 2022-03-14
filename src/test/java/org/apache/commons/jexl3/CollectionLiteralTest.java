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

import org.apache.commons.jexl3.internal.ArrayBuilder;
import org.apache.commons.jexl3.internal.MapBuilder;
import org.apache.commons.jexl3.internal.SetBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Counting the number of times map,sets,array literals are created.
 * <p>
 * Originally intended to prove one could cache results of constant map literals when used to create
 * libraries (a map of lamba).
 * However, those literals must be newly instantiated since their result may be modified; there is
 * thus no point in trying to cache them. (Think harder and nod, dont try again.)
 * These pointless tests as a reminder of 'why' those behave the way they do.
 * </p>
 */
public class CollectionLiteralTest extends JexlTestCase {
    public CollectionLiteralTest() {
        super("CollectionLiteralTest");
    }


    public static class Arithmetic363 extends JexlArithmetic {
        final AtomicInteger maps = new AtomicInteger(0);
        final AtomicInteger sets = new AtomicInteger(0);
        final AtomicInteger arrays = new AtomicInteger(0);

        public Arithmetic363(boolean strict) {
            super(strict);
        }

        @Override public MapBuilder mapBuilder(int size) {
            return new CountingMapBuilder(maps, size);
        }
        @Override public SetBuilder setBuilder(int size) {
            return new CountingSetBuilder(sets, size);
        }
        @Override public ArrayBuilder arrayBuilder(int size) {
            return new CountingArrayBuilder(arrays, size);
        }
    }

    static class CountingSetBuilder extends SetBuilder {
        final AtomicInteger count;
        public CountingSetBuilder(AtomicInteger ai, int size) {
            super(size);
            count = ai;
        }
        @Override public Set<?> create() {
            Set<?> set = super.create();
            count.incrementAndGet();
            return set;
        }
    }

    static class CountingMapBuilder extends MapBuilder {
        final AtomicInteger count;
        public CountingMapBuilder(AtomicInteger ai, int size) {
            super(size);
            count = ai;
        }
        @Override public Map<Object, Object> create() {
            Map<Object, Object> map = super.create();
            count.incrementAndGet();
            return map;
        }
    }

    static class CountingArrayBuilder extends ArrayBuilder {
        final AtomicInteger count;

        public CountingArrayBuilder(AtomicInteger ai, int size) {
            super(size);
            count = ai;
        }

        @Override public Object create(boolean extended) {
            Object array = super.create(extended);
            count.incrementAndGet();
            return array;
        }
    }

    @Test
    public void testMapLBuilder() {
        Arithmetic363 jc = new Arithmetic363(true);
        final JexlEngine jexl = new JexlBuilder().cache(4).arithmetic(jc).create();
        JexlScript script;
        Object result;

        script = jexl.createScript("{ 'x':(x)->{ 1 + x; }, 'y' : (y)->{ y - 1; } }");
        Object previous = null;
        for(int i = 0; i < 4; ++i) {
            result = script.execute(null);
            Assert.assertNotNull(result);
            Assert.assertNotSame(previous, result);
            previous = result;
            Assert.assertEquals(1 + i, jc.maps.get());
        }
    }

    @Test
    public void testSetBuilder() {
        Arithmetic363 jc = new Arithmetic363(true);
        final JexlEngine jexl = new JexlBuilder().cache(4).arithmetic(jc).create();
        JexlScript script;
        Object result;

        script = jexl.createScript("{ (x)->{ 1 + x; }, (y)->{ y - 1; } }");
        Object previous = null;
        for(int i = 0; i < 4; ++i) {
            result = script.execute(null);
            Assert.assertNotNull(result);
            Assert.assertNotSame(previous, result);
            previous = result;
            Assert.assertEquals(1 + i, jc.sets.get());
        }
    }

    @Test
    public void testArrayBuilder() {
        Arithmetic363 jc = new Arithmetic363(true);
        final JexlEngine jexl = new JexlBuilder().cache(4).arithmetic(jc).create();
        JexlScript script;
        Object result;

        script = jexl.createScript("[ (x)->{ 1 + x; }, (y)->{ y - 1; } ]");
        Object previous = null;
        for(int i = 0; i < 4; ++i) {
            result = script.execute(null);
            Assert.assertNotNull(result);
            Assert.assertNotSame(previous, result);
            previous = result;
            Assert.assertEquals( 1 + i, jc.arrays.get());
        }
    }

}
