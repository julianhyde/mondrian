/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2011 Pentaho and others
// All Rights Reserved.
*/
package mondrian.rolap;

import mondrian.calc.TupleCollections;
import mondrian.calc.TupleList;
import mondrian.olap.Member;
import mondrian.olap.Position;
import mondrian.olap.fun.TestMember;
import mondrian.test.FoodMartTestCase;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for lists and iterators over members and tuples.
 */
public class RolapAxisTest extends FoodMartTestCase {
    @Test public void testMemberArrayList() {
        TupleList list = TupleCollections.createList(3);
        list.add(
            Arrays.<Member>asList(
                new TestMember("a"),
                new TestMember("b"),
                new TestMember("c")));
        list.add(
            Arrays.<Member>asList(
                new TestMember("d"),
                new TestMember("e"),
                new TestMember("f")));
        list.add(
            Arrays.<Member>asList(
                new TestMember("g"),
                new TestMember("h"),
                new TestMember("i")));

        StringBuilder buf = new StringBuilder(100);

        RolapAxis axis = new RolapAxis(list);
        List<Position> positions = axis.getPositions();
        boolean firstTimeInner = true;
        for (Position position : positions) {
            if (! firstTimeInner) {
                buf.append(',');
            }
            buf.append(toString(position));
            firstTimeInner = false;
        }
        String s = buf.toString();
        String e = "{a,b,c},{d,e,f},{g,h,i}";
        assertThat(s, is(e));

        positions = axis.getPositions();
        int size = positions.size();
        assertThat(size, is(3));

        buf.setLength(0);
        for (int i = 0; i < size; i++) {
            Position position = positions.get(i);
            if (i > 0) {
                buf.append(',');
            }
            buf.append(toString(position));
        }
        s = buf.toString();
        e = "{a,b,c},{d,e,f},{g,h,i}";
        assertThat(s, is(e));
    }

    protected String toString(List<Member> position) {
        StringBuffer buf = new StringBuffer(100);
        buf.append('{');
        boolean firstTimeInner = true;
        for (Member m : position) {
            if (! firstTimeInner) {
                buf.append(',');
            }
            buf.append(m);
            firstTimeInner = false;
        }
        buf.append('}');
        return buf.toString();
    }
}

// End RolapAxisTest.java
