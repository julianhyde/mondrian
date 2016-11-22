/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2012 Pentaho and others
// All Rights Reserved.
*/
package mondrian.test;

import mondrian.olap.*;

import org.junit.Test;
import static mondrian.test.TestTemp.*;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests intrinsic member and cell properties as specified in OLE DB for OLAP
 * specification.
 *
 * @author anikitin
 * @since 5 July, 2005
 */
public class PropertiesTest extends FoodMartTestCase {
    /**
     * Tests existence and values of mandatory member properties.
     */
    @Test public void testMandatoryMemberProperties() {
        Cube salesCube = getConnection().getSchema().lookupCube("Sales", true);
        SchemaReader scr = salesCube.getSchemaReader(null).withLocus();
        Member member =
            scr.getMemberByUniqueName(
                Id.Segment.toList("Customers", "All Customers", "USA", "CA"),
                true);
        final boolean caseSensitive =
            MondrianProperties.instance().CaseSensitive.get();

        String stringPropValue;
        Integer intPropValue;

        // I'm not sure this property has to store the same value
        // getConnection().getCatalogName() returns.

        // todo:
//        stringPropValue = (String)member.getPropertyValue("CATALOG_NAME");
//        assertEquals(getConnection().getCatalogName(), stringPropValue);

        stringPropValue = (String)member.getPropertyValue("SCHEMA_NAME");
        assertThat(stringPropValue, is(getConnection().getSchema().getName()));

        // todo:
//        stringPropValue = (String)member.getPropertyValue("CUBE_NAME");
//        assertEquals(salesCube.getName(), stringPropValue);

        stringPropValue =
            (String)member.getPropertyValue("DIMENSION_UNIQUE_NAME");
        assertThat(stringPropValue, is(member.getDimension().getUniqueName()));

        // Case sensitivity.
        stringPropValue = (String)member.getPropertyValue(
            "dimension_unique_name", caseSensitive);
        if (caseSensitive) {
            assertThat(stringPropValue, nullValue());
        } else {
            assertThat(stringPropValue, is(member.getDimension().getUniqueName()));
        }

        // Non-existent property.
        stringPropValue =
            (String)member.getPropertyValue("DIMENSION_UNIQUE_NAME_XXXX");
        assertThat(stringPropValue, nullValue());

        // Leading spaces.
        stringPropValue =
            (String)member.getPropertyValue(" DIMENSION_UNIQUE_NAME");
        assertThat(stringPropValue, nullValue());

        // Trailing spaces.
        stringPropValue =
            (String)member.getPropertyValue("DIMENSION_UNIQUE_NAME  ");
        assertThat(stringPropValue, nullValue());

        stringPropValue =
            (String)member.getPropertyValue("HIERARCHY_UNIQUE_NAME");
        assertThat(stringPropValue, is(member.getHierarchy().getUniqueName()));

        // This property works in Mondrian 1.1.5 (due to XMLA support)
        stringPropValue = (String)member.getPropertyValue("LEVEL_UNIQUE_NAME");
        assertThat(stringPropValue, is(member.getLevel().getUniqueName()));

        // This property works in Mondrian 1.1.5 (due to XMLA support)
        intPropValue = (Integer)member.getPropertyValue("LEVEL_NUMBER");
        assertThat(intPropValue, is(Integer.valueOf(member.getLevel().getDepth())));

        // This property works in Mondrian 1.1.5 (due to XMLA support)
        stringPropValue = (String)member.getPropertyValue("MEMBER_UNIQUE_NAME");
        assertThat(stringPropValue, is(member.getUniqueName()));

        stringPropValue = (String)member.getPropertyValue("MEMBER_NAME");
        assertThat(stringPropValue, is(member.getName()));

        intPropValue = (Integer)member.getPropertyValue("MEMBER_TYPE");
        assertThat(intPropValue, is(Integer.valueOf(member.getMemberType().ordinal())));

        stringPropValue = (String)member.getPropertyValue("MEMBER_GUID");
        assertThat(stringPropValue, nullValue());

        // This property works in Mondrian 1.1.5 (due to XMLA support)
        stringPropValue = (String)member.getPropertyValue("MEMBER_CAPTION");
        assertThat(stringPropValue, is(member.getCaption()));

        stringPropValue = (String)member.getPropertyValue("CAPTION");
        assertThat(stringPropValue, is(member.getCaption()));

        // It's worth checking case-sensitivity for CAPTION because it is a
        // synonym, not a true property.
        stringPropValue = (String) member.getPropertyValue(
            "caption", caseSensitive);
        if (caseSensitive) {
            assertThat(stringPropValue, nullValue());
        } else {
            assertThat(stringPropValue, is(member.getCaption()));
        }

        intPropValue = (Integer)member.getPropertyValue("MEMBER_ORDINAL");
        assertThat(intPropValue, is(Integer.valueOf(member.getOrdinal())));

        if (false) {
            intPropValue =
                (Integer)member.getPropertyValue("CHILDREN_CARDINALITY");
            assertThat(intPropValue, is(Integer.valueOf(scr.getMemberChildren(member).size())));
        }

        intPropValue = (Integer)member.getPropertyValue("PARENT_LEVEL");
        assertThat(intPropValue, is(Integer.valueOf(member.getParentMember().getLevel().getDepth())));

        stringPropValue = (String)member.getPropertyValue("PARENT_UNIQUE_NAME");
        assertThat(stringPropValue, is(member.getParentUniqueName()));

        intPropValue = (Integer)member.getPropertyValue("PARENT_COUNT");
        assertThat(intPropValue, is(Integer.valueOf(1)));

        stringPropValue = (String)member.getPropertyValue("DESCRIPTION");
        assertThat(stringPropValue, is(member.getDescription()));

        // Case sensitivity.
        stringPropValue =
            (String)member.getPropertyValue("desCription", caseSensitive);
        if (caseSensitive) {
            assertThat(stringPropValue, nullValue());
        } else {
            assertThat(stringPropValue, is(member.getDescription()));
        }
    }

