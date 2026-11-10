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
package org.apache.commons.jexl3.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Interpreter hot-path benchmarks: property access, method calls, loops, lexical blocks,
 * assignments and antish variables (resolved at runtime or declared at parse time).
 * <p>Run with: {@code mvn clean test -Pbenchmark -Dbenchmark=InterpreterBenchmark}</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class InterpreterBenchmark {

    private JexlContext context;
    private JexlScript propertyAccess;
    private JexlScript methodCall;
    private JexlScript forEachLoop;
    private JexlScript forLoop;
    private JexlScript whileLoop;
    private JexlScript lexicalBlocks;
    private JexlScript assignments;
    private JexlScript antish;
    private JexlScript antishDeclared;

    @Setup
    public void setup() {
        final JexlEngine jexl = new JexlBuilder()
                .features(JexlFeatures.createDefault())
                .cache(64)
                .create();
        final Map<String, Object> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", new HashMap<>(map));
        final List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; ++i) {
            list.add(i);
        }
        context = new MapContext();
        context.set("map", map);
        context.set("list", list);
        context.set("x.y.z", 1);
        propertyAccess = jexl.createScript("var s = 0; for (var i : list) { s += map.a + map.b.a; } s");
        methodCall = jexl.createScript("var s = 0; for (var i : list) { s += list.size() + list.get(i); } s");
        forEachLoop = jexl.createScript("var s = 0; for (var i : list) { if (i % 2 == 0) continue; if (i > 90) break; s += i; } s");
        forLoop = jexl.createScript("var s = 0; for (var i = 0; i < 100; ++i) { if (i % 2 == 0) continue; s += i; } s");
        whileLoop = jexl.createScript("var s = 0; var i = 0; while (i < 100) { i += 1; if (i % 2 == 0) continue; s += i; } s");
        lexicalBlocks = jexl.createScript("var s = 0; for (let i : list) { let j = i; { let k = j; s += k; } } s");
        assignments = jexl.createScript("var s = 0; for (var i : list) { map.c = i; map['d'] = i; s += map.c; } s");
        final String antishSrc = "var s = 0; for (var i : list) { x.y.w = i; s += x.y.z + x.y.w; } s";
        antish = jexl.createScript(antishSrc);
        final JexlFeatures declared = JexlFeatures.createDefault()
                .antishTest(JexlFeatures.namePatterns(Arrays.asList("x.y.z", "x.y.w")));
        antishDeclared = new JexlBuilder().features(declared).cache(64).create().createScript(antishSrc);
    }

    @Benchmark
    public Object propertyAccess() {
        return propertyAccess.execute(context);
    }

    @Benchmark
    public Object methodCall() {
        return methodCall.execute(context);
    }

    @Benchmark
    public Object forEachLoop() {
        return forEachLoop.execute(context);
    }

    @Benchmark
    public Object forLoop() {
        return forLoop.execute(context);
    }

    @Benchmark
    public Object whileLoop() {
        return whileLoop.execute(context);
    }

    @Benchmark
    public Object lexicalBlocks() {
        return lexicalBlocks.execute(context);
    }

    @Benchmark
    public Object antishDeclared() {
        return antishDeclared.execute(context);
    }

    @Benchmark
    public Object assignments() {
        return assignments.execute(context);
    }

    @Benchmark
    public Object antish() {
        return antish.execute(context);
    }
}
