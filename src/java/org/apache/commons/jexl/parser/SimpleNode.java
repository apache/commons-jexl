/*
 * Copyright 2002,2004 The Apache Software Foundation.
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

package org.apache.commons.jexl.parser;

import org.apache.commons.jexl.JexlContext;

/**
 *  useful interface to node. most autogened by javacc
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id$
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

