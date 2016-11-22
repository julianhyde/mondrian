/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.test;

import mondrian.olap.*;
import mondrian.rolap.*;
import mondrian.util.*;

import org.olap4j.*;
import org.olap4j.mdx.*;
import org.olap4j.metadata.*;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Schema;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.sql.SQLException;
import java.util.*;

/**
 * Test suite for internalization and localization.
 *
 * @see mondrian.util.FormatTest
 *
 * @author jhyde
 * @since September 22, 2005
 */
public class I18nTest extends FoodMartTestCase {
    public static final char Euro = '\u20AC';
    public static final char Nbsp = '\u00A0';
    public static final char EA = '\u00e9'; // e acute
    public static final char UC = '\u00FB'; // u circumflex

    @Test public void testFormat() {
        // Make sure Util is loaded, so that the LocaleFormatFactory gets
        // registered.
        Util.discard(Util.nl);

        Locale spanish = new Locale("es", "ES");
        Locale german = new Locale("de", "DE");

        // Thousands and decimal separators are different in Spain
        Format numFormat = new Format("#,000.00", spanish);
        assertThat(numFormat.format(new Double(123456.789)), is("123.456,79"));

        // Currency too
        Format currencyFormat = new Format("Currency", spanish);
        assertThat(currencyFormat.format(new Double(1234567.789)),
            is("1.234.567,79 " + Euro));

        // Dates
        Format dateFormat = new Format("Medium Date", spanish);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2005);
        calendar.set(Calendar.MONTH, 0); // January, 0-based
        calendar.set(Calendar.DATE, 22);
        java.util.Date date = calendar.getTime();
        assertThat(dateFormat.format(date), is("22-ene-05"));

