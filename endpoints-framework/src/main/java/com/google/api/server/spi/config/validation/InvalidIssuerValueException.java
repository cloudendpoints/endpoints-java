package com.google.api.server.spi.config.validation;

import com.google.api.server.spi.config.model.ApiClassConfig;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiMethodConfig;

/**
 * Exception types revolving around invalid issuer values.
 */
public final class InvalidIssuerValueException {
  private InvalidIssuerValueException() { }

  public static class ForApi extends ApiConfigInvalidException {
    public ForApi(ApiConfig config, String message) {
      super(config, message);
    }
  }

  public static class ForApiClass extends ApiClassConfigInvalidException {
    public ForApiClass(ApiClassConfig config, String message) {
      super(config, message);
    }
  }

  public static class ForApiMethod extends ApiMethodConfigInvalidException {
    public ForApiMethod(ApiMethodConfig config, String message) {
      super(config, message);
    }
  }
}
