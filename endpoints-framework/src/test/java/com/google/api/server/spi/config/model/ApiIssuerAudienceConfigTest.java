package com.google.api.server.spi.config.model;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ApiIssuerAudienceConfig}.
 */
@RunWith(JUnit4.class)
public class ApiIssuerAudienceConfigTest {
  @Test
  public void isEmpty() {
    assertThat(ApiIssuerAudienceConfig.builder().build().isEmpty()).isTrue();
    assertThat(ApiIssuerAudienceConfig.UNSPECIFIED.isEmpty()).isFalse();
  }

  @Test
  public void isSpecified() {
    assertThat(ApiIssuerAudienceConfig.UNSPECIFIED.isSpecified()).isFalse();
    assertThat(ApiIssuerAudienceConfig.builder().build().isSpecified()).isTrue();
  }

  @Test
  public void asMap() {
    assertThat(ApiIssuerAudienceConfig.builder().build().asMap()).isEmpty();
    ApiIssuerAudienceConfig config = ApiIssuerAudienceConfig.builder()
        .addIssuerAudiences("issuer", "aud1", "aud2")
        .build();
    assertThat(config.asMap()).containsExactly("issuer", ImmutableSet.of("aud1", "aud2"));
  }

  @Test
  public void equals() {
    ApiIssuerAudienceConfig config1 = ApiIssuerAudienceConfig.builder()
        .addIssuerAudiences("issuer", "aud1", "aud2")
        .build();
    ApiIssuerAudienceConfig config2 = ApiIssuerAudienceConfig.builder()
        .addIssuerAudiences("issuer", "aud1", "aud2")
        .build();
    assertThat(config1).isEqualTo(config2);
    assertThat(config1).isNotEqualTo(ApiIssuerAudienceConfig.UNSPECIFIED);
  }
}
