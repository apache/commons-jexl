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
package org.apache.commons.jexl3.internal.introspection;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Miscellaneous introspection methods.
 * <p>The main algorithm is computing the actual ordered complete set of classes and interfaces that a given class
 * extends or implements. This set order is based on the super-class then (recursive interface) declaration order,
 * attempting to reflect the (hopefully intended) abstraction order (from strong to weak).</p>
 */
public class ClassMisc {
  /**
   * Collect super classes and interfaces in super-order.
   * <p>This orders from stronger to weaker abstraction in the sense that
   * Integer is a stronger abstraction than Number.</p>
   *
   * @param superSet  the set of super classes to collect into
   * @param baseClass the root class.
   */
  private static void addSuperClasses(final Set<Class<?>> superSet, final Class<?> baseClass) {
    for (Class<?> clazz = baseClass.getSuperclass(); clazz != null && !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
      if (Modifier.isPublic(clazz.getModifiers())) {
        superSet.add(clazz);
      }
    }
    // recursive visit interfaces in super order
    for (Class<?> clazz = baseClass; clazz != null && !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
      addSuperInterfaces(superSet, clazz);
    }
  }
  /**
   * Recursively add super-interfaces in super-order.
   * <p>On the premise that a class also tends to enumerate interface in the order of weaker abstraction and
   * that interfaces follow the same convention (strong implements weak).</p>
   *
   * @param superSet the set of super classes to fill
   * @param clazz    the root class.
   */
  private static void addSuperInterfaces(final Set<Class<?>> superSet, final Class<?> clazz) {
    for (final Class<?> inter : clazz.getInterfaces()) {
      superSet.add(inter);
      addSuperInterfaces(superSet, inter);
    }
  }

  /**
   * Gets the closest common super-class of two classes.
   * <p>When building an array, this helps strong-typing the result.</p>
   *
   * @param baseClass the class to serve as base
   * @param other     the other class
   * @return Object.class if nothing in common, the closest common class or interface otherwise
   */
  public static Class<?> getCommonSuperClass(final Class<?> baseClass, final Class<?> other) {
    if (baseClass == null || other == null) {
      return null;
    }
    if (baseClass != Object.class && other != Object.class) {
      final Set<Class<?>> superSet = new LinkedHashSet<>();
      addSuperClasses(superSet, baseClass);
      for (final Class<?> superClass : superSet) {
        if (superClass.isAssignableFrom(other)) {
          return superClass;
        }
      }
    }
    return Object.class;
  }

  /**
   * Build the set of super classes and interfaces common to a collection of classes.
   * <p>The returned set is ordered and puts classes in order of super-class appearance then
   * interfaces of each super-class.</p>
   *
   * @param baseClass    the class to serve as base
   * @param otherClasses the (optional) other classes
   * @return an empty set if nothing in common, the set of common classes and interfaces that
   *  does not contain the baseClass nor Object class
   */
  public static Set<Class<?>> getSuperClasses(final Class<?> baseClass, final Class<?>... otherClasses) {
    if (baseClass == null) {
      return Collections.emptySet();
    }
    final Set<Class<?>> superSet = new LinkedHashSet<>();
    addSuperClasses(superSet, baseClass);
    // intersect otherClasses
    if (otherClasses.length > 0) {
      for (final Class<?> other : otherClasses) {
        // remove classes from $superSet that $other is not assignable to
        final Iterator<Class<?>> superClass = superSet.iterator();
        while (superClass.hasNext()) {
          if (!superClass.next().isAssignableFrom(other)) {
            superClass.remove();
          }
        }
        if (superSet.isEmpty()) {
          return Collections.emptySet();
        }
      }
    }
    return superSet;
  }

  /**
   * Lets not instantiate it.
   */
  private ClassMisc() {}
}
