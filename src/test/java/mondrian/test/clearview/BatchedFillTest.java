/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2010 Pentaho and others
// All Rights Reserved.
*/
package mondrian.test.clearview;

import mondrian.olap.MondrianProperties;
import mondrian.test.DiffRepository;

import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runners.Parameterized;

import junit.framework.TestSuite;

import java.util.List;

/**
 * <code>BatchedFillTest</code> is a test suite which tests
 * complex queries against the FoodMart database. MDX queries and their
 * expected results are maintained separately in BatchedFillTest.xml file.
 *
 * @author Khanh Vu
 */
public class BatchedFillTest extends ClearViewBase {
    public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(BatchedFillTest.class);
    }

    @Parameterized.Parameters(name = "{index} {0}")
    public static List<Object[]> parameters() {
        return DiffRepository.parameters(BatchedFillTest.class);
    }

    @Override public void runTest() throws Exception {
        if (name.getMethodName().equals("testBatchedFill2")
            && MondrianProperties.instance().ReadAggregates.get()
            && MondrianProperties.instance().UseAggregates.get())
        {
            // If agg tables are enabled, the SQL generated is 'better' than
            // expected.
        } else {
            super.assertQuerySql(true);
        }
        super.runTest();
    }

}

// End BatchedFillTest.java
