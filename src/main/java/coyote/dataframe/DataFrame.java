/*
 * DataFrame - a data marshaling toolkit
 * Copyright (c) 2006 Stephan D. Cote' - All rights reserved.
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the MIT License which accompanies this distribution, and is 
 * available at http://creativecommons.org/licenses/MIT/
 *
 * Contributors:
 *   Stephan D. Cote 
 *      - Initial API and implementation
 */
package coyote.dataframe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import coyote.util.ByteUtil;


/**
 * A hierarchical unit of data.
 * 
 * <p>This models a unit of data that can be exchanged over a variety of 
 * transports for a variety communications needs.</p>
 * 
 * <p>This is a surprisingly efficient transmission scheme as all field values 
 * and child frames are stored in their wire format as byte arrays. They are 
 * then marshaled only when accessed and are ready for transmission.</p>
 * 
 * <p>This class was conceived to implement the Data Transfer Object (DTO) 
 * design pattern in distributed applications. Passing a DataFrame as both 
 * argument and return values in remote service calls. Using this 
 * implementation of a DTO allows for more efficient transfer of data between
 * distributed components, reducing latency, improving throughput and 
 * decoupling not only the components of the system, but moving business logic
 * out of the data model.</p>
 */
public class DataFrame implements Cloneable
{
  

  /** The array of fields this frame holds */
  ArrayList<DataField> fields = new ArrayList<DataField>();

  /** Flag indicating the top-level elements of this frame has been changed. */
  private volatile boolean modified = false;




  /**
   * Construct an empty frame.
   */
  public DataFrame()
  {
  }




  /**
   * Construct the frame with the given bytes.
   *
   * @param data The byte array from which to construct the frame.
   */
  public DataFrame( final byte[] data )
  {
    if( data != null )
    {
      try
      {
        final ByteArrayInputStream bais = new ByteArrayInputStream( data );
        final DataInputStream in = new DataInputStream( bais );

        while( in.available() > 0 )
        {
          add( new DataField( in ) );
        }
      }
      catch( final EOFException eof )
      {
        throw new IllegalArgumentException( "Data underflow adding field #" + ( fields.size() + 1 ) );
      }
      catch( final IOException ioe )
      {
        throw new IllegalArgumentException( ioe.getMessage() );
      }
    }
  }




  /**
   * Return the first occurrence of a named field.
   *
   * @param name The name of the field to return.
   *
   * @return The first occurrence of the named frame Field or null if an frame 
   *         Field with the given name was not found.
   */
  public DataField getField( final String name )
  {
    for( int i = 0; i < fields.size(); i++ )
    {
      final DataField field = fields.get( i );

      if( ( field.getName() != null ) && field.getName().equals( name ) )
      {
        return field;
      }
    }

    return null;
  }




  /**
   * Convenience method that allows for the checking of the existence of a 
   * named field.
   * 
   * <p>This is essentially the same as calling: 
   * <pre><code>( getField("NAME") != null )</code></pre>
   * when checking for a field named "NAME", only it reads nicer.</p>
   *  
   * @param name The name of the field to retrieve.
   * 
   * @return True if the field with the given name exists, false otherwise.
   */
  public boolean contains( final String name )
  {
    for( int i = 0; i < fields.size(); i++ )
    {
      final DataField field = fields.get( i );
      if( ( field.getName() != null ) && field.getName().equals( name ) )
      {
        return true;
      }
    }
    return false;
  }




  /**
   * Return the given occurrence of a field.
   *
   * @param indx The zero-based index of the field to return.
   *
   * @return The indexed occurrence of an frame Field or null if the index is 
   *         out of range or less than zero.
   */
  public DataField getField( final int indx )
  {
    if( ( indx < fields.size() ) && ( indx > -1 ) )
    {
      return fields.get( indx );
    }

    return null;
  }




  /**
   * @return The number of fields in the frame.
   */
  public int getFieldCount()
  {
    return fields.size();
  }




  /**
   * Return the value of the named field as a string.
   * 
   * @param name The name of the field to retrieve.
   * 
   * @return The string value of the first file with the given name or null if 
   *         the field could not be found.
   */
  public String getAsString( final String name )
  {
    final Object val = getObject( name );
    if( val != null )
    {
      return val.toString();
    }
    return null;
  }




  /**
   * Convenience method to return the value of the indexed field as a String 
   * value.
   * 
   * @param indx Index of the field value to return.
   * 
   * @return the value of the field
   * 
   * @throws DataFrameException if the field does not exist or if the value of the 
   *         found field could not be parsed or converted to a String value.
   */
  public String getAsString( final int indx ) throws DataFrameException
  {
    final Object val = getObject( indx );
    if( val != null )
    {
      return val.toString();
    }
    throw new DataFrameException( "Indexed field does not exist" );
  }




