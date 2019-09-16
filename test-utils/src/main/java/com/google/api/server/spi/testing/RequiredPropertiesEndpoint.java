package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;

@Api(
    name = "requiredProperties",
    version = "v1",
    title = "API to test required properties")
public class RequiredPropertiesEndpoint {

  public RequiredProperties getRequiredProperties() {
    return null;
  }
  
}
