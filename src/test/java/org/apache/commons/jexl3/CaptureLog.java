/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;

/**
 * A log implementation to help control tests results.
 */
public class CaptureLog implements Log {
    private final List<Object[]> captured = new ArrayList<Object[]>();

    static Object caller() {
        final StackTraceElement[] stack = new Exception().fillInStackTrace().getStackTrace();
        return stack[2];
    }

    public CaptureLog() {
        this("org.apache.commons.jexl3");
    }

    public CaptureLog(final String name) {
        //super(name);
    }

    public boolean isEmpty() {
        return captured.isEmpty();
    }

    public int count(final String type) {
        int count = 0;
        for (final Object[] l : captured) {
            if (type.equals(l[0].toString())) {
                count += 1;
            }
        }
        return count;
    }

    //@Override
    public boolean isEnabledFor(final int /*Priority*/ p) {
        return true;
    }

    @Override
    public void debug(final Object o) {
        captured.add(new Object[]{"debug", caller(), o});
    }

    @Override
    public void debug(final Object o, final Throwable thrwbl) {
        captured.add(new Object[]{"debug", caller(), o, thrwbl});
    }

    @Override
    public void error(final Object o) {
        captured.add(new Object[]{"error", caller(), o});
    }

    @Override
    public void error(final Object o, final Throwable thrwbl) {
        captured.add(new Object[]{"error", caller(), o, thrwbl});
    }

    @Override
    public void fatal(final Object o) {
        captured.add(new Object[]{"fatal", caller(), o});
    }

    @Override
    public void fatal(final Object o, final Throwable thrwbl) {
        captured.add(new Object[]{"fatal", caller(), o, thrwbl});
    }

    @Override
    public void info(final Object o) {
        captured.add(new Object[]{"info", caller(), o});
    }

    @Override
    public void info(final Object o, final Throwable thrwbl) {
        captured.add(new Object[]{"info", caller(), o, thrwbl});
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public boolean isFatalEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void trace(final Object o) {
        captured.add(new Object[]{"trace", caller(), o});
    }

    @Override
    public void trace(final Object o, final Throwable thrwbl) {
        captured.add(new Object[]{"trace", caller(), o, thrwbl});
    }

    @Override
    public void warn(final Object o) {
        captured.add(new Object[]{"warn", caller(), o});
    }

    @Override
    public void warn(final Object o, final Throwable thrwbl) {
        captured.add(new Object[]{"warn", caller(), o, thrwbl});
    }

}
