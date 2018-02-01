/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.server.spi.config.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.auth.EndpointsPeerAuthenticator;
import com.google.api.server.spi.auth.GoogleJwtAuthenticator;
import com.google.api.server.spi.config.ApiConfigInconsistency;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.testing.DumbSerializer1;
import com.google.api.server.spi.testing.FloatToStringSerializer;
import com.google.api.server.spi.testing.IntegerToStringSerializer;
import com.google.api.server.spi.testing.LongToStringSerializer;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

/** Unit tests for {@link ApiConfig}. */
@RunWith(JUnit4.class)
public class ApiConfigTest {

  private ServiceContext serviceContext;
  private TypeLoader typeLoader;
  private ApiConfig apiConfig;
  private ApiConfig apiConfig2;

  @Before
  public void setUp() throws Exception {
    serviceContext = ServiceContext.create();
    typeLoader = new TypeLoader();

    apiConfig = new ApiConfig.Factory().create(serviceContext, typeLoader, TestEndpoint.class);
    apiConfig.getSerializationConfig().addSerializationConfig(IntegerToStringSerializer.class);
    apiConfig.getSerializationConfig().addSerializationConfig(LongToStringSerializer.class);
    apiConfig.getSerializationConfig().addSerializationConfig(FloatToStringSerializer.class);

    apiConfig2 = new ApiConfig.Factory().create(serviceContext, typeLoader, TestEndpoint.class);
    apiConfig2.getSerializationConfig().addSerializationConfig(IntegerToStringSerializer.class);
    apiConfig2.getSerializationConfig().addSerializationConfig(LongToStringSerializer.class);
    apiConfig2.getSerializationConfig().addSerializationConfig(FloatToStringSerializer.class);
  }

  @Test
  public void testGetRoot() {
    assertEquals("https://myapp.appspot.com/_ah/api", apiConfig.getRoot());
  }

  @Test
  public void testGetBns() {
    assertEquals("https://myapp.appspot.com/_ah/spi", apiConfig.getBackendRoot());
  }

  @Test
  public void testGetConfigurationInconsistencies_defaultConfig() throws Exception {
    assertThat(apiConfig.getConfigurationInconsistencies(apiConfig2)).isEmpty();
    assertThat(apiConfig2.getConfigurationInconsistencies(apiConfig)).isEmpty();
  }

  @Test
  public void testGetConfigurationInconsistencies_consistent() throws Exception {
    apiConfig.setDescription("bleh");
    apiConfig2.setDescription("bleh");

    apiConfig.setUseDatastore(true);
    apiConfig2.setUseDatastore(true);

    apiConfig.setAudiences(ImmutableList.of("foo", "bar"));
    apiConfig2.setAudiences(ImmutableList.of("foo", "bar"));

    apiConfig.getSerializationConfig().addSerializationConfig(DumbSerializer1.class);
    apiConfig2.getSerializationConfig().addSerializationConfig(DumbSerializer1.class);

    assertThat(apiConfig.getConfigurationInconsistencies(apiConfig2)).isEmpty();
    assertThat(apiConfig2.getConfigurationInconsistencies(apiConfig)).isEmpty();
  }

  @Test
  public void testGetConfigurationInconsistencies_mismatchedName() throws Exception {
    apiConfig.setName("foo");
    apiConfig2.setName("bar");

    Iterable<ApiConfigInconsistency<Object>> inconsistencies =
        apiConfig.getConfigurationInconsistencies(apiConfig2);
    assertEquals(1, Iterables.size(inconsistencies));
    assertEquals(new ApiConfigInconsistency<String>("name", "foo", "bar"),
        Iterables.getFirst(inconsistencies, null));
  }

  @Test
  public void testGetConfigurationInconsistencies_mismatchedSubAnnotation() throws Exception {
    apiConfig.getCacheControlConfig().setType("foo");
    apiConfig2.getCacheControlConfig().setType("bar");

    Iterable<ApiConfigInconsistency<Object>> inconsistencies =
        apiConfig.getConfigurationInconsistencies(apiConfig2);
    assertEquals(1, Iterables.size(inconsistencies));
    assertEquals(new ApiConfigInconsistency<String>("cacheControl.type", "foo", "bar"),
        Iterables.getFirst(inconsistencies, null));
  }

  @Test
  public void testGetConfigurationInconsistencies_multiple() throws Exception {
    apiConfig.setIsAbstract(true);
    apiConfig2.setIsAbstract(false);

    apiConfig.setBackendRoot("foo");
    apiConfig2.setBackendRoot("bar");

    apiConfig.getCacheControlConfig().setMaxAge(4);
    apiConfig2.getCacheControlConfig().setMaxAge(23);

    List<String> blockedRegions1 = ImmutableList.of("foo", "bar");
    List<String> blockedRegions2 = ImmutableList.of("bar", "foo");
    apiConfig.getAuthConfig().setBlockedRegions(blockedRegions1);
    apiConfig2.getAuthConfig().setBlockedRegions(blockedRegions2);

    assertThat(apiConfig.getConfigurationInconsistencies(apiConfig2))
        .containsExactly(
            new ApiConfigInconsistency<Boolean>("isAbstract", true, false),
            new ApiConfigInconsistency<String>("backendRoot", "foo", "bar"),
            new ApiConfigInconsistency<Integer>("cacheControl.maxAge", 4, 23),
            new ApiConfigInconsistency<List<String>>(
                "auth.blockedRegions", blockedRegions1, blockedRegions2));
  }

  @Test
  public void testCopyConstructor() {
    apiConfig.setRoot("root");
    apiConfig.setName("name");
    apiConfig.setCanonicalName("canonical");
    apiConfig.setVersion("version");
    apiConfig.setTitle("title");
    apiConfig.setDescription("desc");
    apiConfig.setDocumentationLink("link");
    apiConfig.setIconX16("iconX16");
    apiConfig.setIconX32("iconX32");
    apiConfig.setBackendRoot("backend");
    apiConfig.setIsAbstract(true);
    apiConfig.setIsDefaultVersion(true);
    apiConfig.setIsDiscoverable(true);
    apiConfig.setResource("resource");
    apiConfig.setUseDatastore(true);
    apiConfig.setAuthLevel(AuthLevel.REQUIRED);
    apiConfig.setScopeExpression(AuthScopeExpressions.interpret("test"));
    apiConfig.setAudiences(ImmutableList.of("aud1"));
    apiConfig.setIssuers(ApiIssuerConfigs.builder()
        .addIssuer(ApiIssuerConfigs.GOOGLE_ID_TOKEN_ISSUER)
        .build());
    apiConfig.setIssuerAudiences(ApiIssuerAudienceConfig.builder()
        .addIssuerAudiences("iss", "aud")
        .build());
    apiConfig.setClientIds(ImmutableList.of("clientid"));
    apiConfig.setAuthenticators(
        ImmutableList.<Class<? extends Authenticator>>of(GoogleJwtAuthenticator.class));
    apiConfig.setPeerAuthenticators(
        ImmutableList.<Class<? extends PeerAuthenticator>>of(EndpointsPeerAuthenticator.class));
    assertThat(apiConfig).isEqualTo(new ApiConfig(apiConfig));
  }
}
