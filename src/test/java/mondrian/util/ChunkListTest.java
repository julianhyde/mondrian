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

import mondrian.olap.Util;
import mondrian.olap.Util.Function0;
import mondrian.test.PerformanceTest;
import mondrian.test.PerformanceTest.Statistician;

import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit and performance test for {@link ChunkList}.
 */
public class ChunkListTest {
    public static final Factory FACTORY = new Factory() {
        public <E> List<E> create() {
            return new ChunkList<E>();
        }

        public <E> List<E> create(Collection<E> c) {
            return new ChunkList<E>(c);
        }
    };

    public static final Factory REVERSE_FACTORY = new Factory() {
        public <E> List<E> create() {
            return new ReverseList<E>(FACTORY.<E>create());
        }

        public <E> List<E> create(Collection<E> c) {
            final Object[] objects = c.toArray();
            //noinspection unchecked
            final List<E> list = (List) Arrays.asList(objects);
            Collections.reverse(list);
            return new ReverseList<E>(FACTORY.<E>create(list));
        }
    };

    final List<Factory> factories = Arrays.asList(FACTORY, REVERSE_FACTORY);

    /** Unit test for {@link mondrian.util.ChunkList}. */
    @Test public void testChunkList() {
        checkChunkList(FACTORY);
    }

