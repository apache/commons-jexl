/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", "Jexl" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.commons.jexl.parser;

import org.apache.commons.jexl.JexlContext;

/**
 *  useful interface to node. most autogened by javacc
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: SimpleNode.java,v 1.2 2003/10/09 21:28:55 rdonkin Exp $
 */
public class SimpleNode implements Node
{
    protected Node parent;
    protected Node[] children;
    protected int id;
    protected Parser parser;

    public SimpleNode(int i)
    {
        id = i;
    }

    public SimpleNode(Parser p, int i)
    {
        this(i);
        parser = p;
    }

    public void jjtOpen()
    {
    }

    public void jjtClose()
    {
    }

    public void jjtSetParent(Node n)
    {
        parent = n;
    }

    public Node jjtGetParent()
    {
        return parent;
    }

    public void jjtAddChild(Node n, int i)
    {
        if (children == null)
        {
            children = new Node[i + 1];
        }
        else if (i >= children.length)
        {
            Node c[] = new Node[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }

        children[i] = n;
    }

    public Node jjtGetChild(int i)
    {
        return  children[i];
    }

    public int jjtGetNumChildren()
    {
        return (children == null) ? 0 : children.length;
    }

    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    /** Accept the visitor. **/
    public Object childrenAccept(ParserVisitor visitor, Object data)
    {
        if (children != null)
        {
            for (int i = 0; i < children.length; ++i)
            {
                children[i].jjtAccept(visitor, data);
            }
        }
        return data;
    }


    public String toString()
    {
        return ParserTreeConstants.jjtNodeName[id];
    }

    public String toString(String prefix)
    {
        return prefix + toString();
    }

    public void dump(String prefix)
    {
        System.out.println(toString(prefix));

        if (children != null)
        {
            for (int i = 0; i < children.length; ++i)
            {
                SimpleNode n = (SimpleNode)children[i];

                if (n != null)
                {
                    n.dump(prefix + " ");
                }
            }
        }
    }

    /**
     *  basic interpret - just invoke interpret on all children
     */
    public boolean interpret(JexlContext pc)
        throws Exception
    {
        for (int i=0; i<jjtGetNumChildren();i++)
        {
            SimpleNode node = (SimpleNode) jjtGetChild(i);
            if (!node.interpret(pc))
                return false;
        }

        return true;
    }


    /**
     *  Returns the value of the node.
     */
    public Object value(JexlContext context)
            throws Exception
    {
        return null;
    }

    /**
     *  Sets the value for the node - again, only makes sense for some nodes
     *  but lazyness tempts me to put it here.  Keeps things simple.
     */
    public Object setValue(JexlContext context, Object value)
        throws Exception
    {
        return null;
    }

    /**
     *  Used to let a node calcuate it's value..
     */
    public Object execute(Object o, JexlContext ctx)
            throws Exception
    {
        return null;
    }
}

