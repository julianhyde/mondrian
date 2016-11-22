/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2013-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for {@link ConcatenableList}.
 *
 * <p>Currently oriented at testing fixes for a couple of known bugs;
 * these should not be considered to be otherwise comprehensive.</p>
 */
public class ConcatenableListTest {

    // Just some placeholder constants for expected values in backing lists
    private final String NON_EMPTY_MARKER = "Not empty",
        VALUE_1 = "A",
        VALUE_2 = "B",
        VALUE_3 = "C",
        VALUE_4 = "D",
        VALUE_5 = "E",
        VALUE_6 = "F";

    /**
     * Tests that basic iteration over multiple backing lists works properly,
     * whether or not there are intervening empty lists.
     */
    @Test public void testBasicIteration() {
        List<String> testList = new ConcatenableList<String>();
        testList.addAll(Arrays.asList(VALUE_1));
        testList.addAll(new ArrayList<String>());
        testList.addAll(new ArrayList<String>());
        testList.addAll(Arrays.asList(VALUE_2, VALUE_3));
        testList.addAll(Arrays.asList(VALUE_4));
        testList.addAll(new ArrayList<String>());
        testList.addAll(new ArrayList<String>());
        testList.addAll(new ArrayList<String>());
        testList.addAll(Arrays.asList(VALUE_5, VALUE_6));

        Iterator<String> iterator = testList.iterator();
        assertThat("iterator.hasNext() should be true", iterator.hasNext(), is(true));
        assertThat("first value should be A", iterator.next(), is(VALUE_1));
        assertThat("iterator.hasNext() should be true", iterator.hasNext(), is(true));
        assertThat("first value should be B", iterator.next(), is(VALUE_2));
        assertThat("iterator.hasNext() should be true", iterator.hasNext(), is(true));
        assertThat("first value should be C", iterator.next(), is(VALUE_3));
        assertThat("iterator.hasNext() should be true", iterator.hasNext(), is(true));
        assertThat("first value should be D", iterator.next(), is(VALUE_4));
        assertThat("iterator.hasNext() should be true", iterator.hasNext(), is(true));
        assertThat("first value should be E", iterator.next(), is(VALUE_5));
        assertThat("iterator.hasNext() should be true", iterator.hasNext(), is(true));
        assertThat("first value should be F", iterator.next(), is(VALUE_6));
        assertThat("iterator.hasNext() should be false, since no more values",
            iterator.hasNext(), is(false));
    }

    /**
     * Tests that it is possible to iterate through a series of next() calls
     * without first calling hasNext(). (Necessary because an earlier
     * implementation of ConcatenableList would throw a null pointer exception
     * if hasNext() wasn't called first.)
     */
    @Test public void testIteratorNextWithoutHasNext() {
        List<String> testList = new ConcatenableList<String>();
        testList.addAll(Arrays.asList(VALUE_1));
        testList.addAll(new ArrayList<String>());
        testList.addAll(new ArrayList<String>());
        testList.addAll(Arrays.asList(VALUE_2, VALUE_3));
        testList.addAll(Arrays.asList(VALUE_4));
        testList.addAll(new ArrayList<String>());
        testList.addAll(new ArrayList<String>());
        testList.addAll(new ArrayList<String>());
        testList.addAll(Arrays.asList(VALUE_5, VALUE_6));

        Iterator<String> iterator = testList.iterator();
        assertThat("first value should be A", iterator.next(), is(VALUE_1));
        assertThat("first value should be B", iterator.next(), is(VALUE_2));
        assertThat("first value should be C", iterator.next(), is(VALUE_3));
        assertThat("first value should be D", iterator.next(), is(VALUE_4));
        assertThat("first value should be E", iterator.next(), is(VALUE_5));
        assertThat("first value should be F", iterator.next(), is(VALUE_6));
        assertThat("iterator.hasNext() should be false, since no more values",
            iterator.hasNext(), is(false));
    }

    /**
     * Tests that if multiple empty lists are added, followed by a non-empty
     * list, iteration behaves correctly and get(0) does not fail. (An earlier
     * implementation of ConcatenableList would incorrectly throw an
     * IndexOutOfBoundsException when get(0) was called on an instance where the
     * backing lists included two consecutive empty lists.)
     */
    @Test public void testGetZeroWithMultipleEmptyLists() {
        List<String> testList = new ConcatenableList<String>();

        testList.addAll(new ArrayList<String>());
        testList.addAll(new ArrayList<String>());
        testList.addAll(Arrays.asList(NON_EMPTY_MARKER));

        assertThat("ConcatenableList testList should not be empty",
            testList.isEmpty(), is(false));

        assertThat("testList.get(0) should return NON_EMPTY_MARKER",
            testList.get(0), is(NON_EMPTY_MARKER));
    }
}

// End ConcatenableListTest.java
