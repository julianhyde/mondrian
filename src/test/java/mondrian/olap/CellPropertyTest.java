/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 1998-2005 Julian Hyde
// Copyright (C) 2005-2012 Pentaho and others
// All Rights Reserved.
*/
package mondrian.olap;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test for <code>Cell Property<code>.
 *
 * @author Shishir
 * @since 08 May, 2007
 */
public class CellPropertyTest {
    private CellProperty cellProperty;

    @Before public void setUp() throws Exception {
        cellProperty = new CellProperty(Id.Segment.toList("Format_String"));
    }

    @Test public void testIsNameEquals() {
        assertThat(cellProperty.isNameEquals("Format_String"), is(true));
    }

    @Test public void testIsNameEqualsDoesCaseInsensitiveMatch() {
        assertThat(cellProperty.isNameEquals("format_string"), is(true));
    }

    @Test public void testIsNameEqualsParameterShouldNotBeQuoted() {
        assertThat(cellProperty.isNameEquals("[Format_String]"), is(false));
    }

}

// End CellPropertyTest.java
