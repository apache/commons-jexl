package org.apache.commons.jexl.resolver;

import org.apache.commons.jexl.JexlExprResolver;
import org.apache.commons.jexl.JexlContext;

/**
 *  Simple resolver to try the expression as-is from the context.
 *
 *  For example, you could resolve ant-ish properties (foo.bar.woogie)
 *  using this...
 *
 *  hint, hint...
 *
 *  @author <a href="mailto:geirm@adeptra.com">Geir Magnusson Jr.</a>
 *  @version $Id$
 */
public class FlatResolver implements JexlExprResolver
{
    /**
     *  flag to return NO_VALUE on null from context
     *  this allows jexl to try to evaluate
     */
    protected boolean noValOnNull = true;

    /**
     * default CTOR
     */
    public FlatResolver()
    {
    }

    /**
     *  CTOR that lets you override the default behavior of
     *  noValOnNull, which is true (jexl gets a shot after if null)
     */
    public FlatResolver(boolean noValOnNull)
    {
        this.noValOnNull = noValOnNull;
    }

    public Object evaluate(JexlContext context, String expression)
    {
        Object val = context.getVars().get(expression);

        if (val == null && noValOnNull)
        {
            return JexlExprResolver.NO_VALUE;
        }

        return val;
    }
}
