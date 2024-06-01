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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.jexl3.internal.ArrayBuilder;
import org.apache.commons.jexl3.internal.introspection.ClassMisc;
import org.junit.jupiter.api.Test;

/**
 * Ensure ArrayBuilder types its output by finding some common ancestor class or interface (besides Object.class)
 * from its entries when possible.
 */
public class ArrayTypeTest {
  public abstract static class Class0 implements Inter0 {
    private int value;
    public Class0(int v) {
      value = v;
    }
    @Override public String toString() {
      return getClass().getSimpleName() + "{" + value + "}";
    }
  }
  public static class ClassA extends Class0 implements InterA {
    public ClassA(int v) { super(v); }
  }
  public static class ClassB extends ClassA implements InterB {
    public ClassB(int v) { super(v); }
  }
  public static class ClassC extends ClassB implements InterC, InterX {
    public ClassC(int v) { super(v); }
  }
  public static class ClassD implements InterB, Inter0 {
    @Override public String toString() {
      return getClass().getSimpleName() + "{" + Integer.toHexString(hashCode()) + "}";
    }
  }
  public static class ClassX extends Class0 implements InterX {
    public ClassX(int v) { super(v); }
  }
  // A dependency tree with some complexity follows:
  public interface Inter0 {}
  public interface InterA {}
  public interface InterB {}
  public interface InterC {}
  public interface InterX extends InterB {}

  @Test
  public void testArrayTypes() {
    ArrayBuilder ab = new ArrayBuilder(1);
    // An engine for expressions with args
    JexlFeatures features = JexlFeatures.createScript().script(false);
    JexlEngine jexl = new JexlBuilder()
        .features(features)
        .create();
    // Super for ClassC
    Set<Class<?>> superSet = ClassMisc.getSuperClasses(ClassC.class);
    assertTrue(superSet.size() > 0);
    // verify the order
    List<Class<?>> ordered = Arrays.asList(
        ClassB.class, ClassA.class, Class0.class,
        InterC.class, InterX.class, InterB.class,
        InterA.class, Inter0.class);
    int i = 0;
    for(Class<?> clazz : superSet) {
      assertEquals(clazz, ordered.get(i++), "order " + i);
    }
    // intersect ClassC, ClassX -> Class0
    Class<?> inter = ClassMisc.getCommonSuperClass(ClassC.class, ClassX.class);
    assertEquals(Class0.class, inter);

    // intersect ClassC, ClassB -> ClassB
    inter = ClassMisc.getCommonSuperClass(ClassC.class, ClassB.class);
    assertEquals(ClassB.class, inter);

    // intersect ArrayList, ArrayDeque -> AbstractCollection
    Class<?> list = ClassMisc.getCommonSuperClass(ArrayList.class, ArrayDeque.class);
    assertEquals(list, AbstractCollection.class);

    Set<Class<?>> sset = ClassMisc.getSuperClasses(ArrayList.class, ArrayDeque.class);
    assertFalse(sset.isEmpty());
    // in java 21, a SequenceCollection interface is added to the sset
    List<Class<?>> expected = Arrays.asList(AbstractCollection.class, Collection.class, Iterable.class, Cloneable.class, Serializable.class);
    assertTrue(sset.containsAll(expected));

    Class<?> collection = ClassMisc.getCommonSuperClass(ArrayList.class, Collections.emptyList().getClass());
    assertEquals(AbstractList.class, collection);
    collection = ClassMisc.getSuperClasses(ArrayList.class, Collections.emptyList().getClass())
                          .stream().findFirst().orElse(Object.class);

    // apply on objects
    Object a = new ClassA(1);
    Object b = new ClassB(2);
    Object c = new ClassC(3);
    Object x = new ClassX(4);
    JexlScript script;
    Object result;

    script = jexl.createScript("[ a, b, c, d ]", "a", "b", "c", "d");
    // intersect a, b, c, c -> classA
    result = script.execute(null, a, b, c, c);
    assertTrue(result.getClass().isArray()
        && result.getClass().getComponentType().equals(ClassA.class));

    // intersect a, b, c, x -> class0
    result = script.execute(null, a, b, c, x);
    assertTrue(result.getClass().isArray()
        && result.getClass().getComponentType().equals(Class0.class));

    // intersect x, c, b, a -> class0
    result = script.execute(null, x, c, b, a);
    assertTrue(result.getClass().isArray()
        & result.getClass().getComponentType().equals(Class0.class));

    // intersect a, b, c, d -> inter0
    Object d = new ClassD();
    result = script.execute(null, a, b, c, d);
    assertTrue(result.getClass().isArray()
        && result.getClass().getComponentType().equals(Inter0.class));

    script = jexl.createScript("[ a, b, c, d, ... ]", "a", "b", "c", "d");
    // intersect a, b, c, c -> classA
    result = script.execute(null, a, b, c, c);
    assertTrue(result instanceof List);
  }
}
