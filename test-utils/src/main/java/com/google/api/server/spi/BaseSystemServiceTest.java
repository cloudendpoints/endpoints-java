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
package com.google.api.server.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.ApiConfigWriter;
import com.google.api.server.spi.config.jsonwriter.JsonConfigWriter;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.config.validation.ApiConfigValidator;
import com.google.api.server.spi.request.FakeParamReader;
import com.google.api.server.spi.response.ErrorResultWriter;
import com.google.api.server.spi.response.SuccessResultWriter;
import com.google.api.server.spi.testing.Endpoint0;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * Base test class for {@link SystemService}.
 */
public abstract class BaseSystemServiceTest {

  protected Object service;
  protected Method succeed;
  protected Method fail;
  protected Method failOAuth;
  protected Method failIllegalArgumentException;
  protected Method superclassMethod;
  protected SystemService systemService;

  @Before
  public void setUp() throws Exception {
    service = getTestService();
    systemService = getSystemService(new Object[] {service}, false);
    succeed = getTestServiceMethod("succeed");
    fail = getTestServiceMethod("fail");
    failOAuth = getTestServiceMethod("failOAuth");
    failIllegalArgumentException = getTestServiceMethod("failIllegalArgumentException");
    superclassMethod = getTestServiceMethod("superclassMethod");
  }

  @Test
  public void testFindServiceSimpleNameSuccess() throws Exception {
    assertEquals(getTestService().getClass(),
        systemService.findService(TestEndpoint.class.getSimpleName()).getClass());
  }

  @Test
  public void testRegisterMethodsCount() throws Exception {
    SystemService systemService =
        getSystemService(new Object[] {}, false /* isIllegalArgumentBackendError */);
    int numExpectedMethods = TestEndpoint.ExpectedMethod.class.getEnumConstants().length - 1;
    assertEquals(numExpectedMethods, systemService.registerService(getTestService()));
  }

  @Test
  public void testRegisterDuplicateMethod() throws Exception  {
    assertEquals(SystemService.DUPLICATE_SERVICE_REGISTER_COUNT,
        systemService.registerService(getTestService()));
  }

  @Test
  public void testFindServiceSimpleNameFail() throws Exception {
    systemService.registerService(new InnerClasses.TestEndpoint()); // register same twice
    try {
      systemService.findService(TestEndpoint.class.getSimpleName());
      fail("This should result in an ambiguous call.");
    } catch (ServiceException e) {
      assertEquals(500, e.getStatusCode());
    }
  }

  @Test
  public void testFindServiceSimpleNameFailForceful() throws Exception {
    systemService.registerServiceFromName(new Foo(), TestEndpoint.class.getSimpleName(), null);
    try {
      systemService.findService(TestEndpoint.class.getSimpleName());
      fail("This should result in an ambiguous call.");
    } catch (ServiceException e) {
      assertEquals(500, e.getStatusCode());
    }
  }

  @Test
  public void testFullNameSuccess() throws Exception {
    systemService.registerServiceFromName(new Foo(), TestEndpoint.class.getSimpleName(), null);
    assertEquals(getTestService().getClass(),
        systemService.findService(TestEndpoint.class.getName()).getClass());
  }

  @Test
  public void testFindService_notFound() {
    try {
      systemService.findService("cheese");
      fail();
    } catch (ServiceException e) {
      assertEquals(404, e.getStatusCode());
    }
  }

  @Test
  public void testFindServiceMethod() {
    assertEquals("succeed", succeed.getName());
    assertEquals("superclassMethod", superclassMethod.getName());
  }

  @Test
  public void testOverrideMethod() throws Exception {
    assertEquals(String.class, getTestServiceMethod("overrideMethod").getReturnType());
  }

  @Test
  public void testInvokeServiceMethod() throws Exception {
    systemService.invokeServiceMethod(service, succeed,
        new FakeParamReader("string", true, 99, 9999999999L, 9.9f, .99, false, 99, 9999999999L,
            9.9f, .99, null, null, null, null), new SuccessResultWriter(TestEndpoint.RESULT));
  }

  @Test
  public void testServiceException() throws Exception {
    systemService
        .invokeServiceMethod(service, fail, new FakeParamReader("string", 99, null, null, null),
            new ErrorResultWriter(400, TestEndpoint.ERROR_MESSAGE));
  }

  @Test
  public void testWrappedException() throws Exception {
    systemService.invokeServiceMethod(service, getTestServiceMethod("failWrapped"),
        new FakeParamReader(), new ErrorResultWriter(401, TestEndpoint.ERROR_MESSAGE));
  }

  @Test
  public void testOAuthException() throws Exception {
    systemService.invokeServiceMethod(service, failOAuth,
        new FakeParamReader("string", 99, null, null, null),
        new ErrorResultWriter(401, TestEndpoint.ERROR_MESSAGE, true));
  }

