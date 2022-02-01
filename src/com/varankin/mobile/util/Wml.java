/*
 * Wmlc.java
 *
 * Created on March 6, 2005, 10:02 PM
 */

package com.varankin.mobile.util;

import java.lang.*;

/**
 * @author  Nikolai Varankine
 */
public class Wml
{
    // Global tokens
    private final static int TOKEN_SWITCH_PAGE = 0x00; // Change the code page for the current token state. Followed by a single u_int8 indicating the new code page number.
    private final static int TOKEN_END = 0x01; // Indicates the end of an attribute list or the end of an element.
    private final static int TOKEN_ENTITY = 0x02; // A character entity. Followed by a mb_u_int32 encoding the character entity number.
    private final static int TOKEN_STR_I = 0x03; // Inline string. Followed by a termstr. 
    private final static int TOKEN_STR_T = 0x83; // String table reference. Followed by a mb_u_int32 encoding a byte offset from the beginning of the string table.
    private final static int TOKEN_LITERAL = 0x04; // An unknown tag or attribute name. Followed by an mb_u_int32 that encodes an offset into the string table.
    private final static int TOKEN_LITERAL_A = 0x84;
    private final static int TOKEN_LITERAL_C = 0x44;
    private final static int TOKEN_LITERAL_AC = 0xC4;
    private final static int TOKEN_PI = 0x43; // Processing instruction. 
    private final static int TOKEN_OPAQUE = 0xC3; // Opaque document-type-specific data. 
    private final static int TOKEN_EXT_I_0 = 0x40; // Inline string document-type-specific extension token. Token is followed by a termstr. 
    private final static int TOKEN_EXT_I_1 = 0x41;
    private final static int TOKEN_EXT_I_2 = 0x42;
    private final static int TOKEN_EXT_T_0 = 0x80; // Inline integer document-type-specific extension token. Token is followed by a mb_uint_32.
    private final static int TOKEN_EXT_T_1 = 0x81;
    private final static int TOKEN_EXT_T_2 = 0x82;
    private final static int TOKEN_EXT_0 = 0xC0; // Single-byte document-type-specific extension token.
    private final static int TOKEN_EXT_1 = 0xC1;
    private final static int TOKEN_EXT_2 = 0xC2;

    private final static int TOKEN_TERM_STR = 0x00;
    
    private StringBuffer buffer;
    private byte[] string_table; // stores <length>{<stringN>0}
    private int wmlcp;
    private final static String default_encoding = "ASCII";
    private String encoding;
    
    /** Creates a new instance of Wml */
    public Wml( byte[] a_wmlc )
    {
        buffer = new StringBuffer( a_wmlc.length*3 );
        toWml( a_wmlc );
    }
    public Wml( String a_wml )
    {
        buffer = new StringBuffer( a_wml );
    }
    public Wml( int a_variant ) // DEBUG ONLY!!!
    {
    // example #1
    //  <?xml version="1.0"?> 
    //  <!DOCTYPE wml PUBLIC "-//WAPFORUM//DTD WML 1.1//EN" "http://www.wapforum.org/DTD/wml_1.1.xml"> 
    //  <wml>
    //  <card>
    //  </card>
    //  </wml>
    byte[] wml1 = { 0x01, 0x04, 0x6a, 0x00, 0x7f, 0x27, 0x01 };
    
    // example #2
    //  <?xml version="1.0"?> 
    //  <!DOCTYPE wml PUBLIC "-//WAPFORUM//DTD WML 1.1//EN" "http://www.wapforum.org/DTD/wml_1.1.xml"> 
    //  <wml>
    //  <card title="hi">
    //  <p>
    //  bye
    //  </p>
    //  </card>
    //  </wml>
    byte[] wml2 = { 0x01, 0x04, 0x6a, 0x00, 0x7f, 
        (byte)0xe7, 0x36, 0x03, 0x68, 0x69, 0x00, 0x01, 
        0x60, 0x03, 0x20, 0x62, 0x79, 0x65, 0x20, 0x00, 0x01, 
        0x01, 0x01 };
    
    // example #3
    //  <WML>
    //  <CARD NAME="abc" STYLE="LIST">
    //  <DO TYPE="ACCEPT">
    //  <GO URL="http://xyz.org/s"/>
    //  </DO>
    //  X: $(X)<BR/>
    //  Y: $(&#x59;)<BR MODE="NOWRAP"/>
    //  Enter name: <INPUT TYPE="TEXT" KEY="N"/>
    //  </CARD>
    //  </WML>
    byte[] wml3 = { 0x00, 0x02, 0x04, 'X', 0x00, 'Y', 0x00, 
        0x7F, (byte)0xE8, 0x21, 0x03, 'a','b','c', 0x00, 0x33, 0x01, 
        (byte)0xE9, 0x38, 0x01, (byte)0xAD, 0x4B, 0x03, 'x','y','z', 0x00, 
        (byte)0x88, 0x03, 's',  0x00, 0x01, /*0x01,*/ 0x03, ' ','X',':',' ', 0x00, 
        (byte)0x82, 0x00, 0x27, 0x03, ' ','Y',':',' ', 0x00, 
        (byte)0x82, 0x02, (byte)0xA7, 0x1D, 0x01, 0x03, ' ','E','n','t',
        'e','r',' ','n','a','m','e',':',' ', 0x00, 
        (byte)0xB1, 0x48, 0x18, 0x03, 'N', 0x00,  0x01, 0x01, 0x01 };
        
    byte[][] sample = { wml1, wml2, wml3 };
    
    buffer = new StringBuffer( sample[ a_variant ].length*3 );
    toWml( sample[ a_variant ] );
    }

