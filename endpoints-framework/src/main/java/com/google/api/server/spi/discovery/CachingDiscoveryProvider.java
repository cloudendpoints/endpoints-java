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

import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.RestDescription;
import com.google.api.services.discovery.model.RpcDescription;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link DiscoveryProvider} that caches results and delegates computation to another provider.
 */
public class CachingDiscoveryProvider implements DiscoveryProvider {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final int CACHE_EXPIRY_MINS = 10;

  private final Cache<ApiKey, RestDescription> restDocuments;
  private final Cache<ApiKey, RpcDescription> rpcDocuments;
  private final Cache<String, DirectoryList> directoryByRoot;
  private final DiscoveryProvider delegate;

  public CachingDiscoveryProvider(DiscoveryProvider delegate) {
    this(delegate, CACHE_EXPIRY_MINS, TimeUnit.MINUTES);
  }

  public CachingDiscoveryProvider(
      DiscoveryProvider delegate, long cacheExpiry, TimeUnit cacheExpiryUnit) {
    this.delegate = delegate;
    restDocuments = CacheBuilder.newBuilder()
        .expireAfterAccess(cacheExpiry, cacheExpiryUnit)
        .build();
    rpcDocuments = CacheBuilder.newBuilder()
        .expireAfterAccess(cacheExpiry, cacheExpiryUnit)
        .build();
    directoryByRoot = CacheBuilder.newBuilder()
        .expireAfterAccess(cacheExpiry, cacheExpiryUnit)
        .build();
  }

  @Override
  public RestDescription getRestDocument(final String root, final String name, final String version)
      throws NotFoundException, InternalServerErrorException {
    return getDiscoveryDoc(restDocuments, root, name, version, new Callable<RestDescription>() {
      @Override
      public RestDescription call() throws NotFoundException, InternalServerErrorException {
        return delegate.getRestDocument(root, name, version);
      }
    });
  }

  @Override
  public RpcDescription getRpcDocument(final String root, final String name, final String version)
      throws NotFoundException, InternalServerErrorException {
    return getDiscoveryDoc(rpcDocuments, root, name, version, new Callable<RpcDescription>() {
      @Override
      public RpcDescription call() throws NotFoundException, InternalServerErrorException {
        return delegate.getRpcDocument(root, name, version);
      }
    });
  }

  @Override
  public DirectoryList getDirectory(final String root) throws InternalServerErrorException {
    try {
      return directoryByRoot.get(root, new Callable<DirectoryList>() {
        @Override
        public DirectoryList call() throws Exception {
          return delegate.getDirectory(root);
        }
      });
    } catch (ExecutionException | UncheckedExecutionException e) {
      // Cast here so we can maintain specific errors for documentation in throws clauses.
      if (e.getCause() instanceof InternalServerErrorException) {
        throw (InternalServerErrorException) e.getCause();
      } else {
        logger.atSevere().withCause(e.getCause()).log("Could not generate or cache directory");
        throw new InternalServerErrorException("Internal Server Error", e.getCause());
      }
    }
  }

  @VisibleForTesting
  void cleanUp() {
    restDocuments.cleanUp();
    rpcDocuments.cleanUp();
    directoryByRoot.cleanUp();
  }

  private <T> T getDiscoveryDoc(Cache<ApiKey, T> cache, String root, String name, String version,
      Callable<T> loader) throws NotFoundException, InternalServerErrorException {
    ApiKey key = new ApiKey(name, version, root);
    try {
      return cache.get(key, loader);
    } catch (ExecutionException | UncheckedExecutionException e) {
      // Cast here so we can maintain specific errors for documentation in throws clauses.
      if (e.getCause() instanceof NotFoundException) {
        throw (NotFoundException) e.getCause();
      } else if (e.getCause() instanceof InternalServerErrorException) {
        throw (InternalServerErrorException) e.getCause();
      } else {
        logger.atSevere().withCause(e.getCause()).log("Could not generate or cache discovery doc");
        throw new InternalServerErrorException("Internal Server Error", e.getCause());
      }
    }
  }
}
