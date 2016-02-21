/*
 * Copyright (c) 2015 Stephan D. Cote' - All rights reserved.
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the MIT License which accompanies this distribution, and is 
 * available at http://creativecommons.org/licenses/MIT/
 *
 * Contributors:
 *   Stephan D. Cote 
 *      - Initial concept and initial implementation
 */
package coyote.dataframe.marshal.xml;

import java.io.IOException;
import java.io.Reader;

import coyote.commons.SimpleReader;
import coyote.commons.StringParser;
import coyote.dataframe.DataField;
import coyote.dataframe.DataFrame;
import coyote.dataframe.FieldType;
import coyote.dataframe.marshal.ParseException;


/**
 * 
 */
public class XmlFrameParser extends StringParser {
  private static final String XML_DELIMS = " \t\n><";

  private static final int OPEN = (int)'<';
  private static final int CLOSE = (int)'>';
  private static final int QM = (int)'?';
  private static final int SLASH = (int)'/';
  public static final String TYPE_ATTRIBUTE_NAME = "type";




  public XmlFrameParser( String string ) {
    super( new SimpleReader( string ), XML_DELIMS );
  }




  public XmlFrameParser( Reader reader ) {
    super( reader, XML_DELIMS );
  }




  private String readValue() {
    StringBuffer b = new StringBuffer();
    try {
      if ( OPEN == peek() )
        return null;

      // keep reading characters until the next character is an open marker
      while ( OPEN != peek() ) {
        b.append( (char)read() );
      }

    } catch ( IOException e ) {
      e.printStackTrace();
    }
    return b.toString();
  }




  /**
   * Generate a parse exception with the given message.
   * 
   * <p>All the position information is populated in the exception based on the 
   * readers current counters.</p>
   * 
   * @param message The text message to include in the exception 
   * 
   * @return a parse exception with the given message.
   */
  private ParseException error( final String message ) {
    return new ParseException( message, getOffset(), getCurrentLineNumber(), getColumnNumber(), getLastCharacterRead() );
  }




  private ParseException expected( final String expected ) {
    if ( isEndOfText() ) {
      return error( "Unexpected input" );
    }
    return error( "Expected " + expected );
  }




  private boolean isEndOfText() {
    return getLastCharacterRead() == -1;
  }




  private Tag readTag() {
    Tag retval = null;
    String token = null;

    // read to the next open character
    try {
      readTo( OPEN );
    } catch ( Exception e ) {
      // assume there are no more tags
      return null;
    }

    try {
      // read everything up to the closing character into the token
      token = readTo( CLOSE );
      //log.debug( "Read tag of '{}'", token );

      if ( token != null ) {
        token = token.trim();

        if ( token.length() > 0 ) {
          retval = new Tag( token );
        }
      }
    } catch ( IOException e ) {
      throw error( "Could not read a complete tag: IO error" );
    }

    return retval;
  }




  /**
   * Parse the reader into a dataframe
   * 
   * @return a dataframe parsed from the data set in this parser.
   * 
   * @throws ParseException if there are problems parsing the XML
   */
  public DataFrame parse() throws ParseException {

    DataFrame retval = null;

    Tag tag = null;

    // Start reading tags until we pass the preamble and comments
    do {
      tag = readTag();
      if ( tag == null ) {
        break;
      }
    }
    while ( tag.isComment() || tag.isPreamble() );

    // We have a tag which is not a preamble or comment, if it is an open 
    // tag then we have data that goes into a data frame  
    if ( tag != null && tag.isOpenTag() ) {
      retval = readFrame( tag );
    }

    return retval;
  }




  /**
   * Read in the value of a tag creating a data field containing the value as 
   * of the requested data type.
   * 
   * <p>Thoe opsition of the reader will be immediately behind the closing tag 
   * of the read-in field.</p>
   * 
   * @param openTag The opening tag read in for this field
   * 
   * @return a data field constructed from the XML value at the current position in the reader's stream
   */
  private DataField readField( Tag openTag ) {
    DataField retval = null;

    // This will be the name of the field
    String name = openTag.getName();

    // This will be the type into which the string data is converted   
    String type = openTag.getAttribute( TYPE_ATTRIBUTE_NAME );

    FieldType fieldType = DataField.getFieldType( type );

    String value = readValue();

    // read what should be the closing tag
    Tag closeTag = readTag();

    if ( closeTag != null ) {
      if ( closeTag.isCloseTag() ) {
        if ( !closeTag.getName().equals( openTag.getName() ) ) {
          throw error( "Malformed XML: expected closing tag for '" + openTag.getName() + "' not '" + closeTag.getName() + "'" );
        }
      } else {
        // this appears to be a nested field, get a frame for the tag we just read in
        DataFrame frame = readFrame( closeTag );
        retval = new DataField( name, frame );
      } // close tag check
    } else {
      throw error( "Malformed XML: unexpected end of data" );
    }

    // If we don't have a nexted data field as a return value, try to use the 
    // value we read in  
    if ( retval == null ) {

      // TODO try to convert the string data of "value" into an object of the requested type
      if ( fieldType != null ) {
        // TODO all manner of data type parsing goes here
        retval = new DataField( name, value ); // string for now
      } else {
        // not valid field type, just use string (or DataFrame depending on what readValue returned)
        retval = new DataField( name, value );
      }
    }

    return retval;
  }




  /**
   * @return
   */
  private Tag peekNextTag() {
    // TODO Auto-generated method stub
    return null;
  }




  /**
   * Return the data as a frame.
   * 
   * <p>This method is called when the value of a field is another open tag 
   * which indicates a new field. These nested fields are placed in a frame and 
   * returned to the caller.</p>
   * 
   * @param openTag The open tag read in signaling a nested field.
   * 
   * @return a data frame containing the nested field and any other peer field encountered
   */
  private DataFrame readFrame( Tag openTag ) {

    if ( openTag == null )
      throw new IllegalArgumentException( "ReadFrame called will null OpenTag" );

    Tag currentTag = openTag;

    String name = currentTag.getName();
    DataFrame retval = null;
    DataField field = null;

    // We have a tag which is not a preamble or comment; start looping through the tags 
    while ( currentTag != null ) {

      // Skip preamble and comments
      if ( !currentTag.isComment() && !currentTag.isPreamble() ) {
        if ( currentTag.isOpenTag() ) {

          if ( currentTag.isEmptyTag() ) {
            // empty tag, empty field
            field = new DataField( currentTag.getName(), null );
          } else {
            // read the field from the data stream this will consume the close tag for this field
            field = readField( currentTag );
          }

          // add the parsed field into the dataframe
          if ( field != null ) {
            // create the dataframe if this is the first field
            if ( retval == null ) {
              retval = new DataFrame();
            }

            // add the new field to the dataframe we will return
            retval.add( field );
          } else {
            throw error( "Problems reading field: null value" );
          }

          // read the next opening tag
          currentTag = readTag();

        } else {
          break;
        }

      } // not comment or preample

    } // while we have tags

    return retval;
  }

}