  /**
   * Return the object value of the named field.
   *
   * @param name The name of the field containing the object to retrieve.
   *
   * @return The object value of the first occurrence of the named field or null 
   *         if the filed with the given name was not found.
   */
  public Object getObject( final String name )
  {
    for( int i = 0; i < fields.size(); i++ )
    {
      final DataField field = fields.get( i );

      if( ( field.getName() != null ) && field.getName().equals( name ) )
      {
        return field.getObjectValue();
      }
    }

    return null;
  }




  /**
   * Return the object value of the indexed field.
   *
   * @param i The zero-based index of the field to return. The first element is
   *          at index zero, the second is at index 1 and so on.
   *
   * @return The object value of the field at the given index, or null if there 
   *         was no value at that index (out-of-bounds)
   */
  public Object getObject( final int i )
  {
    if( i < fields.size() )
    {
      return ( fields.get( i ) ).getObjectValue();
    }

    return null;
  }




  /**
   * Create a deep-copy of this frame.
   * 
   * <p>The sequence, source and digest are NOT cloned, as they are generated 
   * as a part of the transmission process.</p>
   *
   * @return
   */
  public Object clone()
  {
    final DataFrame retval = new DataFrame();

    // Clone all the fields
    for( int i = 0; i < fields.size(); i++ )
    {
      retval.fields.add( i, (DataField) fields.get( i ).clone() );
    }
    retval.modified = false;

    return retval;
  }




  /**
   * Create a frame with a field with the given name and value.
   *
   * @param name The name of the field to populate.
   * @param value The value to place in the named field
   */
  public DataFrame( final String name, final Object value )
  {
    add( name, value );
    modified = false;
  }




  /**
   * Add a new field with the given value.
   * 
   * <p><strong>NOTICE:</strong> Because values are added as byte arrays and
   * not references, only complete frames can be added. This is because child 
   * frames are stored as their wire format at the time of their being added. 
   * Any fields added to the child after being added to the parent <strong>will 
   * not have their values represented in the child frame</strong>. This is 
   * partially by design as it is then possible to use one frame in the 
   * creation of all children. Also, storing the values in wire format reduces 
   * the number of times field values are marshaled, thereby improving overall
   * performance.</p>  
   *
   * @param name The name of the field to populate.
   * @param value The value to place in the named field
   *
   * @return the index of the field just added.
   */
  public int add( final Object obj )
  {
    modified = true;
    if( obj instanceof DataField )
    {
      fields.add( (DataField)obj );
    }
    else
    {
      fields.add( new DataField( obj ) );
    }

    return fields.size() - 1;
  }




  /**
   * Add an frame field with the given name and value.
   * 
   * <p>The resulting frame field will be added to the frame with its type 
   * being determined by the DataField class.</p>
   *
   * @param name The name of the field to populate.
   * @param value The value to place in the named field
   *
   * @return the index of the placed value.
   * 
   * @throws IllegalArgumentException If the name is longer than 255 characters 
   *         or the value is an unsupported type.
   */
  public int add( final String name, final Object value )
  {
    modified = true;
    fields.add( new DataField( name, value ) );
    return fields.size() - 1;
  }




  /**
   * Place the object in the frame under the given name, overwriting any
   * existing object with the same name.
   * 
   * <p>Note: this is different from <tt>add(String,Object)</tt> in that 
   * this method will not duplicate names.</p>
   *
   * @param name The name of the field in which the value is to be placed. 
   * @param obj The value to place.
   * 
   * @return The index of the field the value was placed.
   */
  public int put( final String name, final Object obj )
  {
    if( ( obj != null ) || ( name != null ) )
    {
      if( name != null )
      {
        for( int i = 0; i < fields.size(); i++ )
        {
          final DataField field = fields.get( i );

          if( ( field.name != null ) && field.name.equals( name ) )
          {
            if( obj != null )
            {
              field.type = DataField.getType( obj );
              field.value = DataField.encode( obj );
            }
            else
            {
              // Null object implies remove the named field
              fields.remove( i );
            }

            modified = true;

            return i;
          }
        }

        return add( name, obj );
      }
      else
      {
        return add( obj );
      }
    }

    return -1;
  }




  /**
   * Remove the first occurrence of a DataField with the given name.
   *
   * @param name name of the DataField to remove.
   * 
   * @return The DataField that was removed.
   */
  public DataField remove( final String name )
  {
    DataField retval = null;
    if( name != null )
    {
      for( int i = 0; i < fields.size(); i++ )
      {
        final DataField field = fields.get( i );

        if( ( field.name != null ) && field.name.equals( name ) )
        {
          retval = fields.remove( i );

          modified = true;
        }
      }
    }
    return retval;
  }




  /**
   * Remove the first field with the given name and add a new field with the 
   * given name and new object value.
   * 
   * <p>This is equivalent to calling:<pre><code>
   * {
   *   remove( name );
   *   add( name, obj );
   * }
   * </code></pre></p>
   * 
   * <p><strong>NOTE:</strong> The value is not checked prior to removing the 
   * existing field which means if the object is not supported, the end state 
   * of the frame will be the named field will have been removed from the 
   * frame and no value replacing it. This is by design, allowing the invalid 
   * value to be deleted as desired and also because it saves time not having 
   * to check for existence prior to each replace.</p>
   *  
   * @param name Name of the field to replace and then add.
   * @param obj The value of the object to set in the new field.
   */
  public void replace( final String name, final Object obj )
  {
    remove( name );
    add( name, obj );
  }




