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
package org.example;

import org.apache.commons.jexl3.annotations.NoJexl;

public class Pair {
  public Object car;
  public Object cdr;

  @NoJexl
  public Pair(Object car, Object cdr) {
    this.car = car;
    this.cdr = cdr;
  }

  public static Pair cons(Number car, Number cdr) {
    return new Pair(car, cdr);
  }

  public static Pair cons(Object car, Object cdr) {
    return new Pair(car, cdr);
  }
}
