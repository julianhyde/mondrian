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

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.core.CombinableMatcher;
import org.junit.Assert;

import junit.framework.TestSuite;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Temporary; remove before committing to lagunitas.
 *
 * <p>Search for uses of:<ul>
 * <li>{@link String#indexOf(String)}
 * <li>{@link junit.framework.Assert}
 * <li>{@link junit.framework.Assert#assertNull}
 * <li>{@link junit.framework.AssertionFailedError}
 * <li>{@link junit.framework.Test}
 * <li>{@link junit.framework.TestCase}
 * <li>{@link junit.framework.TestCase} constructors
 * <li>methods that return {@link TestSuite} especitally "suite()"
 * <li>"Testcase"
 * <li>test names prefixed "_test" and "disabled_"
 * </ul>
 */
public class TestTemp {

  /* */
  private static <T> void assertEquals(T expected, T actual) {
    assertThat(actual, is(expected));
  }
  private static <T> void assertEquals(String s, T expected, T actual) {
    assertThat(s, actual, is(expected));
  }
  private static void assertEquals(double expected, double actual, double delta) {
    assertThat(actual, TestTemp.range(expected, delta));
  }
  private static void assertEquals(String s, double expected, double actual, double delta) {
    assertThat(s, actual, TestTemp.range(expected, delta));
  }

  private static <T> void assertSame(T expected, T actual) {
    assertThat(actual, sameInstance(expected));
  }
  private static <T> void assertNotSame(T expected, T actual) {
    assertThat(actual, not(sameInstance(expected)));
  }
  private static void assertTrue(boolean b) {
    assertThat(b, is(true));
  }
  private static void assertTrue(String s, boolean b) {
    assertThat(s, b, is(true));
  }
  private static void assertFalse(boolean b) {
    assertThat(b, is(false));
  }
  private static void assertFalse(String s, boolean b) {
    assertThat(s, b, is(false));
  }
  private static <T> void assertNull(T t) {
    assertThat(t, nullValue());
  }
  private static <T> void assertNull(String s, T t) {
    assertThat(s, t, nullValue());
  }
  private static <T> void assertNotNull(T t) {
    assertThat(t, notNullValue());
  }
  private static <T> void assertNotNull(String s, T t) {
    assertThat(s, t, notNullValue());
  }
  private static void fail(String t) {
    Assert.fail(t);
  }

 /* */

  public static TypeSafeDiagnosingMatcher<Double> range(
      final double expected, final double delta) {
    return new TypeSafeDiagnosingMatcher<Double>(Double.class) {
      protected boolean matchesSafely(Double item, Description mismatchDescription) {
        return item == expected
               || item >= expected - delta
                  && item <= expected + delta;
      }

      public void describeTo(Description description) {
        description.appendText("equal to ").appendValue(expected)
            .appendText(" (within ").appendValue(delta).appendText(")");
      }
    };
  }

}

// End Util.java
