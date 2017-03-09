package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;

/**
 * Testing for API methods that have absolute paths.
 */
@Api(name = "absolutepath", version = "v1")
public class AbsolutePathEndpoint {
  @ApiMethod(name = "create", path = "create")
  public Foo createFoo() {
    return null;
  }

  @ApiMethod(name = "absolutepath", path = "/absolutepathmethod/v1")
  public void absolutePath() { }

  @ApiMethod(name = "absolutepath2", path = "/absolutepathmethod2/v1")
  public void absolutePath2() { }
}