  /**
   * Remove all the fields with the given name and add a single new field with 
   * the given name and new object value.
   * 
   * <p>This is equivalent to calling:<pre><code>
   * {
   *   removeAll( name );
   *   add( name, obj );
   * }
   * </code></pre></p>
   * 
   * <p><strong>NOTE:</strong> The value is not checked prior to removing the 
   * existing fields which means if the object is not supported, the end state 
   * of the frame will be the named fields will have been removed from the 
   * frame and no value replacing it. This is by design, allowing the invalid 
   * value to be deleted as desired and also because it saves time not having 
   * to check for existence prior to each replace.</p>
   *  
   * @param name Name of the fields to replace and then add.
   * @param obj The value of the object to set in the new field.
   */
  public void replaceAll( final String name, final Object obj )
  {
    removeAll( name );
    add( name, obj );
  }




  /**
   * Remove all occurrences of DataFields with the given name.
   *
   * @param name name of the DataField to remove.
   */
  public void removeAll( final String name )
  {
    modified = true;

    if( name != null )
    {
      for( int i = 0; i < fields.size(); i++ )
      {
        final DataField field = fields.get( i );

        if( ( field.name != null ) && field.name.equals( name ) )
        {
          fields.remove( i-- );
        }
      }
    }
  }




  /**
   * Generate a digest fingerprint for this frame based solely on the wire format
   * of this and all frames contained therein.
   * 
   * <p>This performs a SHA-1 digest on the payload to help determine a unique
   * identifier for the frame. Note: the digest can be used to help determine
   * equivalence between frames.</p>
   * 
   * <p><strong>NOTE:</strong> This is a very expensive function and should not 
   * be used without due consideration.</p>
   *
   * @return the SHA-1 digest for this frame.
   */
  public byte[] getDigest()
  {
    MessageDigest digest = null;
    try
    {
      digest = MessageDigest.getInstance( "SHA-1" );
    }
    catch( final NoSuchAlgorithmException e )
    {
      e.printStackTrace();
      return null;
    }
    digest.reset();
    digest.update( getBytes() );
    return digest.digest();
  }




  /**
   * Generate a digest fingerprint for this message based solely on the wire
   * format of the payload and returns it as a Hex string.
   *
   * <p><strong>NOTE:</strong> This is a very expensive function and should not 
   * be used without due consideration.</p>
   *
   * @see getDigest()
   * 
   * @return A String representation of the digest of the payload.
   */
  public String getDigestString()
  {
    return ByteUtil.bytesToHex( getDigest() );
  }




  /**
   * Get a copy of the frame in its wire format.
   * 
   * <p>This is a way to serialize the frame for any medium that supports binary
   * data. The resultant byte array may then be used to the 
   * <code>DataFrame(byte[])</code> constructor to reconstitute the frame.</p>
   *
   * @return this frame represented in its wire format.
   */
  public byte[] getBytes()
  {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final DataOutputStream dos = new DataOutputStream( baos );

    try
    {
      for( int i = 0; i < fields.size(); i++ )
      {
        dos.write( fields.get( i ).getBytes() );
      }
    }
    catch( final IOException e )
    {
      e.printStackTrace();
    }

    return baos.toByteArray();
  }




  /**
   * Get the byte[] with this name.
   * 
   * <p>Basically, this will present the wire format of the named field.</p>
   *
   * @param name The name of the field to query.
   *
   * @return The bytes[] value or null
   */
  public byte[] getBytes( final String name )
  {
    final Object retval = getObject( name );

    if( ( retval != null ) && ( retval instanceof byte[] ) )
    {
      return (byte[])retval;
    }

    return null;
  }




  /**
   * Obtain the reference to the ordered list of DataFields making up this
   * frame.
   * 
   * <p>Changing this list changes the actual contents of the frame. <strong>Be 
   * Careful!</strong></p>
   *
   * @return The list of frame fields in this frame.
   */
  public List<DataField> getFields()
  {
    return fields;
  }




  /**
   * Set the ArrayList as the backing collection for this frame.
   * 
   * <p><strong>WARNING!</strong> all the elements MUST be DataFields or this 
   * frame will throw class cast exceptions whenever it tries to access the 
   * fields as no casting checks are performed on the backing list. Your code
   * probably should not use this method.</p>
   * 
   * @param list An ordered list of DataFields.
   */
  public void setFields( final ArrayList<DataField> list )
  {
    fields = list;
    modified = true;
  }




  /**
   * @return Returns true if this frame has been modified, false otherwise.
   */
  public boolean isModified()
  {
    return modified;
  }




  /**
   * Remove all the fields from this frame.
   * 
   * <p>The frame will be empty after this method is called.</p>
   */
  public void clear()
  {
    fields.clear();
  }




  public int getTypeCount()
  {
    return DataField.typeCount();
  }

}