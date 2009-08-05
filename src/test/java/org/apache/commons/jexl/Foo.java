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
package org.apache.commons.jexl;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple bean used for testing purposes
 * 
 * @since 1.0
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Revision$
 */
public class Foo {
    
    private boolean beenModified = false;
    private String property1 = "some value";
    public Foo() {}
    
    public String bar()
    {
        return JexlTest.METHOD_STRING;
    }

    public String getBar()
    {
        return JexlTest.GET_METHOD_STRING;
    }

    public Foo getInnerFoo()
    {
        return new Foo();
    }

    public String get(String arg)
    {
        return "Repeat : " + arg;
    }

    public String convertBoolean(boolean b)
    {
        return "Boolean : " + b;
    }

    public int getCount() {
        return 5;
    }

    public List<String> getCheeseList()
    {
        ArrayList<String> answer = new ArrayList<String>();
        answer.add("cheddar");
        answer.add("edam");
        answer.add("brie");
        return answer;
    }

    public String[] getArray()
    {
        return ArrayAccessTest.GET_METHOD_ARRAY;
    }

    public String[][] getArray2()
    {
        return ArrayAccessTest.GET_METHOD_ARRAY2;
    }

    public boolean isSimple()
    {
        return true;
    }

    public int square(int value)
    {
        return value * value;
    }

    public boolean getTrueAndModify()
    {
        beenModified = true;
        return true;
    }

    public boolean getModified()
    {
        return beenModified;
    }


    public int getSize()
    {
        return 22;
    }
    
    public String getProperty1() {
        return property1;
    }

    public void setProperty1(String newValue) {
        property1 = newValue;
    }
}
