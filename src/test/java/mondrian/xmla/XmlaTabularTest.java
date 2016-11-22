/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2009 Pentaho
// All Rights Reserved.
*/
package mondrian.xmla;

import mondrian.test.DiffRepository;
import mondrian.test.TestContext;

import org.junit.Test;

/**
 * Test XMLA output in tabular (flattened) format.
 *
 * @author Julio Caub&iacute;n, jhyde
 */
public class XmlaTabularTest extends XmlaBaseTestCase {

    @Test public void testTabularOneByOne() throws Exception {
        executeMDX();
    }

    @Test public void testTabularOneByTwo() throws Exception {
        executeMDX();
    }

    @Test public void testTabularTwoByOne() throws Exception {
        executeMDX();
    }

    @Test public void testTabularTwoByTwo() throws Exception {
        executeMDX();
    }

    @Test public void testTabularZeroByZero() throws Exception {
        executeMDX();
    }

    @Test public void testTabularVoid() throws Exception {
        executeMDX();
    }

    @Test public void testTabularThreeAxes() throws Exception {
        executeMDX();
    }

    private void executeMDX() throws Exception {
        String requestType = "EXECUTE";
        doTest(
            requestType,
            getDefaultRequestProperties(requestType),
            TestContext.instance());
    }

    protected DiffRepository getDiffRepos() {
        return DiffRepository.lookup(XmlaTabularTest.class);
    }

    protected Class<? extends XmlaRequestCallback> getServletCallbackClass() {
        return null;
    }

    protected String getSessionId(Action action) {
        return null;
    }
}

// End XmlaTabularTest.java
