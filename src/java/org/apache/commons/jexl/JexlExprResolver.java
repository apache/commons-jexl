package org.apache.commons.jexl;

/**
 *  A Resolver allows custom resolution of the expression, and can be
 *  added in front of the jexl engine, or after in the evaluation
 *
 *  @author <a href="mailto:geirm@adeptra.com">Geir Magnusson Jr.</a>
 *  @version $Id: JexlExprResolver.java,v 1.1 2002/06/13 16:09:32 geirm Exp $
 */
public interface JexlExprResolver
{
    public static final Object NO_VALUE = new Object();

    /**
     *  evaluates an expression against the context
     *
     *  @param context current data context
     *  @param expression expression to evauluate
     *  @return value (may be null) po the NO_VALUE object to
     *       indicate no resolution.
     */
    Object evaluate(JexlContext context, String expression);
}
