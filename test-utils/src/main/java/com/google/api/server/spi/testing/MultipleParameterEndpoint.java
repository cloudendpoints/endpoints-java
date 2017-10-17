package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;

/**
 * This endpoint is used for testing that named parameters are output in order.
 */
@Api(name = "multipleparam", version = "v1")
public class MultipleParameterEndpoint {
  @ApiMethod(name = "param", path = "param/{parent}/{child}")
  public void param(
      @Named("parent") String parent,
      @Named("query") @Nullable String query,
      @Named("child") String child) { }
}
