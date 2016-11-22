/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde
// Copyright (C) 2005-2007 Pentaho
// All Rights Reserved.
//
// jhyde, Feb 14, 2003
*/
package mondrian.udf;

import mondrian.test.FoodMartTestCase;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * <code>NullValueTest</code> is a test case which tests simple queries
 * expressions.
 *
 * @author <a>Richard M. Emberson</a>
 * @since Mar 01 2007
 */
public class NullValueTest extends FoodMartTestCase {
    @Test public void testNullValue() {
        String s = executeExpr(" NullValue()/NullValue() ");
        assertThat(s, is(""));

        s = executeExpr(" NullValue()/NullValue() = NULL ");
        assertThat(s, is("false"));

        boolean hasException = false;
        try {
            s = executeExpr(" NullValue() IS NULL ");
        } catch (Exception ex) {
            hasException = true;
        }
        assertThat(hasException, is(true));

        // I believe that these IsEmpty results are correct.
        // The NullValue function does not represent a cell.
        s = executeExpr(" IsEmpty(NullValue()) ");
        assertThat(s, is("false"));

        // NullValue()/NullValue() evaluates to DoubleNull
        // but DoubleNull evaluates to null, so this seems
        // to be broken??
        // s = executeExpr(" IsEmpty(NullValue()/NullValue()) ");
        // assertEquals("false", s);

        s = executeExpr(" 4 + NullValue() ");
        assertThat(s, is("4"));

        s = executeExpr(" NullValue() - 4 ");
        assertThat(s, is("-4"));

        s = executeExpr(" 4*NullValue() ");
        assertThat(s, is(""));

        s = executeExpr(" NullValue()*4 ");
        assertThat(s, is(""));

        s = executeExpr(" 4/NullValue() ");
        assertThat(s, is("Infinity"));

        s = executeExpr(" NullValue()/4 ");
        assertThat(s, is(""));
  /*
*/
    }
}

// End NullValueTest.java
