/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2011-2013 Pentaho and others
// All Rights Reserved.
*/
package mondrian.test;

import mondrian.olap.*;
import mondrian.olap4j.MondrianOlap4jDriver;

import org.olap4j.*;
import org.olap4j.Cell;
import org.olap4j.Position;
import org.olap4j.impl.ArrayMap;
import org.olap4j.mdx.IdentifierNode;
import org.olap4j.mdx.SelectNode;
import org.olap4j.mdx.parser.*;
import org.olap4j.metadata.*;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.Property;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Tests mondrian's olap4j API.
 *
 * <p>Test cases in this test could, in principle, be moved to olap4j's test.
 *
 * @author jhyde
 */
public class Olap4jTest extends FoodMartTestCase {
    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-920">
     * MONDRIAN-920, "olap4j: inconsistent measure's member type"</a>.
     *
     * @throws java.sql.SQLException on error
     */
    @Test public void testSameMemberByVariousMeans() throws SQLException {
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            int n = random.nextInt(7);
            Member member = foo(n);
            String s = "source #" + n;
            assertThat(s, member.getName(), is("Unit Sales"));
            assertThat(s, member.getMemberType(), is(Member.Type.MEASURE));
        }
    }

    private Member foo(int i) throws SQLException {
        final OlapConnection connection =
            getTestContext().getOlap4jConnection();
        final Cube cube;
        final Hierarchy measuresHierarchy;
        final CellSet cellSet;
        switch (i) {
        case 0:
            cellSet = connection.createStatement().executeOlapQuery(
                "select [Measures].[Unit Sales] on 0\n"
                + "from [Sales]");
            return cellSet.getAxes().get(0).getPositions().get(0)
                .getMembers().get(0);

        case 1:
            cellSet =
                connection.createStatement().executeOlapQuery(
                    "select [Measures].Members on 0\n"
                    + "from [Sales]");
            return cellSet.getAxes().get(0).getPositions().get(0)
                .getMembers().get(0);

        case 2:
            cellSet =
                connection.createStatement().executeOlapQuery(
                    "select [Measures].[Measures].Members on 0\n"
                    + "from [Sales]");
            return cellSet.getAxes().get(0).getPositions().get(0)
                .getMembers().get(0);

        case 3:
            cube = connection.getOlapSchema().getCubes().get("Sales");
            measuresHierarchy = cube.getHierarchies().get("Measures");
            final NamedList<Member> rootMembers =
                measuresHierarchy.getRootMembers();
            return rootMembers.get(0);

        case 4:
            cube = connection.getOlapSchema().getCubes().get("Sales");
            measuresHierarchy = cube.getHierarchies().get("Measures");
            final Level measuresLevel = measuresHierarchy.getLevels().get(0);
            final List<Member> levelMembers = measuresLevel.getMembers();
            return levelMembers.get(0);

        case 5:
            cube = connection.getOlapSchema().getCubes().get("Sales");
            measuresHierarchy = cube.getHierarchies().get("Measures");
            return measuresHierarchy.getDefaultMember();

        case 6:
            cube = connection.getOlapSchema().getCubes().get("Sales");
            return
                cube.lookupMember(
                    IdentifierNode.parseIdentifier("[Measures].[Unit Sales]")
                        .getSegmentList());
        default:
            throw new IllegalArgumentException("bad index " + i);
        }
    }

    @Test public void testAnnotation() throws SQLException {
        final OlapConnection connection =
            getTestContext().getOlap4jConnection();
        final CellSet cellSet =
            connection.createStatement().executeOlapQuery(
                "select from [Sales]");
        final CellSetMetaData metaData = cellSet.getMetaData();
        final Cube salesCube = metaData.getCube();
        Annotated annotated = ((OlapWrapper) salesCube).unwrap(Annotated.class);
        final Annotation annotation =
            annotated.getAnnotationMap().get("caption+fr_FR");
        assertThat(annotation.getValue(), is((Object) "Ventes"));

        // the annotation that indicated a localized resource has been removed
        // from the annotation map
        assertThat(annotated.getAnnotationMap().get("caption.fr_FR"), nullValue());
        assertThat(((OlapWrapper) salesCube).unwrap(OlapElement.class)
                    .getLocalized(LocalizedProperty.CAPTION, Locale.FRANCE),
             is("Ventes"));

        final Map<String, Object> map =
            MondrianOlap4jDriver.EXTRA.getAnnotationMap(salesCube);
        assertThat(map.get("caption+fr_FR"), is((Object) "Ventes"));
    }

    @Test public void testFormatString() throws SQLException {
        final OlapConnection connection =
            getTestContext().getOlap4jConnection();
        final CellSet cellSet =
            connection.createStatement().executeOlapQuery(
                "with member [Measures].[Foo] as 1, FORMAT_STRING = Iif(1 < 2, '##.0%', 'foo')\n"
                + "select\n"
                + " [Measures].[Foo] DIMENSION PROPERTIES FORMAT_EXP on 0\n"
                + "from [Sales]");
        final CellSetAxis axis = cellSet.getAxes().get(0);
        final Member member =
            axis.getPositions().get(0).getMembers().get(0);
        Property property = findProperty(axis, "FORMAT_EXP");
        assertThat(property, notNullValue());

        // Note that the expression is returned, unevaluated. You can tell from
        // the parentheses and quotes that it has been un-parsed.
        assertThat(member.getPropertyValue(property),
            is((Object) "IIf((1 < 2), \"##.0%\", \"foo\")"));
    }

    /**
     * Tests that a property that is not a standard olap4j property but is a
     * Mondrian-builtin property (viz, "FORMAT_EXP") is included among a level's
     * properties.
     *
     * @throws SQLException on error
     */
    @Test public void testLevelProperties() throws SQLException {
        final OlapConnection connection =
            getTestContext().getOlap4jConnection();
        final CellSet cellSet =
            connection.createStatement().executeOlapQuery(
                "select [Store].[Store Name].Members on 0\n"
                + "from [Sales]");
        final CellSetAxis axis = cellSet.getAxes().get(0);
        final Member member =
            axis.getPositions().get(0).getMembers().get(0);
        final NamedList<Property> properties =
            member.getLevel().getProperties();
        // UNIQUE_NAME is an olap4j standard property.
        assertThat(properties.get("MEMBER_UNIQUE_NAME"), notNullValue());
        // FORMAT_EXP is a Mondrian built-in but not olap4j standard property.
        assertThat(properties.get("FORMAT_EXP"), notNullValue());
        // [Store Type] is a property of the level.
        assertThat(properties.get("Store Type"), notNullValue());
    }

    private Property findProperty(CellSetAxis axis, String name) {
        for (Property property : axis.getAxisMetaData().getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    @Test public void testCellProperties() throws SQLException {
        final OlapConnection connection =
            getTestContext().getOlap4jConnection();
        final CellSet cellSet =
            connection.createStatement().executeOlapQuery(
                "with member [Customers].[USA].[CA WA] as\n"
                + " Aggregate({[Customers].[USA].[CA], [Customers].[USA].[WA]})\n"
                + "select [Measures].[Unit Sales] on 0,\n"
                + " {[Customers].[USA].[CA], [Customers].[USA].[CA WA]} on 1\n"
                + "from [Sales]\n"
                + "cell properties ACTION_TYPE, DRILLTHROUGH_COUNT");
        final CellSetMetaData metaData = cellSet.getMetaData();
        final Property actionTypeProperty =
            metaData.getCellProperties().get("ACTION_TYPE");
        final Property drillthroughCountProperty =
            metaData.getCellProperties().get("DRILLTHROUGH_COUNT");

        // Cell [0, 0] is drillable
        final Cell cell0 = cellSet.getCell(0);
        final int actionType0 =
            (Integer) cell0.getPropertyValue(actionTypeProperty);
        assertThat(actionType0, is(0x100));
        final int drill0 =
            (Integer) cell0.getPropertyValue(drillthroughCountProperty);
        assertThat(drill0, is(24442));

        // Cell [0, 1] is not drillable
        final Cell cell1 = cellSet.getCell(1);
        final int actionType1 =
            (Integer) cell1.getPropertyValue(actionTypeProperty);
        assertThat(actionType1, is(0x0));
        final int drill1 =
            (Integer) cell1.getPropertyValue(drillthroughCountProperty);
        assertThat(drill1, is(-1));
    }

    /**
     * Same case as
     * {@link mondrian.test.BasicQueryTest#testQueryIterationLimit()}, but this
     * time, check that the OlapException has the required SQLstate.
     *
     * @throws SQLException on error
     */
    @Test public void testLimit() throws SQLException {
        propSaver.set(MondrianProperties.instance().IterationLimit, 11);
        String queryString =
            "With Set [*NATIVE_CJ_SET] as "
            + "'NonEmptyCrossJoin([*BASE_MEMBERS_Dates], [*BASE_MEMBERS_Stores])' "
            + "Set [*BASE_MEMBERS_Dates] as '{[Time].[1997].[Q1], [Time].[1997].[Q2], [Time].[1997].[Q3], [Time].[1997].[Q4]}' "
            + "Set [*GENERATED_MEMBERS_Dates] as 'Generate([*NATIVE_CJ_SET], {[Time].[Time].CurrentMember})' "
            + "Set [*GENERATED_MEMBERS_Measures] as '{[Measures].[*SUMMARY_METRIC_0]}' "
            + "Set [*BASE_MEMBERS_Stores] as '{[Stores].[USA].[CA], [Store].[USA].[WA], [Stores].[USA].[OR]}' "
            + "Set [*GENERATED_MEMBERS_Stores] as 'Generate([*NATIVE_CJ_SET], {[Stores].CurrentMember})' "
            + "Member [Time].[Time].[*SM_CTX_SEL] as 'Aggregate([*GENERATED_MEMBERS_Dates])' "
            + "Member [Measures].[*SUMMARY_METRIC_0] as '[Measures].[Unit Sales]/([Measures].[Unit Sales],[Time].[*SM_CTX_SEL])' "
            + "Member [Time].[Time].[*SUBTOTAL_MEMBER_SEL~SUM] as 'sum([*GENERATED_MEMBERS_Dates])' "
            + "Member [Stores].[*SUBTOTAL_MEMBER_SEL~SUM] as 'sum([*GENERATED_MEMBERS_Stores])' "
            + "select crossjoin({[Time].[*SUBTOTAL_MEMBER_SEL~SUM]}, {[Store].[*SUBTOTAL_MEMBER_SEL~SUM]}) "
            + "on columns from [Sales]";

        final OlapConnection connection =
            getTestContext().getOlap4jConnection();

        try {
            final CellSet cellSet =
                connection.createStatement().executeOlapQuery(queryString);
            fail("expected exception, got " + cellSet);
        } catch (OlapException e) {
            assertThat(e.getSQLState(), is("ResourceLimitExceeded"));
        }
    }

    @Test public void testCloseOnCompletion() throws Exception {
        if (Util.JdbcVersion < 0x0401) {
            // Statement.closeOnCompletion added in JDBC 4.1 / JDK 1.7.
            return;
        }
        final OlapConnection connection =
            getTestContext().getOlap4jConnection();
        for (boolean b : new boolean[] {false, true}) {
            final OlapStatement statement = connection.createStatement();
            final CellSet cellSet = statement.executeOlapQuery(
                "select [Measures].[Unit Sales] on 0\n"
                + "from [Sales]");
            if (b) {
                closeOnCompletion(statement);
            }
            assertThat(isClosed(cellSet), is(false));
            assertThat(isClosed(statement), is(false));
            cellSet.close();
            assertThat(isClosed(cellSet), is(true));
            assertThat(isClosed(statement), is(b));
            statement.close(); // not error to close twice
        }
    }

    /**
     * Calls {@link java.sql.Statement#isClosed()} or
     * {@link java.sql.ResultSet#isClosed()} via reflection.
     *
     * @param statement Statement or result set
     * @return Whether statement or result set is closed
     * @throws Exception on error
     */
    static boolean isClosed(Object statement) throws Exception {
        Method method =
            (statement instanceof Statement
                ? java.sql.Statement.class
                : java.sql.ResultSet.class).getMethod("isClosed");
        return (Boolean) method.invoke(statement);
    }

    /**
     * Calls {@link java.sql.Statement#closeOnCompletion()} via reflection.
     *
     * @param statement Statement or result set
     * @throws Exception on error
     */
    static void closeOnCompletion(Object statement) throws Exception {
        Method method = java.sql.Statement.class.getMethod("closeOnCompletion");
        method.invoke(statement);
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-1123">
     * MONDRIAN-1123, "ClassCastException for calculated members that are not
     * part of the measures dimension"</a>.
     *
     * @throws java.sql.SQLException on error
     */
    @Test public void testCalcMemberInCube() throws SQLException {
        final OlapConnection testContext =
            TestContext.instance().legacy().createSubstitutingCube(
                "Sales",
                null,
                "<CalculatedMember name='H1 1997' formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' dimension='Time' />")
            .getOlap4jConnection();
        final Cube cube = testContext.getOlapSchema().getCubes().get("Sales");
        final List<Measure> measureList = cube.getMeasures();
        StringBuilder buf = new StringBuilder();
        for (Measure measure : measureList) {
            buf.append(measure.getName()).append(";");
        }
        // Calc member in the Time dimension does not appear in the list.
        // Never did, as far as I can tell.
        assertThat(buf.toString(),
            is("Unit Sales;Store Cost;Store Sales;Sales Count;Customer Count;"
            + "Promotion Sales;Profit;Profit last Period;Profit Growth;"));

        final CellSet cellSet = testContext.createStatement().executeOlapQuery(
            "select AddCalculatedMembers([Time].[Time].Members) on 0 from [Sales]");
        int n = 0, n2 = 0;
        for (Position position : cellSet.getAxes().get(0).getPositions()) {
            if (position.getMembers().get(0).getName().equals("H1 1997")) {
                ++n;
            }
            ++n2;
        }
        assertThat(n, is(1));
        assertThat(n2, is(35));

        final CellSet cellSet2 = testContext.createStatement().executeOlapQuery(
            "select Filter(\n"
            + " AddCalculatedMembers([Time].[Time].Members),\n"
            + " [Time].[Time].CurrentMember.Properties('MEMBER_TYPE') = 4) on 0\n"
            + "from [Sales]");
        n = 0;
        n2 = 0;
        for (Position position : cellSet2.getAxes().get(0).getPositions()) {
            if (position.getMembers().get(0).getName().equals("H1 1997")) {
                ++n;
            }
            ++n2;
        }
        assertThat(n, is(1));
        assertThat(n2, is(1));
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-1124">
     * MONDRIAN-1124, "Unique name of hierarchy should always have 2 parts, even
     * if dimension &amp; hierarchy have same name"</a>.
     *
     * @throws java.sql.SQLException on error
     */
    @Test public void testUniqueName() throws SQLException {
        // Things are straightforward if dimension, hierarchy, level have
        // distinct names. This worked even before MONDRIAN-1124 was fixed.
        CellSet x =
            getTestContext().getOlap4jConnection().createStatement()
                .executeOlapQuery(
                    "select [Store].[Stores] on 0\n"
                    + "from [Sales]");
        Member member =
            x.getAxes().get(0).getPositions().get(0).getMembers().get(0);
        assertThat(member.getDimension().getUniqueName(), is("[Store]"));
        assertThat(member.getHierarchy().getUniqueName(), is("[Store].[Stores]"));
        assertThat(member.getLevel().getUniqueName(),
            is("[Store].[Stores].[(All)]"));
        assertThat(member.getUniqueName(), is("[Store].[Stores].[All Stores]"));

        CellSet y =
            getTestContext()
                .createSubstitutingCube(
                    "Sales",
                    "<Dimension name='Store Type' key='Store Id' table='store'>\n"
                    + "  <Attributes>\n"
                    + "    <Attribute name='Store Id' keyColumn='store_id'/>\n"
                    + "    <Attribute name='Store Type' table='store' keyColumn='store_type' hasHierarchy='false'/>\n"
                    + "  </Attributes>\n"
                    + "  <Hierarchies>\n"
                    + "    <Hierarchy name='Store Type'>\n"
                    + "      <Level attribute='Store Type'/>\n"
                    + "    </Hierarchy>\n"
                    + "  </Hierarchies>\n"
                    + "</Dimension>\n",
                    null,
                    null,
                    null,
                    ArrayMap.of(
                        "Sales",
                        "<ForeignKeyLink dimension='Store Type' "
                        + "foreignKeyColumn='store_id'/>"))
                .getOlap4jConnection().createStatement()
                .executeOlapQuery(
                    "select [Store Type].[Store Type] on 0\n"
                    + "from [Sales]");
        member =
            y.getAxes().get(0).getPositions().get(0).getMembers().get(0);
        assertThat(member.getDimension().getUniqueName(), is("[Store Type]"));
        assertThat(member.getHierarchy().getUniqueName(),
            is("[Store Type].[Store Type]"));
        assertThat(member.getLevel().getUniqueName(),
            is("[Store Type].[Store Type].[(All)]"));
        assertThat(member.getUniqueName(),
            is("[Store Type].[Store Type].[All Store Types]"));
    }

    @Test public void testCellSetGetCellPositionArray() throws SQLException {
        // Create a cell set with 1 column and 2 rows.
        // Only coordinates (0, 0) and (0, 1) are valid.
        CellSet x =
            getTestContext().getOlap4jConnection().createStatement()
                .executeOlapQuery(
                    "select {[Customer].[Gender].[M]} on 0,\n"
                    + " {[Customer].[Marital Status].[M],\n"
                    + "  [Customer].[Marital Status].[S]} on 1\n"
                    + "from [Sales]");
        Cell cell;

        // column=0, row=1 via getCell(List<Integer>)
        cell = x.getCell(Arrays.asList(0, 1));
        final String xxx = "68,755";
        assertThat(cell.getFormattedValue(), is(xxx));

        // column=1, row=0 out of range
        try {
            cell = x.getCell(Arrays.asList(1, 0));
            fail("expected exception, got " + cell);
        } catch (IndexOutOfBoundsException e) {
            assertThat(e.getMessage(),
                is("Cell coordinates (1, 0) fall outside CellSet bounds (1, 2)"));
        }

        // ordinal=1 via getCell(int)
        cell = x.getCell(1);
        assertThat(cell.getFormattedValue(), is(xxx));

        // column=0, row=1 via getCell(Position...)
        final Position col0 = x.getAxes().get(0).getPositions().get(0);
        final Position row1 = x.getAxes().get(1).getPositions().get(1);
        final Position row0 = x.getAxes().get(1).getPositions().get(0);
        cell = x.getCell(col0, row1);
        assertThat(cell.getFormattedValue(), is(xxx));

        // row=1, column=0 via getCell(Position...).
        // This is OK, even though positions are not in axis order.
        // Result same as previous.
        cell = x.getCell(row1, col0);
        assertThat(cell.getFormattedValue(), is(xxx));

        try {
            cell = x.getCell(col0);
            fail("expected exception, got " + cell);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),
                is("Coordinates have different dimension (1) than axes (2)"));
        }

        try {
            cell = x.getCell(col0, row1, col0);
            fail("expected exception, got " + cell);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),
                is("Coordinates have different dimension (3) than axes (2)"));
        }

        try {
            cell = x.getCell(row0, row1);
            fail("expected exception, got " + cell);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),
                is("Coordinates contain axis 1 more than once"));
        }
    }

    /**
     * Test case for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1204">MONDRIAN-1204,
     * "Olap4j's method toOlap4j throws NPE if we have a function"</a>.
     */
    @Test public void testBugMondrian1204() throws SQLException {
        final OlapConnection connection =
            TestContext.instance().getOlap4jConnection();
        final String mdx =
            "SELECT\n"
            + "NON EMPTY {Hierarchize({[Measures].[Customer Count]})} ON COLUMNS,\n"
            + "CurrentDateMember([Time].[Year], \"\"\"[Time].[Year].[1997]\"\"\") ON ROWS\n"
            + "FROM [Sales 2]";
        try {
            final MdxParserFactory parserFactory =
                connection.getParserFactory();
            MdxParser mdxParser =
                parserFactory.createMdxParser(connection);
            MdxValidator mdxValidator =
                parserFactory.createMdxValidator(connection);

            SelectNode select = mdxParser.parseSelect(mdx);
            SelectNode validatedSelect = mdxValidator.validateSelect(select);
            Util.discard(validatedSelect);
        } finally {
            Util.close(null, null, connection);
        }
    }

    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1353">MONDRIAN-1353</a>
     *
     * <p>An empty stack exception was thrown from the olap4j API if
     * the hierarchy didn't have a all member and the default member
     * was not explicitly set.
     */
    @Test public void testMondrian1353() throws Exception {
        final TestContext testContext = TestContext.instance().legacy().create(
            null,
            "<Cube name=\"Mondrian1353\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <Dimension name=\"Cities\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"customer_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"City\" column=\"city\" uniqueMembers=\"false\"/> \n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);

        final Member defaultMember =
            testContext.getOlap4jConnection()
                .getOlapSchema()
                .getCubes().get("Mondrian1353")
                .getDimensions().get("Cities")
                .getDefaultHierarchy().getDefaultMember();

        assertThat(defaultMember, notNullValue());
        assertThat(defaultMember.getName(), is("Acapulco"));
    }

    /**
     * Same as {@link SchemaTest#testMondrian1390()} but this time
     * with olap4j.
     */
    @Test public void testMondrian1390() throws Exception {
        final List<Member> members =
            getTestContext().getOlap4jConnection()
                .getOlapSchema()
                .getCubes().get("Sales")
                .getDimensions().get("Store")
                .getHierarchies().get("Store Size in SQFT")
                .getLevels().get("Store Sqft")
                    .getMembers();
        assertThat(members.toString(),
            is("[[Store].[Store Size in SQFT].[#null], "
            + "[Store].[Store Size in SQFT].[20319], "
            + "[Store].[Store Size in SQFT].[21215], "
            + "[Store].[Store Size in SQFT].[22478], "
            + "[Store].[Store Size in SQFT].[23112], "
            + "[Store].[Store Size in SQFT].[23593], "
            + "[Store].[Store Size in SQFT].[23598], "
            + "[Store].[Store Size in SQFT].[23688], "
            + "[Store].[Store Size in SQFT].[23759], "
            + "[Store].[Store Size in SQFT].[24597], "
            + "[Store].[Store Size in SQFT].[27694], "
            + "[Store].[Store Size in SQFT].[28206], "
            + "[Store].[Store Size in SQFT].[30268], "
            + "[Store].[Store Size in SQFT].[30584], "
            + "[Store].[Store Size in SQFT].[30797], "
            + "[Store].[Store Size in SQFT].[33858], "
            + "[Store].[Store Size in SQFT].[34452], "
            + "[Store].[Store Size in SQFT].[34791], "
            + "[Store].[Store Size in SQFT].[36509], "
            + "[Store].[Store Size in SQFT].[38382], "
            + "[Store].[Store Size in SQFT].[39696]]"));
    }
}

// End Olap4jTest.java
