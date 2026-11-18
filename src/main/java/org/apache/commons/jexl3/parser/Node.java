/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.parser;

import java.io.Serializable;

/**
 * The interface all AST nodes implement.
 * <p>It provides the basic machinery for constructing the parent and child relationships between nodes.
 * This is the interface JJTree expects; it is written by hand so the nodes do not depend on
 * the base class the parser generator would otherwise create.</p>
 */
public interface Node extends Serializable {
    /**
     * Called after the node has been made the current node; child nodes can now be added to it.
     */
    void jjtOpen();

    /**
     * Called after all the child nodes have been added.
     */
    void jjtClose();

    /**
     * Sets the parent node of this node.
     *
     * @param n the parent node
     */
    void jjtSetParent(Node n);

    /**
     * Gets the parent node of this node.
     *
     * @return the parent node
     */
    Node jjtGetParent();

    /**
     * Adds a node to the list of children of this node.
     *
     * @param n the node to add as a child
     * @param i the zero-based index where to add the child
     */
    void jjtAddChild(Node n, int i);

    /**
     * Gets a child node; children are numbered from zero, left to right.
     *
     * @param i the zero-based child index
     * @return the child node
     */
    Node jjtGetChild(int i);

    /**
     * Gets the number of children of this node.
     *
     * @return the number of children, always &ge; 0
     */
    int jjtGetNumChildren();

    /**
     * Gets the node identifier, one of the {@link ParserTreeConstants}.
     *
     * @return the node identifier
     */
    int getId();

    /**
     * Accepts a visitor.
     *
     * @param visitor the visitor
     * @param data contextual data
     * @return the result of the visit
     */
    Object jjtAccept(ParserVisitor visitor, Object data);
}
