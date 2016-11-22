/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 1998-2005 Julian Hyde
// Copyright (C) 2005-2011 Pentaho and others
// All Rights Reserved.
*/
package mondrian.test;

import mondrian.olap.*;
import mondrian.server.monitor.ServerInfo;
import mondrian.test.clearview.*;

import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.JUnitCore;

/**
 * The <code>CacheHitTest</code> class contains test suites that return
 * hit ratio of aggregation cache for various sequences of MDX queries.
 *
 * <p>This is not run as part of Main test suite as it only reports
 * ratios for further investigations.</p>
 *
 * @author kvu
 */
@Ignore("OPTIONAL_TEST")
public class CacheHitTest extends FoodMartTestCase {
    @Rule public final TestName name = new TestName();

    /**
     * Runs a set of small MDX queries that targets a small region
     * of aggregation cache sequentially. All queries reference
     * the relational Sales cube.
     *
     * @throws Exception on error
     */
    @Test public void testSmallSetSequential() throws Exception {
        System.out.println("== " + name.getMethodName() + " ==");
        runTestSuiteInOrder(50,
            PartialCacheTest.class,
            MultiLevelTest.class,
            MultiDimTest.class,
            QueryAllTest.class);
        clearCache("Sales");
    }

    /**
     * Runs a set of small MDX queries that targets a small region
     * of aggregation cache in random order. All queries reference
     * the relational Sales cube.
     *
     * @throws Exception on error
     */
    @Test public void testSmallSetRandom() throws Exception {
        System.out.println("== " + name.getMethodName() + " ==");
        runRandomSuite(200,
            PartialCacheTest.class,
            MultiLevelTest.class,
            MultiDimTest.class,
            QueryAllTest.class);
        clearCache("Sales");
    }

    /**
     * Runs a set of small MDX queries that targets a small region
     * of aggregation cache sequentially. All queries reference
     * the virtual Warehouse and Sales cube.
     *
     * @throws Exception on error
     */
    @Test public void testSmallSetVCSequential() throws Exception {
        System.out.println("== " + name.getMethodName() + " ==");
        runTestSuiteInOrder(50,
            PartialCacheTest.class,
            MultiLevelTest.class,
            MultiDimTest.class,
            QueryAllTest.class);
        clearCache("Warehouse and Sales");
    }

    /**
     * Runs a set of small MDX queries that targets a small region
     * of aggregation cache in random order. All queries reference
     * the virtual Warehouse and Sales cube.
     *
     * @throws Exception on error
     */
    @Test public void testSmallSetVCRandom() throws Exception {
        System.out.println("== " + name.getMethodName() + " ==");
        runRandomSuite(200,
            PartialCacheVCTest.class,
            MultiLevelVCTest.class,
            MultiDimVCTest.class,
            QueryAllVCTest.class);
        clearCache("Warehouse and Sales");
    }

    /**
     * Runs a set of bigger MDX queries that requires more memory
     * and targets a bigger region of cache in random order.
     * Queries reference to Sales cube as well as
     * Warehouse and Sales cube.
     *
     * @throws Exception on error
     */
    @Test public void testBigSetRandom() throws Exception {
        System.out.println("== " + name.getMethodName() + " ==");
        runRandomSuite(200,
            MemHungryTest.class,
            PartialCacheTest.class,
            MultiLevelTest.class,
            MultiDimTest.class,
            QueryAllTest.class,
            PartialCacheVCTest.class,
            MultiLevelVCTest.class,
            MultiDimVCTest.class,
            QueryAllVCTest.class,
            CVBasicTest.class,
            GrandTotalTest.class,
            MetricFilterTest.class,
            MiscTest.class,
            PredicateFilterTest.class,
            SubTotalTest.class,
            SummaryMetricPercentTest.class,
            SummaryTest.class,
            TopBottomTest.class);
        clearCache("Sales");
        clearCache("Warehouse and Sales");
    }

    /**
     * Loops a given number of iterations, each time run a random test case
     * in the test suite.
     *
     * @param iterationCount number of times
     * @param classes Classes to run
     * @throws Exception on error
     */
    public void runRandomSuite(int iterationCount, Class... classes)
        throws Exception
    {
        // TODO: run a random sample of the tests
        final MondrianServer server =
            MondrianServer.forConnection(getTestContext().getConnection());
        JUnitCore.runClasses(classes);
        report(server.getMonitor().getServer());
    }

    /**
     * Loops <code>numIter</code> times, each time run all child test
     * suite in the suite.
     *
     * @param iterationCount Number of iterations
     * @param classes Classes to run
     * @throws Exception on error
     */
    public void runTestSuiteInOrder(int iterationCount, Class... classes)
        throws Exception
    {
        // TODO: run tests in sequence, looping until we get to iterationCount
        final MondrianServer server =
            MondrianServer.forConnection(getTestContext().getConnection());
        JUnitCore.runClasses(classes);
        report(server.getMonitor().getServer());
    }

    /**
     * Prints cache hit ratio.
     *
     * @param serverInfo Server statistics
     */
    public void report(ServerInfo serverInfo) {
        System.out.println(
            "Number of requests: " + serverInfo.cellCacheRequestCount);
        System.out.println(
            "Number of misses: " + serverInfo.cellCacheMissCount);
        System.out.println(
            "Hit ratio ---> "
            + ((float) serverInfo.cellCacheHitCount
               / (float) serverInfo.cellCacheRequestCount));
    }

    /**
     * Clears aggregation cache
     *
     * @param cube Cube name
     */
    public void clearCache(String cube) {
        final TestContext testContext = getTestContext();
        final CacheControl cacheControl =
            testContext.getConnection().getCacheControl(null);

        // Flush the entire cache.
        final Connection connection = testContext.getConnection();
        final Cube salesCube = connection.getSchema().lookupCube(cube, true);
        final CacheControl.CellRegion measuresRegion =
            cacheControl.createMeasuresRegion(salesCube);
        cacheControl.flush(measuresRegion);
    }
}

// End CacheHitTest.java
