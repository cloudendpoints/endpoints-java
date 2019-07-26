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
package com.google.api.server.spi.config.annotationreader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.server.spi.Constant;
import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiCacheControl;
import com.google.api.server.spi.config.ApiClass;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiFrontendLimitRule;
import com.google.api.server.spi.config.ApiFrontendLimits;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.ApiReference;
import com.google.api.server.spi.config.ApiTransformer;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Description;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.model.ApiAuthConfig;
import com.google.api.server.spi.config.model.ApiCacheControlConfig;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiFrontendLimitsConfig;
import com.google.api.server.spi.config.model.ApiFrontendLimitsConfig.FrontendLimitsRule;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.testing.BoundedGenericEndpoint;
import com.google.api.server.spi.testing.BridgeInheritanceEndpoint;
import com.google.api.server.spi.testing.CollectionCovarianceEndpoint;
import com.google.api.server.spi.testing.DeepGenericHierarchyFailEndpoint;
import com.google.api.server.spi.testing.DeepGenericHierarchySuccessEndpoint;
import com.google.api.server.spi.testing.DumbSerializer1;
import com.google.api.server.spi.testing.DumbSerializer2;
import com.google.api.server.spi.testing.DuplicateMethodEndpoint;
import com.google.api.server.spi.testing.Endpoint0;
import com.google.api.server.spi.testing.Endpoint1;
import com.google.api.server.spi.testing.Endpoint2;
import com.google.api.server.spi.testing.Endpoint3;
import com.google.api.server.spi.testing.Endpoint4;
import com.google.api.server.spi.testing.FailAuthenticator;
import com.google.api.server.spi.testing.FailPeerAuthenticator;
import com.google.api.server.spi.testing.Foo;
import com.google.api.server.spi.testing.InterfaceReferenceEndpoint;
import com.google.api.server.spi.testing.PassAuthenticator;
import com.google.api.server.spi.testing.PassPeerAuthenticator;
import com.google.api.server.spi.testing.ReferenceOverridingEndpoint;
import com.google.api.server.spi.testing.RestfulResourceEndpointBase;
import com.google.api.server.spi.testing.SimpleBean;
import com.google.api.server.spi.testing.SimpleContravarianceEndpoint;
import com.google.api.server.spi.testing.SimpleCovarianceEndpoint;
import com.google.api.server.spi.testing.SimpleLevelOverridingApi;
import com.google.api.server.spi.testing.SimpleLevelOverridingInheritedApi;
import com.google.api.server.spi.testing.SimpleOverloadEndpoint;
import com.google.api.server.spi.testing.SimpleOverrideEndpoint;
import com.google.api.server.spi.testing.SimpleReferenceEndpoint;
import com.google.api.server.spi.testing.SubclassedEndpoint;
import com.google.api.server.spi.testing.SubclassedOverridingEndpoint;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link ApiConfigAnnotationReader}.
 * TODO test deprecation
 */
@RunWith(JUnit4.class)
public class ApiConfigAnnotationReaderTest {
  private static final String[] DEFAULT_SCOPES = {Constant.API_EMAIL_SCOPE};
  private static final String[] DEFAULT_AUDIENCES = {};
  private static final String[] DEFAULT_CLIENTIDS = {Constant.API_EXPLORER_CLIENT_ID};
  private static final AuthLevel DEFAULT_AUTH_LEVEL = AuthLevel.NONE;

  private ServiceContext serviceContext;
  private ApiConfigAnnotationReader annotationReader;

  @Before
  public void setUp() throws Exception {

    serviceContext = Mockito.mock(ServiceContext.class);
    Mockito.when(serviceContext.getDefaultApiName()).thenReturn("api");
    Mockito.when(serviceContext.getAppHostname()).thenReturn("appHostName.com");

    annotationReader = new ApiConfigAnnotationReader();
  }

  private ApiConfig createConfig(Class<?> endpointClass) throws Exception {
    return (new ApiConfig.Factory()).create(serviceContext, new TypeLoader(), endpointClass);
  }

  @Test
  public void testBasicEndpoint() throws Exception {
    ApiConfig config = createConfig(Endpoint0.class);
    annotationReader.loadEndpointClass(serviceContext, Endpoint0.class, config);
    assertEquals("api", config.getName());
    assertEquals(0, config.getApiClassConfig().getMethods().size());

    annotationReader.loadEndpointMethods(serviceContext, Endpoint0.class,
        config.getApiClassConfig().getMethods());
    assertEquals(1, config.getApiClassConfig().getMethods().size());
    ApiMethodConfig method = config.getApiClassConfig().getMethods().get(
        methodToEndpointMethod(Endpoint0.class.getMethod("getFoo", String.class)));
    validateMethod(method,
        "Endpoint0.getFoo",
        "foo/{id}",
        ApiMethod.HttpMethod.GET,
        DEFAULT_SCOPES,
        DEFAULT_AUDIENCES,
        DEFAULT_CLIENTIDS,
        null,
        null);
  }

  @Test
  public void testFullyConfiguredEndpoint() throws Exception {
    ApiConfig config = createConfig(Endpoint1.class);
    annotationReader.loadEndpointClass(serviceContext, Endpoint1.class, config);
    annotationReader.loadEndpointMethods(serviceContext, Endpoint1.class,
        config.getApiClassConfig().getMethods());
    validateEndpoint1(config, Endpoint1.class);
  }

  @Test
  public void testEndpointWithNoPublicMethods() throws Exception {
    ApiConfig config = createConfig(Endpoint2.class);
    annotationReader.loadEndpointMethods(serviceContext, Endpoint2.class,
        config.getApiClassConfig().getMethods());

    assertEquals(0, config.getApiClassConfig().getMethods().size());
  }

