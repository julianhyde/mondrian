/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 Julian Hyde
// Copyright (C) 2005-2011 Pentaho and others
// All Rights Reserved.
*/
package mondrian.test.comp;

import mondrian.olap.*;
import mondrian.test.FoodMartTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;

/**
 * Unit test based upon an XML file.
 *
 * <p>The file consists of an MDX statement and the expected result. The
 * executes the MDX statement and fails if the actual result does not match the
 * expected result.
 *
 * <p>Here is a typical XML file:
 * <blockquote>&lt;mdbTest&gt;<br/>
 *     &lt;mdxQuery&gt;<br/>
 *         WITH MEMBER [Customers].[Hierarchy Name]<br/>
 *             AS '[Customers].[All Customers].[USA].[CA].hierarchy.Name'<br/>
 *         SELECT {[Customers].[Hierarchy Name]} on columns<br/>
 *         From [Sales]<br/>
 *     &lt;/mdxQuery&gt;<br/>
 *     &lt;dataResult&gt;<br/>
 *         &lt;slicer&gt;<br/>
 *             &lt;dimensions&gt;<br/>
 *                 &lt;dim&gt;[Measures]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Time]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Product]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Store]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Store Size in SQFT]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Store Type]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Promotions]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Education Level]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Marital Status]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Yearly Income]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Promotion].[Media Type]&lt;/dim&gt;<br/>
 *                 &lt;dim&gt;[Gender]&lt;/dim&gt;<br/>
 *             &lt;/dimensions&gt;<br/>
 *             &lt;tuples&gt;<br/>
 *                 &lt;tuple&gt;<br/>
 *                     &lt;member&gt;[Measures].[Unit Sales]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Time].[1997]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Product].[All
 * Products]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Store].[All Stores]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Store Size in SQFT].[All Store Size in
 * SQFTs]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Store Type].[All Store
 * Types]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Promotions].[All
 * Promotions]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Education Level].[All Education
 * Levels]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Marital Status].[All Marital
 * Status]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Yearly Income].[All Yearly
 * Incomes]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Promotion].[Media Type].[All
 * Media]&lt;/member&gt;<br/>
 *                     &lt;member&gt;[Gender].[All Gender]&lt;/member&gt;<br/>
 *                 &lt;/tuple&gt;<br/>
 *             &lt;/tuples&gt;<br/>
 *         &lt;/slicer&gt;<br/>
 *         &lt;columns&gt;<br/>
 *             &lt;dimensions&gt;<br/>
 *                 &lt;dim&gt;[Customers]&lt;/dim&gt;<br/>
 *             &lt;/dimensions&gt;<br/>
 *             &lt;tuples&gt;<br/>
 *                 &lt;tuple&gt;<br/>
 *                     &lt;member&gt;[Customers].[Hierarchy
 * Name]&lt;/member&gt;<br/>
 *                 &lt;/tuple&gt;<br/>
 *             &lt;/tuples&gt;<br/>
 *         &lt;/columns&gt;<br/>
 *         &lt;data&gt;<br/>
 *             &lt;drow&gt;<br/>
 *                 &lt;cell&gt;Customers&lt;/cell&gt;<br/>
 *             &lt;/drow&gt;<br/>
 *         &lt;/data&gt;<br/>
 *     &lt;/dataResult&gt;<br/>
 * &lt;/mdbTest&gt;
 * </blockquote>
 */
@RunWith(Parameterized.class)
public class ResultComparatorTest extends FoodMartTestCase {
    @Parameterized.Parameter public String fileName;

    @Test public void runTest() throws Exception {
        DocumentBuilder db = XmlUtility.createDomParser(
            false, true, false, new XmlUtility.UtilityErrorHandler());

        Document doc = db.parse(fileName);

        Element queryNode =
            (Element) doc.getElementsByTagName("mdxQuery").item(0);
        Element expectedResult =
            (Element) doc.getElementsByTagName("dataResult").item(0);
        if (!isDefaultNullMemberRepresentation()
            && resultHasDefaultNullMemberRepresentation(expectedResult))
        {
            return;
        }
        String queryString = XmlUtility.decodeEncodedString(
            queryNode.getFirstChild().getNodeValue());

        Connection cxn = getConnection();
        try {
            Query query = cxn.parseQuery(queryString);
            Result result = cxn.execute(query);
            ResultComparator comp =
                new ResultComparator(expectedResult, result);

            comp.compareResults();
        } finally {
            cxn.close();
        }
    }

    private boolean resultHasDefaultNullMemberRepresentation(
        Element expectedResult)
    {
        return XmlUtility.toString(expectedResult).contains("#null");
    }

    @Parameterized.Parameters(name = "{index} {0}")
    public static List<Object[]> parameters() {
        MondrianProperties properties = MondrianProperties.instance();
        String filePattern = properties.QueryFilePattern.get();
        String fileDirectory = properties.QueryFileDirectory.get();

        final Pattern pattern =
            filePattern == null
                ? null
                : Pattern.compile(filePattern);
        final File directory;
        if (fileDirectory == null) {
            final File src = new File("src");
            final File test = new File(src, "test");
            final File resources = new File(test, "resources");
            directory = new File(resources, "queryFiles");
        } else {
            directory = new File(fileDirectory);
        }

        File[] files = directory.listFiles(
            new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith("query")
                        && name.endsWith(".xml")
                        && (pattern == null
                            || pattern.matcher(name).matches());
                }
            });
        if (files == null) {
            files = new File[0];
        }
        List<Object[]> list = new ArrayList<Object[]>();
        for (File file : files) {
            list.add(new Object[]{relative(new File(""), file)});
        }
        return list;
    }

    private static String relative(File directory, File file) {
        final String f = file.getAbsolutePath();
        final String d = directory.getAbsolutePath() + File.separator;
        if (f.startsWith(d)) {
            return f.substring(d.length());
        }
        return f;
    }
}

// End ResultComparatorTest.java
