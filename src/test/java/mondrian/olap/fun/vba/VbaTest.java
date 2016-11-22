/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2014 Pentaho
// All Rights Reserved.
*/
package mondrian.olap.fun.vba;

import mondrian.olap.InvalidArgumentException;
import mondrian.util.Bug;

import org.junit.Assert;
import org.junit.Test;

import java.text.*;
import java.util.*;

import static mondrian.test.TestTemp.range;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for implementations of Visual Basic for Applications (VBA)
 * functions.
 *
 * <p>Every function defined in {@link Vba} must have a test here. In addition,
 * there should be MDX tests (usually in
 * {@link mondrian.olap.fun.FunctionTest}) if handling of argument types,
 * result types, operator overloading, exception handling or null handling
 * are non-trivial.
 *
 * @author jhyde
 * @since Dec 31, 2007
 */
public class VbaTest {
    private static final double SMALL = 1e-10d;
    private static final Date SAMPLE_DATE = sampleDate();

    private static final String timeZoneName =
        TimeZone.getDefault().getDisplayName();
    private static final boolean isPST =
        timeZoneName.equals("America/Los_Angeles")
        || timeZoneName.equals("Pacific Standard Time");

    // Conversion functions

    @Test public void testCBool() {
        assertThat(Vba.cBool(Boolean.TRUE), is(true));
        assertThat(Vba.cBool(Boolean.FALSE), is(false));
        assertThat(Vba.cBool(1.5), is(true));
        assertThat(Vba.cBool("1.5"), is(true));
        assertThat(Vba.cBool("0.00"), is(false));
        try {
            Object o = Vba.cBool("a");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "NumberFormatException");
        }
        // Per the spec, the string "true" is no different from any other
        try {
            Object o = Vba.cBool("true");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "NumberFormatException");
        }
    }

    private void assertMessage(RuntimeException e, final String expected) {
        final String message = e.getClass().getName() + ": " + e.getMessage();
        assertThat("expected message to contain '" + expected + "', got '"
            + message + "'", message, containsString(expected));
    }

    @Test public void testCInt() {
        assertThat(Vba.cInt(1), is(1));
        assertThat(Vba.cInt(1.4), is(1));
        // CInt rounds to the nearest even number
        assertThat(Vba.cInt(1.5), is(2));
        assertThat(Vba.cInt(2.5), is(2));
        assertThat(Vba.cInt(1.6), is(2));
        assertThat(Vba.cInt(-1.4), is(-1));
        assertThat(Vba.cInt(-1.5), is(-2));
        assertThat(Vba.cInt(-1.6), is(-2));
        assertThat(Vba.cInt((double) Integer.MAX_VALUE), is(Integer.MAX_VALUE));
        assertThat(Vba.cInt((double) Integer.MIN_VALUE), is(Integer.MIN_VALUE));
        assertThat(Vba.cInt(((float) Short.MAX_VALUE) + .4), is((Number) Short.MAX_VALUE));
        assertThat(Vba.cInt(((float) Short.MIN_VALUE) + .4), is((Number) Short.MIN_VALUE));
        try {
            Object o = Vba.cInt("a");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "NumberFormatException");
        }
    }

    @Test public void testInt() {
        // if negative, Int() returns the closest number less than or
        // equal to the number.
        assertThat(Vba.int_(1), is(1));
        assertThat(Vba.int_(1.4), is(1));
        assertThat(Vba.int_(1.5), is(1));
        assertThat(Vba.int_(2.5), is(2));
        assertThat(Vba.int_(1.6), is(1));
        assertThat(Vba.int_(-2), is(-2));
        assertThat(Vba.int_(-1.4), is(-2));
        assertThat(Vba.int_(-1.5), is(-2));
        assertThat(Vba.int_(-1.6), is(-2));
        assertThat(Vba.int_((double) Integer.MAX_VALUE), is(Integer.MAX_VALUE));
        assertThat(Vba.int_((double) Integer.MIN_VALUE), is(Integer.MIN_VALUE));
        try {
            Object o = Vba.int_("a");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "Invalid parameter.");
        }
    }

    @Test public void testFix() {
        // if negative, Fix() returns the closest number greater than or
        // equal to the number.
        assertThat(Vba.fix(1), is(1));
        assertThat(Vba.fix(1.4), is(1));
        assertThat(Vba.fix(1.5), is(1));
        assertThat(Vba.fix(2.5), is(2));
        assertThat(Vba.fix(1.6), is(1));
        assertThat(Vba.fix(-1), is(-1));
        assertThat(Vba.fix(-1.4), is(-1));
        assertThat(Vba.fix(-1.5), is(-1));
        assertThat(Vba.fix(-1.6), is(-1));
        assertThat(Vba.fix((double) Integer.MAX_VALUE), is(Integer.MAX_VALUE));
        assertThat(Vba.fix((double) Integer.MIN_VALUE), is(Integer.MIN_VALUE));
        try {
            Object o = Vba.fix("a");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "Invalid parameter.");
        }
    }


    @Test public void testCDbl() {
        assertThat(Vba.cDbl(1), is(1.0));
        assertThat(Vba.cDbl(1.4), is(1.4));
        // CInt rounds to the nearest even number
        assertThat(Vba.cDbl(1.5), is(1.5));
        assertThat(Vba.cDbl(2.5), is(2.5));
        assertThat(Vba.cDbl(1.6), is(1.6));
        assertThat(Vba.cDbl(-1.4), is(-1.4));
        assertThat(Vba.cDbl(-1.5), is(-1.5));
        assertThat(Vba.cDbl(-1.6), is(-1.6));
        assertThat(Vba.cDbl(Double.MAX_VALUE), is(Double.MAX_VALUE));
        assertThat(Vba.cDbl(Double.MIN_VALUE), is(Double.MIN_VALUE));
        try {
            Object o = Vba.cDbl("a");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "NumberFormatException");
        }
    }

    @Test public void testHex() {
        assertThat(Vba.hex(0), is("0"));
        assertThat(Vba.hex(1), is("1"));
        assertThat(Vba.hex(10), is("A"));
        assertThat(Vba.hex(100), is("64"));
        assertThat(Vba.hex(-1), is("FFFFFFFF"));
        assertThat(Vba.hex(-10), is("FFFFFFF6"));
        assertThat(Vba.hex(-100), is("FFFFFF9C"));
        try {
            Object o = Vba.hex("a");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "Invalid parameter.");
        }
    }

    @Test public void testOct() {
        assertThat(Vba.oct(0), is("0"));
        assertThat(Vba.oct(1), is("1"));
        assertThat(Vba.oct(10), is("12"));
        assertThat(Vba.oct(100), is("144"));
        assertThat(Vba.oct(-1), is("37777777777"));
        assertThat(Vba.oct(-10), is("37777777766"));
        assertThat(Vba.oct(-100), is("37777777634"));
        try {
            Object o = Vba.oct("a");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "Invalid parameter.");
        }
    }

    @Test public void testStr() {
        assertThat(Vba.str(0), is(" 0"));
        assertThat(Vba.str(1), is(" 1"));
        assertThat(Vba.str(10), is(" 10"));
        assertThat(Vba.str(100), is(" 100"));
        assertThat(Vba.str(-1), is("-1"));
        assertThat(Vba.str(-10), is("-10"));
        assertThat(Vba.str(-100), is("-100"));
        assertThat(Vba.str(-10.123), is("-10.123"));
        assertThat(Vba.str(10.123), is(" 10.123"));
        try {
            Object o = Vba.oct("a");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "Invalid parameter.");
        }
    }

    @Test public void testVal() {
        assertThat(Vba.val(" -  1615 198th Street N.E."), is(-1615198.0));
        assertThat(Vba.val(" 1615 198th Street N.E."), is(1615198.0));
        assertThat(Vba.val(" 1615 . 198th Street N.E."), is(1615.198));
        assertThat(Vba.val(" 1615 . 19 . 8th Street N.E."), is(1615.19));
        assertThat(Vba.val("&HFFFF"), is((double)0xffff));
        assertThat(Vba.val("&O1234"), is(668.0));
    }

    @Test public void testCDate() throws ParseException {
        Date date = new Date();
        assertThat(Vba.cDate(date), is(date));
        assertThat(Vba.cDate(null), nullValue());
        // CInt rounds to the nearest even number
        try {
            assertThat(Vba.cDate("Jan 12, 1952"), is(DateFormat.getDateInstance().parse("Jan 12, 1952")));
            assertThat(Vba.cDate("October 19, 1962"), is(DateFormat.getDateInstance().parse("October 19, 1962")));
            assertThat(Vba.cDate("4:35:47 PM"), is(DateFormat.getTimeInstance().parse("4:35:47 PM")));
            assertThat(Vba.cDate("October 19, 1962 4:35:47 PM"), is(DateFormat.getDateTimeInstance().parse(
                        "October 19, 1962 4:35:47 PM")));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        try {
            Vba.cDate("Jan, 1952");
            fail("expected exception");
        } catch (InvalidArgumentException e) {
            assertThat(e.getMessage(), containsString("Jan, 1952"));
        }
    }

    @Test public void testIsDate() throws ParseException {
        // CInt rounds to the nearest even number
        assertThat(Vba.isDate(null), is(false));
        assertThat(Vba.isDate(new Date()), is(true));
        assertThat(Vba.isDate("Jan 12, 1952"), is(true));
        assertThat(Vba.isDate("October 19, 1962"), is(true));
        assertThat(Vba.isDate("4:35:47 PM"), is(true));
        assertThat(Vba.isDate("October 19, 1962 4:35:47 PM"), is(true));
        assertThat(Vba.isDate("Jan, 1952"), is(false));
    }

    // DateTime

    @Test public void testDateAdd() {
        assertEquals("2008/04/24 19:10:45", SAMPLE_DATE);

        // 2008-02-01 0:00:00
        Calendar calendar = Calendar.getInstance();

        calendar.set(2007, 1 /* 0-based! */, 1, 0, 0, 0);
        final Date feb2007 = calendar.getTime();
        assertEquals("2007/02/01 00:00:00", feb2007);

        assertEquals(
            "2008/04/24 19:10:45", Vba.dateAdd("yyyy", 0, SAMPLE_DATE));
        assertEquals(
            "2009/04/24 19:10:45", Vba.dateAdd("yyyy", 1, SAMPLE_DATE));
        assertEquals(
            "2006/04/24 19:10:45", Vba.dateAdd("yyyy", -2, SAMPLE_DATE));
        // partial years interpolate
        final Date sampleDatePlusTwoPointFiveYears =
            Vba.dateAdd("yyyy", 2.5, SAMPLE_DATE);
        if (isPST) {
            // Only run test in PST, because test would produce different
            // results if start and end are not both in daylight savings time.
            final SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
            final String dateString =
                dateFormat.format(
                    sampleDatePlusTwoPointFiveYears);
            // We allow "2010/10/24 07:10:45" for computers that have an out of
            // date timezone database. 2010/10/24 is in daylight savings time,
            // but was not according to the old rules.
            assertThat("Got " + dateString,
                dateString.equals("2010/10/24 06:40:45")
                || dateString.equals("2010/10/24 07:10:45"), is(true));
        }
        assertEquals("2009/01/24 19:10:45", Vba.dateAdd("q", 3, SAMPLE_DATE));

        // partial months are interesting!
        assertEquals("2008/06/24 19:10:45", Vba.dateAdd("m", 2, SAMPLE_DATE));
        assertEquals("2007/01/01 00:00:00", Vba.dateAdd("m", -1, feb2007));
        assertEquals("2007/03/01 00:00:00", Vba.dateAdd("m", 1, feb2007));
        assertEquals("2007/02/08 00:00:00", Vba.dateAdd("m", .25, feb2007));
        // feb 2008 is a leap month, so a quarter month is 7.25 days
        assertEquals("2008/02/08 06:00:00", Vba.dateAdd("m", 12.25, feb2007));

        assertEquals("2008/05/01 19:10:45", Vba.dateAdd("y", 7, SAMPLE_DATE));
        assertEquals(
            "2008/05/02 01:10:45", Vba.dateAdd("y", 7.25, SAMPLE_DATE));
        assertEquals("2008/04/24 23:10:45", Vba.dateAdd("h", 4, SAMPLE_DATE));
        assertEquals("2008/04/24 20:00:45", Vba.dateAdd("n", 50, SAMPLE_DATE));
        assertEquals("2008/04/24 19:10:36", Vba.dateAdd("s", -9, SAMPLE_DATE));
    }

    @Test public void testDateDiff() {
        // TODO:
    }

    @Test public void testDatePart2() {
        assertThat(Vba.datePart("yyyy", SAMPLE_DATE), is(2008));
        assertThat(Vba.datePart("q", SAMPLE_DATE), is(2));
        assertThat(Vba.datePart("m", SAMPLE_DATE), is(4));
        assertThat(Vba.datePart("w", SAMPLE_DATE), is(5));
        assertThat(Vba.datePart("ww", SAMPLE_DATE), is(17));
        assertThat(Vba.datePart("y", SAMPLE_DATE), is(115));
        assertThat(Vba.datePart("h", SAMPLE_DATE), is(19));
        assertThat(Vba.datePart("n", SAMPLE_DATE), is(10));
        assertThat(Vba.datePart("s", SAMPLE_DATE), is(45));
    }

    @Test public void testDatePart3() {
        assertThat(Vba.datePart("w", SAMPLE_DATE, Calendar.SUNDAY), is(5));
        assertThat(Vba.datePart("w", SAMPLE_DATE, Calendar.MONDAY), is(4));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.SUNDAY), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.WEDNESDAY), is(18));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.THURSDAY), is(18));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.FRIDAY), is(17));
    }

    @Test public void testDatePart4() {
        // 2008 starts on a Tuesday
        // 2008-04-29 is a Thursday
        // That puts it in week 17 by most ways of computing weeks
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.SUNDAY, 0), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.SUNDAY, 1), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.SUNDAY, 2), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.SUNDAY, 3), is(16));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.MONDAY, 0), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.MONDAY, 1), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.MONDAY, 2), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.MONDAY, 3), is(16));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.TUESDAY, 0), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.TUESDAY, 1), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.TUESDAY, 2), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.TUESDAY, 3), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.WEDNESDAY, 0), is(18));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.WEDNESDAY, 1), is(18));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.WEDNESDAY, 2), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.WEDNESDAY, 3), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.THURSDAY, 0), is(18));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.THURSDAY, 1), is(18));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.THURSDAY, 2), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.THURSDAY, 3), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.FRIDAY, 0), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.FRIDAY, 1), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.FRIDAY, 2), is(16));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.FRIDAY, 3), is(16));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.SATURDAY, 0), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.SATURDAY, 1), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.SATURDAY, 2), is(17));
        assertThat(Vba.datePart("ww", SAMPLE_DATE, Calendar.SATURDAY, 3), is(16));
        try {
            int i = Vba.datePart("ww", SAMPLE_DATE, Calendar.SUNDAY, 4);
            fail("expected error, got " + i);
        } catch (RuntimeException e) {
            assertMessage(e, "ArrayIndexOutOfBoundsException");
        }
    }

    @Test public void testDate() {
        final Date date = Vba.date();
        assertThat(date, notNullValue());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        assertThat(calendar.get(Calendar.HOUR_OF_DAY), is(0));
        assertThat(calendar.get(Calendar.MILLISECOND), is(0));
    }

    @Test public void testDateSerial() {
        final Date date = Vba.dateSerial(2008, 2, 1);
        assertEquals("2008/02/01 00:00:00", date);
    }

    private void assertEquals(
        String expected,
        Date date)
    {
        final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        final String dateString = dateFormat.format(date);
        assertThat(dateString, is(expected));
    }

    @Test public void testFormatDateTime() {
        try {
            Date date = DateFormat.getDateTimeInstance().parse(
                "October 19, 1962 4:35:47 PM");
            assertThat(Vba.formatDateTime(date), is("Oct 19, 1962 4:35:47 PM"));
            assertThat(Vba.formatDateTime(date, 0), is("Oct 19, 1962 4:35:47 PM"));
            assertThat(Vba.formatDateTime(date, 1), is("October 19, 1962"));
            assertThat(Vba.formatDateTime(date, 2), is("10/19/62"));
            String datestr = Vba.formatDateTime(date, 3);
            assertThat(datestr, notNullValue());
            // skip the timezone so this test runs everywhere
            // in EST, this string is "4:35:47 PM EST"
            assertThat(datestr.startsWith("4:35:47 PM"), is(true));
            assertThat(Vba.formatDateTime(date, 4), is("4:35 PM"));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test public void testDateValue() {
        Date date = new Date();
        final Date date1 = Vba.dateValue(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date1);
        assertThat(calendar.get(Calendar.HOUR_OF_DAY), is(0));
        assertThat(calendar.get(Calendar.MINUTE), is(0));
        assertThat(calendar.get(Calendar.SECOND), is(0));
        assertThat(calendar.get(Calendar.MILLISECOND), is(0));
    }

    private static Date sampleDate() {
        Calendar calendar = Calendar.getInstance();
        // Thursday 2008-04-24 7:10:45pm
        // Chose a Thursday because 2008 starts on a Tuesday - it makes weeks
        // interesting.
        calendar.set(2008, 3 /* 0-based! */, 24, 19, 10, 45);
        return calendar.getTime();
    }

    @Test public void testDay() {
        assertThat(Vba.day(SAMPLE_DATE), is(24));
    }

    @Test public void testHour() {
        assertThat(Vba.hour(SAMPLE_DATE), is(19));
    }

    @Test public void testMinute() {
        assertThat(Vba.minute(SAMPLE_DATE), is(10));
    }

    @Test public void testMonth() {
        assertThat(Vba.month(SAMPLE_DATE), is(4));
    }

    @Test public void testNow() {
        final Date date = Vba.now();
        assertThat(date, notNullValue());
    }

    @Test public void testSecond() {
        assertThat(Vba.second(SAMPLE_DATE), is(45));
    }

    @Test public void testTimeSerial() {
        final Date date = Vba.timeSerial(17, 42, 10);
        assertEquals("1970/01/01 17:42:10", date);
    }

    @Test public void testTimeValue() {
        assertEquals("1970/01/01 19:10:45", Vba.timeValue(SAMPLE_DATE));
    }

    @Test public void testTimer() {
        final float v = Vba.timer();
        assertThat(v >= 0, is(true));
        assertThat(v < 24 * 60 * 60, is(true));
    }

    @Test public void testWeekday1() {
        if (Calendar.getInstance().getFirstDayOfWeek() == Calendar.SUNDAY) {
            assertThat(Vba.weekday(SAMPLE_DATE), is(Calendar.THURSDAY));
        }
    }

    @Test public void testWeekday2() {
        // 2008/4/24 falls on a Thursday.

        // If Sunday is the first day of the week, Thursday is day 5.
        assertThat(Vba.weekday(SAMPLE_DATE, Calendar.SUNDAY), is(5));

        // If Monday is the first day of the week, then 2008/4/24 falls on the
        // 4th day of the week
        assertThat(Vba.weekday(SAMPLE_DATE, Calendar.MONDAY), is(4));

        assertThat(Vba.weekday(SAMPLE_DATE, Calendar.TUESDAY), is(3));
        assertThat(Vba.weekday(SAMPLE_DATE, Calendar.WEDNESDAY), is(2));
        assertThat(Vba.weekday(SAMPLE_DATE, Calendar.THURSDAY), is(1));
        assertThat(Vba.weekday(SAMPLE_DATE, Calendar.FRIDAY), is(7));
        assertThat(Vba.weekday(SAMPLE_DATE, Calendar.SATURDAY), is(6));
    }

    @Test public void testYear() {
        assertThat(Vba.year(SAMPLE_DATE), is(2008));
    }

    @Test public void testFormatNumber() {
        assertThat(Vba.formatNumber(1.0), is("1"));
        assertThat(Vba.formatNumber(1.0, 1), is("1.0"));

        assertThat(Vba.formatNumber(0.1, -1, -1), is("0.1"));
        assertThat(Vba.formatNumber(0.1, -1, 0), is(".1"));
        assertThat(Vba.formatNumber(0.1, -1, 1), is("0.1"));

        assertThat(Vba.formatNumber(-1, -1, 1, -1), is("-1"));
        assertThat(Vba.formatNumber(-1, -1, 1,  0), is("-1"));
        assertThat(Vba.formatNumber(-1, -1, 1,  1), is("(1)"));

        assertThat(Vba.formatNumber(1, -1, 1, -1), is("1"));
        assertThat(Vba.formatNumber(1, -1, 1,  0), is("1"));
        assertThat(Vba.formatNumber(1, -1, 1,  1), is("1"));

        assertThat(Vba.formatNumber(1000.0, -1, -1, -1, -1), is("1,000"));
        assertThat(Vba.formatNumber(1000.0,  1, -1, -1,  0), is("1000.0"));
        assertThat(Vba.formatNumber(1000.0,  1, -1, -1,  1), is("1,000.0"));
    }

    @Test public void testFormatPercent() {
        assertThat(Vba.formatPercent(1.0), is("100%"));
        assertThat(Vba.formatPercent(1.0, 1), is("100.0%"));

        assertThat(Vba.formatPercent(0.001,1, -1), is("0.1%"));
        assertThat(Vba.formatPercent(0.001, 1, 0), is(".1%"));
        assertThat(Vba.formatPercent(0.001, 1, 1), is("0.1%"));


        assertThat(Vba.formatPercent(0.111, -1), is("11%"));
        assertThat(Vba.formatPercent(0.111, 0), is("11%"));
        assertThat(Vba.formatPercent(0.111, 3), is("11.100%"));

        assertThat(Vba.formatPercent(-1, -1, 1, -1), is("-100%"));
        assertThat(Vba.formatPercent(-1, -1, 1,  0), is("-100%"));
        assertThat(Vba.formatPercent(-1, -1, 1,  1), is("(100%)"));

        assertThat(Vba.formatPercent(1, -1, 1, -1), is("100%"));
        assertThat(Vba.formatPercent(1, -1, 1,  0), is("100%"));
        assertThat(Vba.formatPercent(1, -1, 1,  1), is("100%"));

        assertThat(Vba.formatPercent(1000.0, -1, -1, -1, -1), is("100,000%"));
        assertThat(Vba.formatPercent(1000.0,  1, -1, -1,  0), is("100000.0%"));
        assertThat(Vba.formatPercent(1000.0,  1, -1, -1,  1), is("100,000.0%"));
    }

    @Test public void testFormatCurrency() {
        assertThat(Vba.formatCurrency(1.0), is("$1.00"));
        assertThat(Vba.formatCurrency(0.0), is("$0.00"));
        assertThat(Vba.formatCurrency(1.0, 1), is("$1.0"));
        assertThat(Vba.formatCurrency(1.0, 0), is("$1"));
        assertThat(Vba.formatCurrency(0.10, -1, 0), is("$.10"));
        assertThat(Vba.formatCurrency(0.10, -1, -1), is("$0.10"));
        // todo: still need to implement parens customization
        // assertEquals("-$0.10", Vba.formatCurrency(-0.10, -1, -1, -1));
        assertThat(Vba.formatCurrency(-0.10, -1, -1, 0), is("($0.10)"));

        assertThat(Vba.formatCurrency(1000.0, -1, -1, 0, 0), is("$1,000.00"));
        assertThat(Vba.formatCurrency(1000.0, -1, -1, 0, -1), is("$1000.00"));
    }

    @Test public void testTypeName() {
        assertThat(Vba.typeName(1.0), is("Double"));
        assertThat(Vba.typeName(1), is("Integer"));
        assertThat(Vba.typeName(1.0f), is("Float"));
        assertThat(Vba.typeName((byte)1), is("Byte"));
        assertThat(Vba.typeName(null), is("NULL"));
        assertThat(Vba.typeName(""), is("String"));
        assertThat(Vba.typeName(new Date()), is("Date"));
    }

    // Financial

    @Test public void testFv() {
        double f, r, y, p, x;
        int n;
        boolean t;

        r = 0;
        n = 3;
        y = 2;
        p = 7;
        t = true;
        f = Vba.fV(r, n, y, p, t);
        x = -13;
        assertThat(f, is(x));

        r = 1;
        n = 10;
        y = 100;
        p = 10000;
        t = false;
        f = Vba.fV(r, n, y, p, t);
        x = -10342300;
        assertThat(f, is(x));

        r = 1;
        n = 10;
        y = 100;
        p = 10000;
        t = true;
        f = Vba.fV(r, n, y, p, t);
        x = -10444600;
        assertThat(f, is(x));

        r = 2;
        n = 12;
        y = 120;
        p = 12000;
        t = false;
        f = Vba.fV(r, n, y, p, t);
        x = -6409178400d;
        assertThat(f, is(x));

        r = 2;
        n = 12;
        y = 120;
        p = 12000;
        t = true;
        f = Vba.fV(r, n, y, p, t);
        x = -6472951200d;
        assertThat(f, is(x));

        // cross tests with pv
        r = 2.95;
        n = 13;
        y = 13000;
        p = -4406.78544294496;
        t = false;
        f = Vba.fV(r, n, y, p, t);
        x = 333891.230010986; // as returned by excel
        assertThat(f, range(x, 1e-2));

        r = 2.95;
        n = 13;
        y = 13000;
        p = -17406.7852148156;
        t = true;
        f = Vba.fV(r, n, y, p, t);
        x = 333891.230102539; // as returned by excel
        assertThat(f, range(x, 1e-2));
    }

    @Test public void testNpv() {
        double r, v[], npv, x;

        r = 1;
        v = new double[] {100, 200, 300, 400};
        npv = Vba.nPV(r, v);
        x = 162.5;
        assertThat(npv, is(x));

        r = 2.5;
        v = new double[] {1000, 666.66666, 333.33, 12.2768416};
        npv = Vba.nPV(r, v);
        x = 347.99232604144827;
        assertThat(npv, range(x, SMALL));

        r = 12.33333;
        v = new double[] {1000, 0, -900, -7777.5765};
        npv = Vba.nPV(r, v);
        x = 74.3742433377061;
        assertThat(npv, range(x, 1e-12));

        r = 0.05;
        v = new double[] {
            200000, 300000.55, 400000, 1000000, 6000000, 7000000, -300000
        };
        npv = Vba.nPV(r, v);
        x = 11342283.4233124;
        assertThat(npv, range(x, 1e-8));
    }

    @Test public void testPmt() {
        double f, r, y, p, x;
        int n;
        boolean t;

        r = 0;
        n = 3;
        p = 2;
        f = 7;
        t = true;
        y = Vba.pmt(r, n, p, f, t);
        x = -3;
        assertThat(y, is(x));

        // cross check with pv
        r = 1;
        n = 10;
        p = -109.66796875;
        f = 10000;
        t = false;
        y = Vba.pmt(r, n, p, f, t);
        x = 100;
        assertThat(y, is(x));

        r = 1;
        n = 10;
        p = -209.5703125;
        f = 10000;
        t = true;
        y = Vba.pmt(r, n, p, f, t);
        x = 100;
        assertThat(y, is(x));

        // cross check with fv
        r = 2;
        n = 12;
        f = -6409178400d;
        p = 12000;
        t = false;
        y = Vba.pmt(r, n, p, f, t);
        x = 120;
        assertThat(y, is(x));

        r = 2;
        n = 12;
        f = -6472951200d;
        p = 12000;
        t = true;
        y = Vba.pmt(r, n, p, f, t);
        x = 120;
        assertThat(y, is(x));
    }

    @Test public void testPv() {
        double f, r, y, p, x;
        int n;
        boolean t;

        r = 0;
        n = 3;
        y = 2;
        f = 7;
        t = true;
        f = Vba.pV(r, n, y, f, t);
        x = -13;
        assertThat(f, is(x));

        r = 1;
        n = 10;
        y = 100;
        f = 10000;
        t = false;
        p = Vba.pV(r, n, y, f, t);
        x = -109.66796875;
        assertThat(p, is(x));

        r = 1;
        n = 10;
        y = 100;
        f = 10000;
        t = true;
        p = Vba.pV(r, n, y, f, t);
        x = -209.5703125;
        assertThat(p, is(x));

        r = 2.95;
        n = 13;
        y = 13000;
        f = 333891.23;
        t = false;
        p = Vba.pV(r, n, y, f, t);
        x = -4406.78544294496;
        assertThat(p, range(x, 1e-10));

        r = 2.95;
        n = 13;
        y = 13000;
        f = 333891.23;
        t = true;
        p = Vba.pV(r, n, y, f, t);
        x = -17406.7852148156;
        assertThat(p, range(x, 1e-10));

        // cross tests with fv
        r = 2;
        n = 12;
        y = 120;
        f = -6409178400d;
        t = false;
        p = Vba.pV(r, n, y, f, t);
        x = 12000;
        assertThat(p, is(x));

        r = 2;
        n = 12;
        y = 120;
        f = -6472951200d;
        t = true;
        p = Vba.pV(r, n, y, f, t);
        x = 12000;
        assertThat(p, is(x));
    }

    @Test public void testDdb() {
        double cost, salvage, life, period, factor, result;
        cost = 100;
        salvage = 0;
        life = 10;
        period = 1;
        factor = 2;
        result = Vba.dDB(cost, salvage, life, period, factor);
        assertThat(result, is(20.0));
        result = Vba.dDB(cost, salvage, life, period + 1, factor);
        assertThat(result, is(40.0));
        result = Vba.dDB(cost, salvage, life, period + 2, factor);
        assertThat(result, is(60.0));
        result = Vba.dDB(cost, salvage, life, period + 3, factor);
        assertThat(result, is(80.0));
    }

    @Test public void testRate() {
        double nPer, pmt, PV, fv, guess, result;
        boolean type = false;
        nPer = 12 * 30;
        pmt = -877.57;
        PV = 100000;
        fv = 0;
        guess = 0.10 / 12;
        result = Vba.rate(nPer, pmt, PV, fv, type, guess);

        // compare rate to pV calculation
        double expRate = 0.0083333;
        double expPV = Vba.pV(expRate, 12 * 30, -877.57, 0, false);
        result = Vba.rate(12 * 30, -877.57, expPV, 0, false, 0.10 / 12);
        assertThat(result, range(expRate, 0.0000001));

        // compare rate to fV calculation
        double expFV = Vba.fV(expRate, 12, -100, 0, false);
        result = Vba.rate(12, -100, 0, expFV, false, 0.10 / 12);
        assertThat(result, range(expRate, 0.0000001));
    }

    @Test public void testIRR() {
        double vals[] = {-1000, 50, 50, 50, 50, 50, 1050};
        assertThat(Vba.IRR(vals, 0.1), range(0.05, 0.0000001));

        vals = new double[] {-1000, 200, 200, 200, 200, 200, 200};
        assertThat(Vba.IRR(vals, 0.1), range(0.05471796, 0.0000001));

        // what happens if the numbers are inversed? this may not be
        // accurate

        vals = new double[] {1000, -200, -200, -200, -200, -200, -200};
        assertThat(Vba.IRR(vals, 0.1), range(0.05471796, 0.0000001));
    }

    @Test public void testMIRR() {
        double vals[] = {-1000, 50, 50, 50, 50, 50, 1050};
        assertThat(Vba.MIRR(vals, 0.05, 0.05), range(0.05, 0.0000001));

        vals = new double[] {-1000, 200, 200, 200, 200, 200, 200};
        assertThat(Vba.MIRR(vals, 0.05, 0.05), range(0.05263266, 0.0000001));

        vals = new double[] {-1000, 200, 200, 200, 200, 200, 200};
        assertThat(Vba.MIRR(vals, 0.06, 0.04), range(0.04490701, 0.0000001));
    }

    @Test public void testIPmt() {
        assertThat(Vba.iPmt(0.10, 1, 30, 100000, 0, false), is(-10000.0));
        assertThat(Vba.iPmt(0.10, 15, 30, 100000, 0, false), is(-2185.473324557822));
        assertThat(Vba.iPmt(0.10, 30, 30, 100000, 0, false), is(-60.79248252633988));
    }

    @Test public void testPPmt() {
        assertThat(Vba.pPmt(0.10, 1, 30, 100000, 0, false), is(-607.9248252633897));
        assertThat(Vba.pPmt(0.10, 15, 30, 100000, 0, false), is(-8422.451500705567));
        assertThat(Vba.pPmt(0.10, 30, 30, 100000, 0, false), is(-10547.13234273705));

        // verify that pmt, ipmt, and ppmt add up
        double pmt = Vba.pmt(0.10, 30, 100000, 0, false);
        double ipmt = Vba.iPmt(0.10, 15, 30, 100000, 0, false);
        double ppmt = Vba.pPmt(0.10, 15, 30, 100000, 0, false);
        assertThat(ipmt + ppmt, range(pmt, 0.0000001));
    }

    @Test public void testSLN() {
        assertThat(Vba.sLN(100, 10, 5), is(18.0));
        assertThat(Vba.sLN(100, 10, 0), is(Double.POSITIVE_INFINITY));
    }

    @Test public void testSYD() {
        assertThat(Vba.sYD(1000, 100, 5, 5), is(300.0));
        assertThat(Vba.sYD(1000, 100, 4, 5), is(240.0));
        assertThat(Vba.sYD(1000, 100, 3, 5), is(180.0));
        assertThat(Vba.sYD(1000, 100, 2, 5), is(120.0));
        assertThat(Vba.sYD(1000, 100, 1, 5), is(60.0));
    }

    @Test public void testInStr() {
        assertThat(Vba.inStr("the quick brown fox jumps over the lazy dog", "the"), is(
            1));
        assertThat(Vba.inStr(
                16, "the quick brown fox jumps over the lazy dog", "the"), is(32));
        assertThat(Vba.inStr(
                16, "the quick brown fox jumps over the lazy dog", "cat"), is(0));
        assertThat(Vba.inStr(1, "the quick brown fox jumps over the lazy dog", "cat"), is(
            0));
        assertThat(Vba.inStr(1, "", "cat"), is(0));
        assertThat(Vba.inStr(100, "short string", "str"), is(0));
        try {
            Vba.inStr(0, "the quick brown fox jumps over the lazy dog", "the");
            fail("expected exception");
        } catch (InvalidArgumentException e) {
            assertThat(e.getMessage(), containsString("-1 or a location"));
        }
    }

    @Test public void testInStrRev() {
        assertThat(Vba.inStrRev("the quick brown fox jumps over the lazy dog", "the"), is(
            32));
        assertThat(Vba.inStrRev(
                "the quick brown fox jumps over the lazy dog", "the", 16), is(1));
        try {
            Vba.inStrRev(
                "the quick brown fox jumps over the lazy dog", "the", 0);
            fail("expected exception");
        } catch (InvalidArgumentException e) {
            assertThat(e.getMessage(), containsString("-1 or a location"));
        }
    }

    @Test public void testStrComp() {
        assertThat(Vba.strComp("a", "b", 0), is(-1));
        assertThat(Vba.strComp("a", "a", 0), is(0));
        assertThat(Vba.strComp("b", "a", 0), is(1));
    }

    @Test public void testNper() {
        double f, r, y, p, x, n;
        boolean t;

        r = 0;
        y = 7;
        p = 2;
        f = 3;
        t = false;
        n = Vba.nPer(r, y, p, f, t);
        // can you believe it? excel returns nper as a fraction!??
        x = -0.71428571429;
        assertThat(n, range(x, 1e-10));

        // cross check with pv
        r = 1;
        y = 100;
        p = -109.66796875;
        f = 10000;
        t = false;
        n = Vba.nPer(r, y, p, f, t);
        x = 10;
        assertThat(n, range(x, 1e-12));

        r = 1;
        y = 100;
        p = -209.5703125;
        f = 10000;
        t = true;
        n = Vba.nPer(r, y, p, f, t);
        x = 10;
        assertThat(n, range(x, 1e-14));

        // cross check with fv
        r = 2;
        y = 120;
        f = -6409178400d;
        p = 12000;
        t = false;
        n = Vba.nPer(r, y, p, f, t);
        x = 12;
        assertThat(n, range(x, SMALL));

        r = 2;
        y = 120;
        f = -6472951200d;
        p = 12000;
        t = true;
        n = Vba.nPer(r, y, p, f, t);
        x = 12;
        assertThat(n, range(x, SMALL));
    }

    // String functions

    @Test public void testAsc() {
        assertThat(Vba.asc("abc"), is(0x61));
        assertThat(Vba.asc("\u1234abc"), is(0x1234));
        try {
            Object o = Vba.asc("");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "StringIndexOutOfBoundsException");
        }
    }

    @Test public void testAscB() {
        assertThat(Vba.ascB("abc"), is(0x61));
        assertThat(Vba.ascB("\u1234abc"), is(0x34));
        try {
            Object o = Vba.ascB("");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "StringIndexOutOfBoundsException");
        }
    }

    @Test public void testAscW() {
        // ascW behaves identically to asc
        assertThat(Vba.ascW("abc"), is(0x61));
        assertThat(Vba.ascW("\u1234abc"), is(0x1234));
        try {
            Object o = Vba.ascW("");
            fail("expected error, got " + o);
        } catch (RuntimeException e) {
            assertMessage(e, "StringIndexOutOfBoundsException");
        }
    }

    @Test public void testChr() {
        assertThat(Vba.chr(0x61), is("a"));
        assertThat(Vba.chr(0x1234), is("\u1234"));
    }

    @Test public void testChrB() {
        assertThat(Vba.chrB(0x61), is("a"));
        assertThat(Vba.chrB(0x1234), is("\u0034"));
    }

    @Test public void testChrW() {
        assertThat(Vba.chrW(0x61), is("a"));
        assertThat(Vba.chrW(0x1234), is("\u1234"));
    }

    @Test public void testLCase() {
        assertThat(Vba.lCase(""), is(""));
        assertThat(Vba.lCase("AbC"), is("abc"));
    }

    // NOTE: BuiltinFunTable already implements Left; todo: use this
    @Test public void testLeft() {
        assertThat(Vba.left("abcxyz", 3), is("abc"));
        // length=0 is OK
        assertThat(Vba.left("abcxyz", 0), is(""));
        // Spec says: "If greater than or equal to the number of characters in
        // string, the entire string is returned."
        assertThat(Vba.left("abcxyz", 8), is("abcxyz"));
        assertThat(Vba.left("", 3), is(""));

        // Length<0 is illegal.
        // Note: SSAS 2005 allows length<0, giving the same result as length=0.
        // We favor the VBA spec over SSAS 2005.
        if (Bug.Ssas2005Compatible) {
            assertThat(Vba.left("xyz", -2), is(""));
        } else {
            try {
                String s = Vba.left("xyz", -2);
                fail("expected error, got " + s);
            } catch (RuntimeException e) {
                assertMessage(e, "StringIndexOutOfBoundsException");
            }
        }

        assertThat(Vba.left("Hello World!", 5), is("Hello"));
    }

    @Test public void testLTrim() {
        assertThat(Vba.lTrim(""), is(""));
        assertThat(Vba.lTrim("  "), is(""));
        assertThat(Vba.lTrim(" \n\tabc  \r"), is("abc  \r"));
    }

    @Test public void testMid() {
        String testString = "Mid Function Demo";
        assertThat(Vba.mid(testString, 1, 3), is("Mid"));
        assertThat(Vba.mid(testString, 14, 4), is("Demo"));
        // It's OK if start+length = string.length
        assertThat(Vba.mid(testString, 14, 5), is("Demo"));
        // It's OK if start+length > string.length
        assertThat(Vba.mid(testString, 14, 500), is("Demo"));
        assertThat(Vba.mid(testString, 5), is("Function Demo"));
        assertThat(Vba.mid("yahoo", 5, 1), is("o"));

        // Start=0 illegal
        // Note: SSAS 2005 accepts start<=0, treating it as 1, therefore gives
        // different results. We favor the VBA spec over SSAS 2005.
        if (Bug.Ssas2005Compatible) {
            assertThat(Vba.mid(testString, 0), is("Mid Function Demo"));
            assertThat(Vba.mid(testString, -2), is("Mid Function Demo"));
            assertThat(Vba.mid(testString, -2, 5), is("Mid Function Demo"));
        } else {
            try {
                String s = Vba.mid(testString, 0);
                fail("expected error, got " + s);
            } catch (RuntimeException e) {
                assertMessage(
                    e,
                    "Invalid parameter. Start parameter of Mid function must "
                    + "be positive");
            }
            // Start<0 illegal
            try {
                String s = Vba.mid(testString, -2);
                fail("expected error, got " + s);
            } catch (RuntimeException e) {
                assertMessage(
                    e,
                    "Invalid parameter. Start parameter of Mid function must "
                    + "be positive");
            }
            // Start<0 illegal to 3 args version
            try {
                String s = Vba.mid(testString, -2, 5);
                fail("expected error, got " + s);
            } catch (RuntimeException e) {
                assertMessage(
                    e,
                    "Invalid parameter. Start parameter of Mid function must "
                    + "be positive");
            }
        }

        // Length=0 OK
        assertThat(Vba.mid(testString, 14, 0), is(""));

        // Length<0 illegal
        // Note: SSAS 2005 accepts length<0, treating it as 0, therefore gives
        // different results. We favor the VBA spec over SSAS 2005.
        if (Bug.Ssas2005Compatible) {
            assertThat(Vba.mid(testString, 14, -1), is(""));
        } else {
            try {
                String s = Vba.mid(testString, 14, -1);
                fail("expected error, got " + s);
            } catch (RuntimeException e) {
                assertMessage(
                    e,
                    "Invalid parameter. Length parameter of Mid function must "
                    + "be non-negative");
            }
        }
    }

    @Test public void testMonthName() {
        assertThat(Vba.monthName(1, false), is("January"));
        assertThat(Vba.monthName(1, true), is("Jan"));
        assertThat(Vba.monthName(12, true), is("Dec"));
        try {
            String s = Vba.monthName(0, true);
            fail("expected error, got " + s);
        } catch (RuntimeException e) {
            assertMessage(e, "ArrayIndexOutOfBoundsException");
        }
    }

    @Test public void testReplace3() {
        // replace with longer string
        assertThat(Vba.replace("xyzxyz", "xy", "abc"), is("abczabcz"));
        // replace with shorter string
        assertThat(Vba.replace("wxyzwxyz", "xy", "a"), is("wazwaz"));
        // replace with string which contains seek
        assertThat(Vba.replace("xyz", "xy", "wxy"), is("wxyz"));
        // replace with string which combines with following char to make seek
        assertThat(Vba.replace("xyyzxy", "xy", "wx"), is("wxyzwx"));
        // replace with empty string
        assertThat(Vba.replace("wxxyyzxya", "xy", ""), is("wxyza"));
    }

    @Test public void testReplace4() {
        assertThat(Vba.replace("xyzxyz", "xy", "a", 1), is("azaz"));
        assertThat(Vba.replace("xyzxyz", "xy", "a", 2), is("xyzaz"));
        assertThat(Vba.replace("xyzxyz", "xy", "a", 30), is("xyzxyz"));
        // spec doesn't say, but assume starting before start of string is ok
        assertThat(Vba.replace("xyzxyz", "xy", "a", 0), is("azaz"));
        assertThat(Vba.replace("xyzxyz", "xy", "a", -5), is("azaz"));
    }

    @Test public void testReplace5() {
        assertThat(Vba.replace("xyzxyz", "xy", "a", 1, -1), is("azaz"));
        assertThat(Vba.replace("xyzxyz", "xy", "a", 1, 1), is("azxyz"));
        assertThat(Vba.replace("xyzxyz", "xy", "a", 1, 2), is("azaz"));
        assertThat(Vba.replace("xyzxyzxyz", "xy", "a", 2, 1), is("xyzazxyz"));
    }

    @Test public void testReplace6() {
        // compare is currently ignored
        assertThat(Vba.replace("xyzxyz", "xy", "a", 1, -1, 1000), is("azaz"));
        assertThat(Vba.replace("xyzxyz", "xy", "a", 1, 1, 0), is("azxyz"));
        assertThat(Vba.replace("xyzxyz", "xy", "a", 1, 2, -6), is("azaz"));
        assertThat(Vba.replace("xyzxyzxyz", "xy", "a", 2, 1, 11), is("xyzazxyz"));
    }

    @Test public void testRight() {
        assertThat(Vba.right("abcxyz", 3), is("xyz"));
        // length=0 is OK
        assertThat(Vba.right("abcxyz", 0), is(""));
        // Spec says: "If greater than or equal to the number of characters in
        // string, the entire string is returned."
        assertThat(Vba.right("abcxyz", 8), is("abcxyz"));
        assertThat(Vba.right("", 3), is(""));

        // The VBA spec says that length<0 is error.
        // Note: SSAS 2005 allows length<0, giving the same result as length=0.
        // We favor the VBA spec over SSAS 2005.
        if (Bug.Ssas2005Compatible) {
            assertThat(Vba.right("xyz", -2), is(""));
        } else {
            try {
                String s = Vba.right("xyz", -2);
                fail("expected error, got " + s);
            } catch (RuntimeException e) {
                assertMessage(e, "StringIndexOutOfBoundsException");
            }
        }

        assertThat(Vba.right("Hello World!", 6), is("World!"));
    }

    @Test public void testRTrim() {
        assertThat(Vba.rTrim(""), is(""));
        assertThat(Vba.rTrim("  "), is(""));
        assertThat(Vba.rTrim(" \n\tabc"), is(" \n\tabc"));
        assertThat(Vba.rTrim(" \n\tabc  \r"), is(" \n\tabc"));
    }

    @Test public void testSpace() {
        assertThat(Vba.space(3), is("   "));
        assertThat(Vba.space(0), is(""));
        try {
            String s = Vba.space(-2);
            fail("expected error, got " + s);
        } catch (RuntimeException e) {
            assertMessage(e, "NegativeArraySizeException");
        }
    }

    @Test public void testString() {
        assertThat(Vba.string(3, 'x'), is("xxx"));
        assertThat(Vba.string(0, 'y'), is(""));
        try {
            String s = Vba.string(-2, 'z');
            fail("expected error, got " + s);
        } catch (RuntimeException e) {
            assertMessage(e, "NegativeArraySizeException");
        }
        assertThat(Vba.string(100, '\0'), is(""));
    }

    @Test public void testStrReverse() {
        // odd length
        assertThat(Vba.strReverse("abc"), is("cba"));
        // even length
        assertThat(Vba.strReverse("zyxw"), is("wxyz"));
        // zero length
        assertThat(Vba.strReverse(""), is(""));
    }

    @Test public void testTrim() {
        assertThat(Vba.trim(""), is(""));
        assertThat(Vba.trim("  "), is(""));
        assertThat(Vba.trim("abc"), is("abc"));
        assertThat(Vba.trim(" \n\tabc  \r"), is("abc"));
    }

    @Test public void testWeekdayName() {
        // If Sunday (1) is the first day of the week
        // then day 1 is Sunday,
        // then day 2 is Monday,
        // and day 7 is Saturday
        assertThat(Vba.weekdayName(1, false, 1), is("Sunday"));
        assertThat(Vba.weekdayName(2, false, 1), is("Monday"));
        assertThat(Vba.weekdayName(7, false, 1), is("Saturday"));
        assertThat(Vba.weekdayName(7, true, 1), is("Sat"));

        // If Monday (2) is the first day of the week
        // then day 1 is Monday,
        // and day 7 is Sunday
        assertThat(Vba.weekdayName(1, false, 2), is("Monday"));
        assertThat(Vba.weekdayName(7, false, 2), is("Sunday"));

        // Use weekday start from locale. Test for the 2 most common.
        switch (Calendar.getInstance().getFirstDayOfWeek()) {
        case Calendar.SUNDAY:
            assertThat(Vba.weekdayName(1, false, 0), is("Sunday"));
            assertThat(Vba.weekdayName(2, false, 0), is("Monday"));
            assertThat(Vba.weekdayName(7, false, 0), is("Saturday"));
            assertThat(Vba.weekdayName(7, true, 0), is("Sat"));
            break;
        case Calendar.MONDAY:
            assertThat(Vba.weekdayName(1, false, 0), is("Monday"));
            assertThat(Vba.weekdayName(2, false, 0), is("Tuesday"));
            assertThat(Vba.weekdayName(7, false, 0), is("Sunday"));
            assertThat(Vba.weekdayName(7, true, 0), is("Sun"));
            break;
        }
    }

    // Mathematical

    @Test public void testAbs() {
        assertThat(1.7d, is(Vba.abs(-1.7d)));
    }

    @Test public void testAtn() {
        assertThat(Vba.atn(0), range(0d, SMALL));
        assertThat(Vba.atn(1), range(Math.PI / 4d, SMALL));
    }

    @Test public void testCos() {
        assertThat(Vba.cos(0), range(1d, 0d));
        assertThat(Vba.cos(Math.PI / 4d), range(
            Vba.sqr(0.5d),
            0d));
        assertThat(Vba.cos(Math.PI / 2d), range(
            0d,
            SMALL));
        assertThat(Vba.cos(Math.PI), range(-1d, 0d));
    }

    @Test public void testExp() {
        assertThat(Vba.exp(0), is(1d));
        assertThat(Vba.exp(1), range(Math.E, 1e-10));
    }

    @Test public void testRound() {
        assertThat(Vba.round(123.4567d), range(
            123d,
            SMALL));
    }

    @Test public void testRound2() {
        assertThat(Vba.round(123.4567d, 0), range(
            123d,
            SMALL));
        assertThat(Vba.round(123.4567d, 2), range(
            123.46d,
            SMALL));
        assertThat(Vba.round(123.45d, -1), range(
            120d,
            SMALL));
        assertThat(Vba.round(-123.4567d, 2), range(
            -123.46d,
            SMALL));
    }

    @Test public void testSgn() {
        assertThat((double) Vba.sgn(3.11111d), range(
            (double) 1,
            0d));
        double actual = Vba.sgn(-Math.PI);
        assertThat(actual, range((double) -1, 0d));
        assertThat(Vba.sgn(-0d), is(0));
        assertThat(Vba.sgn(0d), is(0));
    }

    @Test public void testSin() {
        assertThat(Vba.sin(Math.PI / 4d), range(
            Vba.sqr(0.5d),
            SMALL));
    }

    @Test public void testSqr() {
        assertThat(Vba.sqr(4d), range(2d, 0d));
        assertThat(Vba.sqr(0d), range(0d, 0d));
        assertThat(Double.isNaN(Vba.sqr(-4)), is(true));
    }

    @Test public void testTan() {
        assertThat(Vba.tan(Math.PI / 4d), range(
            1d,
            SMALL));
    }
}

// End VbaTest.java
