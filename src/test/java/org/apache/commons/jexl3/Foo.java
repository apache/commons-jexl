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
package org.apache.commons.jexl3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A simple bean used for testing purposes
 *
 * @since 1.0
 */
public class Foo {

    public class Cheezy {
        public Iterator<String> iterator() {
            return getCheeseList().iterator();
        }
    }
    private boolean beenModified;
    private String property1 = "some value";
    public Foo() {}

    public String bar()
    {
        return JexlTest.METHOD_STRING;
    }

    public String convertBoolean(final boolean b)
    {
        return "Boolean : " + b;
    }

    public String[] getArray()
    {
        return ArrayAccessTest.GET_METHOD_ARRAY;
    }

    public String[][] getArray2()
    {
        return ArrayAccessTest.GET_METHOD_ARRAY2;
    }

    public String getBar()
    {
        return JexlTest.GET_METHOD_STRING;
    }

    public List<String> getCheeseList()
    {
        final ArrayList<String> answer = new ArrayList<>();
        answer.add("cheddar");
        answer.add("edam");
        answer.add("brie");
        return answer;
    }

    public Cheezy getCheezy()
    {
        return new Cheezy();
    }

    public int getCount() {
        return 5;
    }

    public Foo getInnerFoo()
    {
        return new Foo();
    }

    public boolean getModified()
    {
        return beenModified;
    }

    public String getProperty1() {
        return property1;
    }

    public String getQuux() {
        return "String : quux";
    }

    public int getSize()
    {
        return 22;
    }

    public boolean getTrueAndModify()
    {
        beenModified = true;
        return true;
    }

    public boolean isSimple()
    {
        return true;
    }

    public String repeat(final String str) {
        return "Repeat : " + str;
    }

    public void setProperty1(final String newValue) {
        property1 = newValue;
    }

    public int square(final int value)
    {
        return value * value;
    }
}
