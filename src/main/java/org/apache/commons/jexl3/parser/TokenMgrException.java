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
package org.apache.commons.jexl3.parser;

/**
 * Token Manager Error.
 */
public class TokenMgrException extends RuntimeException implements JavaccError {
    /**
     * The version identifier for this Serializable class.
     * Increment only if the <i>serialized</i> form of the
     * class changes.
     */
    private static final long serialVersionUID = 1L;

    /*
     * Ordinals for various reasons why an Error of this type can be thrown.
     */
    /**
     * Lexical error occurred.
     */
    public static final int LEXICAL_ERROR = 0;
    /**
     * An attempt was made to create a second instance of a static token manager.
     */
    public static final int STATIC_LEXER_ERROR = 1;
    /**
     * Tried to change to an invalid lexical state.
     */
    public static final int INVALID_LEXICAL_STATE = 2;
    /**
     * Detected (and bailed out of) an infinite loop in the token manager.
     */
    public static final int LOOP_DETECTED = 3;
    /**
     * Indicates the reason why the exception is thrown. It will have
     * one of the above 4 values.
     */
    private final int errorCode;
    /**
     * The lexer state.
     */
    @SuppressWarnings("unused") // not read currently
    private int state;
    /**
     * The current character.
     */
    private char current;
    /**
     * Last correct input before error occurs.
     */
    private String after;
    /**
     * Whether eof was reached whilst expecting more input.
     */
    private boolean eof;
    /**
     * Error line.
     */
    private int line;
    /**
     * Error column.
     */
    private int column;

    /**
     * Returns a detailed message for the Error when it is thrown by the
     * token manager to indicate a lexical error.
     * @return the message
     */
    @Override
    public String getMessage() {
        return ("Lexical error at line "
                + line + ", column "
                + column + ".  Encountered: "
                + (eof ? "<EOF> "
                   : (StringParser.escapeString(String.valueOf(current), '"')) + " (" + (int) current + "), ")
                + "after : " + StringParser.escapeString(after, '"'));
    }


    /** Constructor with message and reason. */
    public TokenMgrException(final String message, final int reason) {
        super(message);
        errorCode = reason;
    }

    /** Full Constructor. */
    public TokenMgrException(final boolean EOFSeen, final int lexState, final int errorLine, final int errorColumn, final String errorAfter, final int curChar, final int reason) {
        eof = EOFSeen;
        state = lexState;
        line = errorLine;
        column = errorColumn;
        after = errorAfter;
        current = (char) curChar;
        errorCode = reason;
    }

    /**
     * Gets the reason why the exception is thrown.
     * @return one of the 4 lexical error codes
     */
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public String getAfter() {
        return after;
    }

     /***
      * Replaces unprintable characters by their espaced (or unicode escaped)
      * equivalents in the given string
      */
     protected static String addEscapes(final String str) {
        final StringBuilder retval = new StringBuilder();
        char ch;
        for (int i = 0; i < str.length(); i++) {
          switch (str.charAt(i))
          {
             case 0 :
                continue;
             case '\b':
                retval.append("//b");
                continue;
             case '\t':
                retval.append("//t");
                continue;
             case '\n':
                retval.append("//n");
                continue;
             case '\f':
                retval.append("//f");
                continue;
             case '\r':
                retval.append("//r");
                continue;
             case '\"':
                retval.append("//\"");
                continue;
             case '\'':
                retval.append("//\'");
                continue;
             case '/':
                retval.append("////");
                continue;
             default:
                if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
                   final String s = "0000" + Integer.toString(ch, 16);
                   retval.append("//u").append(s.substring(s.length() - 4));
                } else {
                   retval.append(ch);
                }
                continue;
          }
        }
        return retval.toString();
     }
}
