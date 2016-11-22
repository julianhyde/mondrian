/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 Julian Hyde
// Copyright (C) 2005-2013 Pentaho and others
// All Rights Reserved.
*/
package mondrian.olap;

import mondrian.olap.Util.ByteMatcher;
import mondrian.rolap.RolapUtil;
import mondrian.util.*;

import org.junit.Test;

import java.sql.Driver;
import java.util.*;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for methods in {@link mondrian.olap.Util} and, sometimes, classes in
 * the {@code mondrian.util} package.
 */
public class UtilTestCase {
    @Test public void testParseConnectStringSimple() {
        // Simple connect string
        Util.PropertyList properties =
            Util.parseConnectString("foo=x;bar=y;foo=z");
        assertThat(properties.get("bar"), is("y"));
        assertThat(properties.get("BAR"), is("y"));
        assertThat(properties.get(" bar"), nullValue());
        assertThat(properties.get("foo"), is("z"));
        assertThat(properties.get("kipper"), nullValue());
        assertThat(properties.list.size(), is(2));
        assertThat(properties.toString(), is("foo=z; bar=y"));
    }

    @Test public void testParseConnectStringComplex() {
        Util.PropertyList properties =
            Util.parseConnectString(
                "normalProp=value;"
                + "emptyValue=;"
                + " spaceBeforeProp=abc;"
                + " spaceBeforeAndAfterProp =def;"
                + " space in prop = foo bar ;"
                + "equalsInValue=foo=bar;"
                + "semiInProp;Name=value;"
                + " singleQuotedValue = "
                + "'single quoted value ending in space ' ;"
                + " doubleQuotedValue = "
                + "\"=double quoted value preceded by equals\" ;"
                + " singleQuotedValueWithSemi = 'one; two';"
                + " singleQuotedValueWithSpecials = "
                + "'one; two \"three''four=five'");
        assertThat(properties.list.size(), is(11));
        String value;
        value = properties.get("normalProp");
        assertThat(value, is("value"));
        value = properties.get("emptyValue");
        assertThat(value, is(""));
        value = properties.get("spaceBeforeProp");
        assertThat(value, is("abc"));
        value = properties.get("spaceBeforeAndAfterProp");
        assertThat(value, is("def"));
        value = properties.get("space in prop");
        assertThat("foo bar", is(value));
        value = properties.get("equalsInValue");
        assertThat(value, is("foo=bar"));
        value = properties.get("semiInProp;Name");
        assertThat(value, is("value"));
        value = properties.get("singleQuotedValue");
        assertThat(value, is("single quoted value ending in space "));
        value = properties.get("doubleQuotedValue");
        assertThat(value, is("=double quoted value preceded by equals"));
        value = properties.get("singleQuotedValueWithSemi");
        assertThat("one; two", is(value));
        value = properties.get("singleQuotedValueWithSpecials");
        assertThat("one; two \"three'four=five", is(value));

        assertThat(properties.toString(),
            is("normalProp=value;"
               + " emptyValue=;"
               + " spaceBeforeProp=abc;"
               + " spaceBeforeAndAfterProp=def;"
               + " space in prop=foo bar;"
               + " equalsInValue=foo=bar;"
               + " semiInProp;Name=value;"
               + " singleQuotedValue=single quoted value ending in space ;"
               + " doubleQuotedValue==double quoted value preceded by equals;"
               + " singleQuotedValueWithSemi='one; two';"
               + " singleQuotedValueWithSpecials='one; two \"three''four=five'"));
    }