        // Dates in German
        dateFormat = new Format("Long Date", german);
        assertThat(dateFormat.format(date), is("Samstag, Januar 22, 2005"));
    }

    @Test public void testAutoFrench() {
        // Create a connection in French.
        String localeName = "fr_FR";
        String resultString = "12" + Nbsp + "345,67";
        assertFormatNumber(localeName, resultString);
    }

    @Test public void testAutoSpanish() {
        // Format a number in (Peninsular) spanish.
        assertFormatNumber("es", "12.345,67");
    }

    @Test public void testAutoMexican() {
        // Format a number in Mexican spanish.
        assertFormatNumber("es_MX", "12,345.67");
    }

    private void assertFormatNumber(String localeName, String resultString) {
        final Util.PropertyList properties =
            TestContext.instance().getConnectionProperties().clone();
        properties.put(RolapConnectionProperties.Locale.name(), localeName);
        Connection connection =
            DriverManager.getConnection(properties, null);
        Query query = connection.parseQuery(
            "WITH MEMBER [Measures].[Foo] AS ' 12345.67 ',\n"
            + " FORMAT_STRING='#,###.00'\n"
            + "SELECT {[Measures].[Foo]} ON COLUMNS\n"
            + "FROM [Sales]");
        Result result = connection.execute(query);
        String actual = TestContext.toString(result);
        TestContext.assertEqualsVerbose(
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Foo]}\n"
            + "Row #0: " + resultString + "\n",
            actual);
    }

    /** Unit test for captions and descriptions defined using annotations. */
    @Test public void testSimple() throws SQLException {
        final OlapConnection olapConnection =
            getTestContext()
                .insertCube(
                    "<Localization>\n"
                    + "  <Locales>\n"
                    + "    <Locale locale='en'/>\n"
                    + "    <Locale locale='fr'/>\n"
                    + "    <Locale locale='fr-CA'/>\n"
                    + "    <Locale locale='de-DE'/>\n"
                    + "  </Locales>\n"
                    + "</Localization>\n")
                .getOlap4jConnection();

        final Schema olapSchema = olapConnection.getOlapSchema();

        final Set<String> localeNames = new TreeSet<String>();
        for (Locale locale : olapSchema.getSupportedLocales()) {
            localeNames.add(locale.toString());
        }
        assertThat(localeNames,
            is((Set<String>) new HashSet<String>(
                Arrays.asList("en", "fr", "fr_CA", "de_DE"))));

        olapConnection.setLocale(Locale.US);
        final Cube salesCubeUs = olapSchema.getCubes().get("Sales");
        assertThat(salesCubeUs.getCaption(), is("Sales"));

        // Switch locales. Note that we have to re-read metadata from the
        // root (getOlapSchema()).
        olapConnection.setLocale(Locale.GERMANY);
        final Cube salesCubeGerman = olapSchema.getCubes().get("Sales");
        assertThat(salesCubeGerman.getCaption(), is("Verkaufen"));
        assertThat(salesCubeGerman.getDescription(), is("Cube Verkaufen"));

        olapConnection.setLocale(Locale.FRANCE);
        final Cube salesCubeFrance = olapSchema.getCubes().get("Sales");
        assertThat(salesCubeFrance.getCaption(), is("Ventes"));
        assertThat(salesCubeFrance.getDescription(), is("Cube des ventes"));

        // According to the olap4j spec,
        // behavior is undefined (e.g. the US sales cube might be invalid).
        // In the mondrian-olap4j driver, the cube object is the same under
        // all locales, and switches based on the connection's locale.
        assertThat(salesCubeUs.getCaption(), is("Ventes"));

        // Reset locale.
        olapConnection.setLocale(Locale.US);
    }

    /** Unit test for captions and descriptions loaded from resource file. */
    @Test public void testFileMissing() throws SQLException {
        getTestContext()
            .insertCube(
                "<Localization>\n"
                + "  <Locales>\n"
                + "    <Locale locale='en-US'/>\n"
                + "    <Locale locale='fr'/>\n"
                + "    <Locale locale='fr-CA'/>\n"
                + "    <Locale locale='de-DE'/>\n"
                + "  </Locales>\n"
                + "  <Translations>\n"
                + "    <Translation path='../target/test-classes/mondrian/test/I18nTest_${locale}.properties'/>\n"
                + "  </Translations>\n"
                + "</Localization>\n")
            .assertErrorList().containsError(
            "(?s).*Error reading resource file.*",
            "<Translation path='../target/test-classes/mondrian/test/I18nTest_${locale}.properties'/>");
    }

    /** Unit test for captions and descriptions loaded from resource file. */
    @Test public void testFromFile() throws SQLException {
        final OlapConnection olapConnection =
            getTestContext()
                .insertCube(
                    "<Localization>\n"
                    + "  <Locales>\n"
                    + "    <Locale locale='en-US'/>\n"
                    + "    <Locale locale='fr'/>\n"
                    + "    <Locale locale='fr-CA'/>\n"
                    + "  </Locales>\n"
                    + "  <Translations>\n"
                    + "    <Translation path='../target/test-classes/mondrian/test/I18nTest_${locale}.properties'/>\n"
                    + "  </Translations>\n"
                    + "</Localization>\n")
                .getOlap4jConnection();

        olapConnection.setLocale(Locale.US);

        final Schema olapSchema = olapConnection.getOlapSchema();

        final Cube salesCubeUs = olapSchema.getCubes().get("Sales");

        // Resource from file overrides resource from annotation. (Is this what
        // we want?)
        assertThat(salesCubeUs.getCaption(), is("Caption of Sales cube in en-US"));

        // Switch locales. Note that we have to re-read metadata from the
        // root (getOlapSchema()).
        olapConnection.setLocale(Locale.GERMANY);
        final Cube salesCubeGerman =
            olapSchema.getCubes().get("Sales");
        assertThat(salesCubeGerman.getCaption(), is("Verkaufen"));
        assertThat(salesCubeGerman.getDescription(), is("Cube Verkaufen"));

        olapConnection.setLocale(Locale.FRANCE);
        final Cube salesCubeFrance =
            olapSchema.getCubes().get("Sales");
        assertThat(salesCubeFrance.getCaption(), is("Ventes"));
        assertThat(salesCubeFrance.getDescription(), is("Cube des ventes"));

        // According to the olap4j spec,
        // behavior is undefined (e.g. the US sales cube might be invalid).
        // In the mondrian-olap4j driver, the cube object is the same under
        // all locales, and switches based on the connection's locale.
        assertThat(salesCubeUs.getCaption(), is("Ventes"));


        // Now the resources coming from files.

        olapConnection.setLocale(Locale.FRANCE);

        // olap4j Schema class does not have i18n APIs yet. So get resources
        // through the back door.
        assertThat(((MetadataElement) olapSchema).getCaption(),
            is("Caption of Sales schema in fr"));
        assertThat(((MetadataElement) olapSchema).getDescription(),
            is("Description of Sales schema in fr"));

        olapConnection.setLocale(Locale.CANADA_FRENCH);

        // Description is overridden in fr-CA, but not caption.
        assertThat(((MetadataElement) olapSchema).getCaption(),
            is("Caption of Sales schema in fr"));
        assertThat(((MetadataElement) olapSchema).getDescription(),
            is("Description of Sales schema in fr-CA"));

        assertThat(olapSchema.getCubes().get("Sales")
            .getDimensions().get("Measures")
            .getHierarchies().get(0)
            .getRootMembers().get("Unit Sales")
            .getCaption(), is("Caption of Unit Sales measure in fr"));

        assertThat(olapSchema.getCubes().get("Sales")
            .lookupMember(parse("[Measures].[Profit]"))
            .getCaption(), is("Profit"));

        assertThat(olapSchema.getSharedDimensions().get("Time").getCaption(),
            is("Caption of Time shared dimension in fr"));

        assertThat(olapSchema.getSharedDimensions().get("Time").getHierarchies()
            .get("Weekly").getCaption(),
            is("Caption of shared Weekly hierarchy in fr"));

        assertThat(olapSchema.getSharedDimensions().get("Time").getHierarchies()
            .get("Weekly").getLevels().get("Year").getCaption(),
            is("Caption of shared Year level in fr"));

        assertThat(olapSchema.getCubes().get("Sales").getDimensions().get("Time")
            .getCaption(),
            is("Caption of Time dimension in Sales cube in fr"));

        assertThat(olapSchema.getCubes().get("Sales").getDimensions().get("Time")
            .getHierarchies().get("Weekly").getCaption(),
            is("Caption of Weekly hierarchy in Sales cube in fr"));

        assertThat(olapSchema.getCubes().get("Sales").getDimensions().get("Time")
            .getHierarchies().get("Weekly").getLevels().get("Year")
            .getCaption(), is("Caption of shared Year level in fr"));

        // The Month level does not have a caption, so it inherits the caption
        // from the Month attribute
        assertThat(olapSchema.getCubes().get("Sales").getDimensions().get("Time")
            .getHierarchies().get("Time").getLevels().get("Month")
            .getCaption(), is("Month"));

        try {
            assertThat(olapSchema.getCubes().get("Sales").getSets()
                .get("Top Sellers").getCaption(),
                is("Caption of Top Sellers named set in Sales cube in fr"));
        } catch (NullPointerException e) {
            // TODO: fix me
        }

        // level 1 member
        assertThat(olapSchema.getCubes().get("Sales")
            .lookupMember(parse("[Store].[Stores].[USA]"))
            .getCaption(), is("Caption of member USA in fr"));

        // level 2 member
        assertThat(olapSchema.getCubes().get("Sales")
            .lookupMember(parse("[Store].[Stores].[USA].[CA]"))
            .getCaption(), is("Caption of member CA in fr"));

        // level 2 member, resource defined against shared dimension
        assertThat(olapSchema.getCubes().get("Sales")
            .lookupMember(parse("[Store].[Stores].[USA].[OR]"))
            .getCaption(), is("Caption of shared member OR in fr"));

        // look for equivalent member (defined from same shared dimension) in
        // a different cube. should not be found
        assertThat(olapSchema.getCubes().get("HR")
            .lookupMember(parse("[Store].[Stores].[USA].[CA]"))
            .getCaption(), is("CA"));

        // same resource as previous, equivalent member from another cube
        assertThat(olapSchema.getCubes().get("HR")
            .lookupMember(parse("[Store].[Stores].[USA].[OR]"))
            .getCaption(), is("Caption of shared member OR in fr"));

        // all member
        assertThat(olapSchema.getCubes().get("Sales")
            .lookupMember(parse("[Store].[Stores].[All Stores]"))
            .getCaption(), is("Caption of member All Stores in fr"));

        // all member of attribute hierarchy
        assertThat(olapSchema.getCubes().get("Sales")
            .lookupMember(parse("[Customer].[Gender].[All Gender]"))
            .getCaption(), is("Caption of member All Gender in fr"));

        // level 1 member of attribute hierarchy
        assertThat(olapSchema.getCubes().get("Sales")
            .lookupMember(parse("[Customer].[Gender].[F]"))
            .getCaption(), is("Caption of member F in fr"));

        // TODO: test named set in schema, e.g. "[Best Customers].set.caption"
        Util.discard(Bug.olap4jUpgrade("bug-31") && Bug.BugOlap4j31Fixed);

        // TODO: test attribute hierarchy (resource coming from attribute)

        // TODO: test level in attribute hierarchy (resource coming from
        // attribute)

        // TODO: test calculated member in non-Measures dimension

        // TODO: test property

        // Reset locale.
        olapConnection.setLocale(Locale.US);
    }

    private static List<IdentifierSegment> parse(String x) {
        return IdentifierNode.parseIdentifier(x)
            .getSegmentList();
    }
}

// End I18nTest.java