  @Test
  public void testIllegalArgumentExceptionUserError() throws Exception {
    systemService.invokeServiceMethod(service, failIllegalArgumentException, new FakeParamReader(),
        new ErrorResultWriter(400));
  }

  @Test
  public void testIllegalArgumentExceptionServerError() throws Exception {
    systemService = getSystemService(new Object[] {service}, true);
    systemService.invokeServiceMethod(service, failIllegalArgumentException, new FakeParamReader(),
        new ErrorResultWriter(500));
  }

  protected SystemService getSystemService(Object[] services, boolean isIllegalArgumentBackendError)
      throws Exception {
    return getSystemService(
        services, isIllegalArgumentBackendError, true /* enableBackendService */);
  }

  protected SystemService getSystemService(Object[] services, boolean isIllegalArgumentBackendError,
      boolean enableBackendService) throws Exception {

    return new SystemService(
        new ApiConfigLoader(), new ApiConfigValidator(), "app", new JsonConfigWriter(), services,
        isIllegalArgumentBackendError);
  }

  protected abstract TestEndpoint getTestService();

  protected abstract Endpoint0 getTestService2();

  protected Method getTestServiceMethod(String name) throws Exception {
    return systemService.findServiceMethod(service, name);
  }

  private static class Foo {
  }

  private static class InnerClasses {
    @Api private static class TestEndpoint {}
  }