    @Test public void testGetChildCardinalityPropertyValue() {
        Cube salesCube = getConnection().getSchema().lookupCube("Sales", true);
        SchemaReader scr = salesCube.getSchemaReader(null).withLocus();
        Member memberForCardinalityTest =
            scr.getMemberByUniqueName(
                Id.Segment.toList("Marital Status", "All Marital Status"),
                true);
        Integer intPropValue =
            (Integer) memberForCardinalityTest.getPropertyValue(
                "CHILDREN_CARDINALITY");
        assertThat(intPropValue, is(Integer.valueOf(111)));
    }

    /**
     * Tests the ability of MDX parser to pass requested member properties
     * to Result object.
     */
    @Test public void testPropertiesMDX() {
        Result result = executeQuery(
            "SELECT {[Customers].[All Customers].[USA].[CA]} DIMENSION PROPERTIES \n"
            + " CATALOG_NAME, SCHEMA_NAME, CUBE_NAME, DIMENSION_UNIQUE_NAME, \n"
            + " HIERARCHY_UNIQUE_NAME, LEVEL_UNIQUE_NAME, LEVEL_NUMBER, MEMBER_UNIQUE_NAME, \n"
            + " MEMBER_NAME, MEMBER_TYPE, MEMBER_GUID, MEMBER_CAPTION, MEMBER_ORDINAL, CHILDREN_CARDINALITY,\n"
            + " PARENT_LEVEL, PARENT_UNIQUE_NAME, PARENT_COUNT, DESCRIPTION ON COLUMNS\n"
            + "FROM [Sales]");
        QueryAxis[] axes = result.getQuery().getAxes();
        Id[] axesProperties = axes[0].getDimensionProperties();
        String[] props = {
            "CATALOG_NAME",
            "SCHEMA_NAME",
            "CUBE_NAME",
            "DIMENSION_UNIQUE_NAME",
            "HIERARCHY_UNIQUE_NAME",
            "LEVEL_UNIQUE_NAME",
            "LEVEL_NUMBER",
            "MEMBER_UNIQUE_NAME",
            "MEMBER_NAME",
            "MEMBER_TYPE",
            "MEMBER_GUID",
            "MEMBER_CAPTION",
            "MEMBER_ORDINAL",
            "CHILDREN_CARDINALITY",
            "PARENT_LEVEL",
            "PARENT_UNIQUE_NAME",
            "PARENT_COUNT",
            "DESCRIPTION"
        };

        assertThat(props.length, is(axesProperties.length));
        int i = 0;
        for (String prop : props) {
            assertThat(axesProperties[i++].toString(), is(prop));
        }
    }

    /**
     * Tests the ability to project non-standard member properties.
     */
    @Test public void testMemberProperties() {
        Result result = executeQuery(
            "SELECT {[Stores].Children} DIMENSION PROPERTIES\n"
            + " CATALOG_NAME, PARENT_UNIQUE_NAME, [Store Type], FORMAT_EXP\n"
            + " ON COLUMNS\n"
            + "FROM [Sales]");
        QueryAxis[] axes = result.getQuery().getAxes();
        Id[] axesProperties = axes[0].getDimensionProperties();

        assertThat(axesProperties.length, is(4));
    }

