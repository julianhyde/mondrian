/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2011-2012 Pentaho and others
// All Rights Reserved.
*/
package mondrian.util;

import mondrian.test.TestContext;

import org.junit.Test;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for {@link PartiallyOrderedSet}.
 */
public class PartiallyOrderedSetTest {
    private static final boolean debug = false;
    private final int SCALE = 250; // 100, 1000, 3000 are also reasonable values
    final long seed = new Random().nextLong();
    final Random random = new Random(seed);

    static final PartiallyOrderedSet.Ordering<String> stringSubsetOrdering =
        new PartiallyOrderedSet.Ordering<String>() {
            public boolean lessThan(String e1, String e2) {
                // e1 < e2 if every char in e1 is also in e2
                for (int i = 0; i < e1.length(); i++) {
                    if (e2.indexOf(e1.charAt(i)) < 0) {
                        return false;
                    }
                }
                return true;
            }
        };

    // Integers, ordered by division. Top is 1, its children are primes,
    // etc.
    static final PartiallyOrderedSet.Ordering<Integer> isDivisor =
        new PartiallyOrderedSet.Ordering<Integer>() {
            public boolean lessThan(Integer e1, Integer e2) {
                return e2 % e1 == 0;
            }
        };

    // Bottom is 1, parents are primes, etc.
    static final PartiallyOrderedSet.Ordering<Integer> isDivisorInverse =
        new PartiallyOrderedSet.Ordering<Integer>() {
            public boolean lessThan(Integer e1, Integer e2) {
                return e1 % e2 == 0;
            }
        };

    // Ordered by bit inclusion. E.g. the children of 14 (1110) are
    // 12 (1100), 10 (1010) and 6 (0110).
    static final PartiallyOrderedSet.Ordering<Integer> isBitSubset =
        new PartiallyOrderedSet.Ordering<Integer>() {
            public boolean lessThan(Integer e1, Integer e2) {
                return (e2 & e1) == e2;
            }
        };

    // Ordered by bit inclusion. E.g. the children of 14 (1110) are
    // 12 (1100), 10 (1010) and 6 (0110).
    static final PartiallyOrderedSet.Ordering<Integer> isBitSuperset =
        new PartiallyOrderedSet.Ordering<Integer>() {
            public boolean lessThan(Integer e1, Integer e2) {
                return (e2 & e1) == e1;
            }
        };

