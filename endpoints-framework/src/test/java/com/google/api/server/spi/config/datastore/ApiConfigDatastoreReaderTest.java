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
package com.google.api.server.spi.config.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.datastore.testing.FakeAsyncMemcacheService;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalModulesServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

/**
 * Tests for {@link ApiConfigDatastoreReader} using actual (local) appengine datastore and memcache
 * services.
 *
 * @author Eric Orth
 */
@RunWith(JUnit4.class)
public class ApiConfigDatastoreReaderTest {
  private ApiConfigDatastoreReader datastoreReader;

  private LocalServiceTestHelper serviceTestHelper;
  private DatastoreService datastoreService;
  private MemcacheService memcacheService;

  private ServiceContext serviceContext;
  private ApiConfig config;
  private ApiConfig expectedConfig;

  private static final String ENTITY_NAME = TestEndpoint.class.getName();
  private static final String MEMCACHE_KEY = ApiConfigDatastoreReader.ENDPOINT_CONFIGURATION_KIND +
      "." + ENTITY_NAME;

  private static final List<String> SCOPES = ImmutableList.of("scope0", "scope1", "scope2");
  private static final AuthScopeExpression SIMPLE_SCOPE_EXPRESSION =
      AuthScopeExpressions.interpret(SCOPES.get(0));
  private static final AuthScopeExpression COMPLEX_SCOPE_EXPRESSION =
      AuthScopeExpressions.interpret(SCOPES);
  private static final List<String> AUDIENCES =
      ImmutableList.of("audience0", "audience1", "audience2");
  private static final List<String> CLIENT_IDS =
      ImmutableList.of("clientId0", "clientId1", "clientId2");
  private static final List<?> BAD_SCOPES = ImmutableList.of("scope0", 4, "scope2");

  @Before
  public void setUp() throws Exception {
    serviceTestHelper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig(),
        new LocalMemcacheServiceTestConfig(), new LocalModulesServiceTestConfig());
    serviceTestHelper.setUp();
    datastoreService = DatastoreServiceFactory.getDatastoreService();
    memcacheService = MemcacheServiceFactory.getMemcacheService();

    serviceContext = ServiceContext.create();
    ApiConfig.Factory configFactory = new ApiConfig.Factory();
    config = configFactory.create(serviceContext, new TypeLoader(), TestEndpoint.class);
    config.setUseDatastore(true);
    expectedConfig = configFactory.copy(config);
    assertEquals(expectedConfig, config);

