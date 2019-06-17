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

package org.apache.commons.jexl3.introspection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A sandbox describes permissions on a class by explicitly allowing or forbidding
 * access to methods and properties through "whitelists" and "blacklists".
 *
 * <p>A <b>whitelist</b> explicitly allows methods/properties for a class;</p>
 *
 * <ul>
 *   <li>If a whitelist is empty and thus does not contain any names,
 *       all properties/methods are allowed for its class.</li>
 *   <li>If it is not empty, the only allowed properties/methods are the ones contained.</li>
 * </ul>
 *
 * <p>A <b>blacklist</b> explicitly forbids methods/properties for a class;</p>
 *
 * <ul>
 *   <li>If a blacklist is empty and thus does not contain any names,
 *       all properties/methods are forbidden for its class.</li>
 *   <li>If it is not empty, the only forbidden properties/methods are the ones contained.</li>
 * </ul>
 *
 * <p>Permissions are composed of three lists, read, write, execute, each being
 * "white" or "black":</p>
 *
 * <ul>
 *   <li><b>read</b> controls readable properties </li>
 *   <li><b>write</b> controls writable properties</li>
 *   <li><b>execute</b> controls executable methods and constructor</li>
 * </ul>
 * 
 * <p>When specified, permissions - white or black lists - can be created inheritable
 * on interfaces or classes and thus applicable to their implementations or derived
 * classes; the sandbox must be created with the 'inheritable' flag for this behavior
 * to be triggered. Note that even in this configuration, it is still possible to
 * add non-inheritable permissions.
 * Adding inheritable lists to a non inheritable sandbox has no added effect;
 * permissions only apply to their specified class.</p>
 *
 * <p>Note that a JexlUberspect always uses a <em>copy</em> of the JexlSandbox
 * used to built it preventing permission changes after its instantiation.</p>
 *
 * @since 3.0
 */
public final class JexlSandbox {
    /**
     * The map from class names to permissions.
     */
    private final Map<String, Permissions> sandbox;
    /**
     * Whether permissions can be inherited (through implementation or extension).
     */
    private final boolean inherit;
    /**
     * Default behavior, black or white.
     */
    private final boolean white;

    /**
     * Creates a new default sandbox.
     * <p>In the absence of explicit permissions on a class, the
     * sandbox is a white-box, white-listing that class for all permissions (read, write and execute).
     */
    public JexlSandbox() {
        this(true, false, null);
    }

    /**
     * Creates a new default sandbox.
     * <p>A white-box considers no permissions as &quot;everything is allowed&quot; when
     * a black-box considers no permissions as &quot;nothing is allowed&quot;.
     * @param wb whether this sandbox is white (true) or black (false)
     * if no permission is explicitly defined for a class.
     * @since 3.1
     */
    public JexlSandbox(boolean wb) {
        this(wb, false, null);
    }
    
    /**
     * Creates a sandbox.
     * @param wb whether this sandbox is white (true) or black (false)
     * @param inh whether permissions on interfaces and classes are inherited (true) or not (false)
     * @since 3.2
     */
    public JexlSandbox(boolean wb, boolean inh) {
        this(wb, inh, null);
    }

    /**
     * Creates a sandbox based on an existing permissions map.
     * @param map the permissions map
     */
    @Deprecated
    protected JexlSandbox(Map<String, Permissions> map) {
        this(true, false, map);
    }

    /**
     * Creates a sandbox based on an existing permissions map.
     * @param wb whether this sandbox is white (true) or black (false)
     * @param map the permissions map
     * @since 3.1
     */
    @Deprecated
    protected JexlSandbox(boolean wb, Map<String, Permissions> map) {
        this(wb, false, map);
    }

    /**
     * Creates a sandbox based on an existing permissions map.
     * @param wb whether this sandbox is white (true) or black (false)
     * @param inh whether permissions are inherited, default false
     * @param map the permissions map
     * @since 3.2
     */
    protected JexlSandbox(boolean wb, boolean inh, Map<String, Permissions> map) {
        white = wb;
        inherit = inh;
        sandbox = map != null? map : new HashMap<String, Permissions>();
    }

