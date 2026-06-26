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

/**
 * Provides a framework for evaluating JEXL expressions and scripts.
 * <ul>
 * <li><a href="#intro">Introduction</a></li>
 * <li><a href="#example">Brief Example</a></li>
 * <li><a href="#usage">Using JEXL</a></li>
 * <li><a href="#configuration">Configuring JEXL</a></li>
 * <li><a href="#customization">Customizing JEXL</a></li>
 * <li><a href="#extension">Extending JEXL</a></li>
 * </ul>
 * <h2><a id="intro">Introduction</a></h2>
 * <p>
 * JEXL is a library intended to facilitate the implementation of dynamic and scripting features in applications
 * and frameworks.
 * </p>
 * <p>
 * The entry point is always a {@link org.apache.commons.jexl3.JexlEngine}, created and configured through a
 * {@link org.apache.commons.jexl3.JexlBuilder}. An engine is thread-safe and meant to be created once and shared;
 * it produces {@link org.apache.commons.jexl3.JexlExpression} (single expressions) and
 * {@link org.apache.commons.jexl3.JexlScript} (multi-statement scripts) which are evaluated against a
 * {@link org.apache.commons.jexl3.JexlContext} carrying the variable bindings. A
 * {@link org.apache.commons.jexl3.JxltEngine} adds a JSP/JSF-like templating layer on top of the same engine.
 * </p>
 * <p>
 * <strong>Security disclaimer.</strong> Neither {@link org.apache.commons.jexl3.introspection.JexlPermissions#RESTRICTED}
 * nor {@link org.apache.commons.jexl3.introspection.JexlPermissions#SECURE} is exhaustive, and neither must be
 * considered completely safe or sufficient on its own for executing untrusted user input. They are hardened
 * baselines, not guarantees. Any application that evaluates untrusted scripts <em>must</em> define its own tailored,
 * strict whitelist of exactly the classes, methods and fields its scripts legitimately need - ideally by composing on
 * top of {@link org.apache.commons.jexl3.introspection.JexlPermissions#NONE} (which denies everything) via
 * {@link org.apache.commons.jexl3.introspection.JexlPermissions#create(java.lang.String...)} - and audit the result
 * with {@link org.apache.commons.jexl3.introspection.JexlPermissions#logging()}.
 * </p>
 * <p>
 * <strong>Security note (since 3.7):</strong> a bare {@code new JexlBuilder().create()} engine is hardened by
 * default. Its permissions are {@link org.apache.commons.jexl3.introspection.JexlPermissions#SECURE} (only a small,
 * safe allow-list of {@code java.lang}/{@code java.math}/{@code java.util} types is reachable) and its default
 * {@link org.apache.commons.jexl3.JexlFeatures} disable {@code new(...)}, global side-effects, pragmas and
 * annotations (lexical scoping is on; loops remain available to scripts). Relax these deliberately - see the
 * <a href="#configuration">configuration</a> section - rather than by accident.
 * </p>
 * <p>
 * Permissions and features can be set programmatically on the {@link org.apache.commons.jexl3.JexlBuilder} or
 * loaded from a YAML document through {@link org.apache.commons.jexl3.JexlConfigLoader}, which keeps security
 * policy out of code and in configuration:
 * </p>
 * <pre>
 * try (InputStream in = getClass().getResourceAsStream("/jexl.yaml")) {
 *     JexlEngine jexl = JexlConfigLoader.load(in).create();
 * }
 * </pre>
 * <p>
 * A ready-made {@code jexl.yaml} that restores the pre-3.7 behavior (RESTRICTED permissions, the full feature
 * set) is bundled as a migration aid; see {@link org.apache.commons.jexl3.JexlConfigLoader} for the full schema.
 * </p>
 * <h2><a id="example">A Brief Example</a></h2>
 * <p>
 * In its simplest form, JEXL merges an
 * {@link org.apache.commons.jexl3.JexlExpression}
 * with a
 * {@link org.apache.commons.jexl3.JexlContext} when evaluating expressions.
 * An expression is created using
 * {@link org.apache.commons.jexl3.JexlEngine#createExpression(String)},
 * passing a String containing valid JEXL syntax.  A simple JexlContext can be created using
 * a {@link org.apache.commons.jexl3.MapContext} instance;
 * a map of variables that will be internally wrapped can be optionally provided through its constructor.
 * The following example binds a 'name' variable, calls a String method on it and concatenates the
 * result. It relies only on a {@code java.lang.String} and arithmetic, so it runs as-is under the
 * default (SECURE) permissions - no configuration needed.
 * </p>
 * <pre>
 * // Create a JexlEngine (could reuse one instead)
 * JexlEngine jexl = new JexlBuilder().create();
 * // An expression using a variable, a String method call and concatenation:
 * String jexlExp = "'Hello ' + name + ', your name has ' + name.length() + ' letters'";
 * JexlExpression e = jexl.createExpression( jexlExp );
 * // Create a context and add data
 * JexlContext jc = new MapContext();
 * jc.set("name", "John");
 * // Now evaluate the expression; result is "Hello John, your name has 4 letters"
 * Object o = e.evaluate(jc);
 * </pre>
 * <p>
 * To expose your <em>own</em> classes (a {@code Car} bean, a service, ...) to scripts, you must permit them
 * explicitly: the default SECURE permissions deny application types, and passing an instance through the
 * {@link org.apache.commons.jexl3.JexlContext} does not bypass that gate - the introspector still checks the
 * object's class. Grant access by composing your package into the permission set
 * (e.g. {@code new JexlBuilder().permissions(JexlPermissions.RESTRICTED.compose("com.example.app +{}"))})
 * or choose a broader base; see the <a href="#configuration">configuration</a> section on permissions.
 * </p>
 * <h2><a id="usage">Using JEXL</a></h2>
 * The API is composed of three levels addressing different functional needs:
 * <ul>
 * <li>Dynamic invocation of setters, getters, methods and constructors</li>
 * <li>Script expressions known as JEXL expressions</li>
 * <li>JSP/JSF like expression known as JXLT expressions</li>
 * </ul>
 * <h3><a id="usage_note">Important note</a></h3>
 * The public API classes reside in the 2 packages:
 * <ul>
 * <li>org.apache.commons.jexl3</li>
 * <li>org.apache.commons.jexl3.introspection</li>
 * </ul>
 * <p>
 * The following packages follow a "use at your own maintenance cost" policy; these are only intended to be used
 * for extending JEXL.
 * Their classes and methods are not guaranteed to remain compatible in subsequent versions.
 * If you think you need to use directly some of their features or methods, it might be a good idea to check with
 * the community through the mailing list first.
 * </p>
 * <ul>
 * <li>org.apache.commons.jexl3.parser</li>
 * <li>org.apache.commons.jexl3.scripting</li>
 * <li>org.apache.commons.jexl3.internal</li>
 * <li>org.apache.commons.jexl3.internal.introspection</li>
 * </ul>
 * <p>
 * Note that the {@code org.apache.commons.jexl3.scripting} package implements the JSR-223
 * ({@code javax.script}) integration through {@link org.apache.commons.jexl3.scripting.JexlScriptEngine}.
 * </p>
 * <h3><a id="usage_api">Dynamic Invocation</a></h3>
 * <p>
 * These functionalities are close to the core level utilities found in
 * <a href="https://commons.apache.org/beanutils/">BeanUtils</a>.
 * For basic dynamic property manipulations and method invocation, you can use the following
 * set of methods:
 * </p>
 * <ul>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#newInstance}</li>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#setProperty}</li>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#getProperty}</li>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#invokeMethod}</li>
 * </ul>
 * The following example illustrate their usage:
 * <pre>
 * // test outer class
 * public static class Froboz {
 *     int value;
 *     public Froboz(int v) { value = v; }
 *     public void setValue(int v) { value = v; }
 *     public int getValue() { return value; }
 * }
 *
 * // test inner class
 * public static class Quux {
 *     String str;
 *     Froboz froboz;
 *
 *     public Quux(String str, int fro) {
 *       this.str = str;
 *       froboz = new Froboz(fro);
 *     }
 *
 *     public Froboz getFroboz() { return froboz; }
 *     public void setFroboz(Froboz froboz) { this.froboz = froboz; }
 *     public String getStr() { return str; }
 *     public void setStr(String str) { this.str = str; }
 * }
 *
 * // test API - Quux and Froboz are in your own package; compose it into the permissions.
 * JexlEngine jexl = new JexlBuilder()
 *     .permissions(JexlPermissions.SECURE.compose("com.example +{}"))
 *     .create();
 * Quux quux = jexl.newInstance(Quux.class, "xuuq", 100);
 * jexl.setProperty(quux, "froboz.value", Integer.valueOf(100));
 * Object o = jexl.getProperty(quux, "froboz.value");
 * assertEquals("Result is not 100", Integer.valueOf(100), o);
 * jexl.setProperty(quux, "['froboz'].value", Integer.valueOf(1000));
 * o = jexl.getProperty(quux, "['froboz']['value']");
 * assertEquals("Result is not 1000", Integer.valueOf(1000), o);
 * </pre>
 * <h3><a id="usage_jexl">Expressions and Scripts</a></h3>
 * <p>
 * If your needs require simple expression evaluation capabilities, the core JEXL features
 * will most likely fit.
 * The main methods are:
 * </p>
 * <ul>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#createScript}</li>
 * <li>{@link org.apache.commons.jexl3.JexlScript#execute}</li>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#createExpression}</li>
 * <li>{@link org.apache.commons.jexl3.JexlExpression#evaluate}</li>
 * </ul>
 * The following example illustrates their usage:
 * <pre>
 * // new(...) needs the newInstance feature; Quux/Froboz need explicit permissions.
 * JexlEngine jexl = new JexlBuilder()
 *     .permissions(JexlPermissions.SECURE.compose("com.example +{}"))
 *     .features(JexlFeatures.createDefault())
 *     .create();
 * JexlContext jc = new MapContext();
 * jc.set("quuxClass", quux.class);
 * JexlExpression create = jexl.createExpression("quux = new(quuxClass, 'xuuq', 100)");
 * JexlExpression assign = jexl.createExpression("quux.froboz.value = 10");
 * JexlExpression check = jexl.createExpression("quux[\"froboz\"].value");
 * Quux quux = (Quux) create.evaluate(jc);
 * Object o = assign.evaluate(jc);
 * assertEquals("Result is not 10", Integer.valueOf(10), o);
 * o = check.evaluate(jc);
 * assertEquals("Result is not 10", Integer.valueOf(10), o);
 * </pre>
 * <h3><a id="usage_ujexl">Unified Expressions and Templates</a></h3>
 * <p>
 * If you are looking for JSP-EL like and basic templating features, you can
 * use Expression from a JxltEngine.
 * </p>
 * The main methods are:
 * <ul>
 * <li>{@link org.apache.commons.jexl3.JxltEngine#createExpression}</li>
 * <li>{@link org.apache.commons.jexl3.JxltEngine.Expression#prepare}</li>
 * <li>{@link org.apache.commons.jexl3.JxltEngine.Expression#evaluate}</li>
 * <li>{@link org.apache.commons.jexl3.JxltEngine#createTemplate}</li>
 * <li>{@link org.apache.commons.jexl3.JxltEngine.Template#prepare}</li>
 * <li>{@link org.apache.commons.jexl3.JxltEngine.Template#evaluate}</li>
 * </ul>
 * The following example illustrates their usage:
 * <pre>
 * JexlEngine jexl = new JexlBuilder().create();
 * JxltEngine jxlt = jexl.createJxltEngine();
 * JxltEngine.Expression expr = jxlt.createExpression("Hello ${user}");
 * String hello = expr.evaluate(context).toString();
 * </pre>
 * <h3>JexlExpression, JexlScript, Expression and Template: summary</h3>
 * <h4>JexlExpression </h4>
 * <p>
 * These are the most basic form of JexlEngine expressions and only allow for a single command
 * to be executed and its result returned. If you try to use multiple commands, it ignores
 * everything after the first semi-colon and just returns the result from
 * the first command.
 * </p>
 * <p>
 * Also note that expressions are not statements (which is what scripts are made of) and do not allow
 * using the flow control (if, while, for), variables or lambdas syntactic elements.
 * </p>
 * <h4>JexlScript</h4>
 * <p>
 * These allow you to use multiple statements and you can
 * use variable assignments, loops, calculations, etc. More or less what can be achieved in Shell or
 * JavaScript at its basic level. The result from the last command is returned from the script.
 * </p>
 * <h4>JxltEngine.Expression</h4>
 * <p>
 * These are ideal to produce "one-liner" text, like a 'toString()' on steroids.
 * To get a calculation you use the EL-like syntax
 * as in ${someVariable}. The expression that goes between the brackets
 * behaves like a JexlScript, not an expression. You can use semi-colons to
 * execute multiple commands and the result from the last command is
 * returned from the script. You also have the ability to use a 2-pass evaluation using
 * the #{someScript} syntax.
 * </p>
 * <h4>JxltEngine.Template</h4>
 * <p>
 * These produce text documents. Each line beginning with '$$' (as a default) is
 * considered JEXL code and all others considered as JxltEngine.Expression.
 * Think of those as simple Velocity templates. A rewritten MudStore initial Velocity sample looks like this:
 * </p>
 * <pre><code>
 * &lt;html&gt;
 * &lt;body&gt;
 * Hello ${customer.name}!
 * &lt;table&gt;
 * $$      for(var mud : mudsOnSpecial ) {
 * $$          if (customer.hasPurchased(mud) ) {
 * &lt;tr&gt;
 * &lt;td&gt;
 * ${flogger.getPromo( mud )}
 * &lt;/td&gt;
 * &lt;/tr&gt;
 * $$          }
 * $$      }
 * &lt;/table&gt;
 * &lt;/body&gt;
 * &lt;/html&gt;
 * </code></pre>
 * <h2><a id="configuration">JEXL Configuration</a></h2>
 * <p>
 * Almost everything is configured through a {@link org.apache.commons.jexl3.JexlBuilder} before the engine is
 * created. The builder controls four largely orthogonal concerns:
 * </p>
 * <ul>
 * <li><strong>Permissions</strong> ({@link org.apache.commons.jexl3.introspection.JexlPermissions}) -
 *     <em>what</em> classes, methods and fields scripts may reach through reflection.</li>
 * <li><strong>Features</strong> ({@link org.apache.commons.jexl3.JexlFeatures}) -
 *     <em>which</em> syntactic constructs are accepted at parse time.</li>
 * <li><strong>Options</strong> ({@link org.apache.commons.jexl3.JexlOptions}) -
 *     <em>how</em> the interpreter behaves at runtime (strict, silent, safe, cancellable, lexical, math).</li>
 * <li><strong>Bindings</strong> (namespaces, imports, arithmetic, class loader, caches) -
 *     the environment scripts execute against.</li>
 * </ul>
 * <p>
 * For a declarative alternative, {@link org.apache.commons.jexl3.JexlConfigLoader} builds a pre-configured
 * {@link org.apache.commons.jexl3.JexlBuilder} from a YAML document covering permissions, features, arithmetic,
 * namespaces and imports - convenient for externalizing configuration or restoring pre-3.7 defaults.
 * </p>
 * <h3><a id="config_security">Security: permissions, sandbox and {@code @NoJexl}</a></h3>
 * <p>
 * {@link org.apache.commons.jexl3.introspection.JexlPermissions} is the primary security gate; it controls which
 * packages, classes and members the introspection layer will expose. Four constants are provided:
 * {@link org.apache.commons.jexl3.introspection.JexlPermissions#UNRESTRICTED} (everything),
 * {@link org.apache.commons.jexl3.introspection.JexlPermissions#RESTRICTED} (a broad but curated allow-list),
 * {@link org.apache.commons.jexl3.introspection.JexlPermissions#SECURE} (the minimal safe allow-list and the
 * default since 3.7) and {@link org.apache.commons.jexl3.introspection.JexlPermissions#NONE} (deny everything).
 * A permission set is configured with
 * {@link org.apache.commons.jexl3.JexlBuilder#permissions(org.apache.commons.jexl3.introspection.JexlPermissions)}
 * and can be composed from a textual DSL through
 * {@link org.apache.commons.jexl3.introspection.JexlPermissions#parse(java.lang.String...)} or
 * {@code RESTRICTED.compose("com.example.api +{}")}. To build a closed-world, deny-by-default policy from scratch,
 * start from {@code NONE} (or {@link org.apache.commons.jexl3.introspection.JexlPermissions#create(java.lang.String...)})
 * and compose only what scripts need, e.g. {@code JexlPermissions.create("com.example.api +{}")}. To diagnose what a
 * permission set allows or denies under your workload, wrap it with
 * {@link org.apache.commons.jexl3.introspection.JexlPermissions#logging()}.
 * </p>
 * <p>
 * Two finer-grained mechanisms complement permissions: the
 * {@link org.apache.commons.jexl3.annotations.NoJexl} annotation completely shields annotated classes and members
 * from introspection at the source level, while a
 * {@link org.apache.commons.jexl3.introspection.JexlSandbox} - set through
 * {@link org.apache.commons.jexl3.JexlBuilder#sandbox(org.apache.commons.jexl3.introspection.JexlSandbox)} - gives
 * per-class allow/deny control over properties and methods at configuration time.
 * </p>
 * <h3><a id="config_features">Language features (parse time)</a></h3>
 * <p>
 * {@link org.apache.commons.jexl3.JexlFeatures} restricts the grammar a script may use; violations are reported as
 * {@link org.apache.commons.jexl3.JexlException.Feature} when the script is compiled, before any evaluation. Among
 * the toggles are {@code newInstance} ({@code new(...)}), {@code loops}, {@code sideEffect}/{@code sideEffectGlobal},
 * {@code lambda}, {@code methodCall}, {@code structuredLiteral}, {@code pragma}, {@code annotation} and lexical
 * scoping. A feature set is applied with {@link org.apache.commons.jexl3.JexlBuilder#features(org.apache.commons.jexl3.JexlFeatures)};
 * convenient bases are {@link org.apache.commons.jexl3.JexlFeatures#createDefault()},
 * {@link org.apache.commons.jexl3.JexlFeatures#createScript()}, {@link org.apache.commons.jexl3.JexlFeatures#createAll()}
 * and {@link org.apache.commons.jexl3.JexlFeatures#createNone()}.
 * </p>
 * <p>
 * Since expressions are parsed as a single expression, statement-level features (loops, multiple statements, ...)
 * never apply to them regardless of the feature set; they are relevant to scripts only.
 * </p>
 * <h3><a id="config_options">Runtime options</a></h3>
 * <p>
 * {@link org.apache.commons.jexl3.JexlOptions} carries the behavioral flags evaluated at runtime. They can be set
 * as engine defaults either through the dedicated builder shortcuts or by mutating the builder's option set returned
 * by {@link org.apache.commons.jexl3.JexlBuilder#options()}:
 * </p>
 * <ul>
 * <li>{@link org.apache.commons.jexl3.JexlBuilder#strict(boolean)} - whether {@code null} operands, unknown
 *     variables or failed calls are errors.</li>
 * <li>{@link org.apache.commons.jexl3.JexlBuilder#silent(boolean)} - whether errors throw
 *     {@link org.apache.commons.jexl3.JexlException} (unchecked) or are logged and yield {@code null}.</li>
 * <li>{@link org.apache.commons.jexl3.JexlBuilder#safe(boolean)} - whether safe-navigation ({@code x?.y}) tolerates
 *     {@code null} on dereference.</li>
 * <li>{@link org.apache.commons.jexl3.JexlBuilder#cancellable(boolean)} - whether interrupting the evaluating
 *     thread raises {@link org.apache.commons.jexl3.JexlException.Cancel}.</li>
 * <li>{@link org.apache.commons.jexl3.JexlBuilder#lexical(boolean)} /
 *     {@link org.apache.commons.jexl3.JexlBuilder#lexicalShade(boolean)} - lexical scoping of local variables.</li>
 * <li>arithmetic strictness and math context, carried by the {@link org.apache.commons.jexl3.JexlArithmetic}
 *     instance set with {@link org.apache.commons.jexl3.JexlBuilder#arithmetic(org.apache.commons.jexl3.JexlArithmetic)}.</li>
 * </ul>
 * <p>
 * These engine defaults can be overridden per evaluation: a {@link org.apache.commons.jexl3.JexlContext} that also
 * implements {@link org.apache.commons.jexl3.JexlContext.OptionsHandle} supplies a fresh
 * {@link org.apache.commons.jexl3.JexlOptions} for each evaluation. (The legacy
 * {@link org.apache.commons.jexl3.JexlEngine.Options} interface is deprecated in favor of this mechanism.)
 * </p>
 * <h3><a id="config_bindings">Namespaces, imports and arithmetic</a></h3>
 * <p>
 * {@link org.apache.commons.jexl3.JexlBuilder#namespaces(java.util.Map)} registers your own classes or instances as
 * namespaces, exposing their methods as {@code prefix:function(...)} calls.
 * </p>
 * <pre>{@code
 * public static class MyMath {
 *   public double cos(double x) {
 *     return Math.cos(x);
 *   }
 * }
 * Map<String, Object> funcs = new HashMap<String, Object>();
 * funcs.put("math", new MyMath());
 * // MyMath is in your own package; compose it into the permissions.
 * JexlEngine jexl = new JexlBuilder()
 *     .namespaces(funcs)
 *     .permissions(JexlPermissions.SECURE.compose("com.example +{}"))
 *     .create();
 * JexlContext jc = new MapContext();
 * jc.set("pi", Math.PI);
 * JexlExpression e = jexl.createExpression("math:cos(pi)");
 * Object o = e.evaluate(jc);
 * assertEquals(Double.valueOf(-1), o);
 * }</pre>
 * <p>
 * If the <em>namespace</em> is a Class and that class declares a constructor that takes a JexlContext (or
 * a class extending JexlContext), one <em>namespace</em> instance is created on first usage in an
 * expression; this instance lifetime is limited to the expression evaluation.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.JexlBuilder#imports(java.util.Collection)} declares packages whose classes can be
 * referenced by their unqualified name in {@code new(...)} and type references, mimicking Java imports.
 * {@link org.apache.commons.jexl3.JexlBuilder#arithmetic(org.apache.commons.jexl3.JexlArithmetic)} substitutes a
 * custom {@link org.apache.commons.jexl3.JexlArithmetic} - see <a href="#customization">customization</a>.
 * </p>
 * <h3><a id="static_configuration">Static &amp; Shared Configuration</a></h3>
 * <p>
 * Both JexlEngine and JxltEngine are thread-safe, most of their inner fields are final; the same instance can
 * be shared between different threads and proper synchronization is enforced in critical areas (introspection caches).
 * </p>
 * <p>
 * The library-wide defaults applied by a bare {@code new JexlBuilder()} are themselves configurable through static
 * setters: {@link org.apache.commons.jexl3.JexlBuilder#setDefaultPermissions(org.apache.commons.jexl3.introspection.JexlPermissions)},
 * {@link org.apache.commons.jexl3.JexlBuilder#setDefaultFeatures(org.apache.commons.jexl3.JexlFeatures)},
 * and {@link org.apache.commons.jexl3.JexlBuilder#setDefaultOptions(String...)} (runtime evaluation flags such as
 * {@code strict}, {@code safe}, and {@code cancellable}; see {@link org.apache.commons.jexl3.JexlOptions#setDefaultFlags(String...)}).
 * The pre-3.7 permissive feature set is exposed as {@link org.apache.commons.jexl3.JexlBuilder#FULL} so an application
 * can restore the legacy behavior process-wide in one place.
 * </p>
 * <p>
 * Of particular importance is {@link org.apache.commons.jexl3.JexlBuilder#loader(java.lang.ClassLoader)} which indicates
 * to the JexlEngine being built which class loader to use to solve a class name;
 * this directly affects how JexlEngine.newInstance and the 'new' script method operates.
 * </p>
 * <p>
 * This can also be very useful in cases where you rely on JEXL to dynamically load and call plugins for your application.
 * To avoid having to restart the server in case of a plugin implementation change, you can call
 * {@link org.apache.commons.jexl3.JexlEngine#setClassLoader} and all the scripts created through this engine instance
 * will automatically point to the newly loaded classes.
 * </p>
 * <h3><a id="config_cache">Caches &amp; debugging</a></h3>
 * <p>
 * JexlEngine and JxltEngine expression caches can be configured as well. If you intend to use JEXL
 * repeatedly in your application, these are worth configuring since expression parsing is quite heavy.
 * Note that all caches created by JEXL are held through {@code SoftReference}; under high memory pressure, the GC will
 * be able to reclaim those caches and JEXL will rebuild them if needed. By default, a JexlEngine does create a cache
 * for "small" expressions and a JxltEngine does create one for Expression.
 * </p>
 * <p>{@link org.apache.commons.jexl3.JexlBuilder#cache(int)} sets how many expressions can be simultaneously cached by
 * the JEXL engine, {@link org.apache.commons.jexl3.JexlBuilder#cacheThreshold(int)} caps the size of cached
 * expressions, and {@link org.apache.commons.jexl3.JexlBuilder#cacheFactory(java.util.function.IntFunction)} lets you
 * supply an alternative {@link org.apache.commons.jexl3.JexlCache} implementation. JxltEngine allows defining the
 * cache size through its constructor.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.JexlBuilder#debug(boolean)}
 * makes stack traces carried by JexlException more meaningful; in particular, these
 * traces will carry the exact caller location the Expression was created from.
 * </p>
 * <h3><a id="dynamic_configuration">Dynamic Configuration</a></h3>
 * <p>
 * Beyond options, a {@link org.apache.commons.jexl3.JexlContext} can implement a number of handles to override
 * engine behavior on a per-evaluation basis. The {@code MapContext}/{@link org.apache.commons.jexl3.ObjectContext}
 * implementations cover the common cases; for finer control, implement one or more of:
 * </p>
 * <ul>
 * <li>{@link org.apache.commons.jexl3.JexlContext.OptionsHandle} - supply per-evaluation
 *     {@link org.apache.commons.jexl3.JexlOptions}.</li>
 * <li>{@link org.apache.commons.jexl3.JexlContext.NamespaceResolver} - override namespace resolution and the
 *     default namespace map defined through {@link org.apache.commons.jexl3.JexlBuilder#namespaces(java.util.Map)}.</li>
 * <li>{@link org.apache.commons.jexl3.JexlContext.AnnotationProcessor} - handle {@code @annotation} statements.</li>
 * <li>{@link org.apache.commons.jexl3.JexlContext.PragmaProcessor} - react to {@code #pragma} directives.</li>
 * <li>{@link org.apache.commons.jexl3.JexlContext.ModuleProcessor} - resolve {@code #pragma jexl.module} imports.</li>
 * <li>{@link org.apache.commons.jexl3.JexlContext.ClassNameResolver} - resolve unqualified class names.</li>
 * <li>{@link org.apache.commons.jexl3.JexlContext.CancellationHandle} - expose a cancellation flag to interrupt
 *     evaluation.</li>
 * <li>{@link org.apache.commons.jexl3.JexlContext.ThreadLocal} - bind context data to the evaluating thread.</li>
 * </ul>
 * <h2><a id="customization">JEXL Customization</a></h2>
 * <p>
 * The {@link org.apache.commons.jexl3.JexlContext}, {@link org.apache.commons.jexl3.JexlBuilder} and
 * {@link org.apache.commons.jexl3.JexlOptions} are
 * the most likely interfaces you'll want to implement for customization. Since they expose variables and options,
 * they are the primary targets. Before you do so, have a look at
 * {@link org.apache.commons.jexl3.ObjectContext} which may already cover some of your needs.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.JexlArithmetic}
 * is the class to derive if you need to change how operators behave or add types upon which they
 * operate.
 * There are 3 entry points that allow customizing the type of objects created:
 * </p>
 * <ul>
 * <li>array literals: {@link org.apache.commons.jexl3.JexlArithmetic#arrayBuilder}</li>
 * <li>map literals: {@link org.apache.commons.jexl3.JexlArithmetic#mapBuilder}</li>
 * <li>set literals: {@link org.apache.commons.jexl3.JexlArithmetic#setBuilder}</li>
 * <li>range objects: {@link org.apache.commons.jexl3.JexlArithmetic#createRange}</li>
 * </ul>
 * <p>
 * You can also overload operator methods; by convention, each operator has a method name associated to it.
 * If you overload some in your JexlArithmetic derived implementation, these methods will be called when the
 * arguments match your method signature.
 * For example, this would be the case if you wanted '+' to operate on arrays; you'd need to derive
 * JexlArithmetic and implement a {@code public Object add(Set<?> x, Set<?> y)} method.
 * Note however that you can <em>not</em> change the operator precedence.
 * The list of operator / method matches is described in {@link org.apache.commons.jexl3.JexlOperator}:
 * </p>
 * <p>
 * You can also add methods to overload property getters and setters operators behaviors.
 * Public methods of the JexlArithmetic instance named propertyGet/propertySet/arrayGet/arraySet are potential
 * overrides that will be called when appropriate.
 * The following table is an overview of the relation between a syntactic form and the method to call
 * where V is the property value class, O the object class and  P the property identifier class (usually String or Integer).
 * </p>
 * <table><caption>Property Accessors</caption>
 * <tr>
 * <th>Expression</th>
 * <th>Method Template</th>
 * </tr>
 * <tr>
 * <td>foo.property</td>
 * <td>public V propertyGet(O obj, P property);</td>
 * </tr>
 * <tr>
 * <td>foo.property = value</td>
 * <td>public V propertySet(O obj, P property, V value);</td>
 * </tr>
 * <tr>
 * <td>foo[property]</td>
 * <td>public V arrayGet(O obj, P property, V value);</td>
 * </tr>
 * <tr>
 * <td>foo[property] = value</td>
 * <td>public V arraySet(O obj, P property, V value);</td>
 * </tr>
 * </table>
 * <p>
 * You can also override the base operator methods, those whose arguments are Object which gives you total
 * control.
 * </p>
 * <h2><a id="extension">Extending JEXL</a></h2>
 * If you need to make JEXL treat some objects in a specialized manner or tweak how it
 * reacts to some settings, you can derive most of its inner-workings. The classes and methods are rarely private or
 * final - only when the inner contract really requires it. However, using the protected methods
 * and internal package classes imply you might have to re-adapt your code when new JEXL versions are released.
 * <p>
 * {@link org.apache.commons.jexl3.internal.Engine} can be
 * extended to let you capture your own configuration defaults regarding cache sizes and various flags.
 * Implementing your own {@link org.apache.commons.jexl3.JexlCache} - instead of the default soft-reference based one -
 * would be another possible extension.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.internal.Interpreter}
 * is the class to derive if you need to add more features to the evaluation
 * itself; for instance, you want pre- and post- resolvers for variables or nested scopes for
 * for variable contexts.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.internal.introspection.Uberspect}
 * is the class to derive if you need to add introspection or reflection capabilities for some objects, for
 * instance adding factory based support to the 'new' operator.
 * The code already reflects public fields as properties on top of Java-beans conventions.
 * </p>
 */
package org.apache.commons.jexl3;
