package com.google.api.server.spi.tools;

import com.google.api.server.spi.EndpointsServlet;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Representation of web.xml
 */
public class WebXml {

  private final Document document;

  public WebXml(Document document) {
    this.document = document;
  }

  /**
   * Create a instance of WebXml from a file.
   */
  public static WebXml parse(File webXml)
      throws IOException, SAXException, ParserConfigurationException {
    return new WebXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(webXml));
  }

  /**
   * Find endpoint service classes defined in the web.xml.
   *
   * Looks for a servlet with servlet-class = SystemServiceServlet or
   * {@link EndpointsServlet}.
   * Does NOT handle the case of multiple mappings to the same servlet-class,
   * it will only return the result to first one that defines the "services" init-param
   * Does NOT validate that the servlet has a corresponding servlet-mapping
   *
   * @return a list of endpoints service classes
   */
  public List<String> endpointsServiceClasses() {
    XPath xpath = XPathFactory.newInstance().newXPath();
    String findService = "/web-app/servlet" + "["
        + "servlet-class = 'com.google.api.server.spi.SystemServiceServlet'"
        + " or "
        + "servlet-class = '" + EndpointsServlet.class.getName() + "'"
        + "]/init-param[param-name = 'services']/param-value/text()";

    try {
      String servicesString = (String) xpath.evaluate(findService, document, XPathConstants.STRING);
      List<String> services = Lists.newArrayList();
      for (String service : servicesString.trim().split(",")) {
        if (!Strings.isNullOrEmpty(service.trim())) {
          services.add(service.trim());
        }
      }
      return services;
    }
    catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }
}
