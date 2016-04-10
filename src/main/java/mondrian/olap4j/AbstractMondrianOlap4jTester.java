/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2010-2016 Pentaho
// All Rights Reserved.
*/
package mondrian.olap4j;

import mondrian.olap.MondrianProperties;

import org.olap4j.test.TestContext;
import org.olap4j.test.TestEnv;

import java.sql.*;
import java.util.Properties;

/**
 * Abstract implementation of the {@link org.olap4j.test.TestContext.Tester}
 * callback required by olap4j's Test Compatibility Kit (TCK).
 *
 * @author Julian Hyde
 */
abstract class AbstractMondrianOlap4jTester implements TestContext.Tester {
    private final TestEnv env;
    private final String driverUrlPrefix;
    private final String driverClassName;
    private final Flavor flavor;

    protected AbstractMondrianOlap4jTester(
        TestEnv env,
        String driverUrlPrefix,
        String driverClassName,
        Flavor flavor)
    {
        this.env = env;
        this.driverUrlPrefix = driverUrlPrefix;
        this.driverClassName = driverClassName;
        this.flavor = flavor;
    }

    public TestEnv env() {
        return env;
    }

    public Connection createConnection() throws SQLException {
        try {
            Class.forName(getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("oops", e);
        }
        return
            DriverManager.getConnection(
                getURL(),
                new Properties());
    }

    public Connection createConnectionWithUserPassword() throws SQLException
    {
        return DriverManager.getConnection(
            getURL(), USER, PASSWORD);
    }

    public String getDriverUrlPrefix() {
        return driverUrlPrefix;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getURL() {
        // This property is usually defined in build.properties. See
        // examples in that file.
        return env.getProperties().getProperty(
            TestContext.Property.CONNECT_URL.path);
    }

    public Flavor getFlavor() {
        return flavor;
    }

    public void setTimeout(int seconds) {
        MondrianProperties.instance().QueryTimeout.set(seconds);
    }

    public TestContext.Wrapper getWrapper() {
        return TestContext.Wrapper.NONE;
    }

    private static final String USER = "sa";
    private static final String PASSWORD = "sa";
}

// End AbstractMondrianOlap4jTester.java