    private void toWml( byte[] a_wmlc )
    {
        // make a copy for internal use
        wmlcp = 0;
        // parse contents
        try
        {
            buffer.append( getVersionNumber( a_wmlc[ wmlcp ] ) ); 
            buffer.append( getPublicId( a_wmlc ) ); 
            encoding = getEncoding( a_wmlc ); 
            readStringTable( a_wmlc );
            while( a_wmlc[ wmlcp ] == TOKEN_PI ) 
                readProcessingInstruction( a_wmlc );
            readElement( a_wmlc );
            while( wmlcp < a_wmlc.length && a_wmlc[ wmlcp ] == TOKEN_PI ) 
                readProcessingInstruction( a_wmlc );
        }
        catch( ArrayIndexOutOfBoundsException e ) { buffer.append( e.toString() ); }
        catch( java.util.NoSuchElementException e ) { buffer.append( e.toString() ); }
        catch( java.lang.Exception e ) { buffer.append( e.toString() ); }
    }
    private void readProcessingInstruction( byte[] a_wmlc )
    {
        while( a_wmlc[ wmlcp++ ] != TOKEN_END ); //DEBUG - skips
    }
    private String getVersionNumber( byte a_wmlc )
    {
        int minor = a_wmlc & 0x0f;
        int major = ( a_wmlc & 0xf0 ) + 1;
        wmlcp++;
        return "<?xml version=\"" +Integer.toString( major ) + 
            "." + Integer.toString( minor ) + "\"?>";
    }
    private String getPublicId( byte[] a_wmlc )
    {
        final String[] versions = 
        {
            null,
            "UNKNOWN",
            "\"-//WAPFORUM//DTD WML 1.0//EN\"\n\"http://www.wapforum.org/DTD/wml.xml\"",
            "\"-//WAPFORUM//DTD WTA 1.0//EN\"",
            "\"-//WAPFORUM//DTD WML 1.1//EN\"\n\"http://www.wapforum.org/DTD/wml_1.1.xml\"",
            "\"-//WAPFORUM//DTD SI 1.0//EN\"\n\"http://www.wapforum.org/DTD/si.dtd\"",
            "\"-//WAPFORUM//DTD SL 1.0//EN\"\n\"http://www.wapforum.org/DTD/sl.dtd\"",
            "\"-//WAPFORUM//DTD CO 1.0//EN\"",
            "\"-//WAPFORUM//DTD CHANNEL 1.1//EN\"",
            "\"-//WAPFORUM//DTD WML 1.2//EN\"\n\"http://www.wapforum.org/DTD/wml12.dtd\""
        };
        int id = getMultiByteInteger( a_wmlc );
        if( id != 0 ) return "<!DOCTYPE wml PUBLIC " + versions[ id ] + ">";
        else 
        {
            id = getMultiByteInteger( a_wmlc ); // index to string table
            return "<!DOCTYPE wml PUBLIC " + versions[ 1 ] + ">"; //DEBUG, string is empty yet
        }
        
    }
    private String getEncoding( byte[] a_wmlc )
    {
        final String[] encodings_s = 
        {
            // taken from http://www.iana.org/assignments/character-sets
            // Assigned MIB enum Numbers:
            // 0-2		Reserved
            // 3-999		Set By Standards Organizations
            // 1000-1999	Unicode / 10646
            // 2000-2999	Vendor
            null, // 0
            null, // 1
            null, // 2
            //--------
            "ASCII", // 3 - ISO646-US
            "ISO-8859-1", // 4 - latin1
            "ISO-8859-2", // 5 - latin2
            "ISO-8859-3", // 6 - latin3
            "ISO-8859-4", // 7 - latin4
            "ISO-8859-5", // 8 - cyrillic
            "ISO-8859-6", // 9 - arabic
            "ISO-8859-7", // 10 - greek8
            "ISO-8859-8", // 11 - hebrew
            "ISO-8859-9", // 12 - latin5
            "ISO-8859-10", // 13 - latin6
            null, // 14
            null, // 15
            "JIS_Encoding", // 16
            "Shift_JIS", // 17 - MS_Kanji
            "EUC-JP", // 18
            null/*
            null, // 19
            null, // 20
            null, // 21
            null, // 22
            null, // 23
            null, // 24
            null, // 25
            null, // 26
            null, // 27
            null, // 28
            null, // 29
            null, // 30
            null, // 31
            null, // 32
            null, // 33
            null, // 34
            null, // 35
            null, // 36
            null, // 37
            null, // 38
            null, // 39
            null, // 40
            null, // 41 - katakana
            null, // 42 or "ISO646-JP"
            null, // 43
            null, // 44
            null, // 45
            null, // 46
            "Latin-greek-1", // 47
            "ISO_5427", // 48 - cyrillic
            null, // 49
            null, // 50
            null, // 51
            null, // 52
            "INIS-cyrillic", // 53
            null, // 54 - cyrillic
            null, // 55
            null, // 56
            null, // 57
            null, // 58
            null, // 59
            null, // 60
            null, // 61
            null, // 62
            null, // 63
            null, // 64
            null, // 65
            null, // 66
            null, // 67
            null, // 68
            null, // 69
            null, // 70
            null, // 71
            null, // 72
            null, // 73
            null, // 74
            null, // 75
            null, // 76
            "KOI8-E", // 77
            null, // 78
            null, // 79
            null, // 80
            "ISO-8859-6-E", // 81
            "ISO-8859-6-I", // 82
            null, // 83
            "ISO-8859-8-E", // 84
            "ISO-8859-8-I", // 85
            null, // 86
            null, // 87
            null, // 88
            null, // 89
            null, // 90
            null, // 91
            null, // 92
            null, // 93
            null, // 94
            null, // 95
            null, // 96
            null, // 97
            null, // 98
            null, // 99
            null, // 100
            null, // 101
            null, // 102
            "UNICODE-1-1-UTF-7", // 103
            "ISO-2022-CN", // 104
            "ISO-2022-CN-EXT", // 105
            "UTF-8", // 106
            null, // 107
            null, // 108
            "ISO-8859-13", // 109
            "ISO-8859-14", // 110
            "ISO-8859-15", // 111 - latin9
            "ISO-8859-16", // 112 - latin10
            null // 113
            */
        };
        final String[] encodings_u = 
        {
            // continuation of previous table
            "ISO-10646-UCS-2",	// 1000
            "ISO-10646-UCS-4",	// 1001
            "ISO-10646-UCS-Basic",	// 1002
            "ISO-10646-Unicode-Latin1",	// 1003
            "ISO-Unicode-IBM-1261",	// 1005
            "ISO-Unicode-IBM-1268",	// 1006
            "ISO-Unicode-IBM-1276",	// 1007
            "ISO-Unicode-IBM-1264",	// 1008
            "ISO-Unicode-IBM-1265",	// 1009
            "UNICODE-1-1",	// 1010
            "SCSU",	// 1011
            "UTF-7",	// 1012
            "UTF-16BE",	// 1013
            "UTF-16LE",	// 1014
            "UTF-16",	// 1015
            "CESU-8",	// 1016
            "UTF-32",	// 1017
            "UTF-32BE",	// 1018
            "UTF-32LE",	// 1019
            "BOCU-1"	// 1020
        };
        
        String enc;
        int mib=0;
        if( a_wmlc[ 0 ] == 0x00 )
            enc = "ISO-10646"; // by default ISO10646 or Unicode 2.0
        else
        {
            mib = getMultiByteInteger( a_wmlc );
            if( mib == 106 )
                enc = "UTF-8";
            else if( mib < encodings_s.length && encodings_s[ mib ] != null )
                enc = encodings_s[ mib ];
            else if( mib - 1000 < encodings_u.length )
                enc = encodings_u[ mib - 1000 ];
            else
                enc = "UTF-8"; // wrong or unknown MIB number
        }
        //try { "abc".getBytes( enc ); }
        //catch( java.io.UnsupportedEncodingException e ) { enc = "ASCII"; }
        //buffer.append( "ENCODING:"+enc+"("+Integer.toString(mib)+")"); //DEBUG
        return enc;
    }
    private void readStringTable( byte[] a_wmlc )
    {
        int length = getMultiByteInteger( a_wmlc );
        string_table = new byte[ length + 1 ]; // reserve byte to avoid zero size
        // copy table as-is
        for( int b = 0; b < length; b++ ) string_table[ b ] = a_wmlc[ wmlcp++ ];
        // end with safety stopper
        string_table[ length ] = 0x00; 
    }
    private String getTableString( byte[] a_wmlc )
    {
        int offset = getMultiByteInteger( a_wmlc );
        String rv = "?";
        if( offset < string_table.length ) 
        {
            // compute length of string
            int length;
            for( length = 0; string_table[ offset + length ] != 0x00; length++ );
            // extract string
            try { rv = new String( string_table, offset, length, encoding ); }
            catch( java.io.UnsupportedEncodingException e ) 
            { rv = e.toString(); }
              //DEBUG new String( string_table, offset, length ); }
        }
        return rv;
    }
    private static String getAttributeStart( byte a_token )
    {
        final String[] type = 
        {
            "accept-charset",	// 0x05
            "align",	// 0x06
            "align",	// 0x07
            "align",	// 0x08
            "align",	// 0x09
            "align",	// 0x0a
            "align",	// 0x0b
            "alt",	// 0x0c
            "content",	// 0x0d
            null,	// 0x0e
            "domain",	// 0x0f
            "emptyok",	// 0x10
            "emptyok",	// 0x11
            "format",	// 0x12
            "height",	// 0x13
            "hspace",	// 0x14
            "ivalue",	// 0x15
            "iname",	// 0x16
            null,	// 0x17
            "label",	// 0x18
            "localsrc",	// 0x19
            "maxlength",// 0x1a
            "method",	// 0x1b
            "method",	// 0x1c
            "mode",	// 0x1d
            "mode",	// 0x1e
            "multiple",	// 0x1f
            "multiple",	// 0x20
            "name",	// 0x21
            "newcontext",	// 0x22
            "newcontext",	// 0x23
            "onpick",	// 0x24
            "onenterbackward",	// 0x25
            "onenterforward",	// 0x26
            "ontimer",	// 0x27
            "optional",	// 0x28
            "optional",	// 0x29
            "path",	// 0x2a
            null,	// 0x2b
            null,	// 0x2c
            null,	// 0x2d
            "scheme",	// 0x2e
            "sendreferer",	// 0x2f
            "sendreferer",	// 0x30
            "size",	// 0x31
            "src",	// 0x32
            "ordered",	// 0x33
            "ordered",	// 0x34
            "tabindex",	// 0x35
            "title",	// 0x36
            "type",	// 0x37
            "type",	// 0x38
            "type",	// 0x39
            "type",	// 0x3a
            "type",	// 0x3b
            "type",	// 0x3c
            "type",	// 0x3d
            "type",	// 0x3e
            "type",	// 0x3f
            null,	// 0x40
            null,	// 0x41
            null,	// 0x42
            null,	// 0x43
            null,	// 0x44
            "type",	// 0x45
            "type",	// 0x46
            "type",	// 0x47
            "type",	// 0x48
            "type",	// 0x49
            "href",	// 0x4a
            "href",	// 0x4b
            "href",	// 0x4c
            "value",	// 0x4d
            "vspace",	// 0x4e
            "width",	// 0x4f
            "xml:lang",	// 0x50
            null,	// 0x51
            "align",	// 0x52
            "columns",	// 0x53
            "class",	// 0x54
            "id",	// 0x55
            "forua",	// 0x56
            "forua",	// 0x57
            "src",	// 0x58
            "src",	// 0x59
            "http-equiv",	// 0x5a
            "http-equiv",	// 0x5b
            "content",	// 0x5c
            "http-equiv",	// 0x5d
            "accesskey",	// 0x5e
            "enctype",	// 0x5f
            "enctype",	// 0x60
            "enctype"	// 0x61
            // ...
            // last     // 0x7f 
        }; //93 total
        return type[ (a_token & 0xff) - 0x05 ];
    }
    private static String getAttributeValuePrefix( byte a_token )
    {
        final String[] prefix = 
        {
            null,	// 0x05
            "bottom",	// 0x06
            "center",	// 0x07
            "left",	// 0x08
            "middle",	// 0x09
            "right",	// 0x0a
            "top",	// 0x0b
            null,	// 0x0c
            null,	// 0x0d
            null,	// 0x0e
            null,	// 0x0f
            "false",	// 0x10
            "true",	// 0x11
            null,	// 0x12
            null,	// 0x13
            null,	// 0x14
            null,	// 0x15
            null,	// 0x16
            null,	// 0x17
            null,	// 0x18
            null,	// 0x19
            null,	// 0x1a
            "get",	// 0x1b
            "post",	// 0x1c
            "nowrap",	// 0x1d
            "wrap",	// 0x1e
            "false",	// 0x1f
            "true",	// 0x20
            null,	// 0x21
            "false",	// 0x22
            "true",	// 0x23
            null,	// 0x24
            null,	// 0x25
            null,	// 0x26
            null,	// 0x27
            "false",	// 0x28
            "true",	// 0x29
            null,	// 0x2a
            null,	// 0x2b
            null,	// 0x2c
            null,	// 0x2d
            null,	// 0x2e
            "false",	// 0x2f
            "true",	// 0x30
            null,	// 0x31
            null,	// 0x32
            "true",	// 0x33
            "false",	// 0x34
            null,	// 0x35
            null,	// 0x36
            null,	// 0x37
            "accept",	// 0x38
            "delete",	// 0x39
            "help",	// 0x3a
            "password",	// 0x3b
            "onpick",	// 0x3c
            "onenterbackward",	// 0x3d
            "onenterforward",	// 0x3e
            "ontimer",	// 0x3f
            null,       // 0x40
            null,       // 0x41
            null,       // 0x42
            null,       // 0x43
            null,       // 0x44
            "options",	// 0x45
            "prev",	// 0x46
            "reset",	// 0x47
            "text",	// 0x48
            "vnd.",	// 0x49
            null,	// 0x4a
            "http://",	// 0x4b
            "https://",	// 0x4c
            null,	// 0x4d
            null,	// 0x4e
            null,	// 0x4f
            null,	// 0x50
            null,	// 0x51
            null,	// 0x52
            null,	// 0x53
            null,	// 0x54
            null,	// 0x55
            "false",	// 0x56
            "true",	// 0x57
            "http://",	// 0x58
            "https://",	// 0x59
            null,	// 0x5a
            "Content-Type",	// 0x5b
            "application/vnd.wap.wmlc;charset=",	// 0x5c
            "Expires",	// 0x5d
            null,	// 0x5e
            null,	// 0x5f
            "application/x-www-form-urlencoded",	// 0x60
            "multipart/form-data" 	// 0x61
        }; //93 total
        String rv = prefix[ (a_token & 0xff) - 0x05 ];
        return rv != null ? rv : "";
    }
    private static String getAttributeValue( byte a_token )
    {
        final String[] value = 
        {
            // first allowable // 0x80
            // ...
            ".com/",           // 0x85
            ".edu/",           // 0x86
            ".net/",           // 0x87
            ".org/",           // 0x88
            "accept",          // 0x89
            "bottom",          // 0x8a
            "clear",           // 0x8b
            "delete",          // 0x8c
            "help",            // 0x8d
            "http://",         // 0x8e
            "http://www.",     // 0x8f
            "https://",        // 0x90
            "https://www.",    // 0x91
            "",                // 0x92
            "middle",          // 0x93
            "nowrap",          // 0x94
            "onpick",          // 0x95
            "onenterbackward", // 0x96
            "onenterforward",  // 0x97
            "ontimer",         // 0x98
            "options",         // 0x99
            "password",        // 0x9a
            "reset",           // 0x9b
            "",                // 0x9c
            "text",            // 0x9d
            "top",             // 0x9e
            "unknown",         // 0x9f
            "wrap",            // 0xa0
            "www."             // 0xa1
};
        return value[ (a_token & 0xff) - 0x85 ];
    }
    private String getTagName( byte[] a_wmlc )
    {
        final String[] tags = 
        {
            "pre",	// 0x1b
            "a",	// 0x1c
            "td",	// 0x1d
            "tr",	// 0x1e
            "table",	// 0x1f
            "p",	// 0x20
            "postfield",// 0x21
            // 14.3.2 Tag Tokens:
            "anchor",	// 0x22 //"a"?
            "access",	// 0x23
            "b",	// 0x24
            "big",	// 0x25
            "br",	// 0x26
            "card",	// 0x27
            "do",	// 0x28
            "em",	// 0x29
            "fieldset",	// 0x2a
            "go",	// 0x2b
            "head",	// 0x2c
            "i",	// 0x2d
            "img",	// 0x2e
            "input",	// 0x2f
            "meta",	// 0x30
            "noop",	// 0x31
            "prev",	// 0x32
            "onevent",	// 0x33
            "optgroup",	// 0x34
            "option",	// 0x35
            "refresh",	// 0x36
            "select",	// 0x37
            "small",	// 0x38
            "strong",	// 0x39
            null,	// 0x3a //"tab"?
            "template",	// 0x3b
            "timer",	// 0x3c
            "u",	// 0x3d
            "setvar",	// 0x3e //"var"?
            "wml" 	// 0x3f
            // no more
        };
        int token = a_wmlc[ wmlcp++ ] & 0x3f;
        return token == TOKEN_LITERAL 
            ? getTableString( a_wmlc ) 
            : tags[ token - 0x1b ];
    }
    private void readElement( byte[] a_wmlc  )
    {
        boolean hasAttribute = ( a_wmlc[ wmlcp ] & 0x80 ) != 0x00;
        boolean hasContent   = ( a_wmlc[ wmlcp ] & 0x40 ) != 0x00;
        String tag = getTagName( a_wmlc );
        
        buffer.append( '<' ).append( tag );
        
        if( hasAttribute )
        {
            while( a_wmlc[ wmlcp ] != TOKEN_END ) 
                readAttribute( a_wmlc );
            wmlcp++; // skip terminator of attribute list
        }
        
        if( hasContent ) 
        {
            buffer.append( '>' ); 
            while( a_wmlc[ wmlcp ] != TOKEN_END ) 
                switch( a_wmlc[ wmlcp++ ] & 0xff )
                {
                    case TOKEN_STR_I:
                        buffer.append( getInlineString( a_wmlc ) );
                        break;
                    case TOKEN_STR_T:
                        buffer.append( getTableString( a_wmlc ) );
                        break;
                    case TOKEN_EXT_I_0:
                    case TOKEN_EXT_I_1:
                    case TOKEN_EXT_I_2:
                        buffer.append( getInlineReference( a_wmlc, a_wmlc[ wmlcp-1 ] ) );
                        break;
                    case TOKEN_EXT_T_0:
                    case TOKEN_EXT_T_1:
                    case TOKEN_EXT_T_2:
                        buffer.append( getTableReference( a_wmlc, a_wmlc[ wmlcp-1 ] ) );
                        break;
                    case TOKEN_EXT_0:
                    case TOKEN_EXT_1:
                    case TOKEN_EXT_2:
                        break;
                    case TOKEN_ENTITY:
                        buffer.append( getEntity( a_wmlc ) );
                        break;
                    case TOKEN_PI:
                        wmlcp--;
                        readProcessingInstruction( a_wmlc );
                        break;
                    case TOKEN_OPAQUE:
                        for( int size = getMultiByteInteger( a_wmlc ); size > 0; size-- )
                            wmlcp++; //DEBUG - skips
                        break;
                    default: // nested element
                        wmlcp--;
                        readElement( a_wmlc );
                }
            wmlcp++; // skip terminator of element
            buffer.append( "</" ).append( tag ).append( '>' );
        }
        else
        {
            buffer.append( "/>" );
        }
    }
    private int getMultiByteInteger( byte[] a_wmlc )
    {
        int value = 0;
        do value = (value << 7) | (a_wmlc[ wmlcp ] & 0x7f);
        while( (a_wmlc[ wmlcp++ ] & 0x80) != 0x00 );
        return value;
    }
    private String getInlineReference( byte[] a_wmlc, byte a_token )
    {
        final String[] suffix = { ":e)", ":u)", ":n)" };
        return "$(" + getInlineString( a_wmlc ) 
            + suffix[ (a_token & 0xff) - TOKEN_EXT_I_0 ];
    }
    private String getTableReference( byte[] a_wmlc, byte a_token )
    {
        final String[] suffix = { ":e)", ":u)", ":n)" };
        return "$(" + getTableString( a_wmlc ) 
            + suffix[ (a_token & 0xff) - TOKEN_EXT_T_0 ];
    }
    private String getEntity( byte[] a_wmlc )
    {
        return "&#" + Integer.toString( getMultiByteInteger( a_wmlc ) ) + ";";
    }
    private String getInlineString( byte[] a_wmlc )
    {
        String rv;
        int len;
        for( len = 0; a_wmlc[ wmlcp + len ] != TOKEN_TERM_STR; len++ );
        try
        {
            rv = new String( a_wmlc, wmlcp, len, encoding );
            /*/DEBUG
            for( int b = 0; b < len; b++ )
                rv = rv.concat( Integer.toString(a_wmlc[wmlcp+b],16) );
            */
        }
        catch( java.io.UnsupportedEncodingException e )
        {
            rv = e.toString();
              //DEBUG new String( a_wmlc, wmlcp, len );
        }
        wmlcp += len + 1;
        return rv;
    }
    private void readAttribute( byte[] a_wmlc )
    {
        // extract name
        byte token = a_wmlc[ wmlcp++ ];
        String attrStart = token == TOKEN_LITERAL ? 
            getTableString( a_wmlc ) : getAttributeStart( token );
        
        // extract values
        StringBuffer attrValue = new StringBuffer( getAttributeValuePrefix( token ) );
        for( boolean finished = false; a_wmlc[ wmlcp ] != TOKEN_END && !finished; ) 
            switch( a_wmlc[ wmlcp++ ] & 0xff )
            {
                case TOKEN_STR_I:
                    attrValue.append( getInlineString( a_wmlc ) );
                    break;
                case TOKEN_STR_T:
                    attrValue.append( getTableString( a_wmlc ) );
                    break;
                case TOKEN_EXT_I_0:
                case TOKEN_EXT_I_1:
                case TOKEN_EXT_I_2:
                    attrValue.append( getInlineReference( a_wmlc, a_wmlc[ wmlcp-1 ] ) );
                    break;
                case TOKEN_EXT_T_0:
                case TOKEN_EXT_T_1:
                case TOKEN_EXT_T_2:
                    attrValue.append( getTableReference( a_wmlc, a_wmlc[ wmlcp-1 ] ) );
                    break;
                case TOKEN_EXT_0:
                case TOKEN_EXT_1:
                case TOKEN_EXT_2:
                    break;
                case TOKEN_ENTITY:
                    attrValue.append( getEntity( a_wmlc ) );
                    break;
                case TOKEN_LITERAL: // denotes start of next attribute
                    finished = true;
                    wmlcp--;
                    break;
                default:
                    if( ( a_wmlc[ wmlcp-1 ] & 0x80 ) == 0x80 )
                        attrValue.append( getAttributeValue( a_wmlc[ wmlcp-1 ] ) );
                    else
                    {
                        // start of next attribute detected
                        finished = true;
                        wmlcp--;
                    }
            }
            
        buffer.append( ' ' ).append( attrStart );
        if( attrValue.length() > 0 ) buffer.append( "=\"" ).append( attrValue.toString() ).append( '"' );
    }

    public String toString()
    {
        return buffer.toString(); 
    }
}
