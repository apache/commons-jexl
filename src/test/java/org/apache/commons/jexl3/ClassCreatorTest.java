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

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic check on automated class creation
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ClassCreatorTest extends JexlTestCase {
    static final Log logger = LogFactory.getLog(JexlTestCase.class);
    static final int LOOPS = 8;
    private File base = null;
    private JexlEngine jexl = null;

    public ClassCreatorTest() {
        super("ClassCreatorTest");
    }

    @Before
    @Override
    public void setUp() throws Exception {
        base = new File(System.getProperty("java.io.tmpdir"), "jexl" + System.currentTimeMillis());
        jexl = JEXL;

    }

    @After
    @Override
    public void tearDown() throws Exception {
        deleteDirectory(base);
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    // A space hog class
    static final int MEGA = 1024 * 1024;

    public class BigObject {
        @SuppressWarnings("unused")
        private final byte[] space = new byte[MEGA];
        private final int id;

        public BigObject(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    // A weak reference on class
    static final class ClassReference extends WeakReference<Class<?>> {
        ClassReference(Class<?> clazz, ReferenceQueue<Object> queue) {
            super(clazz, queue);
        }
    }

    // A soft reference on instance
    static final class InstanceReference extends SoftReference<Object> {
        InstanceReference(Object obj, ReferenceQueue<Object> queue) {
            super(obj, queue);
        }
    }

    @Test
    public void testOne() throws Exception {
        // abort test if class creator can not run
        if (!ClassCreator.canRun) {
            return;
        }
        ClassCreator cctor = new ClassCreator(jexl, base);
        cctor.setSeed(1);
        Class<?> foo1 = cctor.createClass();
        Assert.assertEquals("foo1", foo1.getSimpleName());
        cctor.clear();
    }

    @Test
    public void testMany() throws Exception {
        // abort test if class creator can not run
        if (!ClassCreator.canRun) {
            return;
        }
        int pass = 0;
        int gced = -1;
        ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        List<Reference<?>> stuff = new ArrayList<Reference<?>>();
        // keeping a reference on methods prevent classes from being GCed
//        List<Object> mm = new ArrayList<Object>();
        JexlExpression expr = jexl.createExpression("foo.value");
        JexlExpression newx = jexl.createExpression("foo = new(clazz)");
        JexlEvalContext context = new JexlEvalContext();
        context.setStrict(false);
        context.setSilent(true);

        ClassCreator cctor = new ClassCreator(jexl, base);
        for (int i = 0; i < LOOPS && gced < 0; ++i) {
            cctor.setSeed(i);
            Class<?> clazz;
            if (pass == 0) {
                clazz = cctor.createClass();
            } else {
                clazz = cctor.getClassInstance();
                if (clazz == null) {
                    Assert.assertEquals(i, gced);
                    break;
                }
            }
            // this code verifies the assumption that holding a strong reference to a method prevents
            // its owning class from being GCed
//          Method m = clazz.getDeclaredMethod("getValue", new Class<?>[0]);
//          mm.add(m);
            // we should not be able to create foox since it is unknown to the JEXL classloader
            context.set("clazz", cctor.getClassName());
            context.set("foo", null);
            Object z = newx.evaluate(context);
            Assert.assertNull(z);
            // check with the class itself
            context.set("clazz", clazz);
            z = newx.evaluate(context);
            Assert.assertNotNull(clazz + ": class " + i + " could not be instantiated on pass " + pass, z);
            Assert.assertEquals(new Integer(i), expr.evaluate(context));
            // with the proper class loader, attempt to create an instance from the class name
            jexl.setClassLoader(cctor.getClassLoader());
            z = newx.evaluate(context);
            Assert.assertTrue(z.getClass().equals(clazz));
            Assert.assertEquals(new Integer(i), expr.evaluate(context));
            cctor.clear();
            jexl.setClassLoader(null);

            // on pass 0, attempt to force GC to run and collect generated classes
            if (pass == 0) {
                // add a weak reference on the class
                stuff.add(new ClassReference(clazz, queue));
                // add a soft reference on an instance
                stuff.add(new InstanceReference(clazz.newInstance(), queue));

                // attempt to force GC:
                // while we still have a MB free, create & store big objects
                for (int b = 0; b < 1024 && Runtime.getRuntime().freeMemory() > MEGA; ++b) {
                    BigObject big = new BigObject(b);
                    stuff.add(new InstanceReference(big, queue));
                }
                // hint it...
                System.gc();
                // let's see if some weak refs got collected
                boolean qr = false;
                while (queue.poll() != null) {
                    Reference<?> ref = queue.remove(1);
                    if (ref instanceof ClassReference) {
                        gced = i;
                        qr = true;
                    }
                }
                if (qr) {
                    //logger.warn("may have GCed class around " + i);
                    pass = 1;
                    i = 0;
                }
            }
        }

        if (gced < 0) {
            logger.warn("unable to force GC");
            //Assert.assertTrue(gced > 0);
        }
    }

    public static class TwoCtors {
        int value;

        public TwoCtors(int v) {
            this.value = v;
        }

        public TwoCtors(Number x) {
            this.value = -x.intValue();
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void testBasicCtor() throws Exception {
        JexlScript s = jexl.createScript("(c, v)->{ var ct2 = new(c, v); ct2.value; }");
        Object r = s.execute(null, TwoCtors.class, 10);
        Assert.assertEquals(10, r);
        r = s.execute(null, TwoCtors.class, 5 + 5);
        Assert.assertEquals(10, r);
        r = s.execute(null, TwoCtors.class, 10d);
        Assert.assertEquals(-10, r);
        r = s.execute(null, TwoCtors.class, 100f);
        Assert.assertEquals(-100, r);
    }
}
