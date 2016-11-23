/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2012 Pentaho and others
// All Rights Reserved.
*/
package mondrian.test.clearview;

import mondrian.test.DiffRepository;
import mondrian.util.Bug;

import junit.framework.TestSuite;

import org.junit.runners.Parameterized;

import java.util.Collections;
import java.util.List;

/**
 * Test for the extended syntax of Order
 * function. See
 * <a href="http://pub.eigenbase.org/wiki/MondrianOrderFunctionExtension">
 *     MondrianOrderFunctionExtension</a>
 * for syntax rules.
 *
 * <p>MDX queries and their expected results are maintained separately in
 * OrderTest.xml file.
 *
 * @author Khanh Vu
 */
public class OrderTest extends ClearViewBase {
    public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(OrderTest.class);
    }

    @Parameterized.Parameters(name = "{index} {0}")
    public static List<Object[]> parameters() {
        if (!Bug.LayoutWrongCardinalty) {
            // OrderTest.testSortRowAtt fails until this is fixed
            return Collections.emptyList();
        }
        return DiffRepository.parameters(OrderTest.class);
    }

}

// End OrderTest.java