    /**
     * @return a copy of this sandbox
     */
    public JexlSandbox copy() {
        // modified concurently at runtime so...
        Map<String, Permissions> map = new ConcurrentHashMap<String, Permissions>();
        for (Map.Entry<String, Permissions> entry : sandbox.entrySet()) {
            map.put(entry.getKey(), entry.getValue().copy());
        }
        return new JexlSandbox(white, inherit, map);
    }

    /**
     * Gets a class by name, crude mechanism for backwards (&lt;3.2 ) compatibility.
     * @param cname the class name
     * @return the class
     */
    static Class<?> forName(String cname) {
        try {
            return Class.forName(cname);
        } catch(Exception xany) {
            return null;
        }
    }

    /**
     * Gets the read permission value for a given property of a class.
     *
     * @param clazz the class
     * @param name the property name
     * @return null if not allowed, the name of the property to use otherwise
     */
    public String read(Class<?> clazz, String name) {
        return get(clazz).read().get(name);
    }

    /**
     * Gets the read permission value for a given property of a class.
     *
     * @param clazz the class name
     * @param name the property name
     * @return null if not allowed, the name of the property to use otherwise
     */
    @Deprecated
    public String read(String clazz, String name) {
        return get(clazz).read().get(name);
    }

    /**
     * Gets the write permission value for a given property of a class.
     *
     * @param clazz the class
     * @param name the property name
     * @return null if not allowed, the name of the property to use otherwise
     */
    public String write(Class<?> clazz, String name) {
        return get(clazz).write().get(name);
    }

    /**
     * Gets the write permission value for a given property of a class.
     *
     * @param clazz the class name
     * @param name the property name
     * @return null if not allowed, the name of the property to use otherwise
     */
    @Deprecated
    public String write(String clazz, String name) {
        return get(clazz).write().get(name);
    }

    /**
     * Gets the execute permission value for a given method of a class.
     *
     * @param clazz the class
     * @param name the method name
     * @return null if not allowed, the name of the method to use otherwise
     */
    public String execute(Class<?> clazz, String name) {
        String m = get(clazz).execute().get(name);
        return "".equals(name) && m != null? clazz.getName() : m;
    }

    /**
     * Gets the execute permission value for a given method of a class.
     *
     * @param clazz the class name
     * @param name the method name
     * @return null if not allowed, the name of the method to use otherwise
     */
    @Deprecated
    public String execute(String clazz, String name) {
        String m = get(clazz).execute().get(name);
        return "".equals(name) && m != null? clazz : m;
    }

    /**
     * A base set of names.
     */
    public abstract static class Names {

        /**
         * Adds a name to this set.
         *
         * @param name the name to add
         * @return  true if the name was really added, false if not
         */
        public abstract boolean add(String name);

        /**
         * Adds an alias to a name to this set.
         * <p>This only has an effect on white lists.</p>
         *
         * @param name the name to alias
         * @param alias the alias
         * @return  true if the alias was added, false if it was already present
         */
        public boolean alias(String name, String alias) {
            return false;
        }

        /**
         * Whether a given name is allowed or not.
         *
         * @param name the method/property name to check
         * @return null if not allowed, the actual name to use otherwise
         */
        public String get(String name) {
            return name;
        }

        /**
         * @return a copy of these Names
         */
        protected Names copy() {
            return this;
        }
    }

    /**
     * The pass-thru name set.
     */
    private static final Names WHITE_NAMES = new Names() {
        @Override
        public boolean add(String name) {
            return false;
        }

        @Override
        protected Names copy() {
            return this;
        }
    };

    /**
     * The block-all name set.
     */
    private static final Names BLACK_NAMES = new Names() {
        @Override
        public boolean add(String name) {
            return false;
        }

        @Override
        protected Names copy() {
            return this;
        }

        @Override
        public String get(String name) {
            return null;
        }
    };

    /**
     * A white set of names.
     */
    public static final class WhiteSet extends Names {
        /** The map of controlled names and aliases. */
        private Map<String, String> names = null;

