package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiClass;
import com.google.api.server.spi.config.ApiMethod;

public class MultiVersionEndpoint {

  @Api(
      name = "myapi",
      version = "v1")
  public static class Version1Endpoint {

    public Foo get() {
      return null;
    }

  }
  
  @Api(
      name = "myapi",
      version = "v2")
  public static class Version2Endpoint {

    public Foo get() {
      return null;
    }
    
  }

}
