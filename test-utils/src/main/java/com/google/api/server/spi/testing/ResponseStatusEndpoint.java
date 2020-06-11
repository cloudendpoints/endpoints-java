package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;

@Api(name = "responseStatus", version = "v1")
public class ResponseStatusEndpoint {

  @ApiMethod
  public StringValue responseStatusUnsetReturnString() {
    return null;
  }

  @ApiMethod
  public void responseStatusUnsetReturnVoid() {
  }

  @ApiMethod(responseStatus = 201)
  public StringValue responseStatusCreatedReturnString() {
    return null;
  }

  @ApiMethod(responseStatus = 201)
  public void responseStatusCreatedReturnVoid() {
  }
}
