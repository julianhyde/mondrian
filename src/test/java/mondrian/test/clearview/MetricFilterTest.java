/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2007 Pentaho and others
// All Rights Reserved.
*/
package mondrian.test.clearview;

import mondrian.test.DiffRepository;

import junit.framework.TestSuite;

import org.junit.runners.Parameterized;

import java.util.List;

/**
 * <code>MetricFilterTest</code> is a test suite which tests scenarios of
 * filtering out measures' values in the FoodMart database.
 * MDX queries and their expected results are maintained separately in
 * MetricFilterTest.xml file.
 *
 * @author Khanh Vu
 */
public class MetricFilterTest extends ClearViewBase {
    public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(MetricFilterTest.class);
    }

    @Parameterized.Parameters(name = "{index} {0}")
    public static List<Object[]> parameters() {
        return DiffRepository.parameters(MetricFilterTest.class);
    }

}

// End MetricFilterTest.java
