/*
 * InquireForm.java
 * Created on January 29, 2005, 1:08 PM
 * Copyright 2005 Nikolai Varankine. All rights reserved.
 *
 * This class implements universal query and submit form.
 */

package com.varankin.mobile.util;

import com.varankin.mobile.Dispatcher;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;

/**
 * @author  Nikolai Varankine
 */
public abstract class InquireForm extends Form implements CommandListener
{
    public final static int OK = 0;
    public final static int ERROR = 1;
    public final static int ABORTED = 2;
    
    public Query query;

    protected Dispatcher parent;
    protected Displayable callback;
    protected Command CMD_BACK, CMD_HELP;

    public InquireForm( Dispatcher a_parent, Displayable a_callback, String a_query_name )
    {
        super( null );
        parent = a_parent;
        callback = a_callback;
        // make default menu commands
        CMD_BACK = new Command( parent.getString(this,"Menu.Back"), Command.BACK, 1 );
        CMD_HELP = new Command( parent.getString(this,"Menu.Help"), Command.SCREEN, 9 );
        // insert title
        setTitle( parent.getString(this,"Title") );
        // add query fields to screen
        query = new Query( a_parent, a_query_name );
        for( int q = 0; q < query.size(); q++ ) if( query.isVisible( q ) )
            append( query.getItem( q ) );
        // this class always monitors own commands
        setCommandListener(this);
    }

    public void addCommand( Command a_cmd )
    {
        if( a_cmd == CMD_HELP )
            switch( parent.getHelpMode() )
            {
            case Dispatcher.HELP_TICKER:
                // start dynamic help
                setTicker( new Ticker( parent.getString(this,"Ticker") ) );
                break;
            case Dispatcher.HELP_COMMAND:
                // start static help
                super.addCommand( CMD_HELP );
                break;
            }
        else
            super.addCommand( a_cmd );
    }

    public void commandAction( Command a_command, Displayable a_displayable ) 
    {
        if( a_command == CMD_BACK ) 
            parent.setCurrent( callback );

        else if( a_command == CMD_HELP ) 
        {
            Alert help = new Alert( null, parent.getString(this,"Ticker"), 
                null, AlertType.INFO );
            help.setTimeout( Alert.FOREVER );
            parent.setCurrent( help );
        }
    }

    public void completed( String a_reply, int a_status )
    {
        final String prefix = "InquireForm.Alert.";
        
        switch( a_status )
        {
        case InquireForm.ABORTED:
            // inform and stay back in form
            parent.setCurrent( new Alert( 
                parent.getString(prefix+"1.Title"),     
                parent.getString(prefix+"1.Message"), 
                null, AlertType.CONFIRMATION ), this );
            break;

        case InquireForm.ERROR:
            // inform, stay still, return to form
            Alert error = new Alert( 
                    parent.getString(prefix+"2.Title"), a_reply, 
                    null, AlertType.ERROR );
            //error.setTimeout( Alert.FOREVER );
            parent.setCurrent( error, this );
            break;
        }
    }
}
