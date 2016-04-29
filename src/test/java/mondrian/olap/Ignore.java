/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2016-2016 Pentaho and others
// All Rights Reserved.
*/
package mondrian.olap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that a test is ignored.
 * Will be removed when we upgrade to junit version 4.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD) //can use in method only.
public @interface Ignore {
    String value() default "";
    String reason() default "";
}

// End Ignore.java
