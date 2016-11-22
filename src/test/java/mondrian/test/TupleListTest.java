/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2011-2011 Pentaho
// All Rights Reserved.
*/
package mondrian.test;

import mondrian.calc.TupleCollections;
import mondrian.calc.TupleList;
import mondrian.calc.impl.*;
import mondrian.olap.*;
import mondrian.rolap.RolapConnection;
import mondrian.server.Locus;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.*;

/**
 * Unit test for {@link TupleList} and common implementations.
 *
 * @author jhyde
 */
public class TupleListTest extends FoodMartTestCase {
    @Test public void testTupleList() {
        assertThat(TupleCollections.createList(1) instanceof UnaryTupleList,
            is(true));
        assertThat(TupleCollections.createList(2) instanceof ArrayTupleList,
            is(true));
    }

    @Test public void testUnaryTupleList() {
        // empty list
        final TupleList list0 = new UnaryTupleList();
        assertThat(list0.isEmpty(), is(true));
        assertThat(list0.size(), is(0));

        assertThat(TupleCollections.emptyList(1), is(list0));

        TupleList list1 = new UnaryTupleList();
        assertThat(list1, is(list0));
        final Member storeUsaMember = xxx("[Store].[USA]");
        list1.add(Collections.singletonList(storeUsaMember));
        assertThat(list1.isEmpty(), is(false));
        assertThat(list1.size(), is(1));
        assertThat(list1, not(sameInstance(list0)));

        TupleList list2 = new UnaryTupleList();
        list2.addTuple(new Member[]{storeUsaMember});
        assertThat(list2.isEmpty(), is(false));
        assertThat(list2.size(), is(1));
        assertThat(list2, is(list1));

        list2.clear();
        assertThat(list2, is(list0));
        assertThat(list0, is(list2));

        // For various lists, sublist returns the whole thing.
        for (TupleList list : Arrays.asList(list0, list1, list2)) {
            assertThat(list.subList(0, list.size()), is(list));
            assertThat(list.subList(0, list.size()), not(sameInstance(list)));
        }

        // Null members OK (at least for TupleList).
        list1.addTuple(new Member[]{null});
        list1.add(Collections.<Member>singletonList(null));
    }

    @Test public void testArrayTupleList() {
        final Member genderFMember = xxx("[Gender].[F]");
        final Member genderMMember = xxx("[Gender].[M]");

        // empty list
        final TupleList list0 = new ArrayTupleList(2);
        assertThat(list0.isEmpty(), is(true));
        assertThat(list0.size(), is(0));

        assertThat(TupleCollections.emptyList(2), is(list0));

        TupleList list1 = new ArrayTupleList(2);
        assertThat(list1, is(list0));
        final Member storeUsaMember = xxx("[Store].[USA]");
        list1.add(Arrays.asList(storeUsaMember, genderFMember));
        assertThat(list1.isEmpty(), is(false));
        assertThat(list1.size(), is(1));
        assertThat(list1, not(sameInstance(list0)));

        try {
            list1.add(Arrays.asList(storeUsaMember));
            fail("expected error");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Tuple length does not match arity"));
        }
        try {
            list1.addTuple(new Member[] {storeUsaMember});
            fail("expected error");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Tuple length does not match arity"));
        }
        try {
            list1.add(
                Arrays.asList(storeUsaMember, genderFMember, genderFMember));
            fail("expected error");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Tuple length does not match arity"));
        }
        try {
            list1.addTuple(
                new Member[]{storeUsaMember, genderFMember, genderFMember});
            fail("expected error");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Tuple length does not match arity"));
        }

        TupleList list2 = new ArrayTupleList(2);
        list2.addTuple(new Member[]{storeUsaMember, genderFMember});
        assertThat(list2.isEmpty(), is(false));
        assertThat(list2.size(), is(1));
        assertThat(list2, is(list1));

        list2.clear();
        assertThat(list2, is(list0));
        assertThat(list0, is(list2));

        assertThat(list0.toString(), is("[]"));
        assertThat(list1.toString(), is("[[[Store].[USA], [Gender].[F]]]"));
        assertThat(list2.toString(), is("[]"));

        // For various lists, sublist returns the whole thing.
        for (TupleList list : Arrays.asList(list0, list1, list2)) {
            final TupleList sublist = list.subList(0, list.size());
            assertThat(sublist, notNullValue());
            assertThat(sublist.toString(), notNullValue());
            assertThat(list.isEmpty(), is(sublist.isEmpty()));
            assertThat(sublist, is(list));
            assertThat(sublist, not(sameInstance(list)));
        }

        // Null members OK (at least for TupleList).
        list1.addTuple(storeUsaMember, null);
        list1.add(Arrays.asList(storeUsaMember, null));

        TupleList fm = new ArrayTupleList(2);
        fm.addTuple(genderFMember, storeUsaMember);
        fm.addTuple(genderMMember, storeUsaMember);
        checkProject(fm);
    }

