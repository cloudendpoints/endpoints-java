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
package com.google.api.server.spi.discovery;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.ApiConfigWriter;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.services.discovery.Discovery;
import com.google.api.services.discovery.Discovery.Apis;
import com.google.api.services.discovery.Discovery.Apis.GenerateDirectory;
import com.google.api.services.discovery.Discovery.Apis.GenerateRest;
import com.google.api.services.discovery.Discovery.Apis.GenerateRpc;
import com.google.api.services.discovery.model.ApiConfigs;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.RestDescription;
import com.google.api.services.discovery.model.RpcDescription;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Set;

/**
 * Tests for {@link ProxyingDiscoveryProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProxyingDiscoveryProviderTest {
  private static final String NAME = "name";
  private static final String WRONG_NAME = "wrongname";
  private static final String V1 = "v1";
  private static final String V2 = "v2";
  private static final String REWRITTEN_ROOT = "https://rewritten.appspot.com/api/";
  private static final RestDescription REST_DOC = new RestDescription()
      .setName(NAME)
      .setVersion(V1);
  private static final RpcDescription RPC_DOC = new RpcDescription()
      .setName(NAME)
      .setVersion(V1);
  private static final DirectoryList DIRECTORY = new DirectoryList()
      .setItems(ImmutableList.of(new DirectoryList.Items()
          .setName(NAME)
          .setVersion(V1)));
  private static final ApiKey V1_REWRITTEN_KEY = new ApiKey(REWRITTEN_ROOT, NAME, V1);
  private static final String V1_JSON_API_CONFIG = "not really a json config v1";
  private static final ApiKey V2_REWRITTEN_KEY = new ApiKey(REWRITTEN_ROOT, NAME, V2);
  private static final String V2_JSON_API_CONFIG = "not really a json config v2";

  @Mock private Discovery discovery;
  @Mock private Apis apis;
  @Mock private GenerateRest restRequest;
  @Mock private GenerateRpc rpcRequest;
  @Mock private GenerateDirectory directoryRequest;
  @Mock private ApiConfigWriter configWriter;

  private ProxyingDiscoveryProvider provider;

  @Before
  public void setUp() throws Exception {
    ApiConfigLoader loader = new ApiConfigLoader();
    ServiceContext context = ServiceContext.create();
    ApiConfig apiConfig1 = loader.loadConfiguration(context, TestApi1.class);
    ApiConfig apiConfig2 = loader.loadConfiguration(context, TestApi2.class);
    ApiConfig apiConfig3 = loader.loadConfiguration(context, TestApiV2.class);
    ApiConfig.Factory factory = new ApiConfig.Factory();
    ApiConfig rewrittenApiConfig1 = factory.copy(apiConfig1);
    ApiConfig rewrittenApiConfig2 = factory.copy(apiConfig2);
    ApiConfig rewrittenApiConfig3 = factory.copy(apiConfig3);
    rewrittenApiConfig1.setRoot(REWRITTEN_ROOT);
    rewrittenApiConfig2.setRoot(REWRITTEN_ROOT);
    rewrittenApiConfig3.setRoot(REWRITTEN_ROOT);

    // Setup standard mocks on our discovery API.
    when(discovery.apis()).thenReturn(apis);
    when(apis.generateRest(any(com.google.api.services.discovery.model.ApiConfig.class)))
        .thenReturn(restRequest);
    when(apis.generateRpc(any(com.google.api.services.discovery.model.ApiConfig.class)))
        .thenReturn(rpcRequest);
    when(apis.generateDirectory(any(ApiConfigs.class)))
        .thenReturn(directoryRequest);
    // Used by individual document tests
    when(configWriter.writeConfig(withConfigs(rewrittenApiConfig1, rewrittenApiConfig2)))
        .thenReturn(ImmutableMap.of(V1_REWRITTEN_KEY, V1_JSON_API_CONFIG));
    // Used by directory tests
    when(configWriter
        .writeConfig(withConfigs(rewrittenApiConfig1, rewrittenApiConfig2, rewrittenApiConfig3)))
        .thenReturn(ImmutableMap
            .of(V1_REWRITTEN_KEY, V1_JSON_API_CONFIG, V2_REWRITTEN_KEY, V2_JSON_API_CONFIG));

    provider = new ProxyingDiscoveryProvider(ImmutableList.of(apiConfig1, apiConfig2, apiConfig3),
        configWriter, discovery);
  }

  @Test
  public void getRestDocument() throws Exception {
    when(restRequest.execute()).thenReturn(REST_DOC);

    RestDescription actual = provider.getRestDocument(REWRITTEN_ROOT, NAME, V1);

    assertThat(actual).isEqualTo(REST_DOC);
    verify(apis).generateRest(
        new com.google.api.services.discovery.model.ApiConfig().setConfig(V1_JSON_API_CONFIG));
  }

  @Test
  public void getRestDocument_notFound() throws Exception {
    try {
      provider.getRestDocument(REWRITTEN_ROOT, WRONG_NAME, V1);
      fail("expected NotFoundException");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void getRestDocument_internalServerError() throws Exception {
    when(restRequest.execute()).thenThrow(new IOException());

    try {
      provider.getRestDocument(REWRITTEN_ROOT, NAME, V1);
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  @Test
  public void getRpcDocument() throws Exception {
    when(rpcRequest.execute()).thenReturn(RPC_DOC);

    RpcDescription actual = provider.getRpcDocument(REWRITTEN_ROOT, NAME, V1);

    assertThat(actual).isEqualTo(RPC_DOC);
    verify(apis).generateRpc(
        new com.google.api.services.discovery.model.ApiConfig().setConfig(V1_JSON_API_CONFIG));
  }

  @Test
  public void getRpcDocument_notFound() throws Exception {
    try {
      provider.getRpcDocument(REWRITTEN_ROOT, WRONG_NAME, V1);
      fail("expected NotFoundException");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void getRpcDocument_internalServerError() throws Exception {
    when(rpcRequest.execute()).thenThrow(new IOException());

    try {
      provider.getRpcDocument(REWRITTEN_ROOT, NAME, V1);
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  @Test
  public void getDirectory() throws Exception {
    when(directoryRequest.execute()).thenReturn(DIRECTORY);

    DirectoryList actual = provider.getDirectory(REWRITTEN_ROOT);

    assertThat(actual).isEqualTo(DIRECTORY);
    verify(apis).generateDirectory(withConfigs(V1_JSON_API_CONFIG, V2_JSON_API_CONFIG));
  }

  @Test
  public void getDirectory_internalServerError() throws Exception {
    when(directoryRequest.execute()).thenThrow(new IOException());

    try {
      provider.getDirectory(REWRITTEN_ROOT);
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  private static Iterable<ApiConfig> withConfigs(ApiConfig... configs) {
    return argThat(new ConfigMatcher(Sets.newHashSet(configs)));
  }

  private static class ConfigMatcher implements ArgumentMatcher<Iterable<ApiConfig>> {
    private final Set<ApiConfig> configs;

    ConfigMatcher(Set<ApiConfig> configs) {
      this.configs = configs;
    }

    @Override
    public boolean matches(Iterable<ApiConfig> argument) {
      return argument != null
          && configs.equals(Sets.newHashSet(argument));
    }
  }

  private static ApiConfigs withConfigs(String... jsonConfigs) {
    return argThat(new ApiConfigsMatcher(Sets.newHashSet(jsonConfigs)));
  }

  private static class ApiConfigsMatcher implements ArgumentMatcher<ApiConfigs> {
    private final Set<String> configs;

    ApiConfigsMatcher(Set<String> configs) {
      this.configs = configs;
    }

    @Override
    public boolean matches(ApiConfigs argument) {
      return argument != null
          && configs.equals(Sets.newHashSet(argument.getConfigs()));
    }
  }

  @Api(name = NAME, version = V1)
  public static class TestApi1 {
  }

  @Api(name = NAME, version = V1)
  public static class TestApi2 {
  }

  @Api(name = NAME, version = V2)
  public static class TestApiV2 {
  }
}
