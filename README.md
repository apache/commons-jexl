<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
Apache Commons JEXL Pro
=======================

The Apache Commons JEXL Pro library is an experimental fork of the The Apache Commons JEXL library.

Idea of the fork
----------------
The fork is intended to be source compatible with the latest JEXL version (3.2-SNAPSHOT), but provides some 
enhancements and changes to the capabilities of the scripting language. 

I have no intention of promoting this fork as an alternative implementation, and I would be happy to have all 
the changes to be backported to the base JEXL library one day, but the decision whether these changes are the ones 
the JEXL community would benefit from remains at the descretion of the Apache JEXL team.

Language Compatibility 
----------------------
The library tends to maintain as much syntax compatibility with the original syntax as possible, but there are
some changes that may break your existing scripts. The main reason for this comes from the introduction of the new 
reserved words to support new syntax constructs, so your variables may no longer be named by one of those that were introduced. 
There are also some minor tweaks to the original syntax in order to clarify language structure and align language 
constructs with other popular scripting languages, to minimize the learning curve. 
These changes are all reflected in the documentation, but the breef summary is given below.

Incompatible changes
--------------------
+ Java 8 is the new minimum supported version 

+ New reserved words are introduced. Those are:
  `switch` `case` `default` `try` `catch` `finally` `throw` `synchronized` `this` `instanceof` `in` `remove`.
  You may not longer use them as the names of the variables and methods. The exception is made to the `remove` identifer,
  as it is still may be used in method invocations.

+ Pragmas can only be defined at the beginning of the script. The reason is the pragmas are not executable constructs, 
  so it is pointless and misleading to have them incorporated in flow-control structures somewhere in the middle.

+ Literal `null` can no longer have any properies, so it is forbidden to use it in expressions like `null.prop`.
  If, for some reason, you still want to do this, use parentheses like `(null).prop`.

+ Precedence of the `range` operator (`..`) is changed to be higher than that of relational operators, 
  but lower than that of arithmetic operators.

+ Precedence of the match(`=~`) and not-match(`!~`) operators is changed to be that of equality operators.

+ Passing a lambda more arguments than is specified in the lambda declaration now results in error in a strict mode

New features
------------
+ Java-like `switch` statement is introduced

+ Java-like `synchronized` statement is introduced

+ Java-like `try-with-resources` statement is introduced

+ Java-like `try-catch-finally` statement is introduced

+ Java-like `throw` statement is introduced

+ Java-like `for` classical loop statement is introduced

+ Java-like `assert` statement is introduced

+ New `remove` flow-control statement is introduced

+ New `this` literal is introduced to allow easier access to the current evaluation context

+ Java-like `<<`,`>>`,`>>>` bitwise shift operators are introduced. 

+ Java-like `<<=`,`>>=`,`>>>=` self-assignment operators are introduced. 

+ Java-like `++` and `--` increment/decrement operators are introduced. Prefix and postfix forms are supported.

+ Java-like `instanceof` operator is introduced

+ Java-like `+` unary promotion operator is introduced.

+ Java-like `()` type-cast operator is introduced.

+ Javascript-like `===` and `!==` identity operators are introduced

+ C-like `&` pointer and `*` pointer dereference operators are introduced

+ New iterator `...` operator is introduced

+ New iterator processing (selection/projection/reduction) operators are introduced

+ New multiple assignment statement is introduced

+ New inline property assignment `a{b:3,c:4}` construct is introduced

Enhancements
------------
+ Labeled blocks and statements like `switch`, `for`, `while`, `do` can be used. 
  The defined labels can be further specified for inner `break`, `continue` and `remove` flow-control statements

+ Multidimensional arrays can be accessed by using new syntax `arr[x,y]` as well as by using older syntax `arr[x][y]`

+ Single expression lambdas can be defined by using `=>` fat arrow operator

+ Variable argument lambdas can be defined by using `...` syntax after the last lambda argument

+ Local variables can be declared using java primitive type `int i = 0`

+ Last part of the ternary expression (along with the separating `:`) can be omitted, implying `null` as a result

+ Pattern matching operators `=~` and `!~` can use new `in` and `!in` aliases 

+ Operator `new` can use Java-like syntax `new String()`

+ Foreach statement may also define additional `counter` variable along with current variable

+ Immutable arraylist `#[1,2,3]` literal constructs can be used 

+ Immutable set `#{1,2,3}` literal constructs can be used

+ Immutable map `#{1:2,3:4}` literal constructs can be used

+ Array comprehensions `[...a]` can be used in array literals

+ Set comprehensions `{...a}` can be used in set literals

+ Map comprehensions `{*:...a}` can be used in map literals

+ Function argument comprehensions `func(...a)` can be used 

+ Corresponding unicode characters may be used for the operators like `!=`, `>=` etc

License
-------
This code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0).

See the `NOTICE.txt` file for required notices and attributions.