    @Test public void testConnectStringMore() {
        p("singleQuote=''''", "singleQuote", "'");
        p("doubleQuote=\"\"\"\"", "doubleQuote", "\"");
        p("empty= ;foo=bar", "empty", "");
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-397">
     * MONDRIAN-397, "Connect string parser gives
     * StringIndexOutOfBoundsException instead of a meaningful error"</a>.
     */
    @Test public void testBugMondrian397() {
        Util.PropertyList properties;

        // ends in semi
        properties = Util.parseConnectString("foo=true; bar=xxx;");
        assertThat(properties.list.size(), is(2));

        // ends in semi+space
        properties = Util.parseConnectString("foo=true; bar=xxx; ");
        assertThat(properties.list.size(), is(2));

        // ends in space
        properties = Util.parseConnectString("   ");
        assertThat(properties.list.size(), is(0));

        // actual testcase for bug
        properties = Util.parseConnectString(
            "provider=mondrian; JdbcDrivers=org.hsqldb.jdbcDriver;"
            + "Jdbc=jdbc:hsqldb:./sql/sampledata;"
            + "Catalog=C:\\cygwin\\home\\src\\jfreereport\\engines\\classic"
            + "\\extensions-mondrian\\demo\\steelwheels.mondrian.xml;"
            + "JdbcUser=sa; JdbcPassword=; ");
        assertThat(properties.list.size(), is(6));
        assertThat(properties.get("JdbcPassword"), is(""));
    }

    /**
     * Checks that <code>connectString</code> contains a property called
     * <code>name</code>, whose value is <code>value</code>.
     *
     * @param connectString Connect string
     * @param name Name
     * @param expectedValue Expected value
     */
    void p(String connectString, String name, String expectedValue) {
        Util.PropertyList list = Util.parseConnectString(connectString);
        String value = list.get(name);
        assertThat(value, is(expectedValue));
    }

    @Test public void testOleDbSpec() {
        p("Provider='MSDASQL'", "Provider", "MSDASQL");
        p("Provider='MSDASQL.1'", "Provider", "MSDASQL.1");

        if (false) {
            // If no Provider keyword is in the string, the OLE DB Provider for
            // ODBC (MSDASQL) is the default value. This provides backward
            // compatibility with ODBC connection strings. The ODBC connection
            // string in the following example can be passed in, and it will
            // successfully connect.
            p(
                "Driver={SQL Server};Server={localhost};Trusted_Connection={yes};"
                + "db={Northwind};", "Provider", "MSDASQL");
        }

        // Specifying a Keyword
        //
        // To identify a keyword used after the Provider keyword, use the
        // property description of the OLE DB initialization property that you
        // want to set. For example, the property description of the standard
        // OLE DB initialization property DBPROP_INIT_LOCATION is
        // Location. Therefore, to include this property in a connection
        // string, use the keyword Location.
        p(
            "Provider='MSDASQL';Location='3Northwind'",
            "Location",
            "3Northwind");
        // Keywords can contain any printable character except for the equal
        // sign (=).
        p(
            "Jet OLE DB:System Database=c:\\system.mda",
            "Jet OLE DB:System Database",
            "c:\\system.mda");
        p(
            "Authentication;Info=Column 5",
            "Authentication;Info",
            "Column 5");
        // If a keyword contains an equal sign (=), it must be preceded by an
        // additional equal sign to indicate that it is part of the keyword.
        p(
            "Verification==Security=True",
            "Verification=Security",
            "True");
        // If multiple equal signs appear, each one must be preceded by an
        // additional equal sign.
        p("Many====One=Valid", "Many==One", "Valid");
        p("TooMany===False", "TooMany=", "False");
        // Setting Values That Use Reserved Characters
        //
        // To include values that contain a semicolon, single-quote character,
        // or double-quote character, the value must be enclosed in double
        // quotes.
        p(
            "ExtendedProperties=\"Integrated Security='SSPI';"
            + "Initial Catalog='Northwind'\"",
            "ExtendedProperties",
            "Integrated Security='SSPI';Initial Catalog='Northwind'");
        // If the value contains both a semicolon and a double-quote character,
        // the value can be enclosed in single quotes.
        p(
            "ExtendedProperties='Integrated Security=\"SSPI\";"
            + "Databse=\"My Northwind DB\"'",
            "ExtendedProperties",
            "Integrated Security=\"SSPI\";Databse=\"My Northwind DB\"");
        // The single quote is also useful if the value begins with a
        // double-quote character.
        p(
            "DataSchema='\"MyCustTable\"'",
            "DataSchema",
            "\"MyCustTable\"");
        // Conversely, the double quote can be used if the value begins with a
        // single quote.
        p(
            "DataSchema=\"'MyOtherCustTable'\"",
            "DataSchema",
            "'MyOtherCustTable'");
        // If the value contains both single-quote and double-quote characters,
        // the quote character used to enclose the value must be doubled each
        // time it occurs within the value.
        p(
            "NewRecordsCaption='\"Company''s \"new\" customer\"'",
            "NewRecordsCaption",
            "\"Company's \"new\" customer\"");
        p(
            "NewRecordsCaption=\"\"\"Company's \"\"new\"\" customer\"\"\"",
            "NewRecordsCaption",
            "\"Company's \"new\" customer\"");
        // Setting Values That Use Spaces
        //
        // Any leading or trailing spaces around a keyword or value are
        // ignored. However, spaces within a keyword or value are allowed and
        // recognized.
        p("MyKeyword=My Value", "MyKeyword", "My Value");
        p("MyKeyword= My Value ;MyNextValue=Value", "MyKeyword", "My Value");
        // To include preceding or trailing spaces in the value, the value must
        // be enclosed in either single quotes or double quotes.
        p("MyKeyword=' My Value  '", "MyKeyword", " My Value  ");
        p("MyKeyword=\"  My Value \"", "MyKeyword", "  My Value ");
        if (false) {
            // (Not supported.)
            //
            // If the keyword does not correspond to a standard OLE DB
            // initialization property (in which case the keyword value is
            // placed in the Extended Properties (DBPROP_INIT_PROVIDERSTRING)
            // property), the spaces around the value will be included in the
            // value even though quote marks are not used. This is to support
            // backward compatibility for ODBC connection strings. Trailing
            // spaces after keywords might also be preserved.
        }
        if (false) {
            // (Not supported)
            //
            // Returning Multiple Values
            //
            // For standard OLE DB initialization properties that can return
            // multiple values, such as the Mode property, each value returned
            // is separated with a pipe (|) character. The pipe character can
            // have spaces around it or not.
            //
            // Example   Mode=Deny Write|Deny Read
        }
        // Listing Keywords Multiple Times
        //
        // If a specific keyword in a keyword=value pair occurs multiple times
        // in a connection string, the last occurrence listed is used in the
        // value set.
        p(
            "Provider='MSDASQL';Location='Northwind';"
            + "Cache Authentication='True';Prompt='Complete';"
            + "Location='Customers'",
            "Location",
            "Customers");
        // One exception to the preceding rule is the Provider keyword. If this
        // keyword occurs multiple times in the string, the first occurrence is
        // used.
        p(
            "Provider='MSDASQL';Location='Northwind'; Provider='SQLOLEDB'",
            "Provider",
            "MSDASQL");
        if (false) {
            // (Not supported)
            //
            // Setting the Window Handle Property
            //
            // To set the Window Handle (DBPROP_INIT_HWND) property in a
            // connection string, a long integer value is typically used.
        }
    }

    /**
     * Unit test for {@link Util#convertOlap4jConnectStringToNativeMondrian}.
     */
    @Test public void testConvertConnectString() {
        assertThat(
            Util.convertOlap4jConnectStringToNativeMondrian(
                "jdbc:mondrian:Datasource=jdbc/SampleData;"
                + "Catalog=foodmart/FoodMart.mondrian.xml;"),
            is("Provider=Mondrian; Datasource=jdbc/SampleData;"
               + "Catalog=foodmart/FoodMart.mondrian.xml;"));
    }

    @Test public void testQuoteMdxIdentifier() {
        assertThat(Util.quoteMdxIdentifier("San Francisco"), is("[San Francisco]"));
        assertThat(Util.quoteMdxIdentifier("a [bracketed] string"),
            is("[a [bracketed]] string]"));
        assertThat(Util.quoteMdxIdentifier(
            Arrays.<Id.Segment>asList(
                new Id.NameSegment("Store"),
                new Id.NameSegment("USA"),
                new Id.NameSegment("California"))),
            is("[Store].[USA].[California]"));
    }

    @Test public void testQuoteJava() {
        assertThat(Util.quoteJavaString("San Francisco"), is("\"San Francisco\""));
        assertThat(Util.quoteJavaString("null"), is("\"null\""));
        assertThat(Util.quoteJavaString(null), is("null"));
        assertThat(Util.quoteJavaString("a\\b\"c"), is("\"a\\\\b\\\"c\""));
    }

    @Test public void testBufReplace() {
        // Replace with longer string. Search pattern at beginning & end.
        checkReplace("xoxox", "x", "yy", "yyoyyoyy");

        // Replace with shorter string.
        checkReplace("xxoxxoxx", "xx", "z", "zozoz");

        // Replace with empty string.
        checkReplace("xxoxxoxx", "xx", "", "oo");

        // Replacement string contains search string. (A bad implementation
        // might loop!)
        checkReplace("xox", "x", "xx", "xxoxx");

        // Replacement string combines with characters in the original to
        // match search string.
        checkReplace("cacab", "cab", "bb", "cabb");

        // Seek string does not exist.
        checkReplace(
            "the quick brown fox", "coyote", "wolf",
            "the quick brown fox");

        // Empty buffer.
        checkReplace("", "coyote", "wolf", "");

        // Empty seek string. This is a bit mean!
        checkReplace("fox", "", "dog", "dogfdogodogxdog");
    }

    private static void checkReplace(
        String original, String seek, String replace, String expected)
    {
        // Check whether the JDK does what we expect. (If it doesn't it's
        // probably a bug in the test, not the JDK.)
        assertThat(original.replaceAll(seek, replace), is(expected));

        // Check the StringBuilder version of replace.
        StringBuilder buf = new StringBuilder(original);
        StringBuilder buf2 = Util.replace(buf, 0, seek, replace);
        assertThat(buf.toString(), is(expected));
        assertThat(buf2.toString(), is(expected));
        assertThat(buf, is(buf2));

        // Check the String version of replace.
        assertThat(Util.replace(original, seek, replace), is(expected));
    }

    @Test public void testImplode() {
        List<Id.Segment> fooBar = Arrays.<Id.Segment>asList(
            new Id.NameSegment("foo", Id.Quoting.UNQUOTED),
            new Id.NameSegment("bar", Id.Quoting.UNQUOTED));
        assertThat(Util.implode(fooBar), is("[foo].[bar]"));

        List<Id.Segment> empty = Collections.emptyList();
        assertThat(Util.implode(empty), is(""));

        List<Id.Segment> nasty = Arrays.<Id.Segment>asList(
            new Id.NameSegment("string", Id.Quoting.UNQUOTED),
            new Id.NameSegment("with", Id.Quoting.UNQUOTED),
            new Id.NameSegment("a [bracket] in it", Id.Quoting.UNQUOTED));
        assertThat(Util.implode(nasty), is("[string].[with].[a [bracket]] in it]"));
    }

    @Test public void testParseIdentifier() {
        List<Id.Segment> strings =
            Util.parseIdentifier("[string].[with].[a [bracket]] in it]");
        assertThat(strings.size(), is(3));
        assertThat(name(strings, 2), is("a [bracket] in it"));

        strings =
            Util.parseIdentifier("[Worklog].[All].[calendar-[LANGUAGE]].js]");
        assertThat(strings.size(), is(3));
        assertThat(name(strings, 2), is("calendar-[LANGUAGE].js"));

        // allow spaces before, after and between
        strings = Util.parseIdentifier("  [foo] . [bar].[baz]  ");
        assertThat(strings.size(), is(3));
        final int index = 0;
        assertThat(name(strings, index), is("foo"));

        // first segment not quoted
        strings = Util.parseIdentifier("Time.1997.[Q3]");
        assertThat(strings.size(), is(3));
        assertThat(name(strings, 0), is("Time"));
        assertThat(name(strings, 1), is("1997"));
        assertThat(name(strings, 2), is("Q3"));

        // spaces ignored after unquoted segment
        strings = Util.parseIdentifier("[Time . Weekly ] . 1997 . [Q3]");
        assertThat(strings.size(), is(3));
        assertThat(name(strings, 0), is("Time . Weekly "));
        assertThat(name(strings, 1), is("1997"));
        assertThat(name(strings, 2), is("Q3"));

        // identifier ending in '.' is invalid
        try {
            strings = Util.parseIdentifier("[foo].[bar].");
            fail("expected exception, got " + strings);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),
                is("Expected identifier after '.', "
                   + "in member identifier '[foo].[bar].'"));
        }