  @Test
  public void testResolveAndUpdateServiceConfig() throws Exception {
    ApiConfigLoader configLoader = Mockito.spy(new ApiConfigLoader());
    ApiConfigValidator validator = Mockito.mock(ApiConfigValidator.class);

    SystemService systemService = new SystemService(
        configLoader, validator, "app", new JsonConfigWriter(), new Object[] { service },
        false /* isIllegalArgumentBackendError */);

    // Force the config loader to return a different config from that used during registration.
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), TestEndpoint.class);
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().get(
        methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")));
    methodConfig.setScopeExpression(AuthScopeExpressions.interpret("foo"));
    Mockito.doReturn(config).when(configLoader).reloadConfiguration(
        Mockito.<ServiceContext>any(), Mockito.eq(TestEndpoint.class), Mockito.<ApiConfig>any());
    Mockito.doReturn(false).when(configLoader).isStaticConfig(Mockito.<ApiConfig>any());

    assertEquals(methodConfig,
        systemService.resolveAndUpdateServiceConfig("TestEndpoint", "getResultNoParams"));
  }

  @Test
  public void testResolveAndUpdateServiceConfig_noChange() throws Exception {
    ApiConfigLoader configLoader = Mockito.spy(new ApiConfigLoader());
    ApiConfigValidator validator = Mockito.mock(ApiConfigValidator.class);

    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), TestEndpoint.class);
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().get(
        methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")));

    Mockito.doReturn(false).when(configLoader).isStaticConfig(Mockito.<ApiConfig>any());

    SystemService systemService = new SystemService(
        configLoader, validator, "app", new JsonConfigWriter(), new Object[] { service },
        false /* isIllegalArgumentBackendError */);

    assertEquals(methodConfig,
        systemService.resolveAndUpdateServiceConfig("TestEndpoint", "getResultNoParams"));
  }

  @Test
  public void testResolveAndUpdateServiceConfig_staticConfig() throws Exception {
    ApiConfigLoader configLoader = Mockito.spy(new ApiConfigLoader());
    ApiConfigValidator validator = Mockito.mock(ApiConfigValidator.class);

    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), TestEndpoint.class);
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().get(
        methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")));

    ApiConfig alteredConfig =
        configLoader.loadConfiguration(ServiceContext.create(), TestEndpoint.class);
    ApiMethodConfig alteredMethodConfig = alteredConfig.getApiClassConfig().getMethods().get(
        methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")));
    alteredMethodConfig.setScopeExpression(AuthScopeExpressions.interpret("foo"));
    Mockito.doReturn(alteredConfig).when(configLoader).reloadConfiguration(
        Mockito.<ServiceContext>any(), Mockito.eq(TestEndpoint.class), Mockito.<ApiConfig>any());
    Mockito.doReturn(true).when(configLoader).isStaticConfig(Mockito.<ApiConfig>any());

    SystemService systemService = new SystemService(
        configLoader, validator, "app", new JsonConfigWriter(), new Object[] { service },
        false /* isIllegalArgumentBackendError */);

    assertEquals(methodConfig,
        systemService.resolveAndUpdateServiceConfig("TestEndpoint", "getResultNoParams"));
  }

  @Test
  public void testResolveAndUpdateServiceConfig_badConfig() throws Exception {
    ApiConfigLoader configLoader = Mockito.spy(new ApiConfigLoader());
    ApiConfigValidator validator = Mockito.mock(ApiConfigValidator.class);

    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), TestEndpoint.class);
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().get(
        methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")));

    SystemService systemService = new SystemService(
        configLoader, validator, "app", new JsonConfigWriter(), new Object[] { service },
        false  /* isIllegalArgumentBackendError */);

    Mockito.doThrow(new ApiConfigException("bleh")).when(configLoader).reloadConfiguration(
        Mockito.<ServiceContext>any(), Mockito.eq(TestEndpoint.class), Mockito.<ApiConfig>any());
    Mockito.doReturn(false).when(configLoader).isStaticConfig(Mockito.<ApiConfig>any());

    // Should use previously registered config when new config fails to load.
    assertEquals(methodConfig,
        systemService.resolveAndUpdateServiceConfig("TestEndpoint", "getResultNoParams"));
  }

  @Test
  public void testResolveAndUpdateServiceConfig_badConfigAfterGood() throws Exception {
    ApiConfigLoader configLoader = Mockito.spy(new ApiConfigLoader());
    ApiConfigValidator validator = Mockito.mock(ApiConfigValidator.class);

    SystemService systemService = new SystemService(
        configLoader, validator, "app", new JsonConfigWriter(), new Object[] { service },
        false /* isIllegalArgumentBackendError */);

    // Force the config loader to return a different config from that used during registration.
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), TestEndpoint.class);
    ApiMethodConfig methodConfig =
        config.getApiClassConfig().getMethods().get(methodToEndpointMethod(
            TestEndpoint.class.getMethod("getResultNoParams")));
    methodConfig.setScopeExpression(AuthScopeExpressions.interpret("foo"));
    Mockito.doReturn(config).when(configLoader).reloadConfiguration(
        Mockito.<ServiceContext>any(), Mockito.eq(TestEndpoint.class), Mockito.<ApiConfig>any());
    Mockito.doReturn(false).when(configLoader).isStaticConfig(Mockito.<ApiConfig>any());

    assertEquals(methodConfig,
        systemService.resolveAndUpdateServiceConfig("TestEndpoint", "getResultNoParams"));

    Mockito.doThrow(new ApiConfigException("bleh")).when(configLoader).reloadConfiguration(
        Mockito.<ServiceContext>any(), Mockito.eq(TestEndpoint.class), Mockito.<ApiConfig>any());

    // Should use previously used config when new config fails to load.
    assertEquals(methodConfig,
        systemService.resolveAndUpdateServiceConfig("TestEndpoint", "getResultNoParams"));
  }

  @Test
  public void testGetApiConfigs() throws Exception {
    final String apiConfig1 = "api1 configuration";
    final String apiConfig2 = "api2 configuration";

    ApiConfigLoader configLoader = Mockito.spy(new ApiConfigLoader());
    ApiConfigValidator configValidator = Mockito.mock(ApiConfigValidator.class);
    ApiConfigWriter configWriter = Mockito.mock(ApiConfigWriter.class);

    ApiConfig config1 = createGenericApiConfig();
    config1.setName("api1");
    config1.setVersion("v1");
    ApiConfig config2 = createGenericApiConfig();
    config2.setName("api2");
    config2.setVersion("v10");

    Mockito.doReturn(config1).doReturn(config2).when(configLoader).loadConfiguration(
        Mockito.<ServiceContext>any(), Mockito.<Class<?>>any());

    ImmutableMap<ApiKey, String> expected =
        ImmutableMap.of(new ApiKey("api1", "v1"), apiConfig1, new ApiKey("api2", "v2"), apiConfig2);

    Mockito.doReturn(expected)
        .when(configWriter).writeConfig(setOf(config1, config2));

    SystemService systemService =
        new SystemService(configLoader, configValidator, "app", configWriter,
            new Object[] { service, getTestService2() }, false /* isIllegalArgumentBackendError */);

    Map<ApiKey, String> configs = systemService.getApiConfigs();

    Mockito.verify(configWriter, times(1)).writeConfig(setOf(config1, config2));
    assertEquals(expected, configs);
  }

  @Test
  public void testGetApiConfigsShouldNotIncludeInternalEndpoint() throws Exception {
    assertTrue(getSystemService(new Object[]{}, false).getApiConfigs().isEmpty());
  }

  private static ApiConfig createGenericApiConfig() throws Exception {
    return new ApiConfig.Factory().create(ServiceContext.create(), new TypeLoader(),
        TestEndpoint.class);
  }

  private EndpointMethod methodToEndpointMethod(Method method) {
    return EndpointMethod.create(method.getDeclaringClass(), method);
  }

  private static Iterable<ApiConfig> setOf(ApiConfig... configs) {
    return argThat(new ConfigListMatcher(configs));
  }

  private static class ConfigListMatcher extends ArgumentMatcher<Iterable<ApiConfig>> {
    private Set<ApiConfig> expectedConfigs;

    public ConfigListMatcher(ApiConfig... expectedConfigs) {
      this.expectedConfigs = Sets.newHashSet(expectedConfigs);
    }

    @Override
    public boolean matches(Object argument) {
      return argument instanceof Iterable
          && expectedConfigs.equals(Sets.newHashSet((Iterable<ApiConfig>) argument));
    }
  }
}
