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
 *  @since 1.0
 *  @author <a href="mailto:geirm@adeptra.com">Geir Magnusson Jr.</a>
 *  @version $Id$
 */
public class FlatResolver implements JexlExprResolver {
    /**
     *  Flag to return NO_VALUE on null from context.
     *  this allows jexl to try to evaluate
     */
    protected boolean noValOnNull = true;

    /**
     * Default CTOR.
     */
    public FlatResolver() {
    }

    /**
     *  CTOR that lets you override the default behavior of
     *  noValOnNull, which is true. (jexl gets a shot after if null)
     *
     *  @param noValOnNull Whether NO_VALUE will be returned instead of null.
     */
    public FlatResolver(boolean noValOnNull) {
        this.noValOnNull = noValOnNull;
    }

    /**
     *  Try to resolve expression as-is.
     *
     *  @param context The context for resolution.
     *  @param expression The flat expression.
     *  @return The resolved value.
     */
    public Object evaluate(JexlContext context, String expression) {
        Object val = context.getVars().get(expression);

        if (val == null && noValOnNull) {
            return JexlExprResolver.NO_VALUE;
        }

        return val;
    }
}
