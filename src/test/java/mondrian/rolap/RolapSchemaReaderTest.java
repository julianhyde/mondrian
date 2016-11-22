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
package mondrian.rolap;

import mondrian.olap.*;
import mondrian.test.FoodMartTestCase;
import mondrian.test.TestContext;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.*;

/**
 * Unit test for {@link SchemaReader}.
 */
public class RolapSchemaReaderTest extends FoodMartTestCase {
    @Test public void testGetCubesWithNoHrCubes() {
        String[] expectedCubes = new String[] {
                "Sales", "Warehouse", "Warehouse and Sales", "Store",
                "Sales 2", "$Time", "$Product", "$Warehouse",
                "$Store"
        };

        Connection connection =
            getTestContext().withRole("No HR Cube").getConnection();
        try {
            SchemaReader reader = connection.getSchemaReader().withLocus();

            Cube[] cubes = reader.getCubes();

            assertThat(cubes.length, is(expectedCubes.length));

            assertCubeExists(expectedCubes, cubes);
        } finally {
            connection.close();
        }
    }

    @Test public void testGetCubesWithNoRole() {
        String[] expectedCubes = new String[] {
                "Sales", "Warehouse", "Warehouse and Sales", "Store",
                "Sales 2", "HR", "$Time", "$Product", "$Warehouse",
                "$Store"
        };

        Connection connection = getTestContext().getConnection();
        try {
            SchemaReader reader = connection.getSchemaReader().withLocus();

            Cube[] cubes = reader.getCubes();

            assertThat(cubes.length, is(expectedCubes.length));

            assertCubeExists(expectedCubes, cubes);
        } finally {
            connection.close();
        }
    }

    @Test public void testGetCubesForCaliforniaManager() {
        String[] expectedCubes = new String[] {
                "Sales"
        };

        Connection connection =
                getTestContext().withRole("California manager").getConnection();
        try {
            SchemaReader reader = connection.getSchemaReader().withLocus();

            Cube[] cubes = reader.getCubes();

            assertThat(cubes.length, is(expectedCubes.length));

            assertCubeExists(expectedCubes, cubes);
        } finally {
            connection.close();
        }
    }

    @Test public void testConnectUseContentChecksum() {
        Util.PropertyList properties =
            TestContext.instance().getConnectionProperties().clone();
        properties.put(
            RolapConnectionProperties.UseContentChecksum.name(),
            "true");

        try {
            DriverManager.getConnection(
                properties,
                null);
        } catch (MondrianException e) {
            e.printStackTrace();
            fail("unexpected exception for UseContentChecksum");
        }
    }

    private void assertCubeExists(String[] expectedCubes, Cube[] cubes) {
        List cubesAsList = Arrays.asList(expectedCubes);

        for (Cube cube : cubes) {
            String cubeName = cube.getName();
            assertThat(
                "Cube name not found: " + cubeName,
                cubesAsList.contains(cubeName), is(true));
        }
    }

    /**
     * Test case for {@link SchemaReader#getCubeDimensions(mondrian.olap.Cube)}
     * and {@link SchemaReader#getDimensionHierarchies(mondrian.olap.Dimension)}
     * methods.
     *
     * <p>Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-691">MONDRIAN-691,
     * "RolapSchemaReader is not enforcing access control on two APIs"</a>.
     */
    @Test public void testGetCubeDimensions() {
        final String timeWeekly =
            TestContext.hierarchyName("Time", "Weekly");
        final String timeTime =
            TestContext.hierarchyName("Time", "Time");
        final TestContext testContext =
            TestContext.instance().create(
                null, null, null, null, null,
                "<Role name=\"REG1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <DimensionGrant dimension=\"Store\" access=\"none\"/>\n"
                + "      <HierarchyGrant hierarchy=\""
                + timeTime
                + "\" access=\"none\"/>\n"
                + "      <HierarchyGrant hierarchy=\""
                + timeWeekly
                + "\" access=\"all\"/>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>")
                .withRole("REG1");
        Connection connection = testContext.getConnection();
        try {
            SchemaReader reader = connection.getSchemaReader().withLocus();
            final Map<String, Cube> cubes = new HashMap<String, Cube>();
            for (Cube cube : reader.getCubes()) {
                cubes.put(cube.getName(), cube);
            }
            assertThat(cubes.containsKey("Sales"), is(true));
            assertThat(cubes.containsKey("HR"), is(false));
            assertThat(cubes.containsKey("Bad"), is(false));

            final Cube salesCube = cubes.get("Sales");
            final Map<String, Dimension> dimensions =
                new HashMap<String, Dimension>();
            final Map<String, Hierarchy> hierarchies =
                new HashMap<String, Hierarchy>();
            for (Dimension dimension : reader.getCubeDimensions(salesCube)) {
                dimensions.put(dimension.getName(), dimension);
                for (Hierarchy hierarchy
                    : reader.getDimensionHierarchies(dimension))
                {
                    hierarchies.put(hierarchy.getUniqueName(), hierarchy);
                }
            }
            assertThat(dimensions.containsKey("Store"), is(false));
            assertThat(dimensions.containsKey("Customer"), is(true));
            assertThat(dimensions.containsKey("Time"), is(true));
            assertThat(dimensions.containsKey("Bad dimension"), is(false));

            assertThat(hierarchies.containsKey("[Foo]"), is(false));
            assertThat(hierarchies.containsKey("[Customer].[Marital Status]"), is(true));
            assertThat(hierarchies.containsKey("[Product].[Products]"), is(true));
            assertThat(hierarchies.containsKey(timeWeekly), is(true));
            assertThat(hierarchies.containsKey("[Time]"), is(false));
            assertThat(hierarchies.containsKey("[Time].[Time]"), is(false));
        } finally {
            connection.close();
        }
    }
}

// End RolapSchemaReaderTest.java
