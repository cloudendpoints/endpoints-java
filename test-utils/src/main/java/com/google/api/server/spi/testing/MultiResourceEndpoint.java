package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiClass;
import com.google.api.server.spi.config.ApiMethod;

public class MultiResourceEndpoint {

  @Api(
      name = "multiresource",
      version = "v1")
  public static class NoResourceEndpoint {

    @ApiMethod(path = "noresource")
    public Foo get() {
      return null;
    }

  }
  
  @Api(
      name = "multiresource",
      version = "v1")
  @ApiClass(resource = "resource1")
  public static class Resource1Endpoint {

    @ApiMethod(path = "resource1")
    public Foo get() {
      return null;
    }
    
  }

  @Api(
      name = "multiresource",
      version = "v1")
  @ApiClass(resource = "resource2")
  public static class Resource2Endpoint {

    @ApiMethod(path = "resource2")
    public Foo get() {
      return null;
    }
    
  }

}
