package au.com.digitalspider;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class UtilTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public UtilTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( UtilTest.class );
    }

    public void testGetFileExtension()
    {
        String filename = "C:/opt/david/this_file.json";
        assertEquals("json",Util.getFileExtension(filename));
    }
    
    public void testGetTextFileName()
    {
        String filename = "C:/opt/david/this_file.json";
        assertEquals("C:/opt/david/this_file.txt",Util.getTextFileName(filename));
    }
}
