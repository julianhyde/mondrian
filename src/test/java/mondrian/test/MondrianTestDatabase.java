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

import java.util.Properties;

/** Collection of database instance names. */
public enum MondrianTestDatabase {
    HSQLDB,
    MYSQL;

    private static final Properties DEFAULT_PROPERTIES = new Properties();

    static {
        final String[] strings = {
            "mondrian.foodmart.hsqldb.jdbcURL", "jdbc:hsqldb:res:foodmart",
            "mondrian.foodmart.hsqldb.jdbcUser", "FOODMART",
            "mondrian.foodmart.hsqldb.jdbcPassword", "FOODMART",
            "mondrian.foodmart.hsqldb.jdbcDriver", "org.hsqldb.jdbcDriver",
            "mondrian.steelwheels.hsqldb.jdbcURL", "jdbc:hsqldb:res:steelwheels",
            "mondrian.steelwheels.hsqldb.jdbcUser", "STEELWHEELS",
            "mondrian.steelwheels.hsqldb.jdbcPassword", "STEELWHEELS",
            "mondrian.steelwheels.hsqldb.jdbcDriver", "org.hsqldb.jdbcDriver",
            "mondrian.foodmart.mysql.jdbcURL", "jdbc:mysql://localhost/foodmart",
            "mondrian.foodmart.mysql.jdbcUser", "foodmart",
            "mondrian.foodmart.mysql.jdbcPassword", "foodmart",
            "mondrian.foodmart.mysql.jdbcDriver", "com.mysql.jdbc.Driver",
        };
        for (int i = 0; i < strings.length; i += 2) {
            if (strings[i + 1] != null) {
                DEFAULT_PROPERTIES.setProperty(strings[i], strings[i + 1]);
            }
        }
    }

    public DbSpec spec(TestContext.DataSet dataSet, Properties properties) {
        String base = "mondrian." + dataSet.name().toLowerCase()
            + "." + name().toLowerCase();
        return new DbSpec(get(properties, DEFAULT_PROPERTIES, base + ".jdbcURL"),
            get(properties, DEFAULT_PROPERTIES, base + ".jdbcUser"),
            get(properties, DEFAULT_PROPERTIES, base + ".jdbcPassword"),
            get(properties, DEFAULT_PROPERTIES, base + ".jdbcDriver"));
    }

    private String get(Properties properties, Properties defaultProperties, String name) {
        String s = properties.getProperty(name);
        if (s == null) {
            s = defaultProperties.getProperty(name);
        }
        return s;
    }
}

// End MondrianTestDatabase.java
