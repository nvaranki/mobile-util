/*
 * Query.java
 * Created on February 15, 2005, 4:27 PM
 * Copyright 2005 Nikolai Varankine. All rights reserved.
 *
 * This class implements universal query make-up. For every specified parameter, 
 * found in JAD file, class creates and maintains separate TextField, that can 
 * be used for ceation of automated forms.
 *
 * Format of JAD record: 
 * <query_name><id>: <http_key>;[<http_value>];<field_length>;<TextField_constraints>
 *
 * Comments:
 * 1. field_length <= 0 creates hidden field, i.e. without TextField
 * 2. JAD should contain TextField caption in key <root_class_name>.<http_key>
 */

package com.varankin.mobile.util;

import com.varankin.mobile.Dispatcher;
import java.lang.String;
import java.lang.StringBuffer;
import java.util.Vector;
import java.util.Hashtable;
import javax.microedition.lcdui.*;

/**
 * @author  Nikolai Varankine
 */
public class Query
{
    private Vector v; // holds triples of (html_parameter,label,value)
    
    // field structure:
    private final static int FIELD_PARAM = 0;
    private final static int FIELD_NAME  = 1;
    private final static int FIELD_VALUE = 2;
    private final static int FIELD_SIZE  = 3;
    private final static int FIELD_SPEC  = 4;
    private final static int FIELD_ITEM  = 5;
    // ----------------------total: ---------
    private final static int NUM_FIELDS  = 6;
    
    /** Creates a new instance of Query */
    public Query( Dispatcher a_midlet, String a_query_id )
    {
        v = new Vector( NUM_FIELDS * 10 );
        
        for( int p = 0; a_query_id != null; p++ )
        {
            // read in JAD file for related fields
            String combined = a_midlet.getAppProperty( a_query_id + String.valueOf( p ) );
            if( combined == null ) break;

            // parse parameters in format shown above
            final char sep = ';';
            int st1 = combined.indexOf( sep, 0 ), 
                st2 = combined.indexOf( sep, st1+1 ),
                st3 = combined.indexOf( sep, st2+1 ),
                st4 = combined.indexOf( sep, st3+1 );
            String key = combined.substring( 0, st1 );
            String nam = a_midlet.getString( a_query_id + "." + key ); // localized label;
            String val = combined.substring( ++st1, st2 ); // value
            String siz = combined.substring( ++st2, st3 ); // size
            String con = combined.substring( ++st3 ); // constraints

            // save record, order is important!
            Object[] entry = { key, nam, val, siz, con, null };
            int size = Integer.parseInt ( siz );
            if( size > 0 )  
              // make GUI element and remember
              entry[5] = new TextField( nam + ":", val, size, Integer.parseInt ( con ) );
            addElement( entry );
        }
    }

    public Object[] elementAt( int a_index ) throws ArrayIndexOutOfBoundsException
    {
        Object[] rv = new Object[ NUM_FIELDS ];
        for( int f = 0; f < NUM_FIELDS; f++ ) 
            rv[ f ] = v.elementAt( a_index * NUM_FIELDS + f );
        return rv;
    }
    
    public void addElement( Object[] a_entry )
    {
        for( int e = 0; e < NUM_FIELDS; e++ )
            if( e >= a_entry.length || a_entry[ e ] == null )
                v.setSize( v.size() + 1 ); // adds null item at the end
            else
                v.addElement( a_entry[ e ] );
    }
    
    public String getParameter( int a_index ) throws ArrayIndexOutOfBoundsException
    {
        return (String) v.elementAt( a_index * NUM_FIELDS + FIELD_PARAM );
    }
    
    public String getName( int a_index ) throws ArrayIndexOutOfBoundsException
    {
        return (String) v.elementAt( a_index * NUM_FIELDS + FIELD_NAME );
    }
    public void setName( String a_reference, String a_value ) throws ArrayIndexOutOfBoundsException
    {
        setName( indexByParameter( a_reference ), a_value );
    }
    public void setName( int a_index, String a_value ) throws ArrayIndexOutOfBoundsException
    {
        v.removeElementAt( a_index * NUM_FIELDS + FIELD_NAME );
        v.insertElementAt( a_value, a_index * NUM_FIELDS + FIELD_NAME );
    }
    
    public String getValue( int a_index ) throws ArrayIndexOutOfBoundsException
    {
        return isVisible( a_index ) 
            ? ( (TextField) getItem( a_index ) ).getString()
            : (String) v.elementAt( a_index * NUM_FIELDS + FIELD_VALUE );
    }
    public void setValue( String a_reference, String a_value ) throws ArrayIndexOutOfBoundsException
    {
        setValue( indexByParameter( a_reference ), a_value );
    }
    public void setValue( int a_index, String a_value ) throws ArrayIndexOutOfBoundsException
    {
        if( isVisible( a_index ) )
            ( (TextField) getItem( a_index ) ).setString( a_value );
        else 
        {
            v.removeElementAt( a_index * NUM_FIELDS + FIELD_VALUE );
            v.insertElementAt( a_value, a_index * NUM_FIELDS + FIELD_VALUE );
        }
    }

    public String getSize( int a_index ) throws ArrayIndexOutOfBoundsException
    {
        return (String) v.elementAt( a_index * NUM_FIELDS + FIELD_SIZE );
    }
    
    public String getConstraints( int a_index ) throws ArrayIndexOutOfBoundsException
    {
        return (String) v.elementAt( a_index * NUM_FIELDS + FIELD_SPEC );
    }
    
    public Item getItem( int a_index ) throws ArrayIndexOutOfBoundsException
    {
        return (Item) v.elementAt( a_index * NUM_FIELDS + FIELD_ITEM );
    }
    
    public boolean isVisible( int a_index ) throws ArrayIndexOutOfBoundsException
    {
        return getItem( a_index ) != null;
    }
    
    public int size()
    {
        return v.size() / NUM_FIELDS;
    }
    
    public int indexByName( String a_name )
    {
        for( int q = 0; q < size(); q++ ) 
            if( a_name.compareTo( getName( q ) ) == 0 )
                return q;
        return -1;
    }

    public int indexByParameter( String a_name )
    {
        for( int q = 0; q < size(); q++ ) 
            if( a_name.compareTo( getParameter( q ) ) == 0 )
                return q;
        return -1;
    }
    
    public Hashtable getKeyValue()
    {
        Hashtable ht = new Hashtable( size() );
        for( int q = 0; q < size(); q++ ) 
            ht.put( getParameter( q ), getValue( q ) );
        return ht;
    }
}