        try {
            strings = Util.parseIdentifier("[foo].[bar");
            fail("expected exception, got " + strings);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),
                is("Expected ']', in member identifier '[foo].[bar'"));
        }

        try {
            strings = Util.parseIdentifier("[Foo].[Bar], [Baz]");
            fail("expected exception, got " + strings);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),
                is("Invalid member identifier '[Foo].[Bar], [Baz]'"));
        }
    }

    private String name(List<Id.Segment> strings, int index) {
        final Id.Segment segment = strings.get(index);
        return ((Id.NameSegment) segment).name;
    }

    @Test public void testReplaceProperties() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("foo", "bar");
        map.put("empty", "");
        map.put("null", null);
        map.put("foobarbaz", "bang!");
        map.put("malformed${foo", "groovy");

        assertThat(Util.replaceProperties("a${foo}b", map), is("abarb"));
        assertThat(Util.replaceProperties("twice${foo}${foo}", map),
            is("twicebarbar"));
        assertThat(Util.replaceProperties("${foo} at start", map),
            is("bar at start"));
        assertThat(Util.replaceProperties("x${empty}y${empty}${empty}z", map),
            is("xyz"));
        assertThat(Util.replaceProperties("x${nonexistent}${foo}", map),
            is("x${nonexistent}bar"));

        // malformed tokens are left as is
        assertThat(Util.replaceProperties("${malformed${foo}${foo}", map),
            is("${malformedbarbar"));

        // string can contain '$'
        assertThat(Util.replaceProperties("x$foo", map), is("x$foo"));

        // property with empty name is always ignored -- even if it's in the map
        assertThat(Util.replaceProperties("${}", map), is("${}"));
        map.put("", "v");
        assertThat(Util.replaceProperties("${}", map), is("${}"));

        // if a property's value is null, it's as if it doesn't exist
        assertThat(Util.replaceProperties("${null}", map), is("${null}"));

        // nested properties are expanded, but not recursively
        assertThat(Util.replaceProperties("${foo${foo}baz}", map),
            is("${foobarbaz}"));
    }

    @Test public void testWildcard() {
        assertThat(Util.wildcardToRegexp(Arrays.asList("_Foo_", "Bar%BAZ")),
            is(".\\QFoo\\E.|\\QBar\\E.*\\QBAZ\\E"));
    }

    @Test public void testCamel() {
        assertThat(Util.camelToUpper("FooBar"), is("FOO_BAR"));
        assertThat(Util.camelToUpper("fooBar"), is("FOO_BAR"));
        assertThat(Util.camelToUpper("URL"), is("URL"));
        assertThat(Util.camelToUpper("URLtoClickOn"), is("URLTO_CLICK_ON"));
        assertThat(Util.camelToUpper(""), is(""));
    }

    @Test public void testParseCommaList() {
        assertThat(Util.parseCommaList(""), is((List<String>) new ArrayList<String>()));
        assertThat(Util.parseCommaList("x"), is(Arrays.asList("x")));
        assertThat(Util.parseCommaList("x,y"), is(Arrays.asList("x", "y")));
        assertThat(Util.parseCommaList("x,,y"), is(Arrays.asList("x,y")));
        assertThat(Util.parseCommaList(",,x,y"), is(Arrays.asList(",x", "y")));
        assertThat(Util.parseCommaList("x,,,y"), is(Arrays.asList("x,", "y")));
        assertThat(Util.parseCommaList("x,,,,y"), is(Arrays.asList("x,,y")));
        // ignore trailing comma
        assertThat(Util.parseCommaList("x,y,"), is(Arrays.asList("x", "y")));
        assertThat(Util.parseCommaList("x,y,,"), is(Arrays.asList("x", "y,")));
    }

    /**
     * Unit test for {@link Util#bit}.
     */
    @Test public void testBit() {
        assertThat(Util.bit(0, 0, true), is(1));
        assertThat(Util.bit(0, 0, false), is(0));
        assertThat(Util.bit(1, 2, true), is(5));
        assertThat(Util.bit(1, 2, false), is(1));
        assertThat(Util.bit(5, 2, true), is(5));
        assertThat(Util.bit(5, 2, false), is(1));
    }

    @Test public void testUnionIterator() {
        final List<String> xyList = Arrays.asList("x", "y");
        final List<String> abcList = Arrays.asList("a", "b", "c");
        final List<String> emptyList = Collections.emptyList();

        String total = "";
        for (String s : UnionIterator.over(xyList, abcList)) {
            total += s + ";";
        }
        assertThat(total, is("x;y;a;b;c;"));

        total = "";
        for (String s : UnionIterator.over(xyList, emptyList)) {
            total += s + ";";
        }
        assertThat(total, is("x;y;"));

        total = "";
        for (String s : UnionIterator.over(emptyList, xyList, emptyList)) {
            total += s + ";";
        }
        assertThat(total, is("x;y;"));

        total = "";
        for (String s : UnionIterator.<String>over()) {
            total += s + ";";
        }
        assertThat(total, is(""));

        total = "";
        UnionIterator<String> unionIterator =
            new UnionIterator<String>(xyList, abcList);
        while (unionIterator.hasNext()) {
            total += unionIterator.next() + ";";
        }
        assertThat(total, is("x;y;a;b;c;"));

        if (Util.Retrowoven) {
            // Retrowoven code gives 'ArrayStoreException' when it encounters
            // 'Util.union()' applied to java.util.Iterator objects.
            return;
        }

        total = "";
        for (String s : UnionIterator.over((Iterable<String>) xyList, abcList))
        {
            total += s + ";";
        }
        assertThat(total, is("x;y;a;b;c;"));
    }

    /**
     * Unit test for
     * {@link Util#filter(Iterable, mondrian.olap.Util.Predicate1[])}.
     */
    @Test public void testFilter() {
        class NotMultiple extends Util.Predicate1<Integer> {
            private final int n;

            public NotMultiple(int n) {
                this.n = n;
            }

            public boolean test(Integer integer) {
                return integer % n != 0;
            }
        }

        class IntegersLessThan implements Iterable<Integer> {
            private final int n;

            public IntegersLessThan(int n) {
                this.n = n;
            }

            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int i;

                    public boolean hasNext() {
                        return i < n;
                    }

                    public Integer next() {
                        return i++;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }

        final StringBuilder buf = new StringBuilder();

        // list the natural numbers < 20 that are not multiples of 2, 3 or 5.
        final Iterable<Integer> filter =
            Util.filter(
                new IntegersLessThan(20),
                new NotMultiple(2),
                new NotMultiple(3),
                new NotMultiple(5));
        for (Integer integer : filter) {
            buf.append(integer).append(";");
        }
        assertThat(buf.toString(), is("1;7;11;13;17;19;"));
        buf.setLength(0);

        // underlying iterable is empty
        final Iterable<Integer> filter0 =
            Util.filter(
                new IntegersLessThan(0),
                new NotMultiple(2),
                new NotMultiple(5));
        for (Integer integer : filter0) {
            buf.append(integer).append(";");
        }
        assertThat(buf.toString(), is(""));
        buf.setLength(0);

        // some "always true" predicates to be eliminated
        final Iterable<Integer> filter8 =
            Util.filter(
                new IntegersLessThan(8),
                Util.<Integer>truePredicate1(),
                new NotMultiple(2),
                Util.<Integer>truePredicate1(),
                new NotMultiple(5));
        for (Integer integer : filter8) {
            buf.append(integer).append(";");
        }
        assertThat(buf.toString(), is("1;3;7;"));
        buf.setLength(0);
    }

    @Test public void testAreOccurrencesEqual() {
        assertThat(Util.areOccurencesEqual(Collections.<String>emptyList()), is(false));
        assertThat(Util.areOccurencesEqual(Arrays.asList("x")), is(true));
        assertThat(Util.areOccurencesEqual(Arrays.asList("x", "x")), is(true));
        assertThat(Util.areOccurencesEqual(Arrays.asList("x", "y")), is(false));
        assertThat(Util.areOccurencesEqual(Arrays.asList("x", "y", "x")), is(false));
        assertThat(Util.areOccurencesEqual(Arrays.asList("x", "x", "x")), is(true));
        assertThat(Util.areOccurencesEqual(Arrays.asList("x", "x", "y", "z")), is(false));
    }

    /**
     * Tests {@link mondrian.util.ServiceDiscovery}.
     */
    @Test public void testServiceDiscovery() {
        final ServiceDiscovery<Driver>
            serviceDiscovery = ServiceDiscovery.forClass(Driver.class);
        final List<Class<Driver>> list = serviceDiscovery.getImplementor();
        assertThat(list.isEmpty(), is(false));

        // Check that discovered classes include AT LEAST:
        // JdbcOdbcDriver (in the JDK),
        // MondrianOlap4jDriver (in mondrian) and
        // XmlaOlap4jDriver (in olap4j.jar).
        List<String> expectedClassNames =
            new ArrayList<String>(
                Arrays.asList(
                    // Usually on the list, but not guaranteed:
                    // "sun.jdbc.odbc.JdbcOdbcDriver",
                    "mondrian.olap4j.MondrianOlap4jDriver",
                    "org.olap4j.driver.xmla.XmlaOlap4jDriver"));
        for (Class<Driver> driverClass : list) {
            expectedClassNames.remove(driverClass.getName());
        }
        if (Util.PreJdk15) {
            // JDK only discovers services from jars in JDK 1.5 and later.
            return;
        }
        assertThat(expectedClassNames.toString(), expectedClassNames.isEmpty(), is(true));
    }

    /**
     * Unit test for {@link mondrian.util.ArrayStack}.
     */
    @Test public void testArrayStack() {
        final ArrayStack<String> stack = new ArrayStack<String>();
        assertThat(stack.size(), is(0));
        stack.add("a");
        assertThat(stack.size(), is(1));
        assertThat(stack.peek(), is("a"));
        stack.push("b");
        assertThat(stack.size(), is(2));
        assertThat(stack.peek(), is("b"));
        assertThat(stack.pop(), is("b"));
        assertThat(stack.size(), is(1));
        stack.add(0, "z");
        assertThat(stack.peek(), is("a"));
        assertThat(stack.size(), is(2));
        stack.push(null);
        assertThat(stack.size(), is(3));
        assertThat(Arrays.asList("z", "a", null), is((List<String>) stack));
        String z = "";
        for (String s : stack) {
            z += s;
        }
        assertThat(z, is("zanull"));
        stack.clear();
        try {
            String x = stack.peek();
            fail("expected error, got " + x);
        } catch (EmptyStackException e) {
            // ok
        }
        try {
            String x = stack.pop();
            fail("expected error, got " + x);
        } catch (EmptyStackException e) {
            // ok
        }
    }

    /**
     * Tests {@link Util#appendArrays(Object[], Object[][])}.
     */
    @Test public void testAppendArrays() {
        String[] a0 = {"a", "b", "c"};
        String[] a1 = {"foo", "bar"};
        String[] empty = {};

        final String[] strings1 = Util.appendArrays(a0, a1);
        assertThat(strings1.length, is(5));
        assertThat(Arrays.asList(strings1), is(Arrays.asList("a", "b", "c", "foo", "bar")));

        final String[] strings2 = Util.appendArrays(
            empty, a0, empty, a1, empty);
        assertThat(Arrays.asList(strings2), is(Arrays.asList("a", "b", "c", "foo", "bar")));

        Number[] n0 = {Math.PI};
        Integer[] i0 = {123, null, 45};
        Float[] f0 = {0f};

        final Number[] numbers = Util.appendArrays(n0, i0, f0);
        assertThat(numbers.length, is(5));
        assertThat(Arrays.asList(numbers), is(Arrays.asList((Number) Math.PI, 123, null, 45, 0f)));
    }

    @Test public void testCanCast() {
        assertThat(Util.canCast(Collections.EMPTY_LIST, Integer.class), is(true));
        assertThat(Util.canCast(Collections.EMPTY_LIST, String.class), is(true));
        assertThat(Util.canCast(Collections.EMPTY_SET, String.class), is(true));
        assertThat(Util.canCast(Arrays.asList(1, 2), Integer.class), is(true));
        assertThat(Util.canCast(Arrays.asList(1, 2), Number.class), is(true));
        assertThat(Util.canCast(Arrays.asList(1, 2), String.class), is(false));
        assertThat(Util.canCast(Arrays.asList(1, null, 2d), Number.class), is(true));
        assertThat(Util.canCast(
            new HashSet<Object>(Arrays.asList(1, null, 2d)),
            Number.class), is(true));
        assertThat(Util.canCast(Arrays.asList(1, null, 2d), Integer.class), is(false));
    }

    /**
     * Unit test for {@link Util#parseLocale(String)} method.
     */
    @Test public void testParseLocale() {
        Locale[] locales = {
            Locale.CANADA,
            Locale.CANADA_FRENCH,
            Locale.getDefault(),
            Locale.US,
            Locale.TRADITIONAL_CHINESE,
        };
        for (Locale locale : locales) {
            assertThat(Util.parseLocale(locale.toString()), is(locale));
        }
        // Example locale names in Locale.toString() javadoc.
        String[] localeNames = {
            "en", "de_DE", "_GB", "en_US_WIN", "de__POSIX", "fr__MAC"
        };
        for (String localeName : localeNames) {
            assertThat(Util.parseLocale(localeName).toString(), is(localeName));
        }
    }

    /**
     * Unit test for {@link mondrian.util.LockBox}.
     */
    @Test public void testLockBox() {
        final LockBox box =
            new LockBox();

        final String abc = "abc";
        final String xy = "xy";

        // Register an object.
        final LockBox.Entry abcEntry0 = box.register(abc);
        assertThat(abcEntry0, notNullValue());
        assertThat(abcEntry0.getValue(), sameInstance((Object) abc));
        checkMonikerValid(abcEntry0.getMoniker());

        // Register another object
        final LockBox.Entry xyEntry = box.register(xy);
        checkMonikerValid(xyEntry.getMoniker());
        assertThat(xyEntry.getMoniker(),
            not(sameInstance(abcEntry0.getMoniker())));

        // Register first object again. Moniker is different. It is a different
        // registration.
        final LockBox.Entry abcEntry1 = box.register(abc);
        checkMonikerValid(abcEntry1.getMoniker());
        assertThat(abcEntry1.getMoniker().equals(abcEntry0.getMoniker()),
            is(false));
        assertThat(abc, sameInstance(abcEntry1.getValue()));

        // Retrieve.
        final LockBox.Entry abcEntry0b = box.get(abcEntry0.getMoniker());
        assertThat(abcEntry0b, notNullValue());
        assertThat(abcEntry0.getMoniker(), is(abcEntry0b.getMoniker()));
        assertThat(abcEntry0b, sameInstance(abcEntry0));
        assertThat(abcEntry1, not(sameInstance(abcEntry0b)));
        assertThat(!abcEntry0b.getMoniker().equals(abcEntry1.getMoniker()),
            is(true));

        // Arbitrary moniker retrieves nothing. (A random moniker might,
        // with very very small probability, happen to match that of one of
        // the two registered entries. However, I know that our generation
        // scheme never generates monikers starting with 'x'.)
        assertThat(box.get("xxx"), nullValue());

        // Deregister.
        assertThat(abcEntry0b.isRegistered(), is(true));
        assertThat(box.deregister(abcEntry0b), is(true));
        assertThat(abcEntry0b.isRegistered(), is(false));
        assertThat(box.get(abcEntry0.getMoniker()), nullValue());

        // The other entry created by the same call to 'register' is also
        // deregistered.
        assertThat(abcEntry0.isRegistered(), is(false));
        assertThat(abcEntry1.isRegistered(), is(true));

        // Deregister again.
        assertThat(box.deregister(abcEntry0b), is(false));
        assertThat(abcEntry0b.isRegistered(), is(false));
        assertThat(abcEntry0.isRegistered(), is(false));
        assertThat(box.get(abcEntry0.getMoniker()), nullValue());

        // Entry it no longer registered, therefore cannot get value.
        try {
            Object value = abcEntry0.getValue();
            fail("expected exception, got " + value);
        } catch (RuntimeException e) {
            assertThat(e.getMessage().startsWith("LockBox has no entry with moniker"), is(true));
        }
        assertThat(abcEntry0.getMoniker(), notNullValue());

        // Other registration of same object still works.
        final LockBox.Entry abcEntry1b = box.get(abcEntry1.getMoniker());
        assertThat(abcEntry1b, notNullValue());
        assertThat(abcEntry1, sameInstance(abcEntry1b));
        assertThat(abc, sameInstance(abcEntry1b.getValue()));
        assertThat(abc, sameInstance(abcEntry1.getValue()));

        // Other entry still exists.
        final LockBox.Entry xyEntry2 = box.get(xyEntry.getMoniker());
        assertThat(xyEntry2, notNullValue());
        assertThat(xyEntry, sameInstance(xyEntry2));
        assertThat(xy, sameInstance(xyEntry2.getValue()));
        assertThat(xy, sameInstance(xyEntry.getValue()));

        // Register again. Moniker is different. (Monikers are never recycled.)
        final LockBox.Entry abcEntry3 = box.register(abc);
        checkMonikerValid(abcEntry3.getMoniker());
        assertThat(abcEntry3.getMoniker().equals(abcEntry0.getMoniker()), is(false));
        assertThat(abcEntry3.getMoniker().equals(abcEntry1.getMoniker()), is(false));
        assertThat(abcEntry3.getMoniker().equals(abcEntry0b.getMoniker()), is(false));

        // Previous entry is no longer valid.
        assertThat(abcEntry1.isRegistered(), is(true));
        assertThat(abcEntry0.isRegistered(), is(false));
    }

    void checkMonikerValid(String moniker) {
        final String digits = "0123456789";
        final String validChars =
            "0123456789"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz"
            + "$_";
        assertThat(moniker.length() > 0, is(true));
        // all chars are valid
        for (int i = 0; i < moniker.length(); i++) {
            assertThat(moniker, validChars.indexOf(moniker.charAt(i)) >= 0, is(true));
        }
        // does not start with digit
        assertThat(moniker, digits.indexOf(moniker.charAt(0)) >= 0, is(false));
    }

    /**
     * Unit test that ensures that {@link mondrian.util.LockBox} can "forget"
     * entries whose key has been forgotten.
     */
    @Test public void testLockBoxFull() {
        final LockBox box =
            new LockBox();

        // Generate large, unique strings for values by concatenating a base
        // of "xxx ... x" with a unique integer.
        StringBuilder builder = new StringBuilder();
        final int prevLength = 10000;
        for (int i = 0; i < prevLength; i++) {
            builder.append("x");
        }

        final int max = 1000000;
        LockBox.Entry[] entries = new LockBox.Entry[567];
        for (int i = 0; i < max; i++) {
            builder.setLength(prevLength);
            builder.append(i);
            String value = builder.toString();

            final LockBox.Entry entry = box.register(value);

            // Save most recent keys in a circular array to prevent GC. Older
            // keys are forgotten, therefore the entries can be removed from the
            // lock box.
            entries[i % entries.length] = entry;

            // Test one of the previous entries.
            assertThat(entries[Math.min(i, (i * 19) % 567)].getValue(), notNullValue());
        }
    }

    @Test public void testCartesianProductList() {
        final CartesianProductList<String> list =
            new CartesianProductList<String>(
                Arrays.asList(
                    Arrays.asList("a", "b"),
                    Arrays.asList("1", "2", "3")));
        assertThat(list.size(), is(6));
        assertThat(list.isEmpty(), is(false));
        checkCartesianListContents(list);

        assertThat(list.toString(),
            is("[[a, 1], [a, 2], [a, 3], [b, 1], [b, 2], [b, 3]]"));

        // One element empty
        final CartesianProductList<String> list2 =
            new CartesianProductList<String>(
                Arrays.asList(
                    Arrays.<String>asList(),
                    Arrays.asList("1", "2", "3")));
        assertThat(list2.isEmpty(), is(true));
        assertThat(list2.toString(), is("[]"));
        checkCartesianListContents(list2);

        // Other component empty
        final CartesianProductList<String> list3 =
            new CartesianProductList<String>(
                Arrays.asList(
                    Arrays.asList("a", "b"),
                    Arrays.<String>asList()));
        assertThat(list3.isEmpty(), is(true));
        assertThat(list3.toString(), is("[]"));
        checkCartesianListContents(list3);

        // Zeroary
        final CartesianProductList<String> list4 =
            new CartesianProductList<String>(
                Collections.<List<String>>emptyList());
        assertThat(list4.isEmpty(), is(false));
        //        assertEquals("[[]]", list4.toString());
        checkCartesianListContents(list4);

        // 1-ary
        final CartesianProductList<String> list5 =
            new CartesianProductList<String>(
                Collections.singletonList(
                    Arrays.asList("a", "b")));
        assertThat(list5.toString(), is("[[a], [b]]"));
        checkCartesianListContents(list5);

        // 3-ary
        final CartesianProductList<String> list6 =
            new CartesianProductList<String>(
                Arrays.asList(
                    Arrays.asList("a", "b", "c", "d"),
                    Arrays.asList("1", "2"),
                    Arrays.asList("x", "y", "z")));
        assertThat(list6.size(), is(24));
        assertThat(list6.isEmpty(), is(false));
        assertThat(list6.get(0).toString(), is("[a, 1, x]"));
        assertThat(list6.get(1).toString(), is("[a, 1, y]"));
        assertThat(list6.get(23).toString(), is("[d, 2, z]"));
        checkCartesianListContents(list6);

        final Object[] strings = new Object[6];
        list6.getIntoArray(1, strings);
        assertThat(Arrays.asList(strings).toString(),
            is("[a, 1, y, null, null, null]"));

        CartesianProductList<Object> list7 =
            new CartesianProductList<Object>(
                Arrays.<List<Object>>asList(
                    Arrays.<Object>asList(
                        "1",
                        Arrays.asList("2a", null, "2c"),
                        "3"),
                    Arrays.<Object>asList(
                        "a",
                        Arrays.asList("bb", "bbb"),
                        "c",
                        "d")));
        list7.getIntoArray(1, strings);
        assertThat(Arrays.asList(strings).toString(),
            is("[1, bb, bbb, null, null, null]"));
        list7.getIntoArray(5, strings);
        assertThat(Arrays.asList(strings).toString(),
            is("[2a, null, 2c, bb, bbb, null]"));
        checkCartesianListContents(list7);
    }

    private <T> void checkCartesianListContents(CartesianProductList<T> list) {
        List<List<T>> arrayList = new ArrayList<List<T>>();
        for (List<T> ts : list) {
            arrayList.add(ts);
        }
        assertThat(list, is(arrayList));
    }

    @Test public void testFlatList() {
        final List<String> flatAB = Util.flatList("a", "b");
        final List<String> arrayAB = Arrays.asList("a", "b");
        assertThat(flatAB, is(flatAB));
        assertThat(arrayAB, is(flatAB));
        assertThat(flatAB, is(arrayAB));
        assertThat(flatAB.hashCode(), is(arrayAB.hashCode()));

        final List<String> flatABC = Util.flatList("a", "b", "c");
        final List<String> arrayABC = Arrays.asList("a", "b", "c");
        assertThat(flatABC, is(flatABC));
        assertThat(arrayABC, is(flatABC));
        assertThat(flatABC, is(arrayABC));
        assertThat(flatABC.hashCode(), is(arrayABC.hashCode()));

        assertThat(flatABC.toString(), is("[a, b, c]"));
        assertThat(flatAB.toString(), is("[a, b]"));

        final List<String> arrayEmpty = Arrays.asList();
        final List<String> arrayA = Collections.singletonList("a");

        // mixed 2 & 3
        final List<List<String>> notAB =
            Arrays.asList(arrayEmpty, arrayA, arrayABC, flatABC);
        for (List<String> strings : notAB) {
            assertThat(strings.equals(flatAB), is(false));
            assertThat(flatAB.equals(strings), is(false));
        }
        final List<List<String>> notABC =
            Arrays.asList(arrayEmpty, arrayA, arrayAB, flatAB);
        for (List<String> strings : notABC) {
            assertThat(strings.equals(flatABC), is(false));
            assertThat(flatABC.equals(strings), is(false));
        }
    }

    /**
     * Unit test for {@link Composite#of(Iterable[])}.
     */
    @Test public void testCompositeIterable() {
        final Iterable<String> beatles =
            Arrays.asList("john", "paul", "george", "ringo");
        final Iterable<String> stones =
            Arrays.asList("mick", "keef", "brian", "bill", "charlie");
        final List<String> empty = Collections.emptyList();

        final StringBuilder buf = new StringBuilder();
        for (String s : Composite.of(beatles, stones)) {
            buf.append(s).append(";");
        }
        assertThat(buf.toString(),
            is("john;paul;george;ringo;mick;keef;brian;bill;charlie;"));

        buf.setLength(0);
        for (String s : Composite.of(empty, stones)) {
            buf.append(s).append(";");
        }
        assertThat(buf.toString(), is("mick;keef;brian;bill;charlie;"));

        buf.setLength(0);
        for (String s : Composite.of(stones, empty)) {
            buf.append(s).append(";");
        }
        assertThat(buf.toString(), is("mick;keef;brian;bill;charlie;"));

        buf.setLength(0);
        for (String s : Composite.of(empty)) {
            buf.append(s).append(";");
        }
        assertThat(buf.toString(), is(""));

        buf.setLength(0);
        for (String s : Composite.of(empty, empty, beatles, empty, empty)) {
            buf.append(s).append(";");
        }
        assertThat(buf.toString(), is("john;paul;george;ringo;"));
    }

    /**
     * Unit test for {@link ByteString}.
     */
    @Test public void testByteString() {
        final ByteString empty0 = new ByteString(new byte[]{});
        final ByteString empty1 = new ByteString(new byte[]{});
        assertThat(empty0.equals(empty1), is(true));
        assertThat(empty1.hashCode(), is(empty0.hashCode()));
        assertThat(empty0.toString(), is(""));
        assertThat(empty0.length(), is(0));
        assertThat(empty0.compareTo(empty0), is(0));
        assertThat(empty0.compareTo(empty1), is(0));

        final ByteString two =
            new ByteString(new byte[]{ (byte) 0xDE, (byte) 0xAD});
        assertThat(empty0.equals(two), is(false));
        assertThat(two.equals(empty0), is(false));
        assertThat(two.toString(), is("dead"));
        assertThat(two.length(), is(2));
        assertThat(two.compareTo(two), is(0));
        assertThat(empty0.compareTo(two) < 0, is(true));
        assertThat(two.compareTo(empty0) > 0, is(true));

        final ByteString three =
            new ByteString(new byte[]{ (byte) 0xDE, (byte) 0x02, (byte) 0xAD});
        assertThat(three.length(), is(3));
        assertThat(three.toString(), is("de02ad"));
        assertThat(two.compareTo(three) < 0, is(true));
        assertThat(three.compareTo(two) > 0, is(true));
        assertThat(three.byteAt(1), is((byte) 0x02));

        final HashSet<ByteString> set = new HashSet<ByteString>();
        set.addAll(Arrays.asList(empty0, two, three, two, empty1, three));
        assertThat(set.size(), is(3));
    }

    /**
     * Unit test for {@link Util#binarySearch}.
     */
    @Test public void testBinarySearch() {
        final String[] abce = {"a", "b", "c", "e"};
        assertThat(Util.binarySearch(abce, 0, 4, "a"), is(0));
        assertThat(Util.binarySearch(abce, 0, 4, "b"), is(1));
        assertThat(Util.binarySearch(abce, 1, 4, "b"), is(1));
        assertThat(Util.binarySearch(abce, 0, 4, "d"), is(-4));
        assertThat(Util.binarySearch(abce, 1, 4, "d"), is(-4));
        assertThat(Util.binarySearch(abce, 2, 4, "d"), is(-4));
        assertThat(Util.binarySearch(abce, 2, 3, "d"), is(-4));
        assertThat(Util.binarySearch(abce, 2, 3, "e"), is(-4));
        assertThat(Util.binarySearch(abce, 2, 3, "f"), is(-4));
        assertThat(Util.binarySearch(abce, 0, 4, "f"), is(-5));
        assertThat(Util.binarySearch(abce, 2, 4, "f"), is(-5));
    }

    /**
     * Unit test for {@link mondrian.util.ArraySortedSet}.
     */
    @Test public void testArraySortedSet() {
        String[] abce = {"a", "b", "c", "e"};
        final SortedSet<String> abceSet =
            new ArraySortedSet<String>(abce);

        // test size, isEmpty, contains
        assertThat(abceSet.size(), is(4));
        assertThat(abceSet.isEmpty(), is(false));
        assertThat(abceSet.first(), is("a"));
        assertThat(abceSet.last(), is("e"));
        assertThat(abceSet.contains("a"), is(true));
        assertThat(abceSet.contains("aa"), is(false));
        assertThat(abceSet.contains("z"), is(false));
        assertThat(abceSet.contains(null), is(false));
        checkToString("[a, b, c, e]", abceSet);

        // test iterator
        String z = "";
        for (String s : abceSet) {
            z += s + ";";
        }
        assertThat(z, is("a;b;c;e;"));

        // empty set
        String[] empty = {};
        final SortedSet<String> emptySet =
            new ArraySortedSet<String>(empty);
        int n = 0;
        for (String s : emptySet) {
            Util.discard(s);
            ++n;
        }
        assertThat(n, is(0));
        assertThat(emptySet.size(), is(0));
        assertThat(emptySet.isEmpty(), is(true));
        try {
            String s = emptySet.first();
            fail("expected exception, got " + s);
        } catch (NoSuchElementException e) {
            // ok
        }
        try {
            String s = emptySet.last();
            fail("expected exception, got " + s);
        } catch (NoSuchElementException e) {
            // ok
        }
        assertThat(emptySet.contains("a"), is(false));
        assertThat(emptySet.contains("aa"), is(false));
        assertThat(emptySet.contains("z"), is(false));
        checkToString("[]", emptySet);

        // same hashCode etc. as similar hashset
        final HashSet<String> abcHashset = new HashSet<String>();
        abcHashset.addAll(Arrays.asList(abce));
        assertThat(abceSet, is((Set<String>) abcHashset));
        assertThat(abcHashset, is((Set<String>) abceSet));
        assertThat(abcHashset.hashCode(), is(abceSet.hashCode()));

        // subset to end
        final Set<String> subsetEnd = new ArraySortedSet<String>(abce, 1, 4);
        checkToString("[b, c, e]", subsetEnd);
        assertThat(subsetEnd.size(), is(3));
        assertThat(subsetEnd.isEmpty(), is(false));
        assertThat(subsetEnd.contains("c"), is(true));
        assertThat(subsetEnd.contains("a"), is(false));
        assertThat(subsetEnd.contains("z"), is(false));

        // subset from start
        final Set<String> subsetStart = new ArraySortedSet<String>(abce, 0, 2);
        checkToString("[a, b]", subsetStart);
        assertThat(subsetStart.size(), is(2));
        assertThat(subsetStart.isEmpty(), is(false));
        assertThat(subsetStart.contains("a"), is(true));
        assertThat(subsetStart.contains("c"), is(false));

        // subset from neither start or end
        final Set<String> subset = new ArraySortedSet<String>(abce, 1, 2);
        checkToString("[b]", subset);
        assertThat(subset.size(), is(1));
        assertThat(subset.isEmpty(), is(false));
        assertThat(subset.contains("b"), is(true));
        assertThat(subset.contains("a"), is(false));
        assertThat(subset.contains("e"), is(false));

        // empty subset
        final Set<String> subsetEmpty = new ArraySortedSet<String>(abce, 1, 1);
        checkToString("[]", subsetEmpty);
        assertThat(subsetEmpty.size(), is(0));
        assertThat(subsetEmpty.isEmpty(), is(true));
        assertThat(subsetEmpty.contains("e"), is(false));

        // subsets based on elements, not ordinals
        assertThat(subsetStart, is((Set<String>) abceSet.subSet("a", "c")));
        assertThat(abceSet.subSet("a", "d").toString(), is("[a, b, c]"));
        assertThat(abceSet.subSet("a", "e").equals(subsetStart), is(false));
        assertThat(abceSet.subSet("b", "c").equals(subsetStart), is(false));
        assertThat(abceSet.subSet("c", "z").toString(), is("[c, e]"));
        assertThat(abceSet.subSet("d", "z").toString(), is("[e]"));
        assertThat(abceSet.subSet("e", "c").equals(subsetStart), is(false));
        assertThat(abceSet.subSet("e", "c").toString(), is("[]"));
        assertThat(abceSet.subSet("z", "c").equals(subsetStart), is(false));
        assertThat(abceSet.subSet("z", "c").toString(), is("[]"));

        // merge
        final ArraySortedSet<String> ar1 = new ArraySortedSet<String>(abce);
        final ArraySortedSet<String> ar2 =
            new ArraySortedSet<String>(
                new String[] {"d"});
        final ArraySortedSet<String> ar3 =
            new ArraySortedSet<String>(
                new String[] {"b", "c"});
        checkToString("[a, b, c, e]", ar1);
        checkToString("[d]", ar2);
        checkToString("[b, c]", ar3);
        checkToString("[a, b, c, d, e]", ar1.merge(ar2));
        checkToString("[a, b, c, e]", ar1.merge(ar3));
    }

    private void checkToString(String expected, Set<String> set) {
        assertThat(set.toString(), is(expected));

        final List<String> list = new ArrayList<String>();
        list.addAll(set);
        assertThat(list.toString(), is(expected));

        list.clear();
        for (String s : set) {
            list.add(s);
        }
        assertThat(list.toString(), is(expected));
    }

    @Test public void testIntersectSortedSet() {
        final ArraySortedSet<String> ace =
            new ArraySortedSet<String>(new String[]{ "a", "c", "e"});
        final ArraySortedSet<String> cd =
            new ArraySortedSet<String>(new String[]{ "c", "d"});
        final ArraySortedSet<String> bdf =
            new ArraySortedSet<String>(new String[]{ "b", "d", "f"});
        final ArraySortedSet<String> bde =
            new ArraySortedSet<String>(new String[]{ "b", "d", "e"});
        final ArraySortedSet<String> empty =
            new ArraySortedSet<String>(new String[]{});
        checkToString("[a, c, e]", Util.intersect(ace, ace));
        checkToString("[c]", Util.intersect(ace, cd));
        checkToString("[]", Util.intersect(ace, empty));
        checkToString("[]", Util.intersect(empty, ace));
        checkToString("[]", Util.intersect(empty, empty));
        checkToString("[]", Util.intersect(ace, bdf));
        checkToString("[e]", Util.intersect(ace, bde));
    }

    /**
     * Unit test for {@link mondrian.olap.Util#newIdentityHashSet()}.
     */
    @SuppressWarnings("UnnecessaryBoxing")
    public void _testIdentityHashSet() {
        final Set<Integer> set = Util.newIdentityHashSet();
        assertThat(set.isEmpty(), is(true));
        assertThat(set.size(), is(0));

        final Integer oneA = new Integer(1);
        assertThat(set.add(oneA), is(true));
        assertThat(set.size(), is(1));

        Integer twoA = new Integer(2);
        assertThat(set.add(twoA), is(true));
        assertThat(set.size(), is(2));

        assertThat(set.add(oneA), is(false));
        assertThat(set.size(), is(2));

        final Integer oneB = new Integer(1);
        assertThat(set.add(oneB), is(true));
        assertThat(set.size(), is(3));

        assertThat(set.contains(oneA), is(true));
        assertThat(set.contains(oneA), is(true));
        final Integer oneC = new Integer(1);
        assertThat(set.contains(oneC), is(false));
        assertThat(set.contains(twoA), is(true));

        final List<Integer> list = new ArrayList<Integer>(set);
        Collections.sort(list);
        assertThat(list.toString(), is("[1, 1, 2]"));

        // add via iterator
        list.clear();
        for (Integer integer : set) {
            list.add(integer);
        }
        Collections.sort(list);
        assertThat(list.toString(), is("[1, 1, 2]"));

        set.clear();
        assertThat(set.isEmpty(), is(true));

        set.addAll(list);
        assertThat(set.isEmpty(), is(false));
        assertThat(set.size(), is(3));

        assertThat(set.remove(oneA), is(true));
        assertThat(set.remove(oneC), is(false));
        assertThat(set.remove(oneB), is(true));
        assertThat(set.remove(twoA), is(true));
        assertThat(set.isEmpty(), is(true));
    }

    /**
     * Unit test for {@link Tuple3}.
     */
    @Test public void testTuple3() {
        if (Util.PreJdk15) {
            // Boolean is not Comparable until JDK 1.5. Tuple3 works, but this
            // test does not.
            return;
        }
        final Tuple3<Integer, String, Boolean> tuple0 =
            Tuple3.of(5, "foo", true);
        final Tuple3<Integer, String, Boolean> tuple1 =
            Tuple3.of(5, "foo", false);
        final Tuple3<Integer, String, Boolean> tuple2 =
            Tuple3.of(5, "foo", true);
        final Tuple3<Integer, String, Boolean> tuple3 =
            Tuple3.of(null, "foo", true);

        assertThat(tuple0, is(tuple0));
        assertThat(tuple0.equals(tuple1), is(false));
        assertThat(tuple1.equals(tuple0), is(false));
        assertThat(tuple0.hashCode() == tuple1.hashCode(), is(false));
        assertThat(tuple2, is(tuple0));
        assertThat(tuple2.hashCode(), is(tuple0.hashCode()));
        assertThat(tuple3, is(tuple3));
        assertThat(tuple0.equals(tuple3), is(false));
        assertThat(tuple3.equals(tuple0), is(false));
        assertThat(tuple0.hashCode() == tuple3.hashCode(), is(false));

        final SortedSet<Tuple3<Integer, String, Boolean>> set =
            new TreeSet<Tuple3<Integer, String, Boolean>>(
                Arrays.asList(tuple0, tuple1, tuple2, tuple3, tuple1));
        assertThat(set.size(), is(3));
        assertThat(set.toString(),
            is("[<null, foo, true>, <5, foo, false>, <5, foo, true>]"));

        assertThat(tuple0.toString(), is("<5, foo, true>"));
        assertThat(tuple1.toString(), is("<5, foo, false>"));
        assertThat(tuple2.toString(), is("<5, foo, true>"));
        assertThat(tuple3.toString(), is("<null, foo, true>"));

        // Unzip and zip.
        assertThat(Util.toList(set), is(Util.toList(
            Tuple3.iterate(
                Tuple3.slice0(set),
                Tuple3.slice1(set),
                Tuple3.slice2(set)))));
    }

    @Test public void testRolapUtilComparator() throws Exception {
        final Comparable[] compArray =
            new Comparable[] {
                "1",
                "2",
                "3",
                "4"
            };
        // Will throw a ClassCastException if it fails.
        Util.binarySearch(
            compArray, 0, compArray.length,
            RolapUtil.sqlNullValue);
    }

    @Test public void testDirectedGraph() {
        final DirectedGraph<String, EdgeImpl<String>> graph =
            new DirectedGraph<String, EdgeImpl<String>>();

        // empty graph has no paths
        assertThat(graph.findAllPaths("C", "Z").toString(), is("[]"));
        // there is one path of length 0 from C to C, even in the empty graph
        assertThat(graph.findAllPaths("C", "C").toString(), is("[[]]"));

        graph.addEdge(new EdgeImpl<String>("A", "B"));
        graph.addEdge(new EdgeImpl<String>("B", "C"));

        // there is one path of length 0 from A to A
        assertThat(graph.findAllPaths("A", "A").toString(), is("[[]]"));
        assertThat(graph.findAllPathsUndirected("A", "A").toString(), is("[[]]"));
        assertThat(graph.findAllPaths("A", "B").toString(), is("[[A-B]]"));
        assertThat(graph.findAllPathsUndirected("A", "B").toString(),
            is("[[<A-B, true>]]"));
        assertThat(graph.findAllPaths("A", "C").toString(), is("[[A-B, B-C]]"));
        assertThat(graph.findAllPathsUndirected("A", "C").toString(),
            is("[[<A-B, true>, <B-C, true>]]"));

        // there are no paths C to A
        assertThat(graph.findAllPaths("C", "A").toString(), is("[]"));

        // undirected, there is a path from C to A
        assertThat(graph.findAllPathsUndirected("C", "A").toString(),
            is("[[<B-C, false>, <A-B, false>]]"));

        // no paths to nodes outside graph
        assertThat(graph.findAllPaths("C", "Z").toString(), is("[]"));
        assertThat(graph.findAllPaths("A", "Z").toString(), is("[]"));

        // add alternative path from A-C
        graph.addEdge(new EdgeImpl<String>("A", "C"));
        assertThat(graph.findAllPaths("A", "C").toString(),
            is("[[A-B, B-C], [A-C]]"));

        graph.addEdge(new EdgeImpl<String>("C", "B"));
        try {
            final List<List<EdgeImpl<String>>> pathList =
                graph.findAllPaths("A", "B");
            fail("expected error, got " + pathList);
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Graph contains cycle: [A-B, B-C, C-B]"));
        }
    }

    @Test public void testLazy() {
        final int[] calls = {0};
        final Lazy<Integer> integerLazy =
            new Lazy<Integer>(
                new Util.Function0<Integer>() {
                    public Integer apply() {
                        return calls[0]++;
                    }
                }
            );
        assertThat(calls[0], is(0));
        assertThat((int) integerLazy.get(), is(0));
        assertThat(calls[0], is(1));
        assertThat((int) integerLazy.get(), is(0));
        assertThat(calls[0], is(1));

        final Lazy<String> nullLazy =
            new Lazy<String>(
                new Util.Function0<String>() {
                    public String apply() {
                        calls[0]++;
                        return null;
                    }
                }
            );
        assertThat(calls[0], is(1));
        assertThat(nullLazy.get(), is((String) null));
        assertThat(calls[0], is(2));
        assertThat(nullLazy.get(), is((String) null));
        assertThat(calls[0], is(2));
    }

    /**
     * Unit test for {@link Util.PropertyList}.
     */
    @Test public void testPropertyList() {
        final Util.PropertyList list = new Util.PropertyList();
        list.put("x", "1");
        list.put("y", "2");
        list.put("x", "3");
        assertThat(list.toString(), is("x=3; y=2"));

        // modifying the clone does not affect the original
        final Util.PropertyList list2 = list.clone();
        assertThat(list.toString(), is("x=3; y=2"));
        list2.put("x", "4");
        assertThat(list2.toString(), is("x=4; y=2"));
        assertThat(list.toString(), is("x=3; y=2"));

        // modifying the original does not affect the clone
        list.remove("y");
        list.put("z", "foo bar");
        assertThat(list2.toString(), is("x=4; y=2"));
        assertThat(list.toString(), is("x=3; z=foo bar"));
    }

    /**
     * Unit test for {@link Util#first}.
     */
    @Test public void testFirst() {
        assertThat(Util.first("x", "y"), is("x"));
        assertThat(Util.first(null, "y"), is("y"));
        assertThat(Util.first(null, null), nullValue());
    }

    /**
     * Unit test for {@link Util#julian(long, long, long)}.
     */
    @Test public void testJulian() {
        assertThat(Util.julian(-4713, 1, 1), is(-327L));
        assertThat(Util.julian(-4800, 3, 1), is(-32044L));
        assertThat(Util.julian(1, 1, 1), is(1721426L));
        assertThat(Util.julian(2007, 1, 14), is(2454115L));
        assertThat(Util.julian(2012, 2, 18), is(2455976L));
    }

    /**
     * Unit test for {@link Util#parseInterval}.
     */
    @Test public void testParseInterval() {
        // no default unit
        assertThat(Util.parseInterval("1s", null), is(Pair.of(1L, TimeUnit.SECONDS)));
        // same default unit as actual
        assertThat(Util.parseInterval("1s", TimeUnit.SECONDS), is(Pair.of(1L, TimeUnit.SECONDS)));
        // different default than actual
        assertThat(Util.parseInterval("1s", TimeUnit.MILLISECONDS), is(Pair.of(1L, TimeUnit.SECONDS)));
        assertThat(Util.parseInterval("2ns", TimeUnit.MICROSECONDS), is(Pair.of(2L, TimeUnit.NANOSECONDS)));
        // now each unit in turn (ns, us, ms, s, h, m, d)
        assertThat(Util.parseInterval("5ns", null), is(Pair.of(5L, TimeUnit.NANOSECONDS)));
        assertThat(Util.parseInterval("3us", null), is(Pair.of(3L, TimeUnit.MICROSECONDS)));
        assertThat(Util.parseInterval("4ms", null), is(Pair.of(4L, TimeUnit.MILLISECONDS)));
        if (!Util.PreJdk16) {
            // JDK1.5 does not have TimeUnit.MINUTES, .HOURS or .DAYS.
            assertThat(Util.parseInterval("5m", null), is(Pair.of(5L, TimeUnit.valueOf("MINUTES"))));
            assertThat(Util.parseInterval("6h", null), is(Pair.of(6L, TimeUnit.valueOf("HOURS"))));
            assertThat(Util.parseInterval("7d", null), is(Pair.of(7L, TimeUnit.valueOf("DAYS"))));
        }
        // negative
        assertThat(Util.parseInterval("-8s", null), is(Pair.of(-8L, TimeUnit.SECONDS)));
        assertThat(Util.parseInterval("3", TimeUnit.MICROSECONDS), is(Pair.of(3L, TimeUnit.MICROSECONDS)));
        try {
            Pair<Long, TimeUnit> x = Util.parseInterval("4", null);
            fail("expected error, got " + x);
        } catch (NumberFormatException e) {
            assertThat(e.getMessage(),
                e.getMessage().startsWith("Invalid time interval '4'. Does "
                + "not contain a time unit."), is(true));
        }
        // fractional part rounded away
        assertThat(Util.parseInterval("1234.567s", TimeUnit.MICROSECONDS), is(Pair.of(1234L, TimeUnit.SECONDS)));
        // Invalid unit means that interval cannot be parsed.
        // (No 'S' is not valid for 's'. See 'man sleep'.)
        try {
            Pair<Long, TimeUnit> x = Util.parseInterval("40S", null);
            fail("expected error, got " + x);
        } catch (NumberFormatException e) {
            assertThat(e.getMessage(), e.getMessage().startsWith(
                "Invalid time interval '40S'. Does not contain a time "
                + "unit."), is(true));
        }
        // Even a space is not allowed.
        try {
            Pair<Long, TimeUnit> x = Util.parseInterval("40 m", null);
            fail("expected error, got " + x);
        } catch (NumberFormatException e) {
            assertThat(e.getMessage(), e.getMessage().startsWith(
                "Invalid time interval '40 m'"), is(true));
        }
        // Two time units.
        try {
            Pair<Long, TimeUnit> x = Util.parseInterval("40sms", null);
            fail("expected error, got " + x);
        } catch (NumberFormatException e) {
            assertThat(e.getMessage(), e.getMessage().startsWith(
                "Invalid time interval '40sms'"), is(true));
        }
        // Null
        try {
            Pair<Long, TimeUnit> x = Util.parseInterval(null, null);
            fail("expected error, got " + x);
        } catch (NullPointerException e) {
            // ok
        }
    }

    /** Unit test for {@link IteratorIterable}. */
    public static void testIterableIterator() {
        final List<String> list = Arrays.asList("a", "b", "c", "d");

        // Full through.
        final List<String> list2 = new ArrayList<String>();
        for (String s : new IteratorIterable<String>(list.iterator())) {
            list2.add(s);
        }
        assertThat(list, is(list2));

        // On empty list.
        final IteratorIterable<Object> iterable =
            new IteratorIterable<Object>(Collections.emptyList().iterator());
        assertThat(iterable.iterator().hasNext(), is(false));
        assertThat(iterable.iterator().hasNext(), is(false));

        // Part way through on iterator 1.
        final Iterable<String> iterable1 =
            new IteratorIterable<String>(list.iterator());
        final Iterator<String> iterator1 = iterable1.iterator();
        assertThat(iterator1.hasNext(), is(true));
        assertThat(iterator1.next(), is("a"));
        assertThat(iterator1.hasNext(), is(true));
        assertThat(iterator1.next(), is("b"));

        // Start another iterator 2.
        final Iterator<String> iterator2 = iterable1.iterator();
        assertThat(iterator2.hasNext(), is(true));
        assertThat(iterator2.next(), is("a"));

        // A bit more on 1.
        assertThat(iterator1.hasNext(), is(true));
        assertThat(iterator1.next(), is("c"));

        // Finish on 2.
        assertThat(iterator2.hasNext(), is(true));
        assertThat(iterator2.next(), is("b"));
        assertThat(iterator2.hasNext(), is(true));
        assertThat(iterator2.next(), is("c"));
        assertThat(iterator2.hasNext(), is(true));
        assertThat(iterator2.next(), is("d"));
        assertThat(iterator2.hasNext(), is(false));

        // Finish on 1.
        assertThat(iterator1.hasNext(), is(true));
        assertThat(iterator1.next(), is("d"));
        assertThat(iterator1.hasNext(), is(false));

        // Start on 3.
        final Iterator<String> iterator3 = iterable1.iterator();
        assertThat(iterator3.hasNext(), is(true));
        assertThat(iterator3.next(), is("a"));
    }

    /**
     * Simple implementation of {@link mondrian.util.DirectedGraph.Edge}.
     */
    static class EdgeImpl<E> implements DirectedGraph.Edge<E> {
        final E from;
        final E to;

        public EdgeImpl(E from, E to) {
            this.from = from;
            this.to = to;
        }

        public E getFrom() {
            return from;
        }

        public E getTo() {
            return to;
        }

        public String toString() {
            return from + "-" + to;
        }
    }

    @Test public void testByteMatcher() throws Exception {
        final ByteMatcher bm = new ByteMatcher(new byte[] {(byte)0x2A});
        final byte[] bytesNotPresent =
            new byte[] {(byte)0x2B, (byte)0x2C};
        final byte[] bytesPresent =
            new byte[] {(byte)0x2B, (byte)0x2A, (byte)0x2C};
        final byte[] bytesPresentLast =
            new byte[] {(byte)0x2B, (byte)0x2C, (byte)0x2A};
        final byte[] bytesPresentFirst =
            new byte[] {(byte)0x2A, (byte)0x2C, (byte)0x2B};
        assertThat(bm.match(bytesNotPresent), is(-1));
        assertThat(bm.match(bytesPresent), is(1));
        assertThat(bm.match(bytesPresentLast), is(2));
        assertThat(bm.match(bytesPresentFirst), is(0));
    }
}

// End UtilTestCase.java
