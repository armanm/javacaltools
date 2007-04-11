/*
 * Copyright (C) 2005-2006 Craig Knudsen and other authors
 * (see AUTHORS for a complete list)
 *
 * JavaCalTools is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * A copy of the GNU Lesser General Public License is included in the Wine
 * distribution in the file COPYING.LIB. If you did not receive this copy,
 * write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA.
 */

package us.k5n.ical;

import java.util.Calendar;

/**
 * Base class for use with a variety of date-related iCalendar fields including
 * LAST-MODIFIED, DTSTAMP, DTSTART, etc. This can represent both a date and a
 * date-time.
 * 
 * @version $Id$
 * @author Craig Knudsen, craig@k5n.us
 */
public class Date extends Property implements Constants, Comparable {
	int year, month, day;
	int hour, minute, second;
	boolean isUTC = false;
	boolean dateOnly = false; // is date only (rather than date-time)?
	static int[] monthDays = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
	static int[] leapMonthDays = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	/**
	 * Constructor
	 * 
	 * @param icalStr
	 *          One or more lines of iCalendar that specifies a date. Dates must
	 *          be of one of the following formats:
	 *          <ul>
	 *          <li> 19991231 (date only, no time) </li>
	 *          <li> 19991231T115900 (date with local time) </li>
	 *          <li> 19991231T115900Z (date and time UTC) </li>
	 *          </ul>
	 *          (This format is a based on the ISO 8601 standard.)
	 */
	public Date(String icalStr) throws ParseException, BogusDataException {
		this ( icalStr, PARSE_LOOSE );
	}

	/**
	 * Constructor: create a date based on the specified year, month and day.
	 * 
	 * @param dateType
	 *          Type of date; this should be an ical property name like DTSTART,
	 *          DTEND or DTSTAMP.
	 * @param year
	 *          The 4-digit year
	 * @param month
	 *          The month (1-12)
	 * @param day
	 *          The day of the month (1-31)
	 */

	public Date(String dateType, int year, int month, int day)
	    throws ParseException, BogusDataException {
		super ( dateType, "" );

		this.year = year;
		this.month = month;
		this.day = day;
		hour = minute = second = 0;
		dateOnly = true;

		String yearStr, monthStr, dayStr;

		yearStr = "" + year;
		monthStr = "" + month;
		dayStr = "" + day;
		while ( yearStr.length () < 4 )
			yearStr = '0' + yearStr;
		if ( monthStr.length () < 2 )
			monthStr = '0' + monthStr;
		if ( dayStr.length () < 2 )
			dayStr = '0' + dayStr;
		value = yearStr + monthStr + dayStr;

		// Add attribute that says date-only
		addAttribute ( "VALUE", "DATE" );
	}

	/**
	 * Constructor
	 * 
	 * @param icalStr
	 *          One or more lines of iCalendar that specifies a date
	 * @param parseMode
	 *          PARSE_STRICT or PARSE_LOOSE
	 */
	public Date(String icalStr, int parseMode) throws ParseException,
	    BogusDataException {
		super ( icalStr, parseMode );

		year = month = day = 0;
		hour = minute = second = 0;

		for ( int i = 0; i < attributeList.size (); i++ ) {
			Attribute a = attributeAt ( i );
			String aname = a.name.toUpperCase ();
			String aval = a.value.toUpperCase ();
			// TODO: not sure if any attributes are allowed here...
			// Look for VALUE=DATE or VALUE=DATE-TIME
			// DATE means untimed for the event
			if ( aname.equals ( "VALUE" ) ) {
				if ( aval.equals ( "DATE" ) ) {
					dateOnly = true;
				} else if ( aval.equals ( "DATE-TIME" ) ) {
					dateOnly = false;
				} else {
					if ( parseMode == PARSE_STRICT ) {
						throw new ParseException ( "Unknown date VALUE '" + a.value + "'",
						    icalStr );
					}
				}
			} else {
				// TODO: anything else allowed here?
			}
		}

		String inDate = value;

		if ( inDate.length () < 8 ) {
			// Invalid format
			throw new ParseException ( "Invalid date format '" + inDate + "'", inDate );
		}

		// Make sure all parts of the year are numeric.
		for ( int i = 0; i < 8; i++ ) {
			char ch = inDate.charAt ( i );
			if ( ch < '0' || ch > '9' ) {
				throw new ParseException ( "Invalid date format '" + inDate + "'",
				    inDate );
			}
		}
		year = Integer.parseInt ( inDate.substring ( 0, 4 ) );
		month = Integer.parseInt ( inDate.substring ( 4, 6 ) );
		day = Integer.parseInt ( inDate.substring ( 6, 8 ) );
		if ( day < 1 || day > 31 || month < 1 || month > 12 )
			throw new BogusDataException ( "Invalid date '" + inDate + "'", inDate );
		// Make sure day of month is valid for specified month
		if ( year % 4 == 0 ) {
			// leap year
			if ( day > leapMonthDays[month - 1] ) {
				throw new BogusDataException ( "Invalid day of month '" + inDate + "'",
				    inDate );
			}
		} else {
			if ( day > monthDays[month - 1] ) {
				throw new BogusDataException ( "Invalid day of month '" + inDate + "'",
				    inDate );
			}
		}
		// TODO: parse time, handle localtime, handle timezone
		if ( inDate.length () > 8 ) {
			// TODO make sure dateOnly == false
			if ( inDate.charAt ( 8 ) == 'T' ) {
				try {
					hour = Integer.parseInt ( inDate.substring ( 9, 11 ) );
					minute = Integer.parseInt ( inDate.substring ( 11, 13 ) );
					second = Integer.parseInt ( inDate.substring ( 13, 15 ) );
					if ( hour > 23 || minute > 59 || second > 59 ) {
						throw new BogusDataException ( "Invalid time in date string '"
						    + inDate + "'", inDate );
					}
					if ( inDate.length () > 15 ) {
						isUTC = inDate.charAt ( 15 ) == 'Z';
					}
				} catch ( NumberFormatException nef ) {
					throw new BogusDataException ( "Invalid time in date string '"
					    + inDate + "' - " + nef, inDate );
				}
			} else {
				// Invalid format
				throw new ParseException ( "Invalid date format '" + inDate + "'",
				    inDate );
			}
		} else {
			// Just date, no time
			dateOnly = true;
		}
	}

