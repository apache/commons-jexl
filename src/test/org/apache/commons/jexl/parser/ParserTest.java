package org.apache.commons.jexl.parser;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.io.StringReader;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;

/**
 *
 */
public class ParserTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite(ParserTest.class);
    }

    public ParserTest(String testName)
    {
        super(testName);
    }

    /**
      *  parse test : see if we can parse a little script
      */
     public void testParse1()
         throws Exception
     {
         Parser parser = new Parser(new StringReader(";"));

         SimpleNode sn = parser.parse(new StringReader("foo = 1;"));

         JexlContext jc = JexlHelper.createContext();

         sn.interpret(jc);
     }

    public void testParse2()
        throws Exception
    {
        Parser parser = new Parser(new StringReader(";"));

        JexlContext jc = JexlHelper.createContext();

        SimpleNode sn = parser.parse(new StringReader("foo = \"bar\";"));
        sn.interpret(jc);
        sn = parser.parse(new StringReader("foo = 'bar';"));
        sn.interpret(jc);
    }

    public static void main(String[] args)
        throws Exception
    {
        ParserTest pt = new ParserTest("foo");

        pt.testParse1();
    }

}