  @Test
  public void testEndpointWithInheritance() throws Exception {
    ApiConfig config = createConfig(Endpoint3.class);
    annotationReader.loadEndpointClass(serviceContext, Endpoint3.class, config);
    annotationReader.loadEndpointMethods(serviceContext, Endpoint3.class,
        config.getApiClassConfig().getMethods());
    validateEndpoint1(config, Endpoint3.class);

    String[] defaultScopes = {"ss0", "ss1 ss2"};
    String[] defaultAudiences = {"aa0", "aa1"};
    String[] defaultClientIds = {"cc0", "cc1"};

    ApiMethodConfig getBar =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            Endpoint3.class.getMethod("getBar", String.class)));
    validateMethod(getBar,
        "Endpoint3.getBar",
        "bar/{id}",
        ApiMethod.HttpMethod.GET,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(1, getBar.getParameterConfigs().size());
    validateParameter(getBar.getParameterConfigs().get(0), "id", false, null, String.class);
  }

  @Test
  public void testEndpointWithBridgeMethods() throws Exception {
    ApiConfig config = createConfig(BridgeInheritanceEndpoint.class);
    annotationReader.loadEndpointClass(serviceContext, BridgeInheritanceEndpoint.class, config);
    annotationReader.loadEndpointMethods(serviceContext, BridgeInheritanceEndpoint.class,
        config.getApiClassConfig().getMethods());

    assertEquals(2, config.getApiClassConfig().getMethods().size());
    ApiMethodConfig fn1 = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        BridgeInheritanceEndpoint.class.getMethod("fn1")));
    validateMethod(fn1,
        "api6.foos.fn1",
        "fn1",
        ApiMethod.HttpMethod.GET,
        DEFAULT_SCOPES,
        DEFAULT_AUDIENCES,
        DEFAULT_CLIENTIDS,
        null,
        null);
    ApiMethodConfig fn2 = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        BridgeInheritanceEndpoint.class.getSuperclass().getMethod("fn2")));
    validateMethod(fn2,
        "api6.foos.fn2",
        "fn2",
        ApiMethod.HttpMethod.GET,
        DEFAULT_SCOPES,
        DEFAULT_AUDIENCES,
        DEFAULT_CLIENTIDS,
        null,
        null);

    ApiMethodConfig bridge = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        BridgeInheritanceEndpoint.class.getSuperclass().getMethod("fn1")));
    assertNull(bridge);
  }

  @Test
  public void testDuplicateMethodEndpoint() throws Exception {
    ApiConfig config = createConfig(DuplicateMethodEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext, DuplicateMethodEndpoint.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig method1 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            DuplicateMethodEndpoint.class.getMethod("foo", String.class)));
    assertEquals("api.foos.fn1", method1.getName());

    ApiMethodConfig method2 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            DuplicateMethodEndpoint.class.getMethod("foo", Integer.class)));
    assertEquals("api.foos.fn2", method2.getName());
  }

  @Test
  public void testSimpleOverrideEndpoint() throws Exception {
    ApiConfig config = createConfig(SimpleOverrideEndpoint.class);
    annotationReader.loadEndpointClass(serviceContext, SimpleOverrideEndpoint.class, config);
    annotationReader.loadEndpointMethods(serviceContext, SimpleOverrideEndpoint.class,
        config.getApiClassConfig().getMethods());

    assertEquals(1, config.getApiClassConfig().getMethods().size());

    ApiMethodConfig foo = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        SimpleOverrideEndpoint.class.getMethod("foo", String.class)));
    validateMethod(foo,
        "api.foos.fn",
        "fn",
        ApiMethod.HttpMethod.GET,
        DEFAULT_SCOPES,
        DEFAULT_AUDIENCES,
        DEFAULT_CLIENTIDS,
        null,
        null);
  }

  @Test
  public void testSimpleOverloadEndpoint() throws Exception {
    ApiConfig config = createConfig(SimpleOverloadEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext, SimpleOverloadEndpoint.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig method1 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            SimpleOverloadEndpoint.class.getMethod("foo", Integer.class)));
    assertEquals("api.foos.fn", method1.getName());

    ApiMethodConfig method2 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            SimpleOverloadEndpoint.class.getSuperclass().getMethod("foo", String.class)));
    assertEquals("api.foos.base", method2.getName());
  }

  @Test
  public void testSimpleCovarianceEndpoint() throws Exception {
    ApiConfig config = createConfig(SimpleCovarianceEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext, SimpleCovarianceEndpoint.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig method1 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            SimpleCovarianceEndpoint.class.getMethod("foo", String.class)));
    assertEquals("api.foos.fn", method1.getName());

    ApiMethodConfig method2 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            SimpleCovarianceEndpoint.class.getSuperclass().getMethod("foo", Object.class)));
    assertEquals("api.foos.base", method2.getName());
  }

  @Test
  public void testSimpleContravarianceEndpoint() throws Exception {
    ApiConfig config = createConfig(SimpleContravarianceEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext, SimpleContravarianceEndpoint.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig method1 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            SimpleContravarianceEndpoint.class.getMethod("foo", Object.class)));
    assertEquals("api.foos.fn", method1.getName());

    ApiMethodConfig method2 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            SimpleContravarianceEndpoint.class.getSuperclass().getMethod("foo", Number.class)));
    assertEquals("api.foos.base", method2.getName());
  }

  @Test
  public void testCollectionCovarianceEndpoint() throws Exception {
    ApiConfig config = createConfig(CollectionCovarianceEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext, CollectionCovarianceEndpoint.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig method1 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            CollectionCovarianceEndpoint.class.getMethod("foo", List.class)));
    assertEquals("api.foos.fn", method1.getName());

    ApiMethodConfig method2 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            CollectionCovarianceEndpoint.class.getSuperclass().getMethod("foo", Collection.class)));
    assertEquals("api.foos.base", method2.getName());
  }

  @Test
  public void testFullySpecializedDateEndpoint() throws Exception {
    ApiConfig config = createConfig(RestfulResourceEndpointBase.FullySpecializedEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext,
        RestfulResourceEndpointBase.FullySpecializedEndpoint.class,
        config.getApiClassConfig().getMethods());

    assertEquals(6, config.getApiClassConfig().getMethods().size());
  }

  @Test
  public void testGenericBasePartiallySpecializedEndpoint() throws Exception {
    ApiConfig config = createConfig(RestfulResourceEndpointBase.PartiallySpecializedEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext,
            RestfulResourceEndpointBase.PartiallySpecializedEndpoint.class,
            config.getApiClassConfig().getMethods());

    assertEquals(6, config.getApiClassConfig().getMethods().size());

    assertNull(config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        RestfulResourceEndpointBase.FullySpecializedEndpoint.class.getMethod("list"))));
    assertNotNull(config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        RestfulResourceEndpointBase.PartiallySpecializedEndpoint.class,
        RestfulResourceEndpointBase.PartiallySpecializedEndpoint.class.getMethod(
            "get", long.class))));
    assertNotNull(config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        RestfulResourceEndpointBase.PartiallySpecializedEndpoint.class,
        RestfulResourceEndpointBase.class.getMethod("list"))));
  }

  @Test
  public void testDeepGenericHierarchySuccessEndpoint() throws Exception {
    ApiConfig config = createConfig(DeepGenericHierarchySuccessEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext, DeepGenericHierarchySuccessEndpoint.class,
        config.getApiClassConfig().getMethods());

    assertEquals(1, config.getApiClassConfig().getMethods().size());
    assertNotNull(config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        DeepGenericHierarchySuccessEndpoint.class.getMethod(
            "foo", String.class, Integer.class, Boolean.class))));
  }

  @Test
  public void testBoundedGenericEndpoint() throws Exception {
    ApiConfig config = createConfig(BoundedGenericEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext, BoundedGenericEndpoint.class,
        config.getApiClassConfig().getMethods());
    assertEquals(1, config.getApiClassConfig().getMethods().size());
  }

  @Test
  public void testMethodDescription() throws Exception {
    @Api
    final class MethodDescriptionEndpoint {
      public void noAnnotation() {}
      @ApiMethod
      public void noDescription() {}
      @ApiMethod(description = "description")
      public void withDescription() {}
    }
    ApiConfig config = createConfig(MethodDescriptionEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext, MethodDescriptionEndpoint.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig method1 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            MethodDescriptionEndpoint.class.getMethod("noAnnotation")));
    assertNull(method1.getDescription());
    ApiMethodConfig method2 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            MethodDescriptionEndpoint.class.getMethod("noDescription")));
    assertNull(method2.getDescription());
    ApiMethodConfig method3 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            MethodDescriptionEndpoint.class.getMethod("withDescription")));
    assertEquals("description", method3.getDescription());
  }

  @Test
  public void testWildcardParameterTypes() throws Exception {
    @Api
    final class WildcardEndpoint {
      @SuppressWarnings("unused")
      public void foo(Map<String, ? extends Integer> map) {}
    }
    try {
      ApiConfig config = createConfig(WildcardEndpoint.class);
      annotationReader.loadEndpointMethods(serviceContext, WildcardEndpoint.class,
          config.getApiClassConfig().getMethods());
      fail("Config generation for service class with wildcard parameter type should have failed");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testDeepGenericHierarchyFailEndpoint() throws Exception {
    ApiConfig config = createConfig(DeepGenericHierarchyFailEndpoint.class);
    annotationReader.loadEndpointMethods(serviceContext, DeepGenericHierarchyFailEndpoint.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig method1 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            DeepGenericHierarchyFailEndpoint.class,
            DeepGenericHierarchyFailEndpoint.class.getMethod("foo", String.class, Integer.class,
                Collection.class)));
    assertEquals("DeepGenericHierarchyFailEndpoint.foo", method1.getName());

    ApiMethodConfig method2 =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            DeepGenericHierarchyFailEndpoint.class,
            DeepGenericHierarchyFailEndpoint.class.getSuperclass().getSuperclass().getSuperclass()
                .getMethod("foo", Object.class, Object.class, Object.class)));
    assertEquals("Endpoint3.foo", method2.getName());
  }

  @Test
  public void testServiceWithMergedInheritance() throws Exception {
    ApiConfig config = createConfig(SubclassedEndpoint.class);
    annotationReader.loadEndpointClass(serviceContext, SubclassedEndpoint.class, config);
    annotationReader.loadEndpointMethods(serviceContext, SubclassedEndpoint.class,
        config.getApiClassConfig().getMethods());
    validateEndpoint1(config, SubclassedEndpoint.class);
  }

  @Test
  public void testServiceWithOverridingInheritance() throws Exception {
    ApiConfig config = createConfig(SubclassedOverridingEndpoint.class);
    annotationReader.loadEndpointClass(serviceContext, SubclassedOverridingEndpoint.class, config);

    assertEquals("api", config.getName());
    assertEquals("v2", config.getVersion());
    assertEquals("overridden description", config.getDescription());
    assertFalse(config.getIsDefaultVersion());
    assertFalse(config.getIsDiscoverable());
    assertFalse(config.getUseDatastore());

    ApiAuthConfig auth = config.getAuthConfig();
    assertTrue(auth.getAllowCookieAuth());

    ApiFrontendLimitsConfig frontendLimits = config.getFrontendLimitsConfig();
    assertEquals(1, frontendLimits.getUnregisteredUserQps());
    assertEquals(4, frontendLimits.getUnregisteredQps());

    ApiCacheControlConfig cacheControl = config.getCacheControlConfig();
    assertEquals(2, cacheControl.getMaxAge());
    assertEquals(ApiCacheControl.Type.PUBLIC, cacheControl.getType());

    annotationReader.loadEndpointMethods(serviceContext, SubclassedOverridingEndpoint.class,
        config.getApiClassConfig().getMethods());

    String[] defaultScopes = {"ss0a", "ss1a"};
    String[] defaultAudiences = {"aa0a", "aa1a"};
    String[] defaultClientIds = {"cc0a", "cc1a"};
    assertEquals(8, config.getApiClassConfig().getMethods().size());

    ApiMethodConfig listFoos = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        SubclassedOverridingEndpoint.class.getMethod("listFoos")));
    String[] expectedScopes = { "s0", "s1 s2" };
    String[] expectedAudiences = { "a0", "a1" };
    String[] expectedClientIds = { "c0", "c1" };
    validateMethod(listFoos, "foos.list", "foos", ApiMethod.HttpMethod.GET, expectedScopes,
        expectedAudiences, expectedClientIds, ImmutableList.of(FailAuthenticator.class),
        ImmutableList.of(FailPeerAuthenticator.class));
    assertEquals(0, listFoos.getParameterConfigs().size());

    ApiMethodConfig getFoo = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        SubclassedOverridingEndpoint.class.getMethod("getFoo", String.class)));
    validateMethod(getFoo,
        "foos.get2",
        "foos/{id}",
        ApiMethod.HttpMethod.GET,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(1, getFoo.getParameterConfigs().size());
    validateParameter(getFoo.getParameterConfigs().get(0), "id", false, null, String.class);

    ApiMethodConfig insertFoo = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        Endpoint1.class.getMethod("insertFoo", Foo.class)));
    validateMethod(insertFoo,
        "foos.insert",
        "foos",
        ApiMethod.HttpMethod.POST,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(1, insertFoo.getParameterConfigs().size());
    validateParameter(insertFoo.getParameterConfigs().get(0), null, false, null, Foo.class);

    ApiMethodConfig execute2 = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        Endpoint1.class.getMethod("execute2", SimpleBean.class)));
    validateMethod(execute2, "foos.execute2", "execute2/{serialized}",
        ApiMethod.HttpMethod.POST,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(1, execute2.getParameterConfigs().size());
    validateParameter(execute2.getParameterConfigs().get(0), "serialized", false, null,
        SimpleBean.class, DumbSerializer2.class, Integer.class);
  }

  @Test
  public void testServiceWithOverridingReference() throws Exception {
    ApiConfig config = createConfig(ReferenceOverridingEndpoint.class);
    annotationReader.loadEndpointClass(serviceContext, ReferenceOverridingEndpoint.class, config);

    assertEquals("api", config.getName());
    assertEquals("v3", config.getVersion());
    assertEquals("more overridden description", config.getDescription());
    assertTrue(config.getIsDefaultVersion());
    assertTrue(config.getUseDatastore());

    ApiAuthConfig auth = config.getAuthConfig();
    assertTrue(auth.getAllowCookieAuth());

    ApiFrontendLimitsConfig frontendLimits = config.getFrontendLimitsConfig();
    assertEquals(1, frontendLimits.getUnregisteredUserQps());
    assertEquals(4, frontendLimits.getUnregisteredQps());

    ApiCacheControlConfig cacheControl = config.getCacheControlConfig();
    assertEquals(2, cacheControl.getMaxAge());
    assertEquals(ApiCacheControl.Type.PUBLIC, cacheControl.getType());

    annotationReader.loadEndpointMethods(serviceContext, ReferenceOverridingEndpoint.class,
        config.getApiClassConfig().getMethods());

    String[] defaultScopes = {"ss0b", "ss1b"};
    String[] defaultAudiences = {"aa0b", "aa1b"};
    String[] defaultClientIds = {"cc0b", "cc1b"};
    assertEquals(8, config.getApiClassConfig().getMethods().size());

    ApiMethodConfig getFoo = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        ReferenceOverridingEndpoint.class.getMethod("getFoo", String.class)));
    validateMethod(getFoo,
        "foos.get3",
        "foos/{id}",
        ApiMethod.HttpMethod.GET,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(1, getFoo.getParameterConfigs().size());
    validateParameter(getFoo.getParameterConfigs().get(0), "id", false, null, String.class);
  }

  @Test
  public void testServiceWithOverridingInterfaceReference() throws Exception {
    ApiConfig config = createConfig(InterfaceReferenceEndpoint.class);
    annotationReader.loadEndpointClass(serviceContext, InterfaceReferenceEndpoint.class, config);

    assertEquals("interfaceBasedApi", config.getName());
    assertEquals("v2", config.getVersion());
  }

  @Test
  public void testServiceWithSimpleReference() throws Exception {
    ApiConfig config = createConfig(SimpleReferenceEndpoint.class);
    annotationReader.loadEndpointClass(serviceContext, SimpleReferenceEndpoint.class, config);

    // Especially should not be "api4" as provided by the inherited Endpoint4.
    assertEquals("api", config.getName());
  }

  @Test
  public void testServiceWithApiNameOverride() throws Exception {
    ApiConfig config = createConfig(Endpoint4.class);
    annotationReader.loadEndpointClass(serviceContext, Endpoint4.class, config);

    assertEquals("api4", config.getName());
  }

  @Test
  public void testEndpointNoApiAnnotation() throws Exception {
    ApiConfig config = createConfig(Object.class);
    try {
      annotationReader.loadEndpointClass(serviceContext, Object.class, config);
      fail("No @Api annotation should've caused reader to fail.");
    } catch (ApiConfigException expected) {
      assertEquals("class java.lang.Object has no @Api annotation.", expected.getMessage());
    }
  }

  @Test
  public void testAbstract() throws Exception {
    @Api(isAbstract = AnnotationBoolean.TRUE)
    class Test {}

    ApiConfig config = createConfig(Test.class);
    annotationReader.loadEndpointClass(serviceContext, Test.class, config);
    assertTrue(config.getIsAbstract());
  }

  @Test
  public void testFrontendLimitsRulesNotDuplicatedInInheritance() throws Exception {
    @Api(frontendLimits = @ApiFrontendLimits(
        rules = {@ApiFrontendLimitRule(match = "test", qps = 1)}))
    class Test {
    }

    final class Child extends Test {
    }

    ApiConfig config = createConfig(Child.class);
    annotationReader.loadEndpointClass(serviceContext, Child.class, config);
    assertEquals(1, config.getFrontendLimitsConfig().getRules().size());
  }

  @Test
  public void testFrontendLimitsRuleWithSameMatchOverridesParentRule() throws Exception {
    @Api(frontendLimits = @ApiFrontendLimits(
        rules = {@ApiFrontendLimitRule(match = "test", qps = 1)}))
    class Test {
    }

    @Api(frontendLimits = @ApiFrontendLimits(
        rules = {@ApiFrontendLimitRule(match = "test", userQps = 1)}))
    final class Child extends Test {
    }

    ApiConfig config = createConfig(Child.class);
    annotationReader.loadEndpointClass(serviceContext, Child.class, config);
    assertEquals(1, config.getFrontendLimitsConfig().getRules().size());
    FrontendLimitsRule rule = config.getFrontendLimitsConfig().getRules().get(0);
    assertEquals("test", rule.getMatch());
    assertEquals(-1, rule.getQps());
    assertEquals(1, rule.getUserQps());
  }

  @Test
  public void testReadParametersInApiNamespace() throws Exception {
    @Api
    class ValidNamespaceDefault {}
    ApiConfig config = createConfig(ValidNamespaceDefault.class);
    annotationReader.loadEndpointClass(serviceContext, ValidNamespaceDefault.class, config);

    @Api(namespace = @ApiNamespace(ownerDomain = "domain", ownerName = ""))
    class BadNamespaceEmptyName {}
    config = createConfig(BadNamespaceEmptyName.class);
    annotationReader.loadEndpointClass(serviceContext, BadNamespaceEmptyName.class, config);
    assertEquals("domain", config.getNamespaceConfig().getOwnerDomain());

    @Api(namespace = @ApiNamespace(ownerDomain = "", ownerName = "name"))
    class BadNamespaceEmptyDomain {}
    config = createConfig(BadNamespaceEmptyDomain.class);
    annotationReader.loadEndpointClass(serviceContext, BadNamespaceEmptyDomain.class, config);
    assertEquals("name", config.getNamespaceConfig().getOwnerName());

    @Api(namespace = @ApiNamespace(ownerDomain = "domain", ownerName = "name"))
    class ValidNamespaceEmptyPackage {}
    config = createConfig(ValidNamespaceEmptyPackage.class);
    annotationReader.loadEndpointClass(serviceContext, ValidNamespaceEmptyPackage.class, config);
    assertEquals("domain", config.getNamespaceConfig().getOwnerDomain());
    assertEquals("name", config.getNamespaceConfig().getOwnerName());

    @Api(namespace = @ApiNamespace(
        ownerDomain = "domain", ownerName = "name", packagePath = "package"))
    class ValidNamespaceFullySpecified {}
    config = createConfig(ValidNamespaceFullySpecified.class);
    annotationReader.loadEndpointClass(serviceContext, ValidNamespaceFullySpecified.class, config);
    assertEquals("domain", config.getNamespaceConfig().getOwnerDomain());
    assertEquals("name", config.getNamespaceConfig().getOwnerName());
    assertEquals("package", config.getNamespaceConfig().getPackagePath());
  }

  @Api
  private static class DefaultValuedEndpoint<T> {
    @SuppressWarnings("unused")
    public void foo(T id) {}
  }

  @Test
  public void testValidDefaultValuedParameterString() throws Exception {
    final class Test extends DefaultValuedEndpoint<String> {
      @Override public void foo(@Named("id") @DefaultValue("bar") String id) {}
    }
    assertEquals("bar", implValidTestDefaultValuedParameter(Test.class));
  }

  @Test
  public void testValidDefaultValuedParameterBoolean() throws Exception {
    final class Test extends DefaultValuedEndpoint<Boolean> {
      @Override public void foo(@Named("id") @DefaultValue("true") Boolean id) {}
    }
    assertEquals(true, Boolean.parseBoolean(implValidTestDefaultValuedParameter(Test.class)));
  }

  @Test
  public void testValidDefaultValuedParameterInteger() throws Exception {
    final class Test extends DefaultValuedEndpoint<Integer> {
      @Override public void foo(@Named("id") @DefaultValue("2718") Integer id) {}
    }
    assertEquals(2718, Integer.parseInt(implValidTestDefaultValuedParameter(Test.class)));
  }

  @Test
  public void testValidDefaultValuedParameterLong() throws Exception {
    final class Test extends DefaultValuedEndpoint<Long> {
      @Override public void foo(@Named("id") @DefaultValue("3141") Long id) {}
    }
    assertEquals(3141L, Long.parseLong(implValidTestDefaultValuedParameter(Test.class)));
  }

  @Test
  public void testParameterDescription() throws Exception {
    @Api
    final class TestParameterDescription {
      public void foo(@Description("desc") String param) {}
    }
    ApiConfig config = createConfig(TestParameterDescription.class);
    annotationReader.loadEndpointMethods(serviceContext, TestParameterDescription.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig methodConfig =
        Iterables.getOnlyElement(config.getApiClassConfig().getMethods().values());
    ApiParameterConfig parameterConfig =
        Iterables.getOnlyElement(methodConfig.getParameterConfigs());
    assertEquals("desc", parameterConfig.getDescription());
  }

  @ApiTransformer(TestSerializer.class)
  private static class TestBean {}

  private static class TestSerializer implements Transformer<TestBean, String> {
    @Override
    public String transformTo(TestBean in) {
      return "foo";
    }

    @Override
    public TestBean transformFrom(String in) {
      return new TestBean();
    }
  }

  @Test
  public void testSerializedParameter() throws Exception {
    @Api
    final class Test {
      @SuppressWarnings("unused")
      public void method(@Named("serialized") TestBean tb) {}
    }

    ApiConfig config = createConfig(Test.class);
    annotationReader.loadEndpointClass(serviceContext, Test.class, config);
    annotationReader.loadEndpointMethods(serviceContext, Test.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig methodConfig =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(Test.class.getMethod(
            "method", TestBean.class)));
    validateMethod(methodConfig, "Test.method", "method/{serialized}", ApiMethod.HttpMethod.POST,
        DEFAULT_SCOPES,
        DEFAULT_AUDIENCES,
        DEFAULT_CLIENTIDS,
        null,
        null);
    validateParameter(methodConfig.getParameterConfigs().get(0), "serialized", false, null,
        TestBean.class, TestSerializer.class, String.class);
  }

  @Test
  public void testMethodAuthDefaults() throws Exception {
    // Default.
    @Api
    final class Test1 {
    @SuppressWarnings("unused")
      public void method() {}
    }
    ApiConfig config = createConfig(Test1.class);
    annotationReader.loadEndpointMethods(
        serviceContext, Test1.class, config.getApiClassConfig().getMethods());
    ApiMethodConfig methodConfig = config.getApiClassConfig()
        .getMethods().get(methodToEndpointMethod(Test1.class.getMethod("method")));
    validateMethodForAuth(
        methodConfig, DEFAULT_SCOPES, DEFAULT_AUDIENCES, DEFAULT_CLIENTIDS, DEFAULT_AUTH_LEVEL);

    // Method explicitly set scopes, etc. empty.
    @Api
    final class Test2 {
    @SuppressWarnings("unused")
    @ApiMethod(scopes = {}, audiences = {}, clientIds = {})
      public void method() {}
    }
    config = createConfig(Test2.class);
    annotationReader.loadEndpointMethods(
        serviceContext, Test2.class, config.getApiClassConfig().getMethods());
    methodConfig = config.getApiClassConfig()
        .getMethods().get(methodToEndpointMethod(Test2.class.getMethod("method")));
    String[] empty = {};
    validateMethodForAuth(methodConfig, empty, empty, empty, DEFAULT_AUTH_LEVEL);

    // Api explicitly set scopes, etc. empty.
    @Api(scopes = {}, audiences = {}, clientIds = {})
    final class Test3 {
    @SuppressWarnings("unused")
      public void method() {}
    }
    config = createConfig(Test3.class);
    annotationReader.loadEndpointClass(serviceContext, Test3.class, config);
    annotationReader.loadEndpointMethods(
        serviceContext, Test3.class, config.getApiClassConfig().getMethods());
    methodConfig = config.getApiClassConfig()
        .getMethods().get(methodToEndpointMethod(Test3.class.getMethod("method")));
    validateMethodForAuth(methodConfig, empty, empty, empty, DEFAULT_AUTH_LEVEL);

    // Method explicitly set scopes, etc. empty and overwrite Api.
    @Api(scopes = {"s0", "s1 s2"}, audiences = {"a0", "a1"}, clientIds = {"c0", "c1"})
    final class Test4 {
    @SuppressWarnings("unused")
    @ApiMethod(scopes = {}, audiences = {}, clientIds = {})
      public void method() {}
    }
    config = createConfig(Test4.class);
    annotationReader.loadEndpointClass(serviceContext, Test4.class, config);
    annotationReader.loadEndpointMethods(
        serviceContext, Test4.class, config.getApiClassConfig().getMethods());
    methodConfig = config.getApiClassConfig()
        .getMethods().get(methodToEndpointMethod(Test4.class.getMethod("method")));
    validateMethodForAuth(methodConfig, empty, empty, empty, DEFAULT_AUTH_LEVEL);
  }

  private void verifySimpleLevelOverriding(ApiConfig config) throws Exception {
    assertEquals("resource1", config.getApiClassConfig().getResource());
    assertFalse(config.getApiClassConfig().getUseDatastore());

    ApiMethodConfig noOverrides =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            SimpleLevelOverridingApi.class.getMethod("noOverrides")));
    assertEquals(toScopeExpression("s0a", "s1a"), noOverrides.getScopeExpression());
    assertEquals(Lists.newArrayList("a0a", "a1a"), noOverrides.getAudiences());
    assertEquals(Lists.newArrayList("c0a", "c1a"), noOverrides.getClientIds());
    assertEquals(AuthLevel.REQUIRED, noOverrides.getAuthLevel());

    ApiMethodConfig overrides =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            SimpleLevelOverridingApi.class.getMethod("overrides")));
    assertEquals(toScopeExpression("s0b", "s1b"), overrides.getScopeExpression());
    assertEquals(Lists.newArrayList("a0b", "a1b"), overrides.getAudiences());
    assertEquals(Lists.newArrayList("c0b", "c1b"), overrides.getClientIds());
    assertEquals(AuthLevel.OPTIONAL, overrides.getAuthLevel());
  }

  @Test
  public void testSimpleLevelOverriding() throws Exception {
    ApiConfig config = createConfig(SimpleLevelOverridingApi.class);
    annotationReader.loadEndpointClass(serviceContext, SimpleLevelOverridingApi.class, config);
    annotationReader.loadEndpointMethods(serviceContext, SimpleLevelOverridingApi.class,
        config.getApiClassConfig().getMethods());

    verifySimpleLevelOverriding(config);
  }

  @Test
  public void testSimpleLevelOverridingWithInheritance() throws Exception {
    ApiConfig config = createConfig(SimpleLevelOverridingInheritedApi.class);
    annotationReader.loadEndpointClass(
        serviceContext, SimpleLevelOverridingInheritedApi.class, config);
    annotationReader.loadEndpointMethods(serviceContext, SimpleLevelOverridingInheritedApi.class,
        config.getApiClassConfig().getMethods());

    verifySimpleLevelOverriding(config);
  }

  @Test
  public void testLevelOverridingWithDefaultOverrides() throws Exception {
    @Api(
        scopes = {"s0c", "s1c"},
        audiences = {"a0c", "a1c"},
        clientIds = {"c0c", "c1c"},
        resource = "resource2",
        useDatastoreForAdditionalConfig = AnnotationBoolean.TRUE
    )
    final class Test extends SimpleLevelOverridingInheritedApi {
    }

    ApiConfig config = createConfig(Test.class);
    annotationReader.loadEndpointClass(serviceContext, Test.class, config);
    annotationReader.loadEndpointMethods(serviceContext, Test.class,
        config.getApiClassConfig().getMethods());

    // All values overridden at a lower level, so nothing should change.
    verifySimpleLevelOverriding(config);
  }

  @Test
  public void testLevelOverridingWithClassOverrides() throws Exception {
    @ApiClass(
        scopes = {"s0c", "s1c"},
        audiences = {"a0c", "a1c"},
        clientIds = {"c0c", "c1c"},
        resource = "resource2",
        useDatastoreForAdditionalConfig = AnnotationBoolean.TRUE
    )
    final class Test extends SimpleLevelOverridingInheritedApi {
    }

    ApiConfig config = createConfig(Test.class);
    annotationReader.loadEndpointClass(serviceContext, Test.class, config);
    assertEquals("resource2", config.getApiClassConfig().getResource());
    assertTrue(config.getApiClassConfig().getUseDatastore());

    annotationReader.loadEndpointMethods(serviceContext, Test.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig noOverrides =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            SimpleLevelOverridingApi.class.getMethod("noOverrides")));
    assertEquals(toScopeExpression("s0c", "s1c"), noOverrides.getScopeExpression());
    assertEquals(Lists.newArrayList("a0c", "a1c"), noOverrides.getAudiences());
    assertEquals(Lists.newArrayList("c0c", "c1c"), noOverrides.getClientIds());

    ApiMethodConfig overrides =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            SimpleLevelOverridingApi.class.getMethod("overrides")));
    assertEquals(toScopeExpression("s0b", "s1b"), overrides.getScopeExpression());
    assertEquals(Lists.newArrayList("a0b", "a1b"), overrides.getAudiences());
    assertEquals(Lists.newArrayList("c0b", "c1b"), overrides.getClientIds());
  }

  @Test
  public void testLevelOverridingWithMethodOverrides() throws Exception {
    final class Test extends SimpleLevelOverridingInheritedApi {
      @ApiMethod(
          scopes = {"s0c", "s1c"},
          audiences = {"a0c", "a1c"},
          clientIds = {"c0c", "c1c"}
      )
      @Override
      public void overrides() {
        super.overrides();
      }
    }

    ApiConfig config = createConfig(Test.class);
    annotationReader.loadEndpointClass(serviceContext, Test.class, config);
    annotationReader.loadEndpointMethods(serviceContext, Test.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig overrides =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            Test.class.getMethod("overrides")));
    assertEquals(toScopeExpression("s0c", "s1c"), overrides.getScopeExpression());
    assertEquals(Lists.newArrayList("a0c", "a1c"), overrides.getAudiences());
    assertEquals(Lists.newArrayList("c0c", "c1c"), overrides.getClientIds());
  }

  @Test
  public void testSimpleInheritanceCycleDetection() throws Exception {
    // Nest the classes, so Java doesn't get upset about declaration/reference order.
    @ApiReference(Test1.Test2.class)
    final class Test1 {
      @ApiReference(Test1.class)
      final class Test2 {}
    }

    try {
      ApiConfig config = createConfig(Test1.class);
      annotationReader.loadEndpointClass(serviceContext, Test1.class, config);
      fail();
    } catch (CyclicApiInheritanceException e) {
      // Expected.
    }
  }

  @Test
  public void testDeepInheritanceCycleDetection() throws Exception {
    // Nest the classes, so Java doesn't get upset about declaration/reference order.
    @ApiReference(Test1.Test2.class)
    @Api(name = "Test1")
    final class Test1 {
      @ApiReference(Test2.Test3.class)
      final class Test2 {
        @ApiReference(Test3.Test4.class)
        @Api(name = "Test3")
        final class Test3 {
          @ApiReference(Test4.Test5.class)
          final class Test4 {
            @ApiReference(Test5.Test6.class)
            @Api(name = "Test5")
            final class Test5 {
              @ApiReference(Test6.Test7.class)
              final class Test6 {
                @ApiReference(Test7.Test8.class)
                @Api(name = "Test7")
                final class Test7 {
                  @ApiReference(Test1.class)
                  final class Test8 {}
                }
              }
            }
          }
        }
      }
    }

    try {
      ApiConfig config = createConfig(Test1.class);
      annotationReader.loadEndpointClass(serviceContext, Test1.class, config);
      fail();
    } catch (CyclicApiInheritanceException e) {
      // Expected.
    }
  }

  @Test
  public void testGenericParameterTypes() throws Exception {
    @Api
    final class Test <T> {
      @SuppressWarnings("unused")
      public void setT(T t) {}
    }

    ApiConfig config = createConfig(Test.class);
    annotationReader.loadEndpointMethods(serviceContext, Test.class,
        config.getApiClassConfig().getMethods());

    ApiParameterConfig parameter =
        config.getApiClassConfig().getMethods()
            .get(methodToEndpointMethod(Test.class.getDeclaredMethod("setT", Object.class)))
            .getParameterConfigs()
            .get(0);
    assertEquals(ApiParameterConfig.Classification.UNKNOWN, parameter.getClassification());
  }

  @Test
  public void testGenericParameterTypeThroughMethodCall() throws Exception {
    this.<Integer>genericParameterTypeTestImpl();
  }

  private <T> void genericParameterTypeTestImpl() throws Exception {
    @Api
    class Bar <T1> {
      @SuppressWarnings("unused")
      public void bar(T1 t1) {}
    }
    class Foo extends Bar<T> {}

    ApiConfig config = createConfig(Foo.class);
    annotationReader.loadEndpointMethods(serviceContext, Foo.class,
        config.getApiClassConfig().getMethods());

    ApiParameterConfig parameter =
        config.getApiClassConfig().getMethods()
            .get(methodToEndpointMethod(
                Foo.class.getSuperclass().getDeclaredMethod("bar", Object.class)))
            .getParameterConfigs()
            .get(0);
    assertEquals(ApiParameterConfig.Classification.UNKNOWN, parameter.getClassification());
  }

  @Test
  public void testKnownParameterizedType() throws Exception {
    @Api
    class Bar <T1> {
      @SuppressWarnings("unused")
      public void bar(T1 t1) {}
    }
    class Foo extends Bar<Integer> {}

    ApiConfig config = createConfig(Foo.class);
    annotationReader.loadEndpointMethods(serviceContext, Foo.class,
        config.getApiClassConfig().getMethods());

    ApiParameterConfig parameter =
        config.getApiClassConfig().getMethods()
            .get(methodToEndpointMethod(
                Foo.class.getSuperclass().getDeclaredMethod("bar", Object.class)))
            .getParameterConfigs()
            .get(0);
    assertEquals(ApiParameterConfig.Classification.API_PARAMETER, parameter.getClassification());
  }

  @Test
  public void testSuperclassWithoutApi() throws Exception {
    @Api
    class Foo {
      @SuppressWarnings("unused")
      public void foo() {}
    }

    class Bar extends Foo {
      @ApiMethod(name = "overridden")
      @Override
      public void foo() {}
    }

    ApiConfig config = createConfig(Bar.class);
    annotationReader.loadEndpointClass(serviceContext, Bar.class, config);
    annotationReader.loadEndpointMethods(serviceContext, Bar.class,
        config.getApiClassConfig().getMethods());

    // Make sure method comes from Bar even though that class is not annotated with @Api.
    ApiMethodConfig methodConfig =
        Iterables.getOnlyElement(config.getApiClassConfig().getMethods().values());
    assertEquals("overridden", methodConfig.getName());
    assertEquals(Bar.class.getName() + ".foo", methodConfig.getFullJavaName());
  }

  @Test
  public void testParameterAnnotations() throws Exception {
    @Api
    class Endpoint {
      @SuppressWarnings("unused")
      public void method(@Named("foo") @Nullable @DefaultValue("4") int foo) {}
    }

    ApiConfig config = createConfig(Endpoint.class);
    annotationReader.loadEndpointClass(serviceContext, Endpoint.class, config);
    annotationReader.loadEndpointMethods(serviceContext, Endpoint.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig methodConfig =
        Iterables.getOnlyElement(config.getApiClassConfig().getMethods().values());
    ApiParameterConfig parameterConfig =
        Iterables.getOnlyElement(methodConfig.getParameterConfigs());
    validateParameter(parameterConfig, "foo", true, "4", int.class, null, int.class);
  }

  @Test
  public void testParameterAnnotations_javax() throws Exception {
    @Api
    class Endpoint {
      @SuppressWarnings("unused")
      public void method(@javax.inject.Named("foo") @javax.annotation.Nullable int foo) {}
    }

    ApiConfig config = createConfig(Endpoint.class);
    annotationReader.loadEndpointClass(serviceContext, Endpoint.class, config);
    annotationReader.loadEndpointMethods(serviceContext, Endpoint.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig methodConfig =
        Iterables.getOnlyElement(config.getApiClassConfig().getMethods().values());
    ApiParameterConfig parameterConfig =
        Iterables.getOnlyElement(methodConfig.getParameterConfigs());
    validateParameter(parameterConfig, "foo", true, null, int.class, null, int.class);
  }

  @Test
  public void testParameterAnnotations_none() throws Exception {
    @Api
    class Endpoint {
      @SuppressWarnings("unused")
      public void method(int foo) {}
    }

    ApiConfig config = createConfig(Endpoint.class);
    annotationReader.loadEndpointClass(serviceContext, Endpoint.class, config);
    annotationReader.loadEndpointMethods(serviceContext, Endpoint.class,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig methodConfig =
        Iterables.getOnlyElement(config.getApiClassConfig().getMethods().values());
    ApiParameterConfig parameterConfig =
        Iterables.getOnlyElement(methodConfig.getParameterConfigs());
    validateParameter(parameterConfig, null, false, null, int.class, null, int.class);
  }

  private <T> String implValidTestDefaultValuedParameter(
      Class<? extends DefaultValuedEndpoint<T>> clazz) throws Exception {
    ApiConfig config = createConfig(clazz);
    annotationReader.loadEndpointClass(serviceContext, clazz, config);
    annotationReader.loadEndpointMethods(serviceContext, clazz,
        config.getApiClassConfig().getMethods());

    ApiMethodConfig method =
        Iterables.getOnlyElement(config.getApiClassConfig().getMethods().values());

    String defaultValue = method.getParameterConfigs().get(0).getDefaultValue();
    assertNotNull(defaultValue);

    return defaultValue;
  }

  private void validateEndpoint1(ApiConfig config, Class<? extends Endpoint1> clazz)
      throws Exception {
    assertEquals("api", config.getName());
    assertEquals("v1", config.getVersion());
    assertTrue(config.getIsDefaultVersion());
    assertFalse(config.getIsAbstract());
    assertTrue(config.getApiClassConfig().getUseDatastore());
    assertEquals(ImmutableList.of(PassAuthenticator.class), config.getAuthenticators());

    ApiAuthConfig auth = config.getAuthConfig();
    assertTrue(auth.getAllowCookieAuth());
    assertEquals(1, auth.getBlockedRegions().size());
    assertEquals("CU", auth.getBlockedRegions().get(0));

    ApiFrontendLimitsConfig frontendLimits = config.getFrontendLimitsConfig();
    assertEquals(1, frontendLimits.getUnregisteredUserQps());
    assertEquals(2, frontendLimits.getUnregisteredQps());
    assertEquals(3, frontendLimits.getUnregisteredDaily());
    // TODO: After fixing http://b/7459341, add check for number of rules.
    assertEquals("match0", frontendLimits.getRules().get(0).getMatch());
    assertEquals(1, frontendLimits.getRules().get(0).getQps());
    assertEquals(2, frontendLimits.getRules().get(0).getUserQps());
    assertEquals(3, frontendLimits.getRules().get(0).getDaily());
    assertEquals("analyticsId0", frontendLimits.getRules().get(0).getAnalyticsId());
    assertEquals("match10", frontendLimits.getRules().get(1).getMatch());
    assertEquals(11, frontendLimits.getRules().get(1).getQps());
    assertEquals(12, frontendLimits.getRules().get(1).getUserQps());
    assertEquals(13, frontendLimits.getRules().get(1).getDaily());
    assertEquals("analyticsId10", frontendLimits.getRules().get(1).getAnalyticsId());

    ApiCacheControlConfig cacheControl = config.getCacheControlConfig();
    assertEquals(ApiCacheControl.Type.PUBLIC, cacheControl.getType());
    assertEquals(1, cacheControl.getMaxAge());

    ApiSerializationConfig serialization = config.getSerializationConfig();
    assertEquals(1, serialization.getSerializerConfigs().size());
    ApiSerializationConfig.SerializerConfig serializerConfig =
        serialization.getSerializerConfig(TypeToken.of(SimpleBean.class));
    assertEquals(TypeToken.of(SimpleBean.class), serializerConfig.getSourceType());
    assertEquals(DumbSerializer1.class, serializerConfig.getSerializer());

    String[] defaultScopes = {"ss0", "ss1 ss2"};
    String[] defaultAudiences = {"aa0", "aa1"};
    String[] defaultClientIds = {"cc0", "cc1"};

    ApiMethodConfig listFoos = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        Endpoint1.class.getMethod("listFoos")));
    String[] expectedScopes = { "s0", "s1 s2" };
    String[] expectedAudiences = { "a0", "a1" };
    String[] expectedClientIds = { "c0", "c1" };
    validateMethod(listFoos, "foos.list", "foos", ApiMethod.HttpMethod.GET, expectedScopes,
        expectedAudiences, expectedClientIds, ImmutableList.of(FailAuthenticator.class),
        ImmutableList.of(FailPeerAuthenticator.class));
    assertEquals(0, listFoos.getParameterConfigs().size());

    ApiMethodConfig getFoo = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        Endpoint1.class.getMethod("getFoo", String.class)));
    validateMethod(getFoo,
        "foos.get",
        "foos/{id}",
        ApiMethod.HttpMethod.GET,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(1, getFoo.getParameterConfigs().size());
    validateParameter(getFoo.getParameterConfigs().get(0), "id", false, null, String.class);

    ApiMethodConfig insertFoo = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        Endpoint1.class.getMethod("insertFoo", Foo.class)));
    validateMethod(insertFoo,
        "foos.insert",
        "foos",
        ApiMethod.HttpMethod.POST,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(1, insertFoo.getParameterConfigs().size());
    validateParameter(insertFoo.getParameterConfigs().get(0), null, false, null, Foo.class);

    ApiMethodConfig updateFoo = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        Endpoint1.class.getMethod("updateFoo", String.class, Foo.class)));
    validateMethod(updateFoo,
        "foos.update",
        "foos/{id}",
        ApiMethod.HttpMethod.PUT,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(2, updateFoo.getParameterConfigs().size());
    validateParameter(updateFoo.getParameterConfigs().get(0), "id", false, null, String.class);
    validateParameter(updateFoo.getParameterConfigs().get(1), null, false, null, Foo.class);

    ApiMethodConfig removeFoo = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        Endpoint1.class.getMethod("removeFoo", String.class)));
    validateMethod(removeFoo, "foos.remove", "foos/{id}", ApiMethod.HttpMethod.DELETE,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(1, removeFoo.getParameterConfigs().size());
    validateParameter(removeFoo.getParameterConfigs().get(0), "id", false, null, String.class);

    ApiMethodConfig execute0 = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        Endpoint1.class.getMethod("execute0", String.class, int.class, Integer.class, long.class,
            Long.class, boolean.class, Boolean.class, float.class, Double.class)));
    validateMethod(execute0,
        "foos.execute0",
        "execute0",
        "POST",
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(9, execute0.getParameterConfigs().size());
    validateParameter(execute0.getParameterConfigs().get(0), "id", false, null, String.class);
    validateParameter(execute0.getParameterConfigs().get(1), "i0", false, null, int.class);
    validateParameter(execute0.getParameterConfigs().get(2), "i1", true, null, Integer.class);
    validateParameter(execute0.getParameterConfigs().get(3), "long0", false, null, long.class);
    validateParameter(execute0.getParameterConfigs().get(4), "long1", true, null, Long.class);
    validateParameter(execute0.getParameterConfigs().get(5), "b0", false, null, boolean.class);
    validateParameter(execute0.getParameterConfigs().get(6), "b1", true, null, Boolean.class);
    validateParameter(execute0.getParameterConfigs().get(7), "f", false, null, float.class);
    validateParameter(execute0.getParameterConfigs().get(8), "d", true, null, Double.class);

    ApiMethodConfig execute1 = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        Endpoint1.class.getMethod("execute1", Foo.class)));
    validateMethod(execute1, clazz.getSimpleName() + ".execute1", "execute1",
        ApiMethod.HttpMethod.POST,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(1, execute1.getParameterConfigs().size());
    validateParameter(execute1.getParameterConfigs().get(0), null, false, null, Foo.class);

    ApiMethodConfig execute2 = config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
        Endpoint1.class.getMethod("execute2", SimpleBean.class)));
    validateMethod(execute2, "foos.execute2", "execute2/{serialized}",
        ApiMethod.HttpMethod.POST,
        defaultScopes,
        defaultAudiences,
        defaultClientIds,
        ImmutableList.of(PassAuthenticator.class),
        ImmutableList.of(PassPeerAuthenticator.class));
    assertEquals(1, execute2.getParameterConfigs().size());
    validateParameter(execute2.getParameterConfigs().get(0), "serialized", false, null,
        SimpleBean.class, DumbSerializer1.class, String.class);
  }

  private void validateMethod(ApiMethodConfig method, String name, String path, String httpMethod,
      String[] scopes,
      String[] audiences,
      String[] clientIds,
      List<?> authenticators,
      List<?> peerAuthenticators) {
    assertEquals(name, method.getName());
    assertEquals(path, method.getPath());
    assertEquals(httpMethod, method.getHttpMethod());
    assertEquals(toScopeExpression(scopes), method.getScopeExpression());
    assertEquals(Arrays.asList(audiences), method.getAudiences());
    assertEquals(Arrays.asList(clientIds), method.getClientIds());
    assertEquals(authenticators, method.getAuthenticators());
    assertEquals(peerAuthenticators, method.getPeerAuthenticators());
  }

  private void validateMethodForAuth(ApiMethodConfig method, String[] scopes, String[] audiences,
      String[] clientIds, AuthLevel authLevel) {
    assertEquals(toScopeExpression(scopes), method.getScopeExpression());
    assertEquals(Arrays.asList(audiences), method.getAudiences());
    assertEquals(Arrays.asList(clientIds), method.getClientIds());
    assertEquals(authLevel, method.getAuthLevel());
  }

  private void validateParameter(ApiParameterConfig parameter, String name, boolean nullable,
      String defaultValue, Type type) {
    validateParameter(parameter, name, nullable, defaultValue, type, null, type);
  }

  private void validateParameter(ApiParameterConfig parameter, String name, boolean nullable,
      String defaultValue, Type type, Class<?> serializer, Type serializedType) {
    assertEquals(name, parameter.getName());
    assertEquals(nullable, parameter.getNullable());
    assertEquals(defaultValue, parameter.getDefaultValue());
    assertEquals(TypeToken.of(type), parameter.getType());
    if (serializer == null) {
      assertTrue(parameter.getSerializers().isEmpty());
    } else {
      assertEquals(Collections.singletonList(serializer), parameter.getSerializers());
    }
    assertEquals(TypeToken.of(serializedType), parameter.getSchemaBaseType());
  }

  private EndpointMethod methodToEndpointMethod(Method method) {
    return EndpointMethod.create(method.getDeclaringClass(), method);
  }

  private EndpointMethod methodToEndpointMethod(Class<?> endpointClass, Method method) {
    return EndpointMethod.create(endpointClass, method, TypeToken.of(method.getDeclaringClass()));
  }

  private static AuthScopeExpression toScopeExpression(String... scopes) {
    return AuthScopeExpressions.interpret(scopes);
  }
}
