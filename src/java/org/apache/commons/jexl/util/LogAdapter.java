package org.apache.commons.jexl.util;

import org.apache.commons.logging.Log;
import org.apache.velocity.runtime.RuntimeLogger;

/**
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 */
public class LogAdapter implements RuntimeLogger
{
    Log logger;

    LogAdapter(Log log)
    {
        logger = log;
    }

    public void warn(Object o)
    {
        logger.warn(o);
    }

    public void info(Object o)
    {
        logger.info(o);
    }

    public void error(Object o)
    {
        logger.error(o);
    }

    public void debug(Object o)
    {
        logger.debug(o);
    }
}