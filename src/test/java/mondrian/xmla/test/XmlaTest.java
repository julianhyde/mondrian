/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.xmla.test;

import mondrian.olap.*;
import mondrian.server.StringRepositoryContentFinder;
import mondrian.test.DiffRepository;
import mondrian.test.TestContext;
import mondrian.xmla.*;
import mondrian.xmla.impl.DefaultXmlaRequest;
import mondrian.xmla.impl.DefaultXmlaResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import junit.framework.*;

import org.apache.log4j.Logger;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.w3c.dom.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for refined Mondrian's XML for Analysis API (package
 * {@link mondrian.xmla}).
 *
 * @author Gang Chen
 */
@RunWith(Parameterized.class)
public class XmlaTest {
    @Rule public final TestName name = new TestName();

    @Parameterized.Parameter public String testCaseName;

    private static final Logger LOGGER = Logger.getLogger(XmlaTest.class);

    static {
        XMLUnit.setControlParser(
            "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        XMLUnit.setTestParser(
            "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        XMLUnit.setIgnoreWhitespace(true);
    }

    private static final XmlaTestContext context = new XmlaTestContext();

    private XmlaHandler handler;
    private MondrianServer server;

    @Before public void setUp() throws Exception {
        DiffRepository diffRepos = getDiffRepos();
        diffRepos.setCurrentTestCaseName(testCaseName);
        server = MondrianServer.createWithRepository(
            new StringRepositoryContentFinder(
                context.getDataSourcesString()),
            XmlaTestContext.CATALOG_LOCATOR);
        handler = new XmlaHandler(
            (XmlaHandler.ConnectionFactory) server,
            "xmla");
        XMLUnit.setIgnoreWhitespace(false);
    }

    @After public void tearDown() throws Exception {
        DiffRepository diffRepos = getDiffRepos();
        diffRepos.setCurrentTestCaseName(null);
        server.shutdown();
        server = null;
        handler = null;
    }

    private static DiffRepository getDiffRepos() {
        return DiffRepository.lookup(XmlaTest.class);
    }

    @Test public void runTest() throws Exception {
        DiffRepository diffRepos = getDiffRepos();
        String request = diffRepos.expand(null, "${request}");
        String expectedResponse = diffRepos.expand(null, "${response}");
        Element requestElem = XmlaUtil.text2Element(
            XmlaTestContext.xmlFromTemplate(
                request, XmlaTestContext.ENV));
        Element responseElem =
            ignoreLastUpdateDate(executeRequest(requestElem));

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        StringWriter bufWriter = new StringWriter();
        transformer.transform(
            new DOMSource(responseElem), new StreamResult(bufWriter));
        bufWriter.write(Util.nl);
        String actualResponse =
            TestContext.instance().upgradeActual(
                bufWriter.getBuffer().toString());
        try {
            // Start with a purely logical XML diff to avoid test noise
            // from non-determinism in XML generation.
            XMLAssert.assertXMLEqual(expectedResponse, actualResponse);
        } catch (AssertionFailedError e) {
            // In case of failure, re-diff using DiffRepository's comparison
            // method. It may have noise due to physical vs logical structure,
            // but it will maintain the expected/actual, and some IDEs can even
            // display visual diffs.
            diffRepos.assertEquals("response", "${response}", actualResponse);
        }
    }

    private Element ignoreLastUpdateDate(Element element) {
        NodeList elements = element.getElementsByTagName("LAST_SCHEMA_UPDATE");
        for (int i = elements.getLength(); i > 0; i--) {
            blankNode(elements.item(i - 1));
        }
        return element;
    }

    private void blankNode(Node node) {
        node.setTextContent("");
    }

    private Element executeRequest(Element requestElem) {
        ByteArrayOutputStream resBuf = new ByteArrayOutputStream();
        XmlaRequest request =
            new DefaultXmlaRequest(requestElem, null, null, null, null);
        XmlaResponse response =
            new DefaultXmlaResponse(
                resBuf, "UTF-8", Enumeration.ResponseMimeType.SOAP);
        handler.process(request, response);

        return XmlaUtil.stream2Element(
            new ByteArrayInputStream(resBuf.toByteArray()));
    }

    @Parameterized.Parameters(name = "{index} {0}")
    public static List<Object[]> parameters() {
        DiffRepository diffRepos = getDiffRepos();
        List<String> testCaseNames = diffRepos.getTestCaseNames();
        LOGGER.debug("Found " + testCaseNames.size() + " XML/A test cases");

        MondrianProperties properties = MondrianProperties.instance();
        String filePattern = properties.QueryFilePattern.get();

        final List<Object[]> list = new ArrayList<Object[]>();
        if (filePattern == null) {
            for (String name : testCaseNames) {
                list.add(new Object[] {name});
            }
        } else {
            final Pattern pattern = Pattern.compile(filePattern);
            for (String name : testCaseNames) {
                if (pattern.matcher(name).matches()) {
                    list.add(new Object[] {name});
                }
            }
        }
        return list;
    }

    /**
     * Non diff-based unit tests for XML/A support.
     */
    public static class OtherTest {
        @Test public void testEncodeElementName() {
            final XmlaUtil.ElementNameEncoder encoder =
                XmlaUtil.ElementNameEncoder.INSTANCE;

            assertThat(encoder.encode("Foo"), is("Foo"));
            assertThat(encoder.encode("Foo Bar"), is("Foo_x0020_Bar"));

            if (false) // FIXME:
                assertThat(encoder.encode("Foo_Bar"), is("Foo_x00xx_Bar"));

            // Caching: decode same string, get same string back
            final String s1 = encoder.encode("Abc def");
            final String s2 = encoder.encode("Abc def");
            assertThat(s2, sameInstance(s1));
        }

        /**
         * Unit test for {@link XmlaUtil#chooseResponseMimeType(String)}.
         */
        @Test public void testAccept() {
            // simple
            assertThat(XmlaUtil.chooseResponseMimeType("application/xml"),
                is(Enumeration.ResponseMimeType.SOAP));

            // deal with ",q=<n>" quality codes by ignoring them
            assertThat(
                XmlaUtil.chooseResponseMimeType("text/html,"
                    + "application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"),
                is(Enumeration.ResponseMimeType.SOAP));

            // return null if nothing matches
            assertThat(
                XmlaUtil.chooseResponseMimeType("text/html,"
                    + "application/xhtml+xml"), nullValue());

            // quality codes all over the place; return JSON because we see
            // it before application/xml
            assertThat(
                XmlaUtil.chooseResponseMimeType("text/html;q=0.9,"
                    + "application/xhtml+xml;q=0.9,"
                    + "application/json;q=0.9,"
                    + "application/xml;q=0.9,"
                    + "*/*;q=0.8"),
                is(Enumeration.ResponseMimeType.JSON));

            // allow application/soap+xml as synonym for application/xml
            assertThat(
                XmlaUtil.chooseResponseMimeType("text/html,"
                    + "application/soap+xml"),
                is(Enumeration.ResponseMimeType.SOAP));

            // allow text/xml as synonym for application/xml
            assertThat(
                XmlaUtil.chooseResponseMimeType("text/html,"
                    + "application/soap+xml"),
                is(Enumeration.ResponseMimeType.SOAP));
        }
    }
}

// End XmlaTest.java