        @Override
        protected Names copy() {
            WhiteSet copy = new WhiteSet();
            copy.names = names == null ? null : new HashMap<String, String>(names);
            return copy;
        }

        @Override
        public boolean add(String name) {
            if (names == null) {
                names = new HashMap<String, String>();
            }
            return names.put(name, name) == null;
        }

        @Override
        public boolean alias(String name, String alias) {
            if (names == null) {
                names = new HashMap<String, String>();
            }
            return names.put(alias, name) == null;
        }

        @Override
        public String get(String name) {
            return names == null ? name : names.get(name);
        }
    }

    /**
     * A black set of names.
     */
    public static final class BlackSet extends Names {
        /** The set of controlled names. */
        private Set<String> names = null;

        @Override
        protected Names copy() {
            BlackSet copy = new BlackSet();
            copy.names = names == null ? null : new HashSet<String>(names);
            return copy;
        }

        @Override
        public boolean add(String name) {
            if (names == null) {
                names = new HashSet<String>();
            }
            return names.add(name);
        }

        @Override
        public String get(String name) {
            return names != null && !names.contains(name) ? name : null;
        }
    }

    /**
     * Contains the white or black lists for properties and methods for a given class.
     */
    public static final class Permissions {
        /** Whether these permissions are inheritable, ie can be used by derived classes. */
        private final boolean inheritable;
        /** The controlled readable properties. */
        private final Names read;
        /** The controlled  writable properties. */
        private final Names write;
        /** The controlled methods. */
        private final Names execute;

        /**
         * Creates a new permissions instance.
         *
         * @param inherit whether these permissions are inheritable
         * @param readFlag whether the read property list is white or black
         * @param writeFlag whether the write property list is white or black
         * @param executeFlag whether the method list is white of black
         */
        Permissions(boolean inherit, boolean readFlag, boolean writeFlag, boolean executeFlag) {
            this(inherit,
                    readFlag ? new WhiteSet() : new BlackSet(),
                    writeFlag ? new WhiteSet() : new BlackSet(),
                    executeFlag ? new WhiteSet() : new BlackSet());
        }

        /**
         * Creates a new permissions instance.
         *
         * @param inherit whether these permissions are inheritable
         * @param nread the read set
         * @param nwrite the write set
         * @param nexecute the method set
         */
        Permissions(boolean inherit, Names nread, Names nwrite, Names nexecute) {
            this.read = nread != null ? nread : WHITE_NAMES;
            this.write = nwrite != null ? nwrite : WHITE_NAMES;
            this.execute = nexecute != null ? nexecute : WHITE_NAMES;
            this.inheritable = inherit;
        }

        /**
         * @return a copy of these permissions
         */
        Permissions copy() {
            return new Permissions(inheritable, read.copy(), write.copy(), execute.copy());
        }

        /**
         * @return whether these permissions applies to derived classes.
         */
        public boolean isInheritable() {
            return inheritable;
        }

        /**
         * Adds a list of readable property names to these permissions.
         *
         * @param pnames the property names
         * @return this instance of permissions
         */
        public Permissions read(String... pnames) {
            for (String pname : pnames) {
                read.add(pname);
            }
            return this;
        }

        /**
         * Adds a list of writable property names to these permissions.
         *
         * @param pnames the property names
         * @return this instance of permissions
         */
        public Permissions write(String... pnames) {
            for (String pname : pnames) {
                write.add(pname);
            }
            return this;
        }

        /**
         * Adds a list of executable methods names to these permissions.
         * <p>The constructor is denoted as the empty-string, all other methods by their names.</p>
         *
         * @param mnames the method names
         * @return this instance of permissions
         */
        public Permissions execute(String... mnames) {
            for (String mname : mnames) {
                execute.add(mname);
            }
            return this;
        }

        /**
         * Gets the set of readable property names in these permissions.
         *
         * @return the set of property names
         */
        public Names read() {
            return read;
        }

        /**
         * Gets the set of writable property names in these permissions.
         *
         * @return the set of property names
         */
        public Names write() {
            return write;
        }

        /**
         * Gets the set of method names in these permissions.
         *
         * @return the set of method names
         */
        public Names execute() {
            return execute;
        }
    }

