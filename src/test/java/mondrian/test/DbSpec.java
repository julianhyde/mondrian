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

/** Database specification.
 *
 * <p>Contains all the information necessary to connect to a database. */
public class DbSpec {
    public final String url;
    public final String user;
    public final String password;
    public final String driver;

    /** Creates a DbSpec. */
    public DbSpec(String url, String user, String password, String driver) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.driver = driver;
    }
}

// End DbSpec.java
