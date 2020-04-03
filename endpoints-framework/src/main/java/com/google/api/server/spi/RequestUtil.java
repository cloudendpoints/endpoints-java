package com.google.api.server.spi;

import javax.servlet.http.HttpServletRequest;

public class RequestUtil {

  public static String getOriginalRequestUrl(HttpServletRequest req) {
    String protocolHeader = req.getHeader("X-Forwarded-Proto");
    String requestUrl = stripRedundantPorts(req.getRequestURL().toString());
    if (protocolHeader != null && protocolHeader.equalsIgnoreCase("https")) {
      requestUrl = requestUrl.replaceFirst("^http:", "https:");
    }
    return requestUrl;
  }

  private static String stripRedundantPorts(String url) {
    if (url == null) {
      return null;
    } else if (url.startsWith("http:") && url.contains(":80/")) {
      return url.replace(":80/", "/");
    } else if (url.startsWith("https:") && url.contains(":443/")) {
      return url.replace(":443/", "/");
    }
    return url;
  }

}