    /**
     * The pass-thru permissions.
     */
    private static final Permissions ALL_WHITE = new Permissions(false, WHITE_NAMES, WHITE_NAMES, WHITE_NAMES);
    /**
     * The block-all permissions.
     */
    private static final Permissions ALL_BLACK = new Permissions(false, BLACK_NAMES, BLACK_NAMES, BLACK_NAMES);

    /**
     * Creates the set of permissions for a given class.
     * <p>The sandbox inheritance property will apply to the permissions created by this method
     *
     * @param clazz the class for which these permissions apply
     * @param readFlag whether the readable property list is white - true - or black - false -
     * @param writeFlag whether the writable property list is white - true - or black - false -
     * @param executeFlag whether the executable method list is white white - true - or black - false -
     * @return the set of permissions
     */
    public Permissions permissions(String clazz, boolean readFlag, boolean writeFlag, boolean executeFlag) {
        return permissions(clazz, inherit, readFlag, writeFlag, executeFlag);
    }
        
    /**
     * Creates the set of permissions for a given class.
     *
     * @param clazz the class for which these permissions apply
     * @param inhf whether these permissions are inheritable
     * @param readf whether the readable property list is white - true - or black - false -
     * @param writef whether the writable property list is white - true - or black - false -
     * @param execf whether the executable method list is white white - true - or black - false -
     * @return the set of permissions
     */
    public Permissions permissions(String clazz, boolean inhf,  boolean readf, boolean writef, boolean execf) {
        Permissions box = new Permissions(inhf, readf, writef, execf);
        sandbox.put(clazz, box);
        return box;
    }

    /**
     * Creates a new set of permissions based on white lists for methods and properties for a given class.
     * <p>The sandbox inheritance property will apply to the permissions created by this method
     * 
     * @param clazz the whitened class name
     * @return the permissions instance
     */
    public Permissions white(String clazz) {
        return permissions(clazz, true, true, true);
    }

    /**
     * Creates a new set of permissions based on black lists for methods and properties for a given class.
     * <p>The sandbox inheritance property will apply to the permissions created by this method
     *
     * @param clazz the blackened class name
     * @return the permissions instance
     */
    public Permissions black(String clazz) {
        return permissions(clazz, false, false, false);
    }

    /**
     * Gets the set of permissions associated to a class.
     *
     * @param clazz the class name
     * @return the defined permissions or an all-white permission instance if none were defined
     */
    public Permissions get(String clazz) {
        if (inherit) {
            return get(forName(clazz));
        }
        Permissions permissions = sandbox.get(clazz);
        if (permissions == null) {
            return white ? ALL_WHITE : ALL_BLACK;
        } else {
            return permissions;
        }
    }

    /**
     * Get the permissions associated to a class.
     * @param clazz the class
     * @return the permissions
     */
    @SuppressWarnings("null") // clazz can not be null since permissions would be not null and black;
    public Permissions get(Class<?> clazz) {
        Permissions permissions = clazz == null ? ALL_BLACK : sandbox.get(clazz.getName());
        if (permissions == null) {
            if (inherit) {
                // find first inherited interface that defines permissions
                for (Class<?> inter : clazz.getInterfaces()) {
                    permissions = sandbox.get(inter.getName());
                    if (permissions != null && permissions.isInheritable()) {
                        break;
                    }
                }
                // nothing defined yet, find first superclass that defines permissions
                if (permissions == null) {
                    // lets walk all super classes
                    Class<?> zuper = clazz.getSuperclass();
                    // walk all superclasses
                    while (zuper != null) {
                        permissions = sandbox.get(zuper.getName());
                        if (permissions != null && permissions.isInheritable()) {
                            break;
                        }
                        zuper = zuper.getSuperclass();
                    }
                }
                // nothing was inheritable
                if (permissions == null) {
                    permissions = white ? ALL_WHITE : ALL_BLACK;
                }
                // store the info to avoid doing this costly look up
                sandbox.put(clazz.getName(), permissions);
            } else {
                permissions = white ? ALL_WHITE : ALL_BLACK;
            }
        }
        return permissions;
    }

}
