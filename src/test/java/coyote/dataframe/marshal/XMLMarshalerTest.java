/*
 * Copyright (c) 2016 Stephan D. Cote' - All rights reserved.
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the MIT License which accompanies this distribution, and is 
 * available at http://creativecommons.org/licenses/MIT/
 *
 * Contributors:
 *   Stephan D. Cote 
 *      - Initial concept and initial implementation
 */
package coyote.dataframe.marshal;

//import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import coyote.dataframe.DataField;
import coyote.dataframe.DataFrame;


/**
 * 
 */
public class XMLMarshalerTest {

  static final String XML0 = "<date/>";
  static final String XML1 = "<text>hello</text>";
  static final String XML2 = "<text>hello</text>\r\n<data>world</data>";
  static final String XML3 = "<text>hello</text>\r\n<data>world</data>\r\n<extra>This is fun!</extra>";

  static final String XMLA = "<date>2016-02-01</date>";
  static final String XMLB = "<?xml version=\"1.0\"?>\r\n" + "<doc>\r\n   <assembly>\r\n       <name>Linkage</name>\r\n   </assembly>\r\n   <members>\r\n       <member name=\"T:Linkage.Logging.IFormatter\">\r\n           <summary> Class IFormatter</summary>\r\n       </member>\r\n       <member name=\"M:Linkage.Logging.IFormatter.Initialize\">\r\n           <summary></summary>\r\n       </member>\r\n       <!-- Comments can occur anywhere -->\r\n       <member name=\"M:Linkage.Logging.IFormatter.Format(System.Object,System.String)\">\r\n           <summary> Format the given object into a string based upon the given category.</summary>\r\n           <param name=\"obj\">The object to format into a string.</param>\r\n           <param name=\"category\">The category of the event to be used in optional condition\r\n           formatting.</param>\r\n            <returns> String representation of the event as it will be written to the log</returns>\r\n       </member>\r\n   </members>\r\n" + "</doc>\r\n";




  /**
   * Test method for {@link coyote.dataframe.marshal.XMLMarshaler#marshal(java.lang.String)}.
   */
  @Test
  public void testMarshalString() {
    List<DataFrame> frames = null;
    // XMLMarshaler.marshal( "<date?" );  // bad
    // XMLMarshaler.marshal( "</close>" );  // bad

    frames = XMLMarshaler.marshal( XML0 ); // simple empty tag
    assertNotNull( frames );
    assertTrue( frames.size() == 1 );
    DataFrame frame = frames.get( 0 );
    assertNotNull( frame );
    assertTrue( frame.size() == 1 );
    DataField field = frame.getField( 0 );
    assertNotNull( field );
    assertEquals( "date", field.getName() );

    frames = XMLMarshaler.marshal( XML1 ); // single field
    assertNotNull( frames );
    assertTrue( frames.size() == 1 );
    frame = frames.get( 0 );
    assertNotNull( frame );
    assertTrue( frame.size() == 1 );
    field = frame.getField( 0 );
    assertNotNull( field );
    assertEquals( "text", field.getName() );

    frames = XMLMarshaler.marshal( XML2 ); // multiple fields
    assertNotNull( frames );
    assertTrue( frames.size() == 1 );
    frame = frames.get( 0 );
    assertNotNull( frame );
    assertTrue( frame.size() == 2 );
    field = frame.getField( 0 );
    assertNotNull( field );
    assertEquals( "text", field.getName() );

    frames = XMLMarshaler.marshal( XML3 ); // multiple fields
    assertNotNull( frames );
    assertTrue( frames.size() == 1 );
    frame = frames.get( 0 );
    assertNotNull( frame );
    assertTrue( frame.size() == 3 );
    field = frame.getField( 0 );
    assertNotNull( field );
    assertEquals( "text", field.getName() );
    System.out.println( JSONMarshaler.toFormattedString( frame ) );
  }




  /**
   * Test method for {@link coyote.dataframe.marshal.XMLMarshaler#marshal(coyote.dataframe.DataFrame)}.
   */
  //@Test
  public void testMarshalDataFrame() {
    fail( "Not yet implemented" );
  }




  /**
   * Test method for {@link coyote.dataframe.marshal.XMLMarshaler#toFormattedString(coyote.dataframe.DataFrame)}.
   */
  //@Test
  public void testToFormattedString() {
    fail( "Not yet implemented" );
  }

}
