/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2016-2016 Julian Hyde
// All Rights Reserved.
*/
package mondrian.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import mondrian.olap.Util;

import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Support for Junit version 3.
 *
 * <p>Most tests use Junit version 4 and its idioms, so few tests should
 * import this class.
 */
public class Junit3Tests {
    /**
     * Creates a predicate that accepts tests whose name matches the given
     * regular expression.
     *
     * @param regexp Test case regular expression
     * @return Predicate that accepts tests with the given name
     */
    public static Util.Predicate1<Test> patternPredicate(final String regexp) {
        final Pattern pattern = Pattern.compile(regexp);
        return new Util.Predicate1<junit.framework.Test>() {
            public boolean test(Test test) {
                if (!(test instanceof TestCase)) {
                    return true;
                }
                final TestCase testCase = (TestCase) test;
                final String testCaseName = testCase.getName();
                return pattern.matcher(testCaseName).matches();
            }
        };
    }

    /**
     * Makes a copy of a suite, filtering certain tests.
     *
     * @param suite Test suite
     * @param testPattern Regular expression of name of tests to include
     * @return copy of test suite
     */
    public static TestSuite copySuite(
        TestSuite suite,
        Util.Predicate1<junit.framework.Test> testPattern)
    {
        TestSuite newSuite = new TestSuite(suite.getName());
        copyTests(newSuite, suite, testPattern);
        return newSuite;
    }

    /**
     * Copies tests that match a given predicate into a target sourceSuite.
     *
     * @param targetSuite Target test suite
     * @param suite Source test suite
     * @param predicate Predicate that determines whether to copy a test
     */
    static void copyTests(
        TestSuite targetSuite,
        TestSuite suite,
        Util.Predicate1<junit.framework.Test> predicate)
    {
        //noinspection unchecked
        for (junit.framework.Test test : Collections.list(suite.tests())) {
        if (!predicate.test(test)) {
            continue;
        }
        if (test instanceof TestCase) {
            targetSuite.addTest(test);
        } else if (test instanceof TestSuite) {
            TestSuite subSuite = copySuite((TestSuite) test, predicate);
            if (subSuite.countTestCases() > 0) {
                targetSuite.addTest(subSuite);
            }
        } else {
            // some other kind of test
            targetSuite.addTest(test);
        }
      }
    }
}

// End Junit3Tests.java
