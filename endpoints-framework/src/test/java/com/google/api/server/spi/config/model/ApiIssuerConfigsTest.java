package com.google.api.server.spi.config.model;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.config.model.ApiIssuerConfigs.IssuerConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ApiIssuerConfigs}.
 */
@RunWith(JUnit4.class)
public class ApiIssuerConfigsTest {
  @Test
  public void isSpecified() {
    assertThat(ApiIssuerConfigs.builder().build().isSpecified()).isTrue();
    assertThat(ApiIssuerConfigs.UNSPECIFIED.isSpecified()).isFalse();
  }

  @Test
  public void asMap() {
    assertThat(ApiIssuerConfigs.builder().build().asMap()).isEmpty();
    ApiIssuerConfigs configs = ApiIssuerConfigs.builder()
        .addIssuer(new IssuerConfig("issuerName", "issuer", "jwks", "authUrl", true))
        .build();
    assertThat(configs.asMap()).containsExactly(
        "issuerName", new IssuerConfig("issuerName", "issuer", "jwks", "authUrl", true));
  }

  @Test
  public void equals() {
    ApiIssuerConfigs configs1 = ApiIssuerConfigs.builder()
        .addIssuer(new IssuerConfig("issuerName", "issuer", "jwks", "authUrl", true))
        .build();
    ApiIssuerConfigs configs2 = ApiIssuerConfigs.builder()
        .addIssuer(new IssuerConfig("issuerName", "issuer", "jwks", "authUrl", true))
        .build();
    assertThat(configs1).isEqualTo(configs2);
    assertThat(configs1).isNotEqualTo(ApiIssuerConfigs.UNSPECIFIED);
  }
}