    @Test public void testDelegatingTupleList() {
        final Member genderFMember = xxx("[Gender].[F]");
        final Member genderMMember = xxx("[Gender].[M]");
        final Member storeUsaMember = xxx("[Store].[USA]");

        final List<List<Member>> arrayList = new ArrayList<List<Member>>();
        TupleList fm = new DelegatingTupleList(2, arrayList);

        fm.addTuple(genderFMember, storeUsaMember);
        fm.addTuple(genderMMember, storeUsaMember);

        assertThat(fm.size(), is(2));
        assertThat(fm.getArity(), is(2));
        assertThat(fm.toString(),
            is("[[[Gender].[F], [Store].[USA]], [[Gender].[M], [Store].[USA]]]"));

        checkProject(fm);
    }

    /**
     * This is a test for MONDRIAN-1040. The DelegatingTupleList.slice()
     * method was mixing up the column and index variables.
     */
    @Test public void testDelegatingTupleListSlice() {
        // Functional test.
        assertQueryReturns(
            "select {[Measures].[Store Sales]} ON COLUMNS, Hierarchize(Except({[Customers].[All Customers], [Customers].[All Customers].Children}, {[Customers].[All Customers]})) ON ROWS from [Sales] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Sales]}\n"
            + "Axis #2:\n"
            + "{[Customers].[Canada]}\n"
            + "{[Customers].[Mexico]}\n"
            + "{[Customers].[USA]}\n"
            + "Row #0: \n"
            + "Row #1: \n"
            + "Row #2: 565,238.13\n");
        Locus.execute(
            (RolapConnection)getTestContext().getConnection(),
            "testDelegatingTupleListSlice",
            new Locus.Action<Void>() {
                public Void execute() {
                    // Unit test
                    final Member genderFMember = xxx("[Gender].[F]");
                    final Member storeUsaMember = xxx("[Store].[USA]");
                    final List<List<Member>> arrayList =
                        new ArrayList<List<Member>>();
                    final TupleList fm =
                        new DelegatingTupleList(2, arrayList);
                    fm.addTuple(genderFMember, storeUsaMember);
                    final List<Member> sliced = fm.slice(0);
                    assertThat(sliced.size(), is(2));
                    assertThat(fm.size(), is(1));
                    return null;
                }
            });
    }

    private void checkProject(TupleList fm) {
        assertThat(fm.size(), is(2));
        assertThat(fm.getArity(), is(2));
        assertThat(fm.project(new int[] {0}).size(), is(2));
        assertThat(fm.project(new int[] {0}).slice(0), is(fm.slice(0)));
        assertThat(fm.project(new int[] {1}).slice(0), is(fm.slice(1)));
        assertThat(fm.project(new int[] {0}).toString(),
            is("[[[Gender].[F]], [[Gender].[M]]]"));
        assertThat(fm.project(new int[] {1}).toString(),
            is("[[[Store].[USA]], [[Store].[USA]]]"));

        // Also check cloneList.
        assertThat(fm.cloneList(100).size(), is(0));
        assertThat(fm.cloneList(-1).size(), is(fm.size()));
        assertThat(fm.cloneList(-1), is(fm));
        assertThat(fm.cloneList(-1), not(sameInstance(fm)));
    }

    /**
     * Queries a member of the Sales cube.
     *
     * @param memberName Unique name of member
     * @return The member
     */
    private Member xxx(String memberName) {
        Schema schema = getConnection().getSchema();
        final boolean fail = true;
        Cube salesCube = schema.lookupCube("Sales", fail);
        final SchemaReader schemaReader =
            salesCube.getSchemaReader(null); // unrestricted
        return schemaReader.getMemberByUniqueName(
            Util.parseIdentifier(memberName), true);
    }
}

// End TupleListTest.java
