package org.apache.commons.jexl;

/**
 *  A Resolver allows custom resolution of the expression, and can be
 *  added in front of the jexl engine, or after in the evaluation
 *
 *  @todo This needs to be explained in detail.  Why do this?
 *  @author <a href="mailto:geirm@adeptra.com">Geir Magnusson Jr.</a>
 *  @version $Id: JexlExprResolver.java,v 1.3 2004/08/23 13:53:34 dion Exp $
 */
public interface JexlExprResolver
{
    Object NO_VALUE = new Object();

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
