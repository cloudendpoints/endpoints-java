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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.RestDescription;
import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link CachingDiscoveryProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CachingDiscoveryProviderTest {
  private static final String ROOT = "root";
  private static final String NAME = "name";
  private static final String VERSION = "v1";
  private static final RestDescription REST_DOC = new RestDescription()
      .setName(NAME)
      .setVersion(VERSION);
  private static final DirectoryList DIRECTORY = new DirectoryList()
      .setItems(ImmutableList.of(new DirectoryList.Items()
          .setName(NAME)
          .setVersion(VERSION)));

  @Mock private DiscoveryProvider delegate;

  @Test
  public void getRestDocument() throws Exception {
    CachingDiscoveryProvider provider = createNonExpiringProvider();
    setupNormalMockDelegate();

    // Make the same call twice and ensure that the delegate is only called once.
    assertThat(provider.getRestDocument(ROOT, NAME, VERSION)).isEqualTo(REST_DOC);
    assertThat(provider.getRestDocument(ROOT, NAME, VERSION)).isEqualTo(REST_DOC);
    verify(delegate, times(1)).getRestDocument(ROOT, NAME, VERSION);
  }

  @Test
  public void getRestDocument_cacheExpiry() throws Exception {
    CachingDiscoveryProvider provider = createShortExpiringProvider();
    setupNormalMockDelegate();

    assertThat(provider.getRestDocument(ROOT, NAME, VERSION)).isEqualTo(REST_DOC);

    Thread.sleep(1000);
    provider.cleanUp();

    assertThat(provider.getRestDocument(ROOT, NAME, VERSION)).isEqualTo(REST_DOC);
    verify(delegate, times(2)).getRestDocument(ROOT, NAME, VERSION);
  }

  @Test
  public void getRestDocument_notFound() throws Exception {
    CachingDiscoveryProvider provider = createNonExpiringProvider();
    when(delegate.getRestDocument(ROOT, NAME, VERSION)).thenThrow(new NotFoundException(""));

    try {
      provider.getRestDocument(ROOT, NAME, VERSION);
      fail("expected NotFoundException");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void getRestDocument_internalServerError() throws Exception {
    CachingDiscoveryProvider provider = createNonExpiringProvider();
    when(delegate.getRestDocument(ROOT, NAME, VERSION))
        .thenThrow(new InternalServerErrorException(""));

    try {
      provider.getRestDocument(ROOT, NAME, VERSION);
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  @Test
  public void getRestDocument_runtimeException() throws Exception {
    CachingDiscoveryProvider provider = createNonExpiringProvider();
    when(delegate.getRestDocument(ROOT, NAME, VERSION))
        .thenThrow(new RuntimeException(""));

    try {
      provider.getRestDocument(ROOT, NAME, VERSION);
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  @Test
  public void getDirectory() throws Exception {
    CachingDiscoveryProvider provider = createNonExpiringProvider();
    setupNormalMockDelegate();

    // Make the same call twice and ensure that the delegate is only called once.
    assertThat(provider.getDirectory(ROOT)).isEqualTo(DIRECTORY);
    assertThat(provider.getDirectory(ROOT)).isEqualTo(DIRECTORY);
    verify(delegate, times(1)).getDirectory(ROOT);
  }

  @Test
  public void getDirectory_cacheExpiry() throws Exception {
    CachingDiscoveryProvider provider = createShortExpiringProvider();
    setupNormalMockDelegate();

    assertThat(provider.getDirectory(ROOT)).isEqualTo(DIRECTORY);

    Thread.sleep(1000);
    provider.cleanUp();

    assertThat(provider.getDirectory(ROOT)).isEqualTo(DIRECTORY);
    verify(delegate, times(2)).getDirectory(ROOT);
  }

  @Test
  public void getDirectory_internalServerError() throws Exception {
    CachingDiscoveryProvider provider = createNonExpiringProvider();
    when(delegate.getDirectory(ROOT)).thenThrow(new InternalServerErrorException(""));

    try {
      provider.getDirectory(ROOT);
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  @Test
  public void getDirectory_runtimeException() throws Exception {
    CachingDiscoveryProvider provider = createNonExpiringProvider();
    when(delegate.getDirectory(ROOT)).thenThrow(new RuntimeException(""));

    try {
      provider.getDirectory(ROOT);
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  private CachingDiscoveryProvider createNonExpiringProvider() {
    return createProvider(1, TimeUnit.DAYS);
  }

  private CachingDiscoveryProvider createShortExpiringProvider() {
    return createProvider(500, TimeUnit.MILLISECONDS);
  }

  private CachingDiscoveryProvider createProvider(long cacheExpiry, TimeUnit cacheExpiryUnit) {
    return new CachingDiscoveryProvider(delegate, cacheExpiry, cacheExpiryUnit);
  }

  private void setupNormalMockDelegate() throws Exception {
    when(delegate.getRestDocument(ROOT, NAME, VERSION)).thenReturn(REST_DOC);
    when(delegate.getDirectory(ROOT)).thenReturn(DIRECTORY);
  }
}
