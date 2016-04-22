/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2009-2009 Pentaho and others
// All Rights Reserved.
*/
package mondrian.spi.impl;

import mondrian.olap.Util;

import java.sql.*;

/**
 * Implementation of {@link mondrian.spi.Dialect} for the Hsqldb database.
 *
 * <p>We assume that you are using version 2.0 and higher, which includes the
 * VALUES keyword.
 *
 * @author wgorman
 * @since Aug 20, 2009
 */
public class HsqldbDialect extends JdbcDialectImpl {

    public static final JdbcDialectFactory FACTORY =
        new JdbcDialectFactory(
            HsqldbDialect.class,
            DatabaseProduct.HSQLDB);

    /**
     * Creates an HsqldbDialect.
     *
     * @param connection Connection
     */
    public HsqldbDialect(Connection connection) throws SQLException {
        super(connection);
    }

    protected void quoteDateLiteral(
        StringBuilder buf,
        String value,
        Date date)
    {
        // Hsqldb accepts '2008-01-23' but not SQL:2003 format.
        Util.singleQuoteString(value, buf);
    }

}

// End HsqldbDialect.java
