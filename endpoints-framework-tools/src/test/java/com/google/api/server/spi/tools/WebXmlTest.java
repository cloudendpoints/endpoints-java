package com.google.api.server.spi.tools;

import com.google.api.server.spi.EndpointsServlet;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

@RunWith(JUnit4.class)
public class WebXmlTest {

  @Test
  public void testServiceClasses_singleServiceSystemServiceServlet()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .systemServiceServlet("com.test.EndpointOne")
        .systemServiceServletMapping()
        .build();
    List<String> serviceClasses = webxml.endpointsServiceClasses();
    List<String> expected = Collections.singletonList("com.test.EndpointOne");
    Assert.assertEquals(expected, serviceClasses);
  }

  @Test
  public void testServiceClasses_singleServiceEndpointsServlet()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .endpointsServlet("com.test.EndpointOne")
        .endpointsServletMapping()
        .build();

    List<String> serviceClasses = webxml.endpointsServiceClasses();
    List<String> expected = Collections.singletonList("com.test.EndpointOne");
    Assert.assertEquals(expected, serviceClasses);
  }

  @Test
  public void testServiceClasses_multipleServicesSystemServiceServlet()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .systemServiceServlet("com.test.EndpointOne, com.test.EndpointTwo")
        .systemServiceServletMapping()
        .build();

    List<String> serviceClasses = webxml.endpointsServiceClasses();
    List<String> expected = Arrays.asList("com.test.EndpointOne", "com.test.EndpointTwo");
    Assert.assertEquals(expected, serviceClasses);

  }
  @Test
  public void testServiceClasses_multipleServicesEndpointsServlet()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .endpointsServlet("com.test.EndpointOne, com.test.EndpointTwo")
        .endpointsServletMapping()
        .build();

    List<String> serviceClasses = webxml.endpointsServiceClasses();
    List<String> expected = Arrays.asList("com.test.EndpointOne", "com.test.EndpointTwo");
    Assert.assertEquals(expected, serviceClasses);
  }

  @Test
  public void testServiceClasses_noServices()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .systemServiceServlet("")
        .systemServiceServletMapping()
        .build();
    List<String> serviceClasses = webxml.endpointsServiceClasses();
    Assert.assertEquals(0, serviceClasses.size());
  }

  @Test
  public void testServiceClasses_badSpacing()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .systemServiceServlet("   com.test.EndpointOne, \n    com.test.EndpointTwo     ")
        .systemServiceServletMapping()
        .build();

    List<String> serviceClasses = webxml.endpointsServiceClasses();
    List<String> expected = Arrays.asList("com.test.EndpointOne", "com.test.EndpointTwo");
    Assert.assertEquals(expected, serviceClasses);
  }

  @Test
  public void testServiceClasses_noneWithBadSpacing()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder().systemServiceServlet("    , \n,    ")
        .systemServiceServletMapping().build();
    List<String> serviceClasses = webxml.endpointsServiceClasses();
    Assert.assertEquals("Expecting nothing, found : " + serviceClasses, 0, serviceClasses.size());
  }

  @Test
  public void testServiceClasses_emptyParamValueTag()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .raw(" <servlet>"
            + "   <servlet-name>SystemServiceServlet</servlet-name>"
            + "   <servlet-class>com.google.api.server.spi.SystemServiceServlet</servlet-class>"
            + "   <init-param>"
            + "     <param-name>services</param-name>"
            + "     <param-value/>"
            + "   </init-param>"
            + " </servlet>")
        .systemServiceServletMapping().build();
    List<String> serviceClasses = webxml.endpointsServiceClasses();
    Assert.assertEquals("Expecting nothing, found : " + serviceClasses, 0, serviceClasses.size());
  }

  @Test
  public void testServiceClasses_noInitParams()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .raw(" <servlet>"
            + "   <servlet-name>SystemServiceServlet</servlet-name>"
            + "   <servlet-class>com.google.api.server.spi.SystemServiceServlet</servlet-class>"
            + " </servlet>")
        .systemServiceServletMapping().build();

    List<String> serviceClasses = webxml.endpointsServiceClasses();
    Assert.assertEquals("Expecting nothing, found : " + serviceClasses, 0, serviceClasses.size());
  }

  @Test
  public void testServiceClasses_noMatchingTag()
      throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

    WebXml webxml = new WebXmlBuilder()
        .servlet("HelloServlet", "com.example.Hello", "com.test.HelloService")
        .servletMapping("HelloServlet", "/hello")
        .systemServiceServletMapping().build();

    List<String> serviceClasses = webxml.endpointsServiceClasses();
    Assert.assertEquals("Expecting nothing, found : " + serviceClasses, 0, serviceClasses.size());
  }

  /**
   * Expected behavior for undefined situations (pick first)
   */
  @Test
  public void testServiceClasses_undefinedBehaviorMultipleSystemServiceServlets()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .servlet("SomeJunkServlet", "com.google.api.server.spi.SystemServiceServlet",
            "com.test.JunkEndpoint")
        .endpointsServlet("com.test.EndpointOne")
        .servletMapping("SomeJunkServlet", WebXmlBuilder.SPI_URL)
        .endpointsServletMapping()
        .build();

    List<String> serviceClasses = webxml.endpointsServiceClasses();
    Assert.assertEquals(1, serviceClasses.size());
    Assert.assertEquals(serviceClasses.get(0), "com.test.JunkEndpoint");
  }

  /**
   * Expected behavior for undefined situations (pick first)
   */
  @Test
  public void testServiceClasses_undefinedBehaviorMultipleMatchingServlets()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .endpointsServlet("com.test.EndpointsServletEndpoint")
        .systemServiceServlet("com.test.SystemServiceServletEndpoint")
        .endpointsServletMapping()
        .systemServiceServletMapping()
        .build();

    List<String> serviceClasses = webxml.endpointsServiceClasses();
    Assert.assertEquals(1, serviceClasses.size());
    Assert.assertEquals(serviceClasses.get(0), "com.test.EndpointsServletEndpoint");
  }

  /**
   * Expected behavior for undefined situations (pick first)
   */
  @Test
  public void testServiceClasses_undefinedBehaviorMultipleMatchingServlets2()
      throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    WebXml webxml = new WebXmlBuilder()
        .systemServiceServlet("com.test.SystemServiceServletEndpoint")
        .endpointsServlet("com.test.EndpointsServletEndpoint")
        .endpointsServletMapping()
        .systemServiceServletMapping()
        .build();

    List<String> serviceClasses = webxml.endpointsServiceClasses();
    Assert.assertEquals(1, serviceClasses.size());
    Assert.assertEquals(serviceClasses.get(0), "com.test.SystemServiceServletEndpoint");
  }

  static class WebXmlBuilder {

    static final String SPI_URL = "/_ah/spi";
    static final String API_URL = "/_ah/api";
    static final String SYSTEM_SERVICE_SERVLET = "SystemServiceServlet";
    static final String SYSTEM_SERVICE_SERVLET_CLASS =
        "com.google.api.server.spi.SystemServiceServlet";
    static final String ENDPOINTS_SERVLET = "EndpointsServlet";
    static final String ENDPOINTS_SERVLET_CLASS = EndpointsServlet.class.getName();

    StringBuilder webxml = new StringBuilder();

    public WebXmlBuilder() {
     webxml.append("<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\""
          + "   xmlns:web=\"http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\""
          + "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
          + "   version=\"2.5\""
          + "   xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee"
          + "       http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\">");
    }

    public WebXmlBuilder servlet(String name, String clazz, String services) {
      webxml.append("<servlet>")
          .append("<servlet-name>").append(name).append("</servlet-name>")
          .append("<servlet-class>").append(clazz).append("</servlet-class>")
          .append("<init-param>")
          .append("<param-name>services</param-name>")
          .append("<param-value>").append(services).append("</param-value>")
          .append("</init-param>")
          .append("</servlet>");
      return this;
    }

    public WebXmlBuilder systemServiceServlet(String services) {
      return servlet(SYSTEM_SERVICE_SERVLET, SYSTEM_SERVICE_SERVLET_CLASS, services);
    }

    public WebXmlBuilder endpointsServlet(String services) {
      return servlet(ENDPOINTS_SERVLET, ENDPOINTS_SERVLET_CLASS, services);
    }

    public WebXmlBuilder servletMapping(String name, String urlPattern) {
      webxml.append("<servlet-mapping>")
          .append("<servlet-name>").append(name).append("</servlet-name>")
          .append("<url-pattern>").append(urlPattern).append("</url-pattern>")
          .append("</servlet-mapping>");
      return this;
    }

    public WebXmlBuilder systemServiceServletMapping() {
      return servletMapping(SYSTEM_SERVICE_SERVLET, SPI_URL);
    }

    public WebXmlBuilder endpointsServletMapping() {
      return servletMapping(ENDPOINTS_SERVLET, API_URL);
    }

    public WebXmlBuilder raw(String raw) {
      webxml.append(raw);
      return this;
    }

    public WebXml build() throws ParserConfigurationException, IOException, SAXException {
      webxml.append("</web-app>");
      return new WebXml(DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .parse(new InputSource(new StringReader(webxml.toString()))));
    }
  }
}
