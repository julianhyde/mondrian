/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2013-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.spi;

import mondrian.rolap.DefaultDataServicesProvider;

import org.junit.Test;

import static mondrian.spi.DataServicesLocator.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DataServicesLocatorTest {
    @Test public void testEmptyNameReturnsDefaultProvider() {
        assertDefaultProvider(null);
        assertDefaultProvider("");
    }

    @Test public void testUnrecognizedNameThrowsException() {
        try {
            getDataServicesProvider("somename");
            fail("Expected exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is(
                "Unrecognized Service Provider: somename"));
        }
    }

    @Test public void testLocatesValidProvider() {
        DataServicesProvider provider =
            getDataServicesProvider("mondrian.spi.FakeDataServicesProvider");
        assertThat(provider, instanceOf(FakeDataServicesProvider.class));
    }

    private void assertDefaultProvider(String providerName) {
        DataServicesProvider provider = getDataServicesProvider(providerName);
        assertThat("Expected Default implementation", provider,
            instanceOf(DefaultDataServicesProvider.class));
    }
}

// End DataServicesLocatorTest.java
