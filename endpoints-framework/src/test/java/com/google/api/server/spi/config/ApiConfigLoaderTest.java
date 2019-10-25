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
package com.google.api.server.spi.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;

import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiClassConfig;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.testing.TestEndpoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link ApiConfigLoader}.
 *
 * @author Eric Orth
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiConfigLoaderTest {
  @Mock private ApiConfigAnnotationReader annotationSource;
  @Mock private ApiConfigSource datastoreSource;
  @Mock private ApiConfig.Factory configFactory;
  @Mock private ApiConfig config;
  @Mock private ApiConfig config2;
  @Mock private ApiClassConfig classConfig;
  @Mock private ServiceContext serviceContext;
  @Mock private ApiClassConfig.MethodConfigMap methodsMap;
  private TypeLoader typeLoader;
  private Class<?> endpointClass;

  private ApiConfigLoader loader;

  @Before
  public void setUp() throws Exception {
    typeLoader = new TypeLoader();
    endpointClass = TestEndpoint.class;
    Mockito.when(configFactory.create(serviceContext, typeLoader, endpointClass))
        .thenReturn(config);
    Mockito.when(config.getApiClassConfig()).thenReturn(classConfig);
    Mockito.when(classConfig.getMethods()).thenReturn(methodsMap);
    Mockito.when(configFactory.copy(config)).thenReturn(config2);
    Mockito.when(config2.getApiClassConfig()).thenReturn(classConfig);

    loader = new ApiConfigLoader(configFactory, typeLoader, annotationSource, datastoreSource);
  }

  @Test
  public void testBadLoader() throws Exception {
    try {
      new ApiConfigLoader(null, typeLoader, annotationSource, datastoreSource);
      fail("Null ApiConfig.Factory should've caused creation to fail.");
    } catch (NullPointerException expected) {
    }

    try {
      new ApiConfigLoader(configFactory, null, annotationSource, datastoreSource);
      fail("Null TypeLoader should've caused creation to fail.");
    } catch (NullPointerException expected) {
    }

    try {
      new ApiConfigLoader(configFactory, typeLoader, null, annotationSource, datastoreSource);
      fail("Null ApiConfigValidator should've caused creation to fail.");
    } catch (NullPointerException expected) {
    }

    try {
      new ApiConfigLoader(configFactory, typeLoader, null, datastoreSource);
      fail("Null ApiConfigAnnotationReader should've caused creation to fail.");
    } catch (NullPointerException expected) {
    }

    try {
      new ApiConfigLoader(configFactory, typeLoader, annotationSource, annotationSource);
      fail("Multiple ApiConfigAnnotationReaders should've caused creation to fail.");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testLoadSource() throws Exception {
    assertEquals(config, loader.loadConfiguration(serviceContext, endpointClass));

    Mockito.verify(annotationSource).loadEndpointClass(serviceContext, endpointClass, config);
    Mockito.verify(datastoreSource).loadEndpointClass(serviceContext, endpointClass, config);

    Mockito.verify(annotationSource).loadEndpointMethods(serviceContext, endpointClass, methodsMap);
    Mockito.verify(datastoreSource).loadEndpointMethods(serviceContext, endpointClass, methodsMap);
  }

  @Test
  public void testLoadSourceThrows() throws Exception {
    Mockito.doThrow(new ApiConfigException("error"))
        .when(datastoreSource).loadEndpointClass(serviceContext, endpointClass, config);

    try {
      loader.loadConfiguration(serviceContext, endpointClass);
    } catch (ApiConfigException expected) {
    }

    Mockito.verify(annotationSource).loadEndpointClass(serviceContext, endpointClass, config);
    Mockito.verify(datastoreSource).loadEndpointClass(serviceContext, endpointClass, config);

    Mockito.verifyNoMoreInteractions(annotationSource, datastoreSource);
  }

  @Test
  public void testLoadInternalConfiguration() throws Exception {
    assertEquals(config, loader.loadInternalConfiguration(serviceContext, endpointClass));

    Mockito.verify(config).setName(ApiConfigLoader.INTERNAL_API_NAME);
    Mockito.verify(annotationSource).loadEndpointMethods(serviceContext, endpointClass, methodsMap);
    Mockito.verifyNoMoreInteractions(annotationSource, datastoreSource);
  }

  @Test
  public void testIsStaticConfig_true() {
    Mockito.when(datastoreSource.isStaticConfig(config)).thenReturn(true);

    assertTrue(loader.isStaticConfig(config));

    Mockito.verify(annotationSource, never()).isStaticConfig(config);
  }

  @Test
  public void testIsStaticConfig_false() {
    Mockito.when(datastoreSource.isStaticConfig(config)).thenReturn(false);

    assertFalse(loader.isStaticConfig(config));

    Mockito.verify(annotationSource, never()).isStaticConfig(config);
  }

  @Test
  public void testReloadConfiguration() throws Exception {
    Mockito.when(datastoreSource.isStaticConfig(config2)).thenReturn(false);
    assertSame(config2, loader.reloadConfiguration(serviceContext, endpointClass, config));

    Mockito.verify(annotationSource, never()).loadEndpointClass(
        serviceContext, endpointClass, config2);
    Mockito.verify(annotationSource, never()).loadEndpointMethods(
        serviceContext, endpointClass, methodsMap);
    Mockito.verify(datastoreSource).loadEndpointClass(serviceContext, endpointClass, config2);
    Mockito.verify(datastoreSource).loadEndpointMethods(serviceContext, endpointClass, methodsMap);
  }

  @Test
  public void testReloadConfiguration_staticConfigs() throws Exception {
    Mockito.when(datastoreSource.isStaticConfig(config2)).thenReturn(true);
    assertSame(config2, loader.reloadConfiguration(serviceContext, endpointClass, config));

    Mockito.verify(annotationSource, Mockito.never())
        .loadEndpointClass(serviceContext, endpointClass, config2);
    Mockito.verify(annotationSource, Mockito.never())
        .loadEndpointMethods(serviceContext, endpointClass, methodsMap);
    Mockito.verify(datastoreSource, Mockito.never())
        .loadEndpointClass(serviceContext, endpointClass, config2);
    Mockito.verify(datastoreSource, Mockito.never())
        .loadEndpointMethods(serviceContext, endpointClass, methodsMap);
  }
}
