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
package org.apache.commons.jexl3.introspection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.jexl3.internal.introspection.PermissionsParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This interface describes permissions used by JEXL introspection that constrain which
 * packages/classes/constructors/fields/methods are made visible to JEXL scripts.
 * <p>By specifying or implementing permissions, it is possible to constrain precisely which objects can be manipulated
 * by JEXL, allowing users to enter their own expressions or scripts whilst maintaining tight control
 * over what can be executed. JEXL introspection mechanism will check whether it is permitted to
 * access a constructor, method or field before exposition to the {@link JexlUberspect}. The restrictions
 * are applied in all cases, for any {@link org.apache.commons.jexl3.introspection.JexlUberspect.ResolverStrategy}.
 * </p>
 * <p><strong>Security disclaimer.</strong> Neither {@link #RESTRICTED} nor {@link #SECURE} is exhaustive, and neither
 * must be considered completely safe or sufficient on its own for executing untrusted user input. They are hardened
 * baselines, not guarantees. Any application that evaluates untrusted scripts <em>must</em> define its own tailored,
 * strict whitelist of exactly the classes, methods and fields its scripts legitimately need - ideally by composing on
 * top of {@link #NONE} (which denies everything) via {@link #create(String...)} / {@link #compose(String...)} - and
 * audit the result with {@link #logging()}.</p>
 * <p>This complements using a dedicated {@link ClassLoader} and/or {@link SecurityManager} - being deprecated -
 * and possibly {@link JexlSandbox} with a simpler mechanism. The {@link org.apache.commons.jexl3.annotations.NoJexl}
 * annotation processing is actually performed using the result of calling {@link #parse(String...)} with no arguments;
 * implementations shall delegate calls to its methods for {@link org.apache.commons.jexl3.annotations.NoJexl} to be
 * processed.</p>
 * <p>A simple textual configuration can be used to create user-defined permissions using
 * {@link JexlPermissions#parse(String...)}. The permission syntax supports both positive (+) and negative (-)
 * declarations:</p>
 * <ul>
 * <li><b>Negative restrictions ({@code -})</b>: By default or when prefixed with {@code -}, class restrictions
 * explicitly <b>deny</b> access to the specified members (or the entire class if the block is empty).
 * This is the default mode and works like {@link org.apache.commons.jexl3.annotations.NoJexl}.</li>
 * <li><b>Positive restrictions ({@code +})</b>: When prefixed with {@code +}, class restrictions
 * explicitly <b>allow only</b> the specified members (or the entire class if the block is empty), denying
 * all others. This provides a whitelist approach where you must explicitly list what is permitted.</li>
 * </ul>
 * <p>For example:</p>
 * <pre>
 * // Deny specific methods in a class (negative restriction - default)
 * java.lang { System { exit(); } }  // or -System { exit(); }
 *
 * // Allow only specific methods in a class (positive restriction)
 * java.lang { +System { currentTimeMillis(); nanoTime(); } }
 *
 * // Allow entire class (positive restriction with empty block)
 * java.io -{ +PrintWriter{} +Writer{} }
 * </pre>
 *
 * <p>To build a policy from scratch, start from {@link #NONE} (or {@link #create(String...)}), which denies
 * everything, and compose only what scripts need on top - the closed-world, deny-by-default approach. This is the
 * opposite of {@link #UNRESTRICTED} (the empty {@link #parse(String...)}), which allows everything.</p>
 *
 *<p>To instantiate a JEXL engine using permissions, one should use a {@link org.apache.commons.jexl3.JexlBuilder}
 * and call {@link org.apache.commons.jexl3.JexlBuilder#permissions(JexlPermissions)}. Another approach would
 * be to instantiate a {@link JexlUberspect} with those permissions and call
 * {@link org.apache.commons.jexl3.JexlBuilder#uberspect(JexlUberspect)}.</p>
 *
 * <p>
 *     To help migration from earlier versions, it is possible to revert to the JEXL 3.2 default lenient behavior
 *     by calling {@link org.apache.commons.jexl3.JexlBuilder#setDefaultPermissions(JexlPermissions)} with
 *     {@link #UNRESTRICTED} as parameter before creating a JEXL engine instance.
 * </p>
 * <p>
 *     For the same reason, using JEXL through scripting, it is possible to revert the underlying JEXL behavior to
 *     JEXL 3.2 default by calling {@link org.apache.commons.jexl3.scripting.JexlScriptEngine#setPermissions(JexlPermissions)}
 *     with {@link #UNRESTRICTED} as parameter.
 * </p>
 *
 * @since 3.3
 */
public interface JexlPermissions {

    /**
     * A permission delegation that augments the RESTRICTED permission with an explicit
     * set of classes.
     * <p>A typical use case is to deny access to a package - and thus all its classes - but allow
     * a few specific classes.</p>
     * <p>Note that the newer positive restriction syntax is preferable as in:
     * <code>RESTRICTED.compose("java.lang { +Class {} }")</code>.</p>
     */
    final class ClassPermissions extends JexlPermissions.Delegate {
      /**
       * The set of explicitly allowed classes, overriding the delegate permissions.
       */
      private final Set<String> allowedClasses;

      /**
       * Creates permissions based on the RESTRICTED set but allowing an explicit set.
       *
       * @param allow The set of allowed classes
       */
      public ClassPermissions(final Class<?>... allow) {
        this(JexlPermissions.RESTRICTED, allow);
      }

      /**
       * Creates permissions by augmenting an existing set with an explicit set of allowed classes.
       * @param permissions The base permissions to augment
       * @param allow The set of allowed classes
       */
      public ClassPermissions(final JexlPermissions permissions, final Class<?>... allow) {
          this(permissions, Arrays.stream(Objects.requireNonNull(allow, "allow")).map(Class::getCanonicalName).collect(Collectors.toList()));
      }

      /**
       * Creates permissions by augmenting an existing set with an explicit set of allowed canonical class names.
       *
       * @param delegate The base to delegate to
       * @param allow    The list of class canonical names
       */
      public ClassPermissions(final JexlPermissions delegate, final Collection<String> allow) {
        super(Objects.requireNonNull(delegate, "delegate"));
        allowedClasses = new HashSet<>(Objects.requireNonNull(allow, "allow"));
      }

      @Override
      public boolean allow(final Constructor<?> constructor) {
        return validate(constructor) &&
            (allowedClasses.contains(constructor.getDeclaringClass().getCanonicalName()) || super.allow(constructor));
      }

      @Override
      public boolean allow(final Class<?> clazz) {
        return validate(clazz) &&
            (allowedClasses.contains(clazz.getCanonicalName()) || super.allow(clazz));
      }

      @Override
      public boolean allow(final Class<?> clazz, final Field field) {
        if (!validate(field)) {
          return false;
        }
        if (!validate(clazz)) {
          return false;
        }
        if (!field.getDeclaringClass().isAssignableFrom(clazz)) {
          return false;
        }
        if (super.allow(clazz, field)) {
          return true;
        }
        return isClassAllowed(clazz);
      }

      @Override
      public boolean allow(final Class<?> clazz, final Method method) {
        if (!validate(method)) {
          return false;
        }
        if (!method.getDeclaringClass().isAssignableFrom(clazz)) {
          return false;
        }
        if (super.allow(clazz, method)) {
          return true;
        }
        return isClassAllowed(clazz);
      }

      @Override
      public JexlPermissions compose(final String... src) {
        return new ClassPermissions(base.compose(src), allowedClasses);
      }

      private boolean isClassAllowed(final Class<?> aClass) {
        Class<?> clazz = aClass;
        // let's walk all interfaces
        for (final Class<?> inter : clazz.getInterfaces()) {
          if (allowedClasses.contains(inter.getCanonicalName())) {
            return true;
          }
        }
        // let's walk all super classes
        while (clazz != null) {
          if (allowedClasses.contains(clazz.getCanonicalName())) {
            return true;
          }
          clazz = clazz.getSuperclass();
        }
        return false;
      }
    }

    /**
     * A base for permission delegation allowing functional refinement.
     * Overloads should call the appropriate validate() method early in their body.
     */
    class Delegate implements JexlPermissions {
        /**
         * The permissions we delegate to.
         */
        protected final JexlPermissions base;

        /**
         * Constructs a new instance.
         *
         * @param delegate The delegate.
         */
        protected Delegate(final JexlPermissions delegate) {
            base = delegate;
        }

        @Override
        public boolean allow(final Class<?> clazz) {
            return base.allow(clazz);
        }

        @Override
        public boolean allow(final Constructor<?> ctor) {
            return base.allow(ctor);
        }

        @Override
        public boolean allow(final Field field) {
            return validate(field) && allow(field.getDeclaringClass(), field);
        }

        @Override
        public boolean allow(final Class<?> clazz, final Field field) {
            return base.allow(clazz, field);
        }

        @Override
        public boolean allow(final Method method) {
            return validate(method) && allow(method.getDeclaringClass(), method);
        }

        @Override
        public boolean allow(final Class<?> clazz, final Method method) {
            return base.allow(clazz, method);
        }

        @Override
        public boolean allow(final Package pack) {
            return base.allow(pack);
        }

        @Override
        public JexlPermissions compose(final String... src) {
            return new Delegate(base.compose(src));
        }
    }

    /**
     * A permission delegate that logs every allow/deny decision.
     * <p>This is a debugging aid to determine which reflective elements (classes, constructors, methods, fields)
     * a permission set allows or denies; wrap any permissions with {@link JexlPermissions#logging()} (or
     * {@link JexlPermissions#logging(String)} to pick the logger name) and inspect the log to diagnose why a
     * given object is or is not reachable from scripts.</p>
     *
     * @since 3.7.0
     */
    class LoggingPermissions extends Delegate {
        /** The logger that decisions are written to (at info level). */
        private final Log logger;
        /** The set of already-emitted log lines, so each decision is logged only once. */
        private final Set<String> logged = ConcurrentHashMap.newKeySet();

        /**
         * Constructs an instance logging to a logger named after this class.
         *
         * @param delegate The permissions to delegate to
         */
        public LoggingPermissions(final JexlPermissions delegate) {
            this(LogFactory.getLog(LoggingPermissions.class), delegate);
        }

        /**
         * Constructs an instance logging to a named logger.
         *
         * @param loggerName The name of the logger to use
         * @param delegate The permissions to delegate to
         */
        public LoggingPermissions(final String loggerName, final JexlPermissions delegate) {
            this(LogFactory.getLog(loggerName), delegate);
        }

        /**
         * Constructs an instance with an explicit logger.
         *
         * @param log The logger
         * @param delegate The permissions to delegate to
         */
        protected LoggingPermissions(final Log log, final JexlPermissions delegate) {
            super(delegate);
            this.logger = log;
        }

        /**
         * Logs a decision once: the first time a given message is seen, it is written to the logger;
         * subsequent identical messages are suppressed.
         *
         * @param allowed The decision to return
         * @param message The message to log
         * @return The decision
         */
        private boolean log(final boolean allowed, final String message) {
            if (logged.add(message)) {
                logger.info(message);
            }
            return allowed;
        }

        @Override
        public boolean allow(final Class<?> clazz) {
            final boolean allowed = super.allow(clazz);
            return log(allowed, String.format("Class %s is %s",
                clazz.getCanonicalName(), allowed ? "allowed" : "denied"));
        }

        @Override
        public boolean allow(final Constructor<?> ctor) {
            final boolean allowed = super.allow(ctor);
            return log(allowed, String.format("Constructor %s.%s() is %s",
                ctor.getDeclaringClass().getCanonicalName(), ctor.getName(),
                allowed ? "allowed" : "denied"));
        }

        @Override
        public boolean allow(final Field field) {
            final boolean allowed = super.allow(field);
            return log(allowed, String.format("Field %s.%s is %s",
                field.getDeclaringClass().getCanonicalName(), field.getName(),
                allowed ? "allowed" : "denied"));
        }

        @Override
        public boolean allow(final Class<?> clazz, final Field field) {
            final boolean allowed = super.allow(clazz, field);
            return log(allowed, String.format("Field %s.%s is %s for class %s",
                field.getDeclaringClass().getCanonicalName(), field.getName(),
                allowed ? "allowed" : "denied", clazz.getCanonicalName()));
        }

        @Override
        public boolean allow(final Method method) {
            final boolean allowed = super.allow(method);
            return log(allowed, String.format("Method %s.%s() is %s",
                method.getDeclaringClass().getCanonicalName(), method.getName(),
                allowed ? "allowed" : "denied"));
        }

        @Override
        public boolean allow(final Class<?> clazz, final Method method) {
            final boolean allowed = super.allow(clazz, method);
            return log(allowed, String.format("Method %s.%s() is %s for class %s",
                method.getDeclaringClass().getCanonicalName(), method.getName(),
                allowed ? "allowed" : "denied", clazz.getCanonicalName()));
        }

        @Override
        public JexlPermissions compose(final String... src) {
            return new LoggingPermissions(logger, base.compose(src));
        }
    }

    /**
     * The unrestricted permissions.
     * <p>This enables any public class, method, constructor or field to be visible to JEXL and used in scripts.</p>
     * <p>It is <em>highly</em> discouraged to use this permissions outside of testing.</p>
     * @since 3.3
     */
    JexlPermissions UNRESTRICTED = JexlPermissions.parse();

    /**
     * A permission set that denies everything: the empty base to build permissions from scratch.
     * <p>Unlike {@link #UNRESTRICTED} (the empty {@link #parse(String...)}, which allows everything), NONE allows
     * nothing. Compose positive declarations on top to grant access, for example:</p>
     * <pre>JexlPermissions.NONE.compose("java.lang { +String{} }")</pre>
     * <p>or use the {@link #create(String...)} factory. This is the recommended starting point when you want
     * a closed-world, deny-by-default policy listing only what your scripts actually need.</p>
     * @since 3.7.0
     */
    JexlPermissions NONE = new JexlPermissions() {
        @Override public boolean allow(final Package pack)        { return false; }
        @Override public boolean allow(final Class<?> clazz)      { return false; }
        @Override public boolean allow(final Constructor<?> ctor) { return false; }
        @Override public boolean allow(final Field field)         { return false; }
        @Override public boolean allow(final Method method)       { return false; }
        @Override public JexlPermissions compose(final String... src) {
            // NONE has no state to merge; composing rules builds a closed-world set from scratch
            return src == null || src.length == 0 ? this : JexlPermissions.parse(src);
        }
    };

    /**
     * A restricted singleton.
     * <p>The RESTRICTED set is built using the following allowed packages and denied packages/classes.</p>
     * <p>
     * RESTRICTED attempts to strike a balance between reasonable out-of-the-box isolation and allowing most
     * legitimate features; it is convenient when scripts need a broad slice of the JDK. In a mission-critical
     * scenario, prefer {@link #SECURE} as a base instead and {@link #compose(String...) compose} only what your
     * scripts actually need on top of it. Be aware that the isolation RESTRICTED provides may be incomplete and
     * could expose more than intended; should such a case be identified, we will endeavour to resolve it in a
     * subsequent release. Use {@link #logging()} to audit exactly which elements your workload reaches.
     * </p>
     * <p>RESTRICTED is not exhaustive and must not be considered sufficient on its own for executing untrusted user
     * input. For untrusted scripts, define a tailored, strict whitelist of exactly what your scripts need - ideally
     * composed on top of {@link #NONE} - rather than relying on RESTRICTED as-is.</p>
     * <p>Of particular importance are the restrictions on the {@link System},
     * {@link Runtime}, {@link ProcessBuilder}, {@link Class} and those on {@link java.net},
     * {@link java.io} and {@link java.lang.reflect} that should provide a decent level of isolation between the scripts
     * and its host.
     * </p>
     * <p>
     * Every allowed package is declared explicitly using the positive {@code +{}} syntax rather than a
     * {@code .*} wildcard. A wildcard matches a package <em>and all of its sub-packages</em>, which is not
     * future-proof: a sub-package added by a later JDK (or a dangerous existing one such as
     * {@code java.util.zip}/{@code java.util.jar} - which can read files - or {@code java.nio.file}) would be
     * silently exposed. Listing each package explicitly keeps the perimeter closed: only the packages below are
     * visible, nothing else.
     * </p>
     * <p>Allowed packages (each member is visible unless explicitly denied):</p>
     * <ul>
     * <li>java.math</li>
     * <li>java.text</li>
     * <li>java.time, java.time.chrono, java.time.format, java.time.temporal, java.time.zone</li>
     * <li>java.util, java.util.concurrent, java.util.concurrent.atomic, java.util.function, java.util.stream, java.util.regex</li>
     * <li>java.nio, java.nio.charset</li>
     * <li>org.w3c.dom</li>
     * <li>java.lang (minus the denied classes below)</li>
     * <li>org.apache.commons.jexl3 (minus JexlBuilder)</li>
     * </ul>
     * <p>Denied classes / members (carved out of otherwise-allowed packages):</p>
     * <ul>
     * <li>java.lang { Runtime, System, ProcessBuilder, Process, RuntimePermission, SecurityManager, Thread, ThreadGroup, Class, ClassLoader }
     * and the system-property readers Integer.getInteger, Long.getLong, Boolean.getBoolean</li>
     * <li>java.io { everything except PrintWriter, Writer, StringWriter, Reader, InputStream, OutputStream }</li>
     * <li>java.util: the classes stay visible but their file/loader members are carved out -
     * Formatter and Scanner constructors (file I/O), Properties.load/store/loadFromXML/storeToXML/save (file I/O),
     * ResourceBundle.getBundle/clearCache and PropertyResourceBundle constructors (property-file/class loading),
     * ServiceLoader.load/loadInstalled (service/class loading). No file can be read or written and no class or
     * service loaded through java.util.</li>
     * <li>java.util.concurrent { Executors and the thread-pool / fork-join executor classes }</li>
     * <li>java.time.zone { ZoneRulesProvider } (prevents JVM-wide time-zone provider registration)</li>
     * <li>org.apache.commons.jexl3 { JexlBuilder }</li>
     * </ul>
     * <p>Notably absent (and therefore denied) are file/IO/persistence/loader-bearing packages such as
     * {@code java.util.zip}, {@code java.util.jar}, {@code java.util.prefs}, {@code java.util.logging},
     * {@code java.util.concurrent.locks}, {@code java.nio.file}, {@code java.lang.reflect},
     * {@code java.lang.invoke} and {@code org.w3c.dom.ls}.</p>
     * <p>A class is visible only when its <em>own</em> package or class declaration permits it; it is never made
     * visible merely because one of its super-types is allowed. Consequently a foreign implementation of an allowed
     * type (for instance a {@code java.util.Map} provided by another library) is not visible unless its own package
     * is explicitly allowed, e.g. {@code RESTRICTED.compose("com.example.foreign +{}")}. Use {@link #logging()} to
     * diagnose which elements are allowed or denied.</p>
     */

    JexlPermissions RESTRICTED = JexlPermissions.parse(
        "# Default Uberspect Permissions",
        "java.math +{}",
        "java.text +{}",
        "java.time +{}",
        "java.time.chrono +{}",
        "java.time.format +{}",
        "java.time.temporal +{}",
        "java.time.zone +{ -ZoneRulesProvider{} }",
        "java.util +{" +
            " -Formatter { Formatter(); }" +
            " -Scanner { Scanner(); }" +
            " -Properties { load(); store(); loadFromXML(); storeToXML(); save(); }" +
            " -ResourceBundle { getBundle(); clearCache(); }" +
            " -PropertyResourceBundle { PropertyResourceBundle(); }" +
            " -ServiceLoader { load(); loadInstalled(); }" +
            " }",
        "java.util.concurrent +{" +
            "-Executors{} -ExecutorService{} -AbstractExecutorService{}" +
            "-ThreadPoolExecutor{} -ScheduledThreadPoolExecutor{} -ScheduledExecutorService{}" +
            "-ForkJoinPool{} -ForkJoinTask{} -ForkJoinWorkerThread{}" +
            "}",
        "java.util.concurrent.atomic +{}",
        "java.util.function +{}",
        "java.util.stream +{}",
        "java.util.regex +{}",
        "org.w3c.dom +{}",
        "java.lang +{" +
            "-Runtime{} -System{} -ProcessBuilder{} -Process{}" +
            "-RuntimePermission{} -SecurityManager{}" +
            "-Thread{} -ThreadGroup{} -Class{} -ClassLoader{}" +
            "-Integer { getInteger(); } -Long { getLong(); } -Boolean { getBoolean(); }" +
            "}",
        "java.io -{ +PrintWriter{ -PrintWriter(); } +Writer{} +StringWriter{} +Reader{} +InputStream{} +OutputStream{} }",
        "java.nio +{}",
        "java.nio.charset +{}",
        "org.apache.commons.jexl3 +{ -JexlBuilder{} -JexlConfigLoader{} }"
    );

    /**
     * An absolute-minimum, allow-list-first permission set.
     * <p>This is the tightest sensible baseline: nothing is reachable unless explicitly whitelisted here.
     * It exposes only the safe {@code java.lang} value types, {@code java.math} big numbers and the
     * {@code java.util} collection types - enough for arithmetic, string and collection scripting.</p>
     * <p>Allowed:</p>
     * <ul>
     * <li>{@code java.lang}: {@code Object} (minus {@code getClass}/{@code wait}/{@code notify}/{@code notifyAll}),
     * {@code Number} and the boxed primitives, {@code String}, {@code CharSequence}, {@code StringBuilder},
     * {@code Math}, {@code Comparable}, {@code Iterable}; everything else in {@code java.lang}
     * (e.g. {@code System}, {@code Runtime}, {@code Thread}, {@code Class}, {@code ClassLoader}) is denied.</li>
     * <li>{@code java.math} (for {@code BigInteger}/{@code BigDecimal}, i.e. the {@code 1B}/{@code 1H} literals).</li>
     * <li>{@code java.util} - the collection types produced by list/map/set literals (and their iterators, views
     * and entries), <em>minus</em> the file/loader/thread-bearing classes which are denied: {@code Formatter} and
     * {@code Scanner} (file I/O), {@code ServiceLoader} and the {@code ResourceBundle} family (class/resource
     * loading), {@code Properties} (file {@code load}/{@code store}) and {@code Timer}/{@code TimerTask} (threads).
     * Because a positive package does not cover sub-packages, {@code java.util.zip}/{@code concurrent}/{@code jar}/…
     * stay denied as well.</li>
     * </ul>
     * <p><strong>Guarantee:</strong> no class that SECURE allows <em>by default</em> can read or write files, read
     * environment variables or system properties, load classes, or start threads. In particular {@code Object.getClass()}
     * is denied (so no {@link Class} can be obtained), and the boxed-type system-property readers
     * {@code Integer.getInteger}, {@code Long.getLong} and {@code Boolean.getBoolean} are denied.</p>
     * <p>SECURE is nonetheless a hardened baseline, <em>not</em> a turnkey sandbox: it is not exhaustive and must not be
     * considered sufficient on its own for executing untrusted user input. For that, define a tailored, strict whitelist
     * of exactly what your scripts need - ideally composed on top of {@link #NONE} - rather than relying on SECURE as-is.</p>
     * <p>Arithmetic, comparisons and string concatenation require no permission at all (they are handled by
     * {@link org.apache.commons.jexl3.JexlArithmetic}); ranges ({@code 1..n}) iterate as a language primitive.
     * Compose more in with {@link #compose(String...)} (e.g. {@code SECURE.compose("java.time +{}")}), and use
     * {@link #logging()} to discover what a script is denied.</p>
     * @since 3.7.0
     */
    JexlPermissions SECURE = JexlPermissions.parse(
        "# Absolute-minimum permissions: safe java.lang value types + java.math + java.util collections",
        "java.lang -{"
            + " +Object{ -getClass(); -wait(); -notify(); -notifyAll(); }"
            + " +Number{} +Boolean{ -getBoolean(); } "
            + " +Character{} +Byte{} +Short{} +Integer{ -getInteger(); } +Long{ -getLong(); } +Float{} +Double{}"
            + " +String{} +CharSequence{} +StringBuilder{} +Math{} +Comparable{} +Iterable{}"
            + " }",
        "java.math +{}",
        "java.util +{"
            + " -Formatter{} -Scanner{} -ServiceLoader{}"
            + " -ResourceBundle{} -PropertyResourceBundle{} -ListResourceBundle{}"
            + " -Properties{} -Timer{} -TimerTask{}"
            + " }"
    );

    /**
     * Parses a set of permissions.
     * <p>
     * In JEXL 3.3, the syntax recognizes 2 types of permissions:
     * </p>
     * <ul>
     * <li>Allowing access to a wildcard restricted set of packages. </li>
     * <li>Denying access to packages, classes (and inner classes), methods and fields</li>
     * </ul>
     * <p>Wildcards specifications determine the set of allowed packages. When empty, all packages can be
     * used. When using JEXL to expose functional elements, their packages should be exposed through wildcards.
     * These allow composing the volume of what is allowed by addition.</p>
     * <p>Restrictions behave exactly like the {@link org.apache.commons.jexl3.annotations.NoJexl} annotation;
     * they can restrict access to package, class, inner-class, methods and fields.
     *  These allow refining the volume of what is allowed by extrusion.</p>
     *  An example of a tight environment that would not allow scripts to wander could be:
     *  <pre>
     *  # allow a very restricted set of base classes
     *  java.math.*
     *  java.text.*
     *  java.util.*
     *  # deny classes that could pose a security risk
     *  java.lang { Runtime {} System {} ProcessBuilder {} Class {} }
     *  org.apache.commons.jexl3 { JexlBuilder {} }
     *  </pre>
     *  <p><b>Syntax Overview:</b></p>
     *  <ul>
     *  <li>Syntax for wildcards is the name of the package suffixed by {@code .*}.</li>
     *  <li>Syntax for restrictions is a list of package restrictions.</li>
     *  <li>A package restriction is a package name followed by a block (as in curly-bracket block {})
     *  that contains a list of class restrictions.</li>
     *  <li>A class restriction is a class name prefixed by an optional {@code -} or {@code +} sign
     *  followed by a block of member restrictions.</li>
     *  <li>A member restriction can be a class restriction - to restrict
     *  nested classes -, a field which is the Java field name suffixed with {@code ;}, a method composed of
     *  its Java name suffixed with {@code ();}. Constructor restrictions are specified like methods using the
     *  class name as method name.</li>
     *  </ul>
     *  <p><b>Negative ({@code -}) vs Positive ({@code +}) Restrictions:</b></p>
     *  <ul>
     *  <li><b>Negative restriction (default or {@code -} prefix)</b>: Explicitly <b>denies</b> access to the members
     *  declared in its block. If the block is empty, the entire class is denied.
     *  <br>Example: {@code java.lang { -System { exit(); } }} denies System.exit() but allows other System methods.
     *  <br>Example: {@code java.lang { Runtime {} }} denies the entire Runtime class (empty block means deny all).</li>
     *  <li><b>Positive restriction ({@code +} prefix)</b>: Explicitly <b>allows only</b> the members declared
     *  in its block, denying all others not listed. If the block is empty, the entire class is allowed.
     *  <br>Example: {@code java.lang { +System { currentTimeMillis(); } }} allows only System.currentTimeMillis(),
     *  denying all other System methods.
     *  <br>Example: {@code java.io -{ +PrintWriter{} +Writer{} }} in the context of a denied java.io package,
     *  allows only PrintWriter and Writer classes entirely (empty blocks mean allow all members).</li>
     *  </ul>
     *  <p>
     *  All overrides and overloads of constructors or methods are allowed or restricted at the same time,
     *  the restriction being based on their names, not their whole signature. This differs from the @NoJexl annotation.
     *  </p>
     *  <p><b>Complete Example:</b></p>
     *  <pre>
     *  # some wildcards
     *  java.util.* # java.util is pretty much a must-have
     *  my.allowed.package0.*
     *  another.allowed.package1.*
     *  # nojexl like restrictions
     *  my.package.internal {} # the whole package is hidden
     *  my.package {
     *   +class4 { theMethod(); } # POSITIVE: only theMethod can be called in class4, all others denied
     *   class0 {
     *     class1 {} # NEGATIVE (default): the whole class1 is hidden
     *     class2 {
     *         class2(); # class2 constructors cannot be invoked
     *         class3 {
     *             aMethod(); # aMethod cannot be called
     *             aField; # aField cannot be accessed
     *         }
     *     } # end of class2
     *     class0(); # class0 constructors cannot be invoked
     *     method(); # method cannot be called
     *     field; # field cannot be accessed
     *   } # end class0
     * } # end package my.package
     * </pre>
     *
     * @param src The permissions source, the default (NoJexl aware) permissions if null
     * @return The permissions instance
     * @since 3.3
     */
    static JexlPermissions parse(final String... src) {
        return new PermissionsParser().parse(src);
    }

    /**
     * Creates a permission set from scratch: everything is denied unless a rule explicitly allows it.
     * <p>Equivalent to composing the rules onto {@link #NONE}. Use positive declarations - for instance
     * {@code "java.lang { +String{} }"} or {@code "java.util.*"} - to grant access; {@code create()} with no
     * rules denies everything. This differs from {@link #parse(String...)}, whose empty form
     * ({@link #UNRESTRICTED}) allows everything.</p>
     *
     * @param rules The permission DSL declarations
     * @return The closed-world permission set
     * @since 3.7.0
     */
    static JexlPermissions create(final String... rules) {
        return NONE.compose(rules);
    }

    /**
     * Wraps these permissions in a {@link LoggingPermissions} that logs every allow/deny decision.
     * <p>Useful to discover which reflective elements a permission set allows or denies.</p>
     *
     * @return A logging view of these permissions
     * @since 3.7.0
     */
    default JexlPermissions logging() {
        return new LoggingPermissions(this);
    }

    /**
     * Wraps these permissions in a {@link LoggingPermissions} that logs every allow/deny decision
     * to a named logger.
     *
     * @param loggerName The name of the logger to log decisions to
     * @return A logging view of these permissions
     * @since 3.7.0
     */
    default JexlPermissions logging(final String loggerName) {
        return new LoggingPermissions(loggerName, this);
    }

    /**
     * Wraps these permissions in a {@link LoggingPermissions} that logs every allow/deny decision
     * to the given logger.
     *
     * @param log The logger to log decisions to
     * @return A logging view of these permissions
     * @since 3.7.0
     */
    default JexlPermissions logging(final Log log) {
        return new LoggingPermissions(log, this);
    }

    /**
     * Checks whether a class allows JEXL introspection.
     * <p>If the class disallows JEXL introspection, none of its constructors, methods or fields
     * as well as derived classes are visible to JEXL and cannot be used in scripts or expressions.
     * If one of its super-classes is not allowed, tbe class is not allowed either.</p>
     * <p>For interfaces, only methods and fields are disallowed in derived interfaces or implementing classes.</p>
     *
     * @param clazz The class to check
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.3
     */
    boolean allow(Class<?> clazz);

    /**
     * Checks whether a constructor allows JEXL introspection.
     * <p>If a constructor is not allowed, the new operator cannot be used to instantiate its declared class
     * in scripts or expressions.</p>
     *
     * @param ctor The constructor to check
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.3
     */
    boolean allow(Constructor<?> ctor);

    /**
     * Checks whether a field explicitly allows JEXL introspection.
     * <p>If a field is not allowed, it cannot be resolved and accessed in scripts or expressions.</p>
     *
     * @param field The field to check
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.3
     */
    boolean allow(Field field);

    /**
     * Checks whether a field explicitly allows JEXL introspection.
     * <p>If a field is not allowed, it cannot be resolved and accessed in scripts or expressions.</p>
     * @param clazz The class from which the field is accessed, used to check that the field is allowed for this class
     * @param field The field to check
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.6.3
   */
    default boolean allow(Class<?> clazz, Field field) {
      return allow(field);
    }

    /**
     * Checks whether a method allows JEXL introspection.
     * <p>If a method is not allowed, it cannot be resolved and called in scripts or expressions.</p>
     * <p>Since methods can be overridden and overloaded, this also checks that no superclass or interface
     * explicitly disallows this method.</p>
     *
     * @param method The method to check
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.3
     */
    boolean allow(Method method);

    /**
     * Checks whether a method allows JEXL introspection.
     * <p>If a method is not allowed, it cannot be resolved and called in scripts or expressions.</p>
     * <p>Since methods can be overridden and overloaded, this checks that this class explicitly allows
     * this method - superseding any superclass or interface specified permissions.</p>
     *
     * @param clazz The class from which the method is accessed, used to check that the method is allowed for this class
     * @param method The method to check
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.6.3
     */
    default boolean allow(Class<?> clazz, Method method) {
      return allow(method);
    }

    /**
     * Checks whether a package allows JEXL introspection.
     * <p>If the package disallows JEXL introspection, none of its classes or interfaces are visible
     * to JEXL and cannot be used in scripts or expression.</p>
     *
     * @param pack The package
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.3
     */
    boolean allow(Package pack);

    /**
     * Compose these permissions with a new set.
     * <p>This is a convenience method meant to easily give access to the packages JEXL is
     * used to integrate with. For instance, using <code>{@link #RESTRICTED}.compose("com.my.app.*")</code>
     * would extend the restricted set of permissions by allowing the com.my.app package.</p>
     *
     * @param src The new constraints
     * @return The new permissions
     */
    JexlPermissions compose(String... src);

    /**
     * Checks that a class is valid for permission check.
     *
     * @param clazz The class
     * @return true if the class is not null, false otherwise
     */
    default boolean validate(final Class<?> clazz) {
        return clazz != null;
    }

    /**
     * Checks that a constructor is valid for permission check.
     *
     * @param constructor The constructor
     * @return true if constructor is not null and public, false otherwise
     */
    default boolean validate(final Constructor<?> constructor) {
        return constructor != null && Modifier.isPublic(constructor.getModifiers());
    }

    /**
     * Checks that a field is valid for permission check.
     *
     * @param field The constructor
     * @return true if field is not null and public, false otherwise
     */
    default boolean validate(final Field field) {
        return field != null && Modifier.isPublic(field.getModifiers());
    }

    /**
     * Checks that a method is valid for permission check.
     *
     * @param method The method
     * @return true if method is not null and public, false otherwise
     */
    default boolean validate(final Method method) {
        return method != null && Modifier.isPublic(method.getModifiers());
    }

    /**
     * Checks that a package is valid for permission check.
     *
     * @param pack The package
     * @return true if the class is not null, false otherwise
     */
    default boolean validate(final Package pack) {
        return pack != null;
    }
}