    @Test public void testPoset() {
        String empty = "''";
        String abcd = "'abcd'";
        PartiallyOrderedSet<String> poset =
            new PartiallyOrderedSet<String>(stringSubsetOrdering);
        assertThat(poset.size(), is(0));

        final StringBuilder buf = new StringBuilder();
        poset.out(buf);
        TestContext.assertEqualsVerbose(
            "PartiallyOrderedSet size: 0 elements: {\n"
            + "}",
            buf.toString());

        poset.add("a");
        printValidate(poset);
        poset.add("b");
        printValidate(poset);

        poset.clear();
        assertThat(poset.size(), is(0));
        poset.add(empty);
        printValidate(poset);
        poset.add(abcd);
        printValidate(poset);
        assertThat(poset.size(), is(2));
        assertThat(poset.getNonChildren().toString(), is("['abcd']"));
        assertThat(poset.getNonParents().toString(), is("['']"));

        final String ab = "'ab'";
        poset.add(ab);
        printValidate(poset);
        assertThat(poset.size(), is(3));
        assertThat(poset.getChildren(empty).toString(), is("[]"));
        assertThat(poset.getParents(empty).toString(), is("['ab']"));
        assertThat(poset.getChildren(abcd).toString(), is("['ab']"));
        assertThat(poset.getParents(abcd).toString(), is("[]"));
        assertThat(poset.getChildren(ab).toString(), is("['']"));
        assertThat(poset.getParents(ab).toString(), is("['abcd']"));

        // "bcd" is child of "abcd" and parent of ""
        final String bcd = "'bcd'";
        poset.add(bcd);
        printValidate(poset);
        assertThat(poset.isValid(false), is(true));
        assertThat(poset.getChildren(bcd).toString(), is("['']"));
        assertThat(poset.getParents(bcd).toString(), is("['abcd']"));
        assertThat(poset.getChildren(abcd).toString(), is("['ab', 'bcd']"));

        buf.setLength(0);
        poset.out(buf);
        TestContext.assertEqualsVerbose(
            "PartiallyOrderedSet size: 4 elements: {\n"
            + "  'abcd' parents: [] children: ['ab', 'bcd']\n"
            + "  'ab' parents: ['abcd'] children: ['']\n"
            + "  'bcd' parents: ['abcd'] children: ['']\n"
            + "  '' parents: ['ab', 'bcd'] children: []\n"
            + "}",
            buf.toString());

        final String b = "'b'";

        // ancestors of an element not in the set
        assertThat(sortedStr(poset.getAncestors(b)),
            is("['ab', 'abcd', 'bcd']"));

        poset.add(b);
        printValidate(poset);
        assertThat(poset.getNonChildren().toString(), is("['abcd']"));
        assertThat(poset.getNonParents().toString(), is("['']"));
        assertThat(poset.getChildren(b).toString(), is("['']"));
        assertThat(sortedStr(poset.getParents(b)), is("['ab', 'bcd']"));
        assertThat(poset.getChildren(b).toString(), is("['']"));
        assertThat(poset.getChildren(abcd).toString(), is("['ab', 'bcd']"));
        assertThat(poset.getChildren(bcd).toString(), is("['b']"));
        assertThat(poset.getChildren(ab).toString(), is("['b']"));
        assertThat(sortedStr(poset.getAncestors(b)),
            is("['ab', 'abcd', 'bcd']"));

        // descendants and ancestors of an element with no descendants
        assertThat(poset.getDescendants(empty).toString(), is("[]"));
        assertThat(sortedStr(poset.getAncestors(empty)),
            is("['ab', 'abcd', 'b', 'bcd']"));

        // some more ancestors of missing elements
        assertThat(sortedStr(poset.getAncestors("'ac'")), is("['abcd']"));
        assertThat(sortedStr(poset.getAncestors("'z'")), is("[]"));
        assertThat(sortedStr(poset.getAncestors("'a'")), is("['ab', 'abcd']"));
    }

    @Test public void testPosetTricky() {
        PartiallyOrderedSet<String> poset =
            new PartiallyOrderedSet<String>(stringSubsetOrdering);

        // A tricky little poset with 4 elements:
        // {a <= ab and ac, b < ab, ab, ac}
        poset.clear();
        poset.add("'a'");
        printValidate(poset);
        poset.add("'b'");
        printValidate(poset);
        poset.add("'ac'");
        printValidate(poset);
        poset.add("'ab'");
        printValidate(poset);
    }

    @Test public void testPosetBits() {
        final PartiallyOrderedSet<Integer> poset =
            new PartiallyOrderedSet<Integer>(isBitSuperset);
        poset.add(2112); // {6, 11} i.e. 64 + 2048
        poset.add(2240); // {6, 7, 11} i.e. 64 + 128 + 2048
        poset.add(2496); // {6, 7, 8, 11} i.e. 64 + 128 + 256 + 2048
        printValidate(poset);
        poset.remove(2240);
        printValidate(poset);
        poset.add(2240); // {6, 7, 11} i.e. 64 + 128 + 2048
        printValidate(poset);
    }

    @Test public void testPosetBitsRemoveParent() {
        final PartiallyOrderedSet<Integer> poset =
            new PartiallyOrderedSet<Integer>(isBitSuperset);
        poset.add(66); // {bit 2, bit 6}
        poset.add(68); // {bit 3, bit 6}
        poset.add(72); // {bit 4, bit 6}
        poset.add(64); // {bit 6}
        printValidate(poset);
        poset.remove(64); // {bit 6}
        printValidate(poset);
    }

    @Test public void testDivisorPoset() {
        PartiallyOrderedSet<Integer> integers =
            new PartiallyOrderedSet<Integer>(isDivisor, range(1, 1000));
        assertThat(new TreeSet<Integer>(integers.getDescendants(120)).toString(),
            is("[1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 24, 30, 40, 60]"));
        assertThat(new TreeSet<Integer>(integers.getAncestors(120)).toString(),
            is("[240, 360, 480, 600, 720, 840, 960]"));
        assertThat(integers.getDescendants(1).isEmpty(), is(true));
        assertThat(integers.getAncestors(1).size(), is(998));
        assertThat(integers.isValid(true), is(true));
    }

