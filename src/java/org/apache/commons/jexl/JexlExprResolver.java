package org.apache.commons.jexl;

/**
 *  A Resolver allows custom resolution of the expression, and can be
 *  added in front of the jexl engine, or after in the evaluation
 *
 *  @todo This needs to be explained in detail.  Why do this?
 *  @author <a href="mailto:geirm@adeptra.com">Geir Magnusson Jr.</a>
 *  @version $Id: JexlExprResolver.java,v 1.2 2004/06/12 23:53:17 tobrien Exp $
 */
public interface JexlExprResolver
{
    public static final Object NO_VALUE = new Object();

    /**
     *  evaluates an expression against the context
     *
     *  @todo Must detail the expectations and effects of this resolver.
     *  @param context current data context
     *  @param expression expression to evauluate
     *  @return value (may be null) po the NO_VALUE object to
     *       indicate no resolution.
     */
    Object evaluate(JexlContext context, String expression);
}
