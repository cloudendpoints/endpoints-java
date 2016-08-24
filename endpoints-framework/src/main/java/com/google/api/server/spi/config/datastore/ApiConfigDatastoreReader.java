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

import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigSource;
import com.google.api.server.spi.config.model.ApiClassConfig;
import com.google.api.server.spi.config.model.ApiClassConfig.MethodConfigMap;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration source for configuration data stored in appengine datastore.
 *
 * @author Eric Orth
 */
public final class ApiConfigDatastoreReader implements ApiConfigSource {
  private static final Logger logger = Logger.getLogger(ApiConfigDatastoreReader.class.getName());

  private final DatastoreService datastoreService;
  private final AsyncMemcacheService memcacheService;

  public static final String ENDPOINT_CONFIGURATION_KIND = "GoogleCloudEndpointConfiguration";
  public static final String METHOD_CONFIGURATION_KIND = "GoogleCloudEndpointMethodConfiguration";

  public ApiConfigDatastoreReader(DatastoreService datastoreService,
      AsyncMemcacheService memcacheService) {
    this.datastoreService = datastoreService;
    this.memcacheService = memcacheService;
  }

  public ApiConfigDatastoreReader() {
    this(DatastoreServiceFactory.getDatastoreService(),
        MemcacheServiceFactory.getAsyncMemcacheService());
  }

  @Override
  public void loadEndpointClass(ServiceContext serviceContext, Class<?> endpointClass,
      ApiConfig config) throws ApiConfigException {
    ApiClassConfig classConfig = config.getApiClassConfig();
    Entity endpointEntity =
        classConfig.getUseDatastore() ? getEndpointEntityOrNull(endpointClass) : null;
    if (endpointEntity != null) {
      processEndpointEntity(endpointEntity, classConfig);
    }
  }

  private Entity getEndpointEntityOrNull(Class<?> endpointClass) {
    Entity entity = getMemcacheEntityOrNull(endpointClass);
    if (entity != null) {
      return entity;
    }

    entity = getDatastoreEntityOrNull(endpointClass);
    if (entity == null) {
      return null;
    }

    // We got the value from datastore, so now write it to memcache.  Don't wait for completion as
    // we don't actually care, except in unittests for confirmation.
    memcacheService.put(getEndpointMemcacheKey(endpointClass.getName()), entity, null,
        MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
    return entity;
  }

  private Entity getMemcacheEntityOrNull(Class<?> endpointClass) {
    try {
      // Check for the entity keyed by either full or simple java names as we allow external devs
      // to use either.
      Object memcacheValue =
          memcacheService.get(getEndpointMemcacheKey(endpointClass.getName())).get();
      if (memcacheValue == null || !(memcacheValue instanceof Entity)) {
        memcacheValue =
            memcacheService.get(getEndpointMemcacheKey(endpointClass.getSimpleName())).get();
      }
      return (Entity) memcacheValue;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.log(Level.WARNING, "Error while retrieving memcached endpoint configuration.", e);
      return null;
    } catch (ExecutionException e) {
      logger.log(Level.WARNING, "Error while retrieving memcached endpoint configuration.", e);
      return null;
    }
  }

  private Entity getDatastoreEntityOrNull(Class<?> endpointClass) {
    // Check for the entity keyed by either full or simple java names as we allow external devs
    // to use either.
    try {
      return datastoreService.get(getEndpointDatastoreKey(endpointClass.getName()));
    } catch (EntityNotFoundException e) {
      try {
        return datastoreService.get(getEndpointDatastoreKey(endpointClass.getSimpleName()));
      } catch (EntityNotFoundException ex) {
        return null;
      }
    }
  }

  private void processEndpointEntity(Entity endpointEntity, ApiClassConfig config)
      throws ApiConfigException {
    List<String> scopes = processStringListProperty(endpointEntity, "scopes");
    if (scopes != null) {
      config.setScopeExpression(AuthScopeExpressions.interpret(scopes));
    }

    List<String> audiences = processStringListProperty(endpointEntity, "audiences");
    if (audiences != null) {
      config.setAudiences(audiences);
    }

    List<String> clientIds = processStringListProperty(endpointEntity, "clientIds");
    if (clientIds != null) {
      config.setClientIds(clientIds);
    }
  }

  private List<String> processStringListProperty(Entity endpointEntity, String propertyName)
      throws ApiConfigException {
    if (!endpointEntity.hasProperty(propertyName)) {
      return null;
    }

    Object property = endpointEntity.getProperty(propertyName);

    if (property instanceof String) {
      return ImmutableList.of((String) property);
    }

    if (property instanceof List) {
      List<String> propertyList = safeCast((List<?>) property);
      if (propertyList != null) {
        return propertyList;
      }
    }

    throw new ApiConfigException(
        endpointEntity + "." + propertyName + " was not of type String or List<String>.");
  }

  /**
   * Checks if the given list contains only String (or is empty) and casts to {@code List<String>}
   * if so.
   *
   * @return {@code null} iff the given list contains any non-String elements.
   */
  @SuppressWarnings("unchecked")
  private static List<String> safeCast(List<?> list) {
    for (Object o : list) {
      if (!(o instanceof String)) {
        return null;
      }
    }

    return (List<String>) list;
  }

  @Override
  public void loadEndpointMethods(ServiceContext serviceContext, Class<?> endpointClass,
      MethodConfigMap methodConfigMap) {
    // Noop.  Currently no method configuration is allowed through datastore.
  }

  @Override
  public boolean isStaticConfig(ApiConfig config) {
    return !config.getApiClassConfig().getUseDatastore();
  }

  private Key getEndpointDatastoreKey(String endpointClassName) {
    return KeyFactory.createKey(ENDPOINT_CONFIGURATION_KIND, endpointClassName);
  }

  private Object getEndpointMemcacheKey(String endpointClassName) {
    return ENDPOINT_CONFIGURATION_KIND + "." + endpointClassName;
  }
}

