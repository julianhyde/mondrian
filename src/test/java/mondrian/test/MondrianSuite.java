/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2014 Pentaho and others
// All Rights Reserved.
*/
package mondrian.test;

import mondrian.calc.impl.ConstantCalcTest;
import mondrian.olap.*;
import mondrian.olap.fun.*;
import mondrian.olap.fun.vba.ExcelTest;
import mondrian.olap.fun.vba.VbaTest;
import mondrian.olap.type.TypeTest;
import mondrian.rolap.*;
import mondrian.rolap.agg.*;
import mondrian.rolap.aggmatcher.*;
import mondrian.rolap.sql.SelectNotInGroupByTest;
import mondrian.rolap.sql.SqlQueryTest;
import mondrian.test.clearview.*;
import mondrian.test.comp.ResultComparatorTest;
import mondrian.udf.CurrentDateMemberUdfTest;
import mondrian.udf.NullValueTest;
import mondrian.util.*;
import mondrian.xmla.*;
import mondrian.xmla.impl.DynamicDatasourceXmlaServletTest;
import mondrian.xmla.test.XmlaTest;

import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static org.junit.Assert.assertThat;

/**
 * Search for uses of:<ul>
 * <li>{@link String#indexOf(String)}
 * <li>{@link junit.framework.AssertionFailedError}
 * <li>{@link junit.framework.Test}
 * <li>methods that return {@link TestSuite} especitally "suite()"
 * <li>"Testcase"
 * <li>test names prefixed "_test" and "disabled_"
 * </ul>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    // if (RUN_OPTIONAL_TESTS) {
    SegmentLoaderTest.class,
    AggGenTest.class,
    DefaultRuleTest.class,
    SelectNotInGroupByTest.class,
    CVConcurrentMdxTest.class,
    CacheHitTest.class,
    ConcurrentMdxTest.class,
    MemHungryTest.class,
    MultiDimTest.class,
    MultiDimVCTest.class,
    MultiLevelTest.class,
    MultiLevelVCTest.class,
    PartialCacheTest.class,
    PartialCacheVCTest.class,
    QueryAllTest.class,
    QueryAllVCTest.class,
    Base64Test.class,
    // }
    SegmentBuilderTest.class,
    NativeFilterMatchingTest.class,
    RolapConnectionTest.class,
    FilteredIterableTest.class,
    IndexedValuesTest.class,
    MemoryMonitorTest.class,
    ObjectPoolTest.class,
    Ssas2005CompatibilityTest.class,
    DialectTest.class,
    ResultComparatorTest.class,
    DrillThroughTest.class,
    ScenarioTest.class,
    BasicQueryTest.class,
    SegmentCacheTest.class,
    CVBasicTest.class,
    GrandTotalTest.class,
    HangerDimensionClearViewTest.class,
    MetricFilterTest.class,
    MiscTest.class,
    PredicateFilterTest.class,
    SubTotalTest.class,
    SummaryMetricPercentTest.class,
    SummaryTest.class,
    TopBottomTest.class,
    OrderTest.class,
    CacheControlTest.class,
    MemberCacheControlTest.class,
    FunctionTest.class,
    CurrentDateMemberUdfTest.class,
    PartialSortTest.class,
    VbaTest.class,
    ExcelTest.class,
    HierarchyBugTest.class,
    ScheduleTest.class,
    UtilTestCase.class,
    PartiallyOrderedSetTest.class,
    ConcatenableListTest.class,
    Olap4jTest.class,
    SortTest.class,
//    if (isRunOnce()) {
    TestAggregationManager.class,
//    }
    VirtualCubeTest.class,
    ParameterTest.class,
    AccessControlTest.class,
    ParserTest.class,
    CustomizedParserTest.class,
    SolveOrderScopeIsolationTest.class,
    ParentChildHierarchyTest.class,
    ClosureSqlTest.class,
    Olap4jTckTest.class,
    MondrianServerTest.class,
    XmlaBasicTest.class,
    XmlaMetaDataConstraintsTest.class,
    XmlaErrorTest.class,
    XmlaExcel2000Test.class,
    XmlaExcelXPTest.class,
    XmlaExcel2007Test.class,
    XmlaCognosTest.class,
    XmlaTabularTest.class,
    XmlaTests.class,
    DynamicDatasourceXmlaServletTest.class,
    XmlaTest.class,
//    if (isRunOnce()) {
    TestCalculatedMembers.class,
//    }
    CompoundSlicerTest.class,
    RaggedHierarchyTest.class,
    NonEmptyPropertyForAllAxisTest.class,
    InlineTableTest.class,
    CompatibilityTest.class,
    CaptionTest.class,
    UdfTest.class,
    NullValueTest.class,
    NamedSetTest.class,
    PropertiesTest.class,
    MultipleHierarchyTest.class,
    I18nTest.class,
    FormatTest.class,
    ParallelTest.class,
    SchemaVersionTest.class,
    SchemaTest.class,
    HangerDimensionTest.class,
    DateTableBuilderTest.class,
    PerformanceTest.class,
    // GroupingSetQueryTest must be run before any test derived from
    // CsvDBTestCase
    GroupingSetQueryTest.class,
    CmdRunnerTest.class,
    ModulosTest.class,
    PrimeFinderTest.class,
    CellKeyTest.class,
    RolapAxisTest.class,
    CrossJoinTest.class,
//    if (Bug.BugMondrian503Fixed) {
    RolapResultTest.class,
//    }
    ConstantCalcTest.class,
    SharedDimensionTest.class,
    CellPropertyTest.class,
    QueryTest.class,
    RolapSchemaReaderTest.class,
    RolapSchemaTest.class,
    RolapCubeTest.class,
    NullMemberRepresentationTest.class,
    IgnoreUnrelatedDimensionsTest.class,
    IgnoreMeasureForNonJoiningDimensionInAggregationTest.class,
    SetFunDefTest.class,
    VisualTotalsTest.class,
    AggregationOnDistinctCountMeasuresTest.class,
    NonCollapsedAggTest.class,
    BitKeyTest.class,
    TypeTest.class,
    SteelWheelsSchemaTest.class,
    MultipleColsInTupleAggTest.class,
    DynamicSchemaProcessorTest.class,
    MonitorTest.class,
    BlockingHashMapTest.class,

//    boolean testNonEmpty = isRunOnce();
//    if (!MondrianProperties.instance().EnableNativeNonEmpty.get()) {
//    testNonEmpty = false;
//    }
//    if (!MondrianProperties.instance().EnableNativeCrossJoin.get()) {
//    testNonEmpty = false;
//    }
//    if (testNonEmpty) {
    NonEmptyTest.class,
    FilterTest.class,
//    if (Bug.BugMondrian1315Fixed) {
    NativizeSetFunDefTest.class,
//    }
//    } else {
//    logger.warn("skipping NonEmptyTests");
//    }

    FastBatchingCellReaderTest.class,
    SqlQueryTest.class,

//    if (MondrianProperties.instance().EnableNativeCrossJoin.get()) {
    BatchedFillTest.class,
//    } else {
//    logger.warn("skipping BatchedFillTests");
//    }

    // Must be the last test.
    TerminatorTest.class
})
public class MondrianSuite {
  
}

// End Util.java
