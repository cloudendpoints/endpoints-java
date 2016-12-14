package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

/**
 * API endpoint for testing namespace configuration.
 */
@Api(
    name = "namespace",
    version = "v1",
    namespace = @ApiNamespace(ownerDomain = "domain", ownerName = "name", packagePath = "path"))
public class NamespaceEndpoint {
  @ApiMethod
  public void empty() { }
}
