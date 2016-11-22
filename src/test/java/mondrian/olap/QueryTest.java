/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 1998-2005 Julian Hyde
// Copyright (C) 2005-2012 Pentaho and others
// All Rights Reserved.
//
// Shishir, 08 May, 2007
*/
package mondrian.olap;

import mondrian.server.Statement;
import mondrian.test.FoodMartTestCase;
import mondrian.test.TestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Query test.
 */
public class QueryTest extends FoodMartTestCase {
    private QueryPart[] cellProps = {
        new CellProperty(Id.Segment.toList("Value")),
        new CellProperty(Id.Segment.toList("Formatted_Value")),
        new CellProperty(Id.Segment.toList("Format_String")),
    };
    private QueryAxis[] axes = new QueryAxis[0];
    private Formula[] formulas = new Formula[0];
    private Query queryWithCellProps;
    private Query queryWithoutCellProps;

    @Before public void setUp() throws Exception {
        TestContext testContext = getTestContext();
        ConnectionBase connection =
            (ConnectionBase) testContext.getConnection();
        final Statement statement =
            connection.getInternalStatement();

        try {
            queryWithCellProps =
                new Query(
                    statement, formulas, axes, "Sales",
                    null, cellProps, false);
            queryWithoutCellProps =
                new Query(
                    statement, formulas, axes, "Sales",
                    null, new QueryPart[0], false);
        } finally {
            statement.close();
        }
    }

    @After public void tearDown() throws Exception {
        queryWithCellProps = null;
        queryWithoutCellProps = null;
    }

    @Test public void testHasCellPropertyWhenQueryHasCellProperties() {
        assertThat(queryWithCellProps.hasCellProperty("Value"), is(true));
        assertThat(queryWithCellProps.hasCellProperty("Language"), is(false));
    }

    @Test public void testIsCellPropertyEmpty() {
        assertThat(queryWithoutCellProps.isCellPropertyEmpty(), is(true));
    }
}

// End QueryTest.java