    @Test public void testDivisorSeries() {
        checkPoset(isDivisor, debug, range(1, SCALE * 3), false);
    }

    @Test public void testDivisorRandom() {
        boolean ok = false;
        try {
            checkPoset(
                isDivisor, debug, random(random, SCALE, SCALE * 3), false);
            ok = true;
        } finally {
            if (!ok) {
                System.out.println("Random seed: " + seed);
            }
        }
    }

    @Test public void testDivisorRandomWithRemoval() {
        boolean ok = false;
        try {
            checkPoset(
                isDivisor, debug, random(random, SCALE, SCALE * 3), true);
            ok = true;
        } finally {
            if (!ok) {
                System.out.println("Random seed: " + seed);
            }
        }
    }

    @Test public void testDivisorInverseSeries() {
        checkPoset(isDivisorInverse, debug, range(1, SCALE * 3), false);
    }

    @Test public void testDivisorInverseRandom() {
        boolean ok = false;
        try {
            checkPoset(
                isDivisorInverse, debug, random(random, SCALE, SCALE * 3),
                false);
            ok = true;
        } finally {
            if (!ok) {
                System.out.println("Random seed: " + seed);
            }
        }
    }

    @Test public void testDivisorInverseRandomWithRemoval() {
        boolean ok = false;
        try {
            checkPoset(
                isDivisorInverse, debug, random(random, SCALE, SCALE * 3),
                true);
            ok = true;
        } finally {
            if (!ok) {
                System.out.println("Random seed: " + seed);
            }
        }
    }

    @Test public void testSubsetSeries() {
        checkPoset(isBitSubset, debug, range(1, SCALE / 2), false);
    }

    @Test public void testSubsetRandom() {
        boolean ok = false;
        try {
            checkPoset(
                isBitSubset, debug, random(random, SCALE / 4, SCALE), false);
            ok = true;
        } finally {
            if (!ok) {
                System.out.println("Random seed: " + seed);
            }
        }
    }

    private <E> void printValidate(PartiallyOrderedSet<E> poset) {
        if (debug) {
            dump(poset);
        }
        assertThat(poset.isValid(debug), is(true));
    }

    public void checkPoset(
        PartiallyOrderedSet.Ordering<Integer> ordering,
        boolean debug,
        Iterable<Integer> generator,
        boolean remove)
    {
        final PartiallyOrderedSet<Integer> poset =
            new PartiallyOrderedSet<Integer>(ordering);
        int n = 0;
        int z = 0;
        if (debug) {
            dump(poset);
        }
        for (int i : generator) {
            if (remove && z++ % 2 == 0) {
                if (debug) {
                    System.out.println("remove " + i);
                }
                poset.remove(i);
                if (debug) {
                    dump(poset);
                }
                continue;
            }
            if (debug) {
                System.out.println("add " + i);
            }
            poset.add(i);
            if (debug) {
                dump(poset);
            }
            Integer expected = ++n;
            assertThat(poset.size(), is(expected));
            if (i < 100) {
                if (!poset.isValid(false)) {
                    dump(poset);
                }
                assertThat(poset.isValid(true), is(true));
            }
        }
        assertThat(poset.isValid(true), is(true));

        final StringBuilder buf = new StringBuilder();
        poset.out(buf);
        assertThat(buf.length() > 0, is(true));
    }

    private <E> void dump(PartiallyOrderedSet<E> poset) {
        final StringBuilder buf = new StringBuilder();
        poset.out(buf);
        System.out.println(buf);
    }

    private static Collection<Integer> range(
        final int start, final int end)
    {
        return new AbstractList<Integer>() {
            @Override
            public Integer get(int index) {
                return start + index;
            }

            @Override
            public int size() {
                return end - start;
            }
        };
    }

    private static Iterable<Integer> random(
        Random random, final int size, final int max)
    {
        final Set<Integer> set = new LinkedHashSet<Integer>();
        while (set.size() < size) {
            set.add(random.nextInt(max) + 1);
        }
        return set;
    }

    private static String sortedStr(List<String> ss) {
        return new TreeSet<String>(ss).toString();
    }
}

// End PartiallyOrderedSetTest.java
