/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2016-2016 Julian Hyde
// All Rights Reserved.
*/
package mondrian.test;

import mondrian.util.Counters;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test that executes last. It can be used to check invariants.
 */
public class TerminatorTest {
    @Test public void testSqlStatementExecuteMatchesClose() {
        // Number of successful calls to SqlStatement.execute
        // should match number of calls to SqlStatement.close
        // (excluding calls to close where close has already been called).
        // If there is a mismatch, try debugging by adding SqlStatement.id
        // values to a Set<Long>.
        Assert.assertThat("SqlStatement instances still open: "
                + Counters.SQL_STATEMENT_EXECUTING_IDS,
            Counters.SQL_STATEMENT_EXECUTE_COUNT.get(),
            CoreMatchers.is(Counters.SQL_STATEMENT_CLOSE_COUNT.get()));
    }
}

// End TerminatorTest.java
