

package us.k5n.ical;

import java.util.Vector;


/**
  * iCal Property class.  A Property represents one field in the iCal
  * data (ATTENDEE, DTSTART, SUMMARY, etc.)
  * A Property can contain multiple attributes.
  * See section 4.1.1 of RFC 2445 for details.
  * @version $Id$
  * @author Craig Knudsen, craig@k5n.us
  */
public class Property
  implements Constants
{
  /** Property name (DTSTART, SUMMARY, etc.)  This is always uppercase */
  public String name;
  /** Value of property (unfolded).  This may contain multiple values
      separated by commas. */
  public String value;
  protected Vector attributeList;

  public Property ( String name, String value )
  {
    this.name = name.toUpperCase ();
    this.value = value;
    attributeList = new Vector ();
  }

  /**
    * Construct a property from iCal data.
    * Parse an iCal line (or lines) into the name, properties and value.
    * This is typically a one-line String, but can be multiple lines
    * if iCal line folding was used (for Strings longer than 75 characters).
    * @param line	The iCal input String
    */
  public Property ( String line )
    throws ParseException
  {
    this ( line, PARSE_LOOSE );
  }

  /**
    * Construct a property from iCal data.
    * Parse an iCal line (or lines) into the name, properties and value.
    * This is typically a one-line String but can be multiple lines
    * if iCal line folding was used (for Strings longer than 75 characters).
    * See section 4.2 of RFC 2445 for details on attributes.
    * @param line	The iCal input String
    * @param parseMode	PARSE_STRICT or PARSE_LOOSE
    */
  public Property ( String line, int parseMode )
    throws ParseException
  {
    attributeList = new Vector ();
    String s = StringUtils.unfoldLine ( line, parseMode );

    // Find first ':'
    int loc = s.indexOf ( ':' );
    if ( loc < 0 ) {
      throw new ParseException ( "Could not find ':'", line );
    }

    String nameAndAttr = s.substring ( 0, loc );
    value = s.substring ( loc + 1, s.length() );

    // divide nameAndAttr up into the name and the various attributes
    // we need to be careful since a ';' might also be in quotes in
    // an attribute like: ATTENDEE;X="1;2";Y=4:ABC
    loc = nameAndAttr.indexOf ( ';' );
    if ( loc < 0 ) {
      // no attributes
      name = nameAndAttr.toUpperCase ();
    } else {
      name = nameAndAttr.substring ( 0, loc ).toUpperCase();
      StringBuffer p = new StringBuffer ();
      StringBuffer pv = new StringBuffer ();
      boolean inQuote = false;
      boolean inPName = true;
      for ( int i = loc; i < nameAndAttr.length(); i++ ) {
        char ch = nameAndAttr.charAt ( i );
        if ( ch == ';' && ! inQuote ) {
          if ( p.length() > 0 ) {
            addAttribute ( p.toString(), pv.toString() );
            p.setLength ( 0 );
            pv.setLength ( 0 );
            inPName = true;
          }
        } else if ( ch == '=' && ! inQuote ) {
          inPName = false;
        } else if ( ch == ',' && ! inQuote && parseMode == PARSE_STRICT ) {
          // ',' should be quoted
          throw new ParseException ( "Found unquoted comma in attribute value",
            line );
        } else if ( ch == '"' ) {
          inQuote = ! inQuote;
        } else {
          if ( inPName )
            p.append ( ch );
          else
            pv.append ( ch );
        }
      }
      if ( p.length() > 0 ) {
        addAttribute ( p.toString(), pv.toString() );
      }
    }
  }

  public void addAttribute ( Attribute a )
  {
    attributeList.addElement ( a );
  }

  public void addAttribute ( String name, String val )
  {
    attributeList.addElement ( new Attribute ( name, val ) );
  }

  public Attribute attributeAt ( int i )
  {
    return (Attribute) attributeList.elementAt ( i );
  }

  /**
    * Export to a properly folded iCal line.
    */
  public String toIcal ()
  {
    StringBuffer ret = new StringBuffer ( 40 );
    ret.append ( name );
    if ( attributeList.size() > 0 ) {
      for ( int i = 0; i < attributeList.size(); i++ ) {
        ret.append ( ';' );
        Attribute a = (Attribute) attributeList.elementAt ( i );
        ret.append ( a.name );
        // Always quote the value just to be safe
        ret.append ( "=\"" );
        ret.append ( a.value );
        ret.append ( '"' );
      }
    }
    ret.append ( ':' );
    ret.append ( value );

    return ( StringUtils.foldLine ( ret.toString () ) );
  }
}