    datastoreReader = new ApiConfigDatastoreReader(
        datastoreService, new FakeAsyncMemcacheService(memcacheService));
  }

  @After
  public void tearDown() throws Exception {
    serviceTestHelper.tearDown();
  }

  private Entity createEntity() {
    return new Entity(ApiConfigDatastoreReader.ENDPOINT_CONFIGURATION_KIND, ENTITY_NAME);
  }

  @Test
  public void testAssertStaticConfig() throws Exception {
    config.getApiClassConfig().setUseDatastore(false);
    assertTrue(datastoreReader.isStaticConfig(config));
    config.getApiClassConfig().setUseDatastore(true);
    assertFalse(datastoreReader.isStaticConfig(config));
  }

  @Test
  public void testLoadNoDatastore() throws Exception {
    Entity entity = createEntity();
    entity.setUnindexedProperty("scopes", SCOPES);
    entity.setUnindexedProperty("audiences", AUDIENCES);
    entity.setUnindexedProperty("clientIds", CLIENT_IDS);
    datastoreService.put(entity);

    config.setUseDatastore(false);
    datastoreReader.loadEndpointClass(serviceContext, TestEndpoint.class, config);

    expectedConfig.setUseDatastore(false);
    assertEquals(expectedConfig, config);

    assertFalse(memcacheService.contains(MEMCACHE_KEY));
  }

  @Test
  public void testLoadSource() throws Exception {
    Entity entity = createEntity();
    entity.setUnindexedProperty("scopes", SCOPES);
    entity.setUnindexedProperty("audiences", AUDIENCES);
    entity.setUnindexedProperty("clientIds", CLIENT_IDS);
    datastoreService.put(entity);

    datastoreReader.loadEndpointClass(serviceContext, TestEndpoint.class, config);

    expectedConfig.getApiClassConfig().setScopeExpression(COMPLEX_SCOPE_EXPRESSION);
    expectedConfig.getApiClassConfig().setAudiences(AUDIENCES);
    expectedConfig.getApiClassConfig().setClientIds(CLIENT_IDS);
    assertEquals(expectedConfig, config);

    Object memcacheValue = memcacheService.get(MEMCACHE_KEY);
    assertEquals(entity, memcacheValue);
  }

  @Test
  public void testLoadSource_singleStrings() throws Exception {
    Entity entity = createEntity();
    entity.setUnindexedProperty("scopes", SCOPES.get(0));
    entity.setUnindexedProperty("audiences", AUDIENCES.get(0));
    entity.setUnindexedProperty("clientIds", CLIENT_IDS.get(0));
    datastoreService.put(entity);

    datastoreReader.loadEndpointClass(serviceContext, TestEndpoint.class, config);

    expectedConfig.getApiClassConfig().setScopeExpression(SIMPLE_SCOPE_EXPRESSION);
    expectedConfig.getApiClassConfig().setAudiences(ImmutableList.of(AUDIENCES.get(0)));
    expectedConfig.getApiClassConfig().setClientIds(ImmutableList.of(CLIENT_IDS.get(0)));
    assertEquals(expectedConfig, config);

    Object memcacheValue = memcacheService.get(MEMCACHE_KEY);
    assertEquals(entity, memcacheValue);
  }

  @Test
  public void testLoadSource_noValues() throws Exception {
    Entity entity = createEntity();
    datastoreService.put(entity);

    datastoreReader.loadEndpointClass(serviceContext, TestEndpoint.class, config);
    assertEquals(expectedConfig, config);

    Object memcacheValue = memcacheService.get(MEMCACHE_KEY);
    assertEquals(entity, memcacheValue);
  }

  @Test
  public void testLoadSource_notPresent() throws Exception {
    datastoreReader.loadEndpointClass(serviceContext, TestEndpoint.class, config);
    assertEquals(expectedConfig, config);

    assertFalse(memcacheService.contains(MEMCACHE_KEY));
  }

  @Test
  public void testLoadSource_badProperty() throws Exception {
    Entity entity = createEntity();
    entity.setUnindexedProperty("scopes", true);
    datastoreService.put(entity);

    try {
      datastoreReader.loadEndpointClass(serviceContext, TestEndpoint.class, config);
      fail("Expected exception not thrown.");
    } catch (ApiConfigException expected) {
    }

    assertEquals(expectedConfig, config);

    Object memcacheValue = memcacheService.get(MEMCACHE_KEY);
    assertEquals(entity, memcacheValue);
  }

  @Test
  public void testLoadSource_badArray() throws Exception {
    Entity entity = createEntity();
    entity.setUnindexedProperty("scopes", BAD_SCOPES);
    datastoreService.put(entity);

    try {
      datastoreReader.loadEndpointClass(serviceContext, TestEndpoint.class, config);
      fail("Expected exception not thrown.");
    } catch (ApiConfigException expected) {
    }

    assertEquals(expectedConfig, config);

    Object memcacheValue = memcacheService.get(MEMCACHE_KEY);
    assertEquals(entity, memcacheValue);
  }

  @Test
  public void testLoadFromMemcache() throws Exception {
    Entity entity = createEntity();
    entity.setUnindexedProperty("scopes", SCOPES.get(0));
    entity.setUnindexedProperty("audiences", AUDIENCES.get(0));
    entity.setUnindexedProperty("clientIds", CLIENT_IDS.get(0));
    memcacheService.put(MEMCACHE_KEY, entity);

    // Put bad data in datastore to make sure we're not reading out of there.
    Entity badEntity = createEntity();
    badEntity.setUnindexedProperty("scopes", BAD_SCOPES);
    datastoreService.put(badEntity);

    datastoreReader.loadEndpointClass(serviceContext, TestEndpoint.class, config);

    expectedConfig.getApiClassConfig().setScopeExpression(SIMPLE_SCOPE_EXPRESSION);
    expectedConfig.getApiClassConfig().setAudiences(ImmutableList.of(AUDIENCES.get(0)));
    expectedConfig.getApiClassConfig().setClientIds(ImmutableList.of(CLIENT_IDS.get(0)));
    assertEquals(expectedConfig, config);

    Object memcacheValue = memcacheService.get(MEMCACHE_KEY);
    assertEquals(entity, memcacheValue);
  }
}

