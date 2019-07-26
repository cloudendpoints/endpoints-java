package com.google.api.server.spi.config.model;

import com.google.api.server.spi.Constant;
import com.google.common.collect.ImmutableMap;

import java.util.Objects;

/**
 * Encapsulates a set of issuer configurations.
 */
public class ApiIssuerConfigs {
  static final String UNSPECIFIED_NAME = "_unspecified_issuer_name";

  //according to https://developers.google.com/identity/protocols/OpenIDConnect#server-flow
  //issuer can be either with or without https:// prefix
  public static final IssuerConfig GOOGLE_ID_TOKEN_ISSUER = new IssuerConfig(
      Constant.GOOGLE_ID_TOKEN_NAME, "https://accounts.google.com",
      Constant.GOOGLE_JWKS_URI,
      Constant.GOOGLE_AUTH_URL, true);
  public static final IssuerConfig GOOGLE_ID_TOKEN_ISSUER_ALT = new IssuerConfig(
      Constant.GOOGLE_ID_TOKEN_ALT, "accounts.google.com",
      Constant.GOOGLE_JWKS_URI,
      Constant.GOOGLE_AUTH_URL, true);
  public static final ApiIssuerConfigs UNSPECIFIED = builder()
      .addIssuer(new IssuerConfig(UNSPECIFIED_NAME, null, null, "", false))
      .build();
  public static final ApiIssuerConfigs EMPTY = builder().build();
  private final ImmutableMap<String, IssuerConfig> issuerConfigs;

  private ApiIssuerConfigs(Builder builder) {
    issuerConfigs = builder.issuerConfigs.build();
  }

  public ImmutableMap<String, IssuerConfig> asMap() {
    return issuerConfigs;
  }

  public boolean hasIssuer(String issuer) {
    return issuerConfigs.containsKey(issuer);
  }

  public IssuerConfig getIssuer(String issuer) {
    return issuerConfigs.get(issuer);
  }

  public boolean isSpecified() {
    return !this.equals(UNSPECIFIED);
  }

  public ApiIssuerConfigs withGoogleIdToken() {
    if (hasIssuer(Constant.GOOGLE_ID_TOKEN_NAME) && hasIssuer(Constant.GOOGLE_ID_TOKEN_ALT)) {
      return this;
    }
    Builder builder = builder();
    if (isSpecified()) {
      builder.issuerConfigs.putAll(issuerConfigs);
    }
    builder.addIssuer(GOOGLE_ID_TOKEN_ISSUER);
    builder.addIssuer(GOOGLE_ID_TOKEN_ISSUER_ALT);
    return builder.build();
  }

  @Override
  public boolean equals(Object o) {
    return o != null && o instanceof ApiIssuerConfigs
        && issuerConfigs.equals(((ApiIssuerConfigs) o).issuerConfigs);
  }

  @Override
  public int hashCode() {
    return issuerConfigs.hashCode();
  }

  /**
   * Represents a single issuer configuration.
   */
  public static class IssuerConfig {
    private final String name;
    private final String issuer;
    private final String jwksUri;
    private final String authorizationUrl;
    private final boolean useScopesInAuthFlow;

    public IssuerConfig(String name, String issuer, String jwksUri, String authorizationUrl, 
        boolean useScopesInAuthFlow) {
      this.name = name;
      this.issuer = issuer;
      this.jwksUri = jwksUri;
      this.authorizationUrl = authorizationUrl;
      this.useScopesInAuthFlow = useScopesInAuthFlow;
    }

    public String getName() {
      return name;
    }

    public String getIssuer() {
      return issuer;
    }

    public String getJwksUri() {
      return jwksUri;
    }

    public String getAuthorizationUrl() {
      return authorizationUrl;
    }

    public boolean isUseScopesInAuthFlow() {
      return useScopesInAuthFlow;
    }

    @Override
    public boolean equals(Object o) {
      return o != null && o instanceof IssuerConfig
          && Objects.equals(name, ((IssuerConfig) o).name)
          && Objects.equals(issuer, ((IssuerConfig) o).issuer)
          && Objects.equals(jwksUri, ((IssuerConfig) o).jwksUri)
          && Objects.equals(authorizationUrl, ((IssuerConfig) o).authorizationUrl)
          && Objects.equals(useScopesInAuthFlow, ((IssuerConfig) o).useScopesInAuthFlow);
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final ImmutableMap.Builder<String, IssuerConfig> issuerConfigs;

    public Builder() {
      issuerConfigs = ImmutableMap.builder();
    }

    public Builder addIssuer(IssuerConfig issuer) {
      issuerConfigs.put(issuer.getName(), issuer);
      return this;
    }

    public ApiIssuerConfigs build() {
      return new ApiIssuerConfigs(this);
    }
  }
}
