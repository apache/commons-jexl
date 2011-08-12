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
package org.apache.commons.jexl2.introspection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A sandbox describes permissions on a class by explicitly allowing or forbidding access to methods and properties
 * through "whitelists" and "blacklists".
 * <p>
 * A <b>whitelist</b> explicitly allows methods/properties for a class;
 * <ul>
 * <li>
 * If a whitelist is empty and thus does not contain any names, all properties/methods are allowed for its class.
 * </li>
 * <li>
 * If it is not empty, the only allowed properties/methods are the ones contained.
 * </li>
 * </ul>
 * </p>
 * <p>
 * A <b>blacklist</b> explicitly forbids methods/properties for a class;
 * <ul>
 * <li>
 * If a blacklist is empty and thus does not contain any names, all properties/methods are forbidden for its class.
 * </li>
 * <li>
 * If it is not empty, the only forbidden properties/methods are the ones contained.
 * </li>
 * </ul>
 * <p>
 * Permissions are composed of three lists, read, write, execute, each being "white" or "black":
 * <ul>
 * <li><b>read</b> controls readable properties </li>
 * <li><b>write</b> controls writeable properties</li>
 * <li><b>execute</b> controls executable methods and constructor</li>
 * </ul>
 * </p>
 * 
 * @since 2.1
 */
public class Sandbox {
    /**
     * The map from class names to permissions.
     */
    private final Map<String, Permissions> sandbox;
    
    /**
     * Creates a new default sandbox.
     */
    public Sandbox() {
        sandbox = new HashMap<String, Permissions>();
    }

    /**
     * Creates a sandbox based on an existing permissions map.
     * @param map the permissions map
     */
    private Sandbox(Map<String, Permissions> map) {
        sandbox = map;
    }

    /**
     * A base set of names.
     */
    public abstract static class Names {
        /**
         * Adds a name to this set.
         * @param name the name to add
         * @return  true if the name was really added, false if it was already present
         */
        public abstract boolean add(String name);
        
        /**
         * Adds an alias to a name to this set.
         * <p>This only has an effect on white lists.</p>
         * @param name the name to alias
         * @param alias the alias
         * @return  true if the alias was added, false if it was already present
         */
        public boolean alias(String name, String alias) {
            return false;
        }

        /**
         * Whether a given name is allowed or not.
         * @param name the method/property name to check
         * @return null if not allowed, the actual name to use otherwise
         */
        public abstract String get(String name);
    }

    /**
     * A white set of names.
     */
    public static class WhiteSet extends Names {
        /** The set of controlled names. */
        protected Map<String,String> names = null;
        
        @Override
        public boolean add(String name) {
            if (names == null) {
                names = new HashMap<String,String>();
            }
            return names.put(name,name) == null;
        }
        
        @Override
        public boolean alias(String name, String alias) {
            if (names == null) {
                names = new HashMap<String,String>();
            }
            return names.put(alias,name) == null;
        }
                
        @Override
        public String get(String name) {
            if (names == null) {
                return name;
            } else {
                return names.get(name);
            }
        }
    }

    /**
     * A black set of names.
     */
    public static class BlackSet extends Names {
        /** The set of controlled names. */
        protected Set<String> names = null;
        
        @Override
        public boolean add(String name) {
            if (names == null) {
                names = new HashSet<String>();
            }
            return names.add(name);
        }

        @Override
        public String get(String name) {
            return names != null && !names.contains(name)? name : null;
        }
    }

    /**
     * Contains the white or black lists for properties and methods for a given class.
     */
    public static class Permissions {
        /** The controlled readable properties. */
        private final Names read;
        /** The controlled  writeable properties. */
        private final Names write;
        /** The controlled methods. */
        private final Names execute;

        /**
         * Creates a new permissions instance.
         * @param readFlag whether the read property list is white or black
         * @param writeFlag whether the write property list is white or black
         * @param executeFlag whether the method list is white of black
         */
        Permissions(boolean readFlag, boolean writeFlag, boolean executeFlag) {
            this.read = readFlag ? new WhiteSet() : new BlackSet();
            this.write = writeFlag ? new WhiteSet() : new BlackSet();
            this.execute = executeFlag ? new WhiteSet() : new BlackSet();
        }

        /**
         * Adds a list of readable property names to these permissions.
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
         * Adds a list of writeable property names to these permissions.
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
         * @return the set of property names
         */
        public Names read() {
            return read;
        }
        
        /**
         * Gets the set of writeable property names in these permissions.
         * @return the set of property names
         */
        public Names write() {
            return write;
        }
        
        /**
         * Gets the set of method names in these permissions.
         * @return the set of method names
         */
        public Names execute() {
            return execute;
        }

    }

    /**
     * Creates the set of permissions for a given class.
     * @param clazz the class for which these permissions apply
     * @param readFlag whether the readable property list is white or black
     * @param writeFlag whether the writeable property list is white or black
     * @param executeFlag whether the executable method list is white or black
     * @return the 
     */
    public Permissions permissions(String clazz, boolean readFlag, boolean writeFlag, boolean executeFlag) {
        Permissions box = new Permissions(readFlag, writeFlag, executeFlag);
        sandbox.put(clazz, box);
        return box;
    }

    /**
     * Creates a new set of permissions based on white lists for methods and properties for a given class.
     * @param clazz the whitened class name
     * @return the permissions instance
     */
    public Permissions white(String clazz) {
        return permissions(clazz, true, true, true);
    }

    /**
     * Creates a new set of permissions based on black lists for methods and properties for a given class.
     * @param clazz the blackened class name
     * @return the permissions instance
     */
    public Permissions black(String clazz) {
        return permissions(clazz, false, false, false);
    }

    /**
     * Gets the set of permissions associated to a class.
     * @param clazz the class name
     * @return the permissions or null if none were defined
     */
    public Permissions get(String clazz) {
        return sandbox.get(clazz);
    }
}
