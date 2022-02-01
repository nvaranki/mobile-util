/*
 * Registar.java
 * Created on September 3, 2005, 5:55 PM
 * Copyright 2005 Nikolai Varankine. All rights reserved.
 *
 * This class implements standard query to registration server, parses reply 
 * and registers received IDs using class Registry.
 */

package com.varankin.mobile.util;

import com.varankin.mobile.Dispatcher;
import com.varankin.mobile.http.*;
import java.util.Hashtable;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;

/**
 * @author  Nikolai Varankine
 */
public class Registar extends InquireForm implements ItemStateListener, HttpLinker 
{
    private TextField field_pass;
    private String url;
    private Command CMD_REGISTER;
    private Gauge m_gauge;
    private int m_gauge_index = -1, m_index_pass;
    private Hashtable m_options;
    private final static String accepted_option = "Accept";
    private final static String accepted_value = "application/vnd.wap.wmlc";
    
    /** Creates a new instance of Registar */
    public Registar( Dispatcher a_parent, Displayable a_callback, String a_url ) 
    {
        super( a_parent, a_callback, "Pass" );
        url = a_url;
        m_options = new Hashtable(); 
        m_options.put( accepted_option, accepted_value );

        // set up menu
        CMD_REGISTER = new Command( parent.getString(this,"Menu.Run"), Command.SCREEN, 1 );
        addCommand( CMD_BACK ); 
        addCommand( CMD_HELP ); 
        setItemStateListener( this );

        // update registration code
        m_index_pass = query.indexByParameter( "code" );
        field_pass = (TextField) query.getItem( m_index_pass );
        String value_pass = parent.registry.getValue( Dispatcher.RKEY_PASS );
        if( value_pass != null ) query.setValue( m_index_pass, value_pass );

        // setup extra GUI controls, informational for registration process
        try
        {
            // identification number of device
            append( new StringItem( parent.getString(this,"DeviceID"), 
                parent.registry.getValue( Dispatcher.RKEY_INIT ) ) );
            
            // software license expiration date
            append( new StringItem( parent.getString(this,"Expires"), 
                parent.formatDateTime( parent.registry.getValue( Dispatcher.RKEY_EXPIRES ) ) ) );
        } 
        catch( Exception e )
        {
            parent.setCurrent( AlertType.ERROR, 2, callback, e );
        }
        m_gauge = new Gauge( parent.getString( this, "Progress" ), false, 100, 0 );

        // final steps
        if( query.getValue( m_index_pass ).trim().length() != 0 ) 
        {
            addCommand( CMD_REGISTER ); // for manual inquire
            if( false && parent.registry.getValue( HttpLink.ACCOUNT ) == null ) inquire( null ); // auto-inquire
            else parent.setCurrent( this );
        }
        else parent.setCurrent( this );
    }
    
    public void itemStateChanged( Item a_item )
    {
      try
      {
        if( a_item == field_pass )
        {
            // reflect new/updated value immediately in Registry
            parent.registry.setValue( Dispatcher.RKEY_PASS, field_pass.getString().trim() );
            // update GUI accordingly
            if( field_pass.getString().length() != 0 ) 
                addCommand( CMD_REGISTER );
            else
                removeCommand( CMD_REGISTER );
        }
      }
      catch( RecordStoreException e )
      {
            parent.setCurrent( AlertType.ERROR, 1, this, e );
      }
    }    
    
    /**
     * Called when action should be handled
     */
    public void commandAction( Command a_command, Displayable a_displayable )
    {
        if( a_command == CMD_REGISTER ) inquire( null ); // start binary request to server
        else super.commandAction( a_command, a_displayable );
    }
    
    private void inquire( Alert a_message )
    {
        removeCommand( CMD_REGISTER );
        m_gauge_index = append( m_gauge );
        if( false )
        {
            parent.setCurrent( a_message, this );
            ( new HttpLink( HttpConnection.POST, parent.getServerURL( url ), 
                    query.getKeyValue(), m_options, m_gauge, this ) ).start();
        }
        else if( query.getValue( m_index_pass ).trim().length() != 0 ) 
            parent.setCurrent( a_message, new HttpLinkMonitor( HttpConnection.POST, 
                parent.getServerURL( url ), query.getKeyValue(), m_options, 
                new StringItem( null, parent.getString( this, "Progress" ) ), this ) );
        else
        {
            parent.setCurrent( a_message, this );
            addCommand( CMD_REGISTER );
        }
    }

    private void parseReply( String a_reply )
    {
        final String[] token = { "content=\"", "id=\"" };
        final String[] key = { Dispatcher.RKEY_EXPIRES, Dispatcher.RKEY_INIT };
        String[] value = { null, null };
        String meta;

        for( int mp = 0; a_reply != null; mp += meta.length() )
        {
            // find and extract <meta ...>
            mp = a_reply.indexOf( "<meta", mp );
            if( mp++ < 0 ) break;
            meta = a_reply.substring( mp, a_reply.indexOf( '>', mp ) );

            if( meta.indexOf( "http-equiv=\"Expires\"" ) > 0 )
                for( int t = 0; t < token.length; t++ )
                {
                    // find and extract attributes; order of them varies
                    int tp = meta.indexOf( token[ t ] );
                    if( tp > 0 ) 
                    {
                        // extract value
                        tp += token[ t ].length();
                        value[ t ] = meta.substring( tp, meta.indexOf( '"', tp ) );
                        // convert from hex to decimal
                        value[ t ] = Long.toString( Long.parseLong( value[ t ], 16 ) );
                        try 
                        {
                            // save value
                            parent.registry.setValue( key[ t ], value[ t ] ); 
                        }
                        catch( Exception e ) 
                        {  
                            // inform and stay back in form
                            parent.setCurrent( AlertType.ERROR, 6, this, e );
                            return;
                        }
                    }
                }
        } 
        
        if( value[0] == null || value[1] == null )
            // inform and stay back in form
            parent.setCurrent( AlertType.ERROR, 5, this, null );

        else 
            // inform and switch to previous screen
            parent.setCurrent( new Alert( 
                parent.getString( this, "Alert.4.Title" ), 
                parent.getString( this, "Expires" ) 
                + "\n" + parent.formatDateTime( value[0] ), 
                null, AlertType.INFO ), callback );
    }
    
    public void completed( byte[] a_reply, int a_reply_size, HttpConnection a_conn )
    {
        String type = a_conn.getType();
        if( type.regionMatches( true, 0, accepted_value, 0, accepted_value.length() ) )
            // general rule for binary WAP formats, content type is embedded
            parseReply( (new Wml( a_reply )).toString() );
        else
            // unrecognized feedback
            parent.setCurrent( AlertType.ERROR, 5, this, null );
        delete( m_gauge_index );
        addCommand( CMD_REGISTER );
    }
    public void interrupted( Exception a_problem )
    {
        delete( m_gauge_index );
        addCommand( CMD_REGISTER );
        parent.setCurrent( AlertType.ERROR, 5, this, a_problem );
    }
    public Dispatcher getDispatcher()
    {
        return parent;
    }

}