    private void checkChunkList(Factory factory) {
        final List<Integer> list = factory.create();
        assertThat(list.size(), is(0));
        assertThat(list.isEmpty(), is(true));
        assertThat(list.toString(), is("[]"));

        try {
            list.remove(0);
            fail("expected exception");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            int x = list.get(0);
            fail("expected exception, got " + x);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            int x = list.get(100);
            fail("expected exception, got " + x);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        list.add(7);
        assertThat(list.size(), is(1));
        assertThat(list.get(0), is(7));
        assertThat(list.isEmpty(), is(false));
        assertThat(list.toString(), is("[7]"));

        list.add(9);
        list.add(null);
        list.add(11);
        assertThat(list.size(), is(4));
        assertThat(list.get(0), is(7));
        assertThat(list.get(1), is(9));
        assertThat(list.get(2), nullValue());
        assertThat(list.get(3), is(11));
        assertThat(list.isEmpty(), is(false));
        assertThat(list.toString(), is("[7, 9, null, 11]"));

        assertThat(list.contains(9), is(true));
        assertThat(list.contains(8), is(false));

        list.addAll(Collections.nCopies(40, 1));
        assertThat(list.size(), is(44));
        assertThat(list.get(40), is(1));

        int n = 0;
        for (Integer integer : list) {
            ++n;
        }
        assertThat(list.size(), is(n));

        int i = list.indexOf(null);
        assertThat(i, is(2));

        // can't sort if null is present
        list.set(2, 123);

        i = list.indexOf(null);
        assertThat(i, is(-1));

        Collections.sort(list);
        assertThat(list.toString(), is(
            "[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, "
            + "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 7, 9, "
            + "11, 123]"));

        // sort a multi-chunk list
        Collections.sort(
            factory.create(Collections.nCopies(1000000, 1)));

        Collections.sort(factory.<Integer>create());

        list.remove((Integer) 7);
        Collections.sort(list);
        assertThat(list.get(3), is(1));

        // remove all instances of a value that exists
        boolean b = list.removeAll(Arrays.asList(9));
        assertThat(b, is(true));

        // remove all instances of a non-existent value
        b = list.removeAll(Arrays.asList(99));
        assertThat(b, is(false));

        // remove all instances of a value that occurs in the last chunk
        list.add(12345);
        b = list.removeAll(Arrays.asList(12345));
        assertThat(b, is(true));

        // remove all instances of a value that occurs in the last chunk but
        // not as the last value
        list.add(12345);
        list.add(123);
        b = list.removeAll(Arrays.asList(12345));
        assertThat(b, is(true));

        final List<Integer> list1 =
            factory.create(Collections.nCopies(1000, 77));
        assertThat(list1.size(), is(1000));
        list1.add(1234);
        assertThat(list1.contains(1234), is(true));

        // add to an empty list via iterator
        //noinspection MismatchedQueryAndUpdateOfCollection
        final List<String> list2 = factory.create();
        list2.listIterator(0).add("x");
        assertThat(list2.toString(), is("[x]"));

        // add at start
        list2.add(0, "y");
        assertThat(list2.toString(), is("[y, x]"));
    }

    @Test public void testClear() {
        for (Factory factory : factories) {
            checkClear(factory);
        }
    }

    public void checkClear(Factory factory) {
        // clear using AbstractList.removeRange
        final List<Integer> list =
            factory.create(Arrays.asList(1, 2, 3, 4));
        list.subList(0, list.size()).clear();
        assertThat(list.isEmpty(), is(true));
        list.subList(0, list.size()).clear();
        assertThat(list.isEmpty(), is(true));
    }

    @Test public void testContainsAll() {
        for (Factory factory : factories) {
            checkContainsAll(factory);
        }
    }

    private void checkContainsAll(Factory factory) {
        assertThat(factory.create(Arrays.asList(1, 2, 3))
                .containsAll(Collections.singletonList(1)), is(true));
        assertThat(factory.create(Arrays.asList(1, 2, 3))
                .containsAll(Collections.emptyList()), is(true));
        assertThat(factory.create()
                .containsAll(Collections.emptyList()), is(true));
        assertThat(factory.create()
                .containsAll(Collections.singletonList(1)), is(false));
        assertThat(factory.create(Arrays.asList(1, 2, null, 3))
                .containsAll(Collections.singletonList(null)), is(true));
        assertThat(factory.create(Arrays.asList(1, 2, null, 3))
                .containsAll(Collections.singletonList(4)), is(false));
        assertThat(factory.create(Arrays.asList(1, 2, null, 3))
                .containsAll(Arrays.asList(4, null)), is(false));
        assertThat(factory.create(Arrays.asList(1, 2, 3))
                .containsAll(Collections.singletonList(null)), is(false));
        assertThat(factory.create(Arrays.asList(1, 2, null, 3))
                .containsAll(Collections.singletonList("xxx")), is(false));
    }

    @Test public void testIndexOf() {
        for (Factory factory : factories) {
            checkIndexOf(factory);
        }
    }

    public void checkIndexOf(Factory factory) {
        assertThat(factory.create(Arrays.asList(1, 2, 3, 4, 5)).indexOf(2), is(1));
        assertThat(factory.create(Arrays.asList(1, 2, 3, 4, null, 2)).indexOf(2), is(
            1));
        assertThat(factory.create(Arrays.asList(1, 2, 3, null, 2)).indexOf(null), is(
            3));
        assertThat(factory.create(Arrays.asList(1, 2, 3)).indexOf(5), is(-1));
        assertThat(factory.create().indexOf(5), is(-1));
        assertThat(factory.create(Arrays.asList(1, 2, 3, 2)).lastIndexOf(2), is(
            3));
    }

    @Test public void testContains() {
        for (Factory factory : factories) {
            checkContains(factory);
        }
    }

    public void checkContains(Factory factory) {
        assertThat(factory.create(Arrays.asList(1, 2)).contains(1), is(true));
        assertThat(factory.create(Arrays.asList(1, 2)).contains(11), is(false));
        assertThat(factory.create(Arrays.asList(1, 2)).contains(null), is(false));
        assertThat(factory.create(Arrays.asList(1, null, 2)).contains(null), is(true));
        assertThat(factory.create().contains(null), is(false));
        assertThat(factory.create().contains(123), is(false));

        final List<Integer> list =
            factory.create(Collections.nCopies(10000, 7));
        assertThat(list.contains(null), is(false));
        assertThat(list.contains(99), is(false));
        list.add(99);
        list.add(98);
        assertThat(list.contains(99), is(true));
        assertThat(list.contains(null), is(false));
        list.add(null);
        assertThat(list.contains(null), is(true));
    }

    @Test public void testFragmentation() {
        final ChunkList<Integer> list =
            new ChunkList<Integer>(Arrays.asList(1, 2, 3, 4));
        assertThat(list.chunkSizeDistribution(), is(
            "size: 4, distribution: [4:1], chunks: 1, elements per chunk: 4.0"));
        list.add(0, 5);
        assertThat(list.chunkSizeDistribution(), is(
            "size: 5, distribution: [5:1], chunks: 1, elements per chunk: 5.0"));
        list.addAll(Collections.nCopies(100, 6));
        assertThat(list.chunkSizeDistribution(), is(
            "size: 105, distribution: [41:1, 64:1], chunks: 2, elements per chunk: 52.5"));

        // Adding element at 0 causes the 64 block to split into 33 + 32.
        list.add(0, 7);
        assertThat(list.get(0), is(7));
        assertThat(list.chunkSizeDistribution(), is(
            "size: 106, distribution: [32:1, 33:1, 41:1], chunks: 3, elements per chunk: 35.333332"));

        // Adding another element at 0 causes no further split.
        list.add(0, 8);
        assertThat(list.chunkSizeDistribution(), is(
            "size: 107, distribution: [32:1, 34:1, 41:1], chunks: 3, elements per chunk: 35.666668"));
    }

    /** Unit test for {@link mondrian.util.ChunkList} that applies random
     * operations. */
    @Test public void testRandom() {
        final int ITERATION_COUNT = 3; //10000;
        for (Factory factory : factories) {
            checkRandom0(factory, ITERATION_COUNT);
        }
    }

    void checkRandom0(Factory factory, int ITERATION_COUNT) {
        checkRandom(new Random(1), factory.<Integer>create(), ITERATION_COUNT);
        final Random random = new Random(2);
        for (int j = 0; j < 10; j++) {
            checkRandom(random, factory.<Integer>create(), ITERATION_COUNT);
        }
        checkRandom(
            new Random(3), factory.create(Collections.nCopies(1000, 5)),
            ITERATION_COUNT);
    }

    void checkRandom(
        Random random,
        List<Integer> list,
        int iterationCount)
    {
        int removeCount = 0;
        int addCount = 0;
        final int initialCount = list.size();
        for (int i = 0; i < iterationCount; i++) {
            if (i == 227) {
                Util.discard(0);
            }
            assertValid(list, false);
            switch (random.nextInt(8)) {
            case 0:
                // remove last
                if (!list.isEmpty()) {
                    list.remove(list.size() - 1);
                    ++removeCount;
                }
                break;
            case 1:
                // add to end
                list.add(random.nextInt(1000));
                ++addCount;
                break;
            case 2:
                int n = 0;
                final int size = list.size();
                for (Integer integer : list) {
                    boolean b = n++ < size;
                    assertThat(b, is(true));
                }
                break;
            case 3:
                // remove all instances of a particular value
                int sizeBefore = list.size();
                boolean b = list.removeAll(
                    Collections.singletonList(random.nextInt(500)));
                if (b) {
                    assertThat(list.size() < sizeBefore, is(true));
                } else {
                    assertThat(list.size() == sizeBefore, is(true));
                }
                removeCount += (sizeBefore - list.size());
                break;
            case 4:
                // remove at random position
                if (!list.isEmpty()) {
                    list.remove(random.nextInt(list.size()));
                    ++removeCount;
                }
                break;
            case 5:
                // add at random position
                int count = random.nextInt(list.size() + 1);
                ListIterator<Integer> it = list.listIterator();
                for (int j = 0; j < count; j++) {
                    it.next();
                }
                it.add(list.size());
                ++addCount;
                break;
            default:
                // add at random position
                list.add(random.nextInt(list.size() + 1), list.size());
                ++addCount;
                break;
            }
            assertThat(initialCount + addCount - removeCount, is(list.size()));
        }
        assertValid(list, true);
    }

    @Test public void testPerformance() {
        //noinspection unchecked
        final Iterable<Pair<Function0<List<Integer>>, String>> factories0 =
            Pair.iterate(
                Arrays.asList(
                    new Function0<List<Integer>>() {
                        public List<Integer> apply() {
                            return new ArrayList<Integer>();
                        }
                    },
                    new Function0<List<Integer>>() {
                        public List<Integer> apply() {
                            return new LinkedList<Integer>();
                        }
                    },
                    new Function0<List<Integer>>() {
                        public List<Integer> apply() {
                            return new ChunkList<Integer>();
                        }
                    }
                ),
                Arrays.asList("ArrayList", "LinkedList", "ChunkList-64"));
        final List<Pair<Function0<List<Integer>>, String>> factories1 =
            new ArrayList<Pair<Function0<List<Integer>>, String>>();
        for (Pair<Function0<List<Integer>>, String> pair : factories0) {
            factories1.add(pair);
        }
        List<Pair<Function0<List<Integer>>, String>> factories =
            factories1.subList(2, 3);
        Iterable<Pair<Integer, String>> sizes =
            Pair.iterate(
                Arrays.asList(100000, 1000000, 10000000),
                Arrays.asList("100k", "1m", "10m"));
        for (final Pair<Function0<List<Integer>>, String> pair : factories) {
            new PerformanceTest.Benchmarker(
                "add 10m values, " + pair.right,
                new Util.Function1<Statistician, Void>() {
                    public Void apply(Statistician statistician) {
                        final List<Integer> list = pair.left.apply();
                        long start = System.currentTimeMillis();
                        for (int i = 0; i < 10000000; i++) {
                            list.add(1);
                        }
                        statistician.record(start);
                        assertValid(list, true);
                        return null;
                    }
                },
                10).run();
        }
        for (final Pair<Function0<List<Integer>>, String> pair : factories) {
            new PerformanceTest.Benchmarker(
                "iterate over 10m values, " + pair.right,
                new Util.Function1<Statistician, Void>() {
                    public Void apply(Statistician statistician) {
                        final List<Integer> list = pair.left.apply();
                        list.addAll(Collections.nCopies(10000000, 1));
                        long start = System.currentTimeMillis();
                        int count = 0;
                        for (Integer integer : list) {
                            count += integer;
                        }
                        statistician.record(start);
                        assert count == 10000000;
                        assertValid(list, true);
                        return null;
                    }
                },
                10).run();
        }
        for (final Pair<Function0<List<Integer>>, String> pair : factories) {
            for (final Pair<Integer, String> size : sizes) {
                if (size.left > 1000000) {
                    continue;
                }
                new PerformanceTest.Benchmarker(
                    "delete 10% of " + size.right + " values, " + pair.right,
                    new Util.Function1<Statistician, Void>() {
                        public Void apply(Statistician statistician) {
                            final List<Integer> list = pair.left.apply();
                            list.addAll(Collections.nCopies(size.left, 1));
                            long start = System.currentTimeMillis();
                            int n = 0;
                            for (Iterator<Integer> it = list.iterator();
                                 it.hasNext();)
                            {
                                Integer next = it.next();
                                if (n++ % 10 == 0) {
                                    it.remove();
                                }
                            }
                            statistician.record(start);
                            assertValid(list, true);
                            return null;
                        }
                    },
                    10).run();
            }
        }
        for (final Pair<Function0<List<Integer>>, String> pair : factories) {
            for (final Pair<Integer, String> size : sizes) {
                if (size.left > 1000000) {
                    continue;
                }
                new PerformanceTest.Benchmarker(
                    "get from " + size.right + " values, " + (size.left / 1000)
                    + " times, " + pair.right,
                    new Util.Function1<Statistician, Void>() {
                        public Void apply(Statistician statistician) {
                            final List<Integer> list = pair.left.apply();
                            list.addAll(Collections.nCopies(size.left, 1));
                            final int probeCount = size.left / 1000;
                            final Random random = new Random(1);
                            long start = System.currentTimeMillis();
                            int n = 0;
                            for (int i = 0; i < probeCount; i++) {
                                n += list.get(random.nextInt(list.size()));
                            }
                            assert n == probeCount;
                            statistician.record(start);
                            assertValid(list, true);
                            return null;
                        }
                    },
                    10).run();
            }
        }
        for (final Pair<Function0<List<Integer>>, String> pair : factories) {
            for (final Pair<Integer, String> size : sizes) {
                if (size.left > 1000000) {
                    continue;
                }
                new PerformanceTest.Benchmarker(
                    "add " + size.right
                    + " values, delete 10%, insert 20%, get 1%, using "
                    + pair.right,
                    new Util.Function1<Statistician, Void>() {
                        public Void apply(Statistician statistician) {
                            final List<Integer> list = pair.left.apply();
                            final int probeCount = size.left / 100;
                            long start = System.currentTimeMillis();
                            list.addAll(Collections.nCopies(size.left, 1));
                            final Random random = new Random(1);
                            for (Iterator<Integer> it = list.iterator();
                                 it.hasNext();)
                            {
                                Integer next = it.next();
                                if (random.nextInt(10) == 0) {
                                    it.remove();
                                }
                            }
                            for (ListIterator<Integer> it =
                                     list.listIterator(); it.hasNext();)
                            {
                                Integer next = it.next();
                                if (random.nextInt(5) == 0) {
                                    it.add(2);
                                }
                            }
                            int n = 0;
                            for (int i = 0; i < probeCount; i++) {
                                n += list.get(random.nextInt(list.size()));
                            }
                            assert n > probeCount;
                            statistician.record(start);
                            assertValid(list, true);
                            return null;
                        }
                    },
                    10).run();
            }
        }
    }

    private void assertValid(List list, boolean print) {
        if (list instanceof ChunkList) {
            ChunkList chunkList = (ChunkList) list;
            assert chunkList.isValid(print, true);
        }
    }

    private static class ReverseList<E> extends AbstractList<E> {
        private final List<E> list;

        protected ReverseList(List<E> list) {
            this.list = list;
        }

        @Override
        public E get(int index) {
            return list.get(size() - 1 - index);
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public void add(int index, E element) {
            list.add(size() - index, element);
        }

        @Override
        public E remove(int index) {
            return list.remove(size() - 1 - index);
        }

        @Override
        public int indexOf(Object o) {
            return super.indexOf(o);
        }
    }

    private interface Factory {
        <E> List<E> create();

        <E> List<E> create(Collection<E> c);
    }
}

// End ChunkListTest.java
