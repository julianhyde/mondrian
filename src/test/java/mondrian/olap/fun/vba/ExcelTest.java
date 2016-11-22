/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2010 Pentaho
// All Rights Reserved.
*/
package mondrian.olap.fun.vba;

import mondrian.test.TestTemp;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for implementations of Excel worksheet functions.
 *
 * <p>Every function defined in {@link Excel} must have a test here. In addition,
 * there should be MDX tests (usually in
 * {@link mondrian.olap.fun.FunctionTest}) if handling of argument types,
 * result types, operator overloading, exception handling or null handling
 * are non-trivial.
 *
 * @author jhyde
 * @since Jan 16, 2008
 */
public class ExcelTest {
    private static final double SMALL = 1e-10d;

    @Test public void testAcos() {
        // Cos(0) = 1
        // Cos(60 degrees) = .5
        // Cos(90 degrees) = 0
        // Cos(180 degrees) = -1
        assertThat(Excel.acos(1.0), is(0.0));
        assertThat(Excel.acos(.5), TestTemp.range(Math.PI / 3.0, SMALL));
        assertThat(Excel.acos(0.0), is(Math.PI / 2.0));
        assertThat(Excel.acos(-1.0), is(Math.PI));
    }

    @Test public void testAcosh() {
        // acosh(1) = 0
        // acosh(2) ~= 1
        // acosh(4) ~= 2
        assertThat(Excel.acosh(1.0), is(0.0));
        assertThat(Excel.acosh(2.0), TestTemp.range(1.3169578969248166, SMALL));
        assertThat(Excel.acosh(4.0), TestTemp.range(2.0634370688955608, SMALL));
    }

    @Test public void testAsinh() {
        // asinh(0) = 0
        // asinh(1) ~= 1
        // asinh(10) ~= 3
        // asinh(-x) = -asinh(x)
        assertThat(Excel.asinh(0.0), is(0.0));
        assertThat(Excel.asinh(1.0), TestTemp.range(0.8813735870195429, SMALL));
        assertThat(Excel.asinh(10.0), TestTemp.range(2.99822295029797, SMALL));
        assertThat(Excel.asinh(-10.0), TestTemp.range(-2.99822295029797, SMALL));
    }

    @Test public void testAtan2() {
        assertThat(Excel.atan2(0, 10), is(Math.atan2(0, 10)));
        assertThat(Excel.atan2(1, .8), is(Math.atan2(1, .8)));
        assertThat(Excel.atan2(-5, 0), is(Math.atan2(-5, 0)));
    }

    @Test public void testAtanh() {
        // atanh(0) = 0
        // atanh(1) = +inf
        // atanh(-x) = -atanh(x)
        assertThat(Excel.atanh(0), is(0.0));
        assertThat(Excel.atanh(0.01), TestTemp.range(0.0100003333533347, SMALL));
        assertThat(Excel.atanh(0.5), TestTemp.range(0.549306144334054, SMALL));
        assertThat(Excel.atanh(0.9), TestTemp.range(1.4722194895832, SMALL));
        assertThat(Excel.atanh(0.99), TestTemp.range(2.64665241236224, SMALL));
        assertThat(Excel.atanh(0.99999), TestTemp.range(
            6.1030338227611125,
            SMALL));
        assertThat(Excel.atanh(-0.99999), TestTemp.range(
            -6.1030338227611125,
            SMALL));
    }

    @Test public void testCosh() {
        assertThat(Excel.cosh(0), is(Math.cosh(0)));
    }

    @Test public void testDegrees() {
        assertThat(Excel.degrees(Math.PI / 2), is(90.0));
    }

    @Test public void testLog10() {
        assertThat(Excel.log10(10), is(1.0));
        assertThat(Excel.log10(.01), TestTemp.range(-2.0, 0.00000000000001));
    }

    @Test public void testPi() {
        assertThat(Excel.pi(), is(Math.PI));
    }

    @Test public void testPower() {
        assertThat(Excel.power(0, 5), is(0.0));
        assertThat(Excel.power(5, 0), is(1.0));
        assertThat(Excel.power(4, 0.5), is(2.0));
        assertThat(Excel.power(2, -3), is(0.125));
    }

    @Test public void testRadians() {
        assertThat(Excel.radians(180.0), is(Math.PI));
        Double expected = -Math.PI * 3.0;
        assertThat(Excel.radians(-540.0), is(expected));
    }

    @Test public void testSinh() {
        assertThat(Excel.sinh(0), is(Math.sinh(0)));
    }

    @Test public void testSqrtPi() {
        // sqrt(2 pi) = sqrt(6.28) ~ 2.5
        assertThat(Excel.sqrtPi(2.0), TestTemp.range(2.506628274631, SMALL));
    }

    @Test public void testTanh() {
        assertThat(Excel.tanh(0), is(Math.tanh(0)));
        assertThat(Excel.tanh(0.44), is(Math.tanh(0.44)));
    }

    @Test public void testMod() {
        assertThat(Excel.mod(28, 13), is(2.0));
        assertThat(Excel.mod(28, -13), is(-11.0));
    }

    @Test public void testIntNative() {
        assertThat(Vba.intNative(5.1), is(5));
        assertThat(Vba.intNative(5.9), is(5));
        assertThat(Vba.intNative(-5.9), is(-6));
        assertThat(Vba.intNative(0.1), is(0));
        assertThat(Vba.intNative(0), is(0));
    }
}

// End ExcelTest.java