	public Calendar toCalendar () {
		Calendar c = Calendar.getInstance ();
		c.set ( Calendar.YEAR, year );
		c.set ( Calendar.MONTH, month - 1 );
		c.set ( Calendar.DAY_OF_MONTH, day );
		c.set ( Calendar.HOUR_OF_DAY, hour );
		c.set ( Calendar.MINUTE, minute );
		c.set ( Calendar.SECOND, second );
		return c;
	}

	/**
	 * Generate the iCalendar string for this Date.
	 */
	public String toICalendar () {
		StringBuffer sb = new StringBuffer ( dateOnly ? 8 : 15 );
		sb.append ( year );
		if ( month < 10 )
			sb.append ( '0' );
		sb.append ( month );
		if ( day < 10 )
			sb.append ( '0' );
		sb.append ( day );

		if ( !dateOnly ) {
			sb.append ( 'T' );
			if ( hour < 10 )
				sb.append ( '0' );
			sb.append ( hour );
			if ( minute < 10 )
				sb.append ( '0' );
			sb.append ( minute );
			if ( second < 10 )
				sb.append ( '0' );
			sb.append ( second );
			if ( isUTC )
				sb.append ( 'Z' );
		}
		value = sb.toString ();
		return super.toICalendar ();
	}

	public boolean isDateOnly () {
		return dateOnly;
	}

	public void setDateOnly ( boolean dateOnly ) {
		this.dateOnly = dateOnly;
	}

	public int getDay () {
		return day;
	}

	public void setDay ( int day ) {
		this.day = day;
	}

	public int getHour () {
		return hour;
	}

	public void setHour ( int hour ) {
		this.hour = hour;
	}

	public int getMinute () {
		return minute;
	}

	public void setMinute ( int minute ) {
		this.minute = minute;
	}

	public int getMonth () {
		return month;
	}

	public void setMonth ( int month ) {
		this.month = month;
	}

	public int getSecond () {
		return second;
	}

	public void setSecond ( int second ) {
		this.second = second;
	}

	public int getYear () {
		return year;
	}

	public void setYear ( int year ) {
		this.year = year;
	}

	public int compareTo ( Object anotherDate ) throws ClassCastException {
		System.out.println ( "anotherDate " + anotherDate + " - " + anotherDate.getClass ().getName ());
		Date d2 = (Date) anotherDate;
		if ( this.year < d2.year )
			return -1;
		if ( this.year > d2.year )
			return 1;
		if ( this.month < d2.month )
			return -1;
		if ( this.month > d2.month )
			return 1;
		if ( this.day < d2.day )
			return -1;
		if ( this.day > d2.day )
			return 1;
		if ( this.hour < d2.hour )
			return -1;
		if ( this.hour > d2.hour )
			return 1;
		if ( this.minute < d2.minute )
			return -1;
		if ( this.minute > d2.minute )
			return 1;
		if ( this.second < d2.second )
			return -1;
		if ( this.second > d2.second )
			return 1;
		return 0;

	}

}
