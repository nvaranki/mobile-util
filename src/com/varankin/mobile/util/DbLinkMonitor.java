/*
 * DbLinkMonitor.java
 * Created on January 29, 2005, 12:18 PM
 * Copyright 2006 Nikolai Varankine. All rights reserved.
 *
 * This class implements dialog starting download process, 
 * indication of progress and analysis of experienced problems.
 */

package com.varankin.mobile.util;

import com.varankin.mobile.Dispatcher;
import com.varankin.mobile.http.HttpLink;
import com.varankin.mobile.http.HttpLinker;
import java.util.Hashtable;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;

/**
 * @author  Nikolai Varankine
 */
public class DbLinkMonitor extends Form implements CommandListener, HttpLinker
{
    public Gauge progress; // threads update its value, supposed inside 0..100
    
    private Command command_stop;
    private HttpLink link;
    private InquireForm form;
    private Dispatcher m_parent;

    /**
     * constructors
     *
     * NOTE: this is a non-permanent object, don't reuse!
     */
    public DbLinkMonitor( Dispatcher a_parent, String a_method, String a_accepted, 
        String a_url, Query a_query, Item a_contents, InquireForm a_form )
    {
        super( null );
        form = a_form;
        m_parent = a_parent;
        setTitle( a_parent.getString(this,"Title") );
        
        // insert GUI elements
        if( a_contents != null ) append( a_contents );
        progress = new Gauge( null, false, 100, 0 );
        progress.setValue( 0 );
        append( progress );

        // Set up this form to listen to command events
        command_stop = new Command( a_parent.getString(this,"Menu.Stop"), Command.STOP, 1 );
        addCommand( command_stop );
        setCommandListener(this);
        
        // start connection as separate thread immediately
        //link = new DbLink( a_parent, a_method, a_accepted, a_url, a_query, progress, form );
        Hashtable params = null, options = null;
        if( a_method.compareTo( HttpConnection.GET ) == 0 )
            params = a_query.getKeyValue();
        else
            options = a_query.getKeyValue();
        if( options == null ) options = new Hashtable( 1 );
        options.put( "Accept",  a_accepted );
        link = new HttpLink( a_method, a_url, params, options, progress, this );
    }

    private String toString( byte[] data, int bytesread, String a_content_type )
    {
        String rv = null, charset = "charset=";
        if( a_content_type == null )
        {
            // look for and examine this meta
            // <META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=Windows-1251">
            
            String temp = new String( data, 0, bytesread );
            // verify charset using document data
            int eqp = temp.indexOf( "http-equiv=\"Content-Type\"" );
            if( eqp < 0 ) eqp = temp.indexOf( "HTTP-EQUIV=\"Content-Type\"" );
            if( eqp >= 0 )
            {
                // cut string to have meta attributes only
                temp = temp.substring( temp.lastIndexOf( '<', eqp )+1, temp.indexOf( '>', eqp ) );
                
                // cut string to have content only
                eqp = temp.indexOf("content=\"");
                if( eqp < 0 ) eqp = temp.indexOf("CONTENT=\"");
                eqp += 9;
                a_content_type = temp.substring( eqp, temp.indexOf( '"', eqp ) );
            }
            else
                a_content_type = "text/html";
        }

        // parse content type
        if( a_content_type.indexOf( charset ) >= 0 )
            charset = a_content_type.substring( a_content_type.indexOf( charset ) + charset.length() );
        else
            charset = "ASCII";
        if( a_content_type.indexOf( ';' ) >= 0 )
            a_content_type = a_content_type.substring( 0, a_content_type.indexOf( ';' ) );

        // convert byte stream to string
        if( a_content_type.regionMatches( true, 0, "text/", 0, 5 ) )
        {
            // general rule for texts
            try { rv = new String( data, 0, bytesread, charset ); }
            catch( java.io.UnsupportedEncodingException e )
                { rv = new String( data, 0, bytesread ); }
        }
        else if( a_content_type.regionMatches( true, 0, "application/", 0, 12 ) )
        {
            // general rule for binary WAP formats, content type is embedded
            rv = (new Wml( data )).toString();
        }
        else
        {
            rv = new String( data, 0, bytesread );
        }
        
        return rv;
    }
        
    /**
     * Called when user action should be handled
     */
    public void commandAction( Command a_command, Displayable a_displayable ) 
    {
        if( a_command == command_stop ) 
        {
            // place stop indicator in DbLink, no promise it will terminate instantly
            link.terminate();
            // return back to form, link becomes unlinked and unhandled but still working - BAD (Java)
            form.completed( null, InquireForm.ABORTED );
        }
    }

    public void completed( byte[] a_reply, int a_reply_size, HttpConnection a_conn )
    {
        form.completed( toString( a_reply, a_reply_size, a_conn.getType() ), InquireForm.OK );
    }
    public void interrupted( Exception a_problem )
    {
        form.completed( a_problem != null ? a_problem.getMessage() : null, InquireForm.ERROR );
    }

    public Dispatcher getDispatcher()
    {
        return m_parent;
    }
}