    /**
     * Tests the ability to project non-standard member properties.
     */
    @Test public void testMemberPropertiesBad() {
        Result result = executeQuery(
            "SELECT {[Stores].Children} DIMENSION PROPERTIES\n"
            + " CATALOG_NAME, PARENT_UNIQUE_NAME, [Store Type], BAD\n"
            + " ON COLUMNS\n"
            + "FROM [Sales]");
        QueryAxis[] axes = result.getQuery().getAxes();
        Id[] axesProperties = axes[0].getDimensionProperties();

        assertThat(axesProperties.length, is(4));
    }

    @Test public void testMandatoryCellProperties() {
        Connection connection = getConnection();
        Query salesCube = connection.parseQuery(
            "select \n"
            + " {[Measures].[Store Sales], [Measures].[Unit Sales]} on columns, \n"
            + " {[Gender].members} on rows \n"
            + "from [Sales]");
        Result result = connection.execute(salesCube);
        int x = 1;
        int y = 2;
        Cell cell = result.getCell(new int[] {x, y});

        assertThat(cell.getPropertyValue("BACK_COLOR"), nullValue());
        assertThat(cell.getPropertyValue("CELL_EVALUATION_LIST"), nullValue());
        assertThat(cell.getPropertyValue("CELL_ORDINAL"), is((Object) (y * 2
                                                                       + x)));
        assertThat(cell.getPropertyValue("FORE_COLOR"), nullValue());
        assertThat(cell.getPropertyValue("FONT_NAME"), nullValue());
        assertThat(cell.getPropertyValue("FONT_SIZE"), nullValue());
        assertThat(cell.getPropertyValue("FONT_FLAGS"), is((Object) 0));
        assertThat(cell.getPropertyValue("FORMAT_STRING"), is((Object) "Standard"));
        // FORMAT is a synonym for FORMAT_STRING
        assertThat(cell.getPropertyValue("FORMAT"), is((Object) "Standard"));
        assertThat(cell.getPropertyValue("FORMATTED_VALUE"), is((Object) "135,215"));
        assertThat(cell.getPropertyValue("NON_EMPTY_BEHAVIOR"), nullValue());
        assertThat(cell.getPropertyValue("SOLVE_ORDER"), is((Object) 0));
        assertThat(((Number) cell.getPropertyValue("VALUE")).doubleValue(),
            range(135215.0, 0.1));

        // Case sensitivity.
        if (MondrianProperties.instance().CaseSensitive.get()) {
            assertThat(cell.getPropertyValue("cell_ordinal"), nullValue());
            assertThat(cell.getPropertyValue("font_flags"), nullValue());
            assertThat(cell.getPropertyValue("format_string"), nullValue());
            assertThat(cell.getPropertyValue("format"), nullValue());
            assertThat(cell.getPropertyValue("formatted_value"), nullValue());
            assertThat(cell.getPropertyValue("solve_order"), nullValue());
            assertThat(cell.getPropertyValue("value"), nullValue());
        } else {
            assertThat(cell.getPropertyValue("cell_ordinal"), is((Object) (y * 2
                                                                           + x)));
            assertThat(cell.getPropertyValue("font_flags"), is((Object) 0));
            assertThat(cell.getPropertyValue("format_string"), is((Object) "Standard"));
            assertThat(cell.getPropertyValue("format"), is((Object) "Standard"));
            assertThat(cell.getPropertyValue("formatted_value"), is((Object) "135,215"));
            assertThat(cell.getPropertyValue("solve_order"), is((Object) 0));
            assertThat(((Number) cell.getPropertyValue("value")).doubleValue(),
                range(135215.0, 0.1));
        }
    }

    @Test public void testPropertyDescription() throws Exception {
        TestContext context = getTestContext().legacy().create(
            null,
            "<Cube name=\"Foo\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\" defaultMember=\"[All Promotions]\">\n"
            + "      <Table name=\"promotion\"/>\n"
            + "      <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\">\n"
            + "   <Property name=\"BarProp\" column=\"promotion_name\" description=\"BaconDesc\"/>\n"
            + "   </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "</Cube>\n",
            null, null, null, null);
        assertThat(context.getOlap4jConnection().getOlapSchema()
                    .getCubes().get("Foo")
                    .getDimensions().get("Promotions")
                    .getHierarchies().get(0)
                    .getLevels().get(1)
                    .getProperties().get("BarProp")
                    .getDescription(), is("BaconDesc"));
    }
}

// End PropertiesTest.java
