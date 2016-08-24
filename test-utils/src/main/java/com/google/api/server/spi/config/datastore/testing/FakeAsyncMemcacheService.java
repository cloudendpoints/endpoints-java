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
package com.google.api.server.spi.config.datastore.testing;

import com.google.api.server.spi.config.datastore.ApiConfigDatastoreReader;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.ErrorHandler;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.CasValues;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.Stats;
import com.google.common.util.concurrent.Futures;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * An {@link AsyncMemcacheService} that delegates to a synchronous {@link MemcacheService}.
 *
 * This is used for testing {@link ApiConfigDatastoreReader}.
 */
@SuppressWarnings("deprecation")
public class FakeAsyncMemcacheService implements AsyncMemcacheService {

  private final MemcacheService memcacheService;

  public FakeAsyncMemcacheService(MemcacheService memcacheService) {
    this.memcacheService = memcacheService;
  }

  @Override
  public String getNamespace() {
    return memcacheService.getNamespace();
  }

  @Override
  public ErrorHandler getErrorHandler() {
    return memcacheService.getErrorHandler();
  }

  @Override
  public void setErrorHandler(ErrorHandler handler) {
    memcacheService.setErrorHandler(handler);
  }

  @Override
  public Future<Void> clearAll() {
    memcacheService.clearAll();
    return Futures.immediateFuture(null);
  }

  @Override
  public Future<Boolean> contains(Object key) {
    return Futures.immediateFuture(memcacheService.contains(key));
  }

  @Override
  public Future<Boolean> delete(Object key) {
    return Futures.immediateFuture(memcacheService.delete(key));
  }

  @Override
  public Future<Boolean> delete(Object key, long millisNoReAdd) {
    return Futures.immediateFuture(memcacheService.delete(key, millisNoReAdd));
  }

  @Override
  public <T> Future<Set<T>> deleteAll(Collection<T> keys) {
    return Futures.immediateFuture(memcacheService.deleteAll(keys));
  }

  @Override
  public <T> Future<Set<T>> deleteAll(Collection<T> keys, long millisNoReAdd) {
    return Futures.immediateFuture(memcacheService.deleteAll(keys, millisNoReAdd));
  }

  @Override
  public Future<Object> get(Object key) {
    return Futures.immediateFuture(memcacheService.get(key));
  }

  @Override
  public <T> Future<Map<T, Object>> getAll(Collection<T> keys) {
    return Futures.immediateFuture(memcacheService.getAll(keys));
  }

  @Override
  public Future<IdentifiableValue> getIdentifiable(Object key) {
    return Futures.immediateFuture(memcacheService.getIdentifiable(key));
  }

  @Override
  public <T> Future<Map<T, IdentifiableValue>> getIdentifiables(Collection<T> keys) {
    return Futures.immediateFuture(memcacheService.getIdentifiables(keys));
  }

  @Override
  public Future<Stats> getStatistics() {
    return Futures.immediateFuture(memcacheService.getStatistics());
  }

  @Override
  public Future<Long> increment(Object key, long delta) {
    return Futures.immediateFuture(memcacheService.increment(key, delta));
  }

  @Override
  public Future<Long> increment(Object key, long delta, Long initialValue) {
    return Futures.immediateFuture(memcacheService.increment(key, delta, initialValue));
  }

  @Override
  public <T> Future<Map<T, Long>> incrementAll(Map<T, Long> offsets) {
    return Futures.immediateFuture(memcacheService.incrementAll(offsets));
  }

  @Override
  public <T> Future<Map<T, Long>> incrementAll(Collection<T> keys, long delta) {
    return Futures.immediateFuture(memcacheService.incrementAll(keys, delta));
  }

  @Override
  public <T> Future<Map<T, Long>> incrementAll(Map<T, Long> offsets, Long initialValue) {
    return Futures.immediateFuture(memcacheService.incrementAll(offsets, initialValue));
  }

  @Override
  public <T> Future<Map<T, Long>> incrementAll(Collection<T> keys, long delta, Long initialValue) {
    return Futures.immediateFuture(memcacheService.incrementAll(keys, delta, initialValue));
  }

  @Override
  public Future<Void> put(Object key, Object value) {
    memcacheService.put(key, value);
    return Futures.immediateFuture(null);
  }

  @Override
  public Future<Void> put(Object key, Object value, Expiration expires) {
    memcacheService.put(key, value, expires);
    return Futures.immediateFuture(null);
  }

  @Override
  public Future<Boolean> put(Object key, Object value, Expiration expires, SetPolicy policy) {
    return Futures.immediateFuture(memcacheService.put(key, value, expires, policy));
  }

  @Override
  public Future<Void> putAll(Map<?, ?> values) {
    memcacheService.putAll(values);
    return Futures.immediateFuture(null);
  }

  @Override
  public Future<Void> putAll(Map<?, ?> values, Expiration expires) {
    memcacheService.putAll(values, expires);
    return Futures.immediateFuture(null);
  }

  @Override
  public <T> Future<Set<T>> putAll(Map<T, ?> values, Expiration expires, SetPolicy policy) {
    return Futures.immediateFuture(memcacheService.putAll(values, expires, policy));
  }

  @Override
  public <T> Future<Set<T>> putIfUntouched(Map<T, CasValues> values) {
    return Futures.immediateFuture(memcacheService.putIfUntouched(values));
  }

  @Override
  public <T> Future<Set<T>> putIfUntouched(Map<T, CasValues> values, Expiration expiration) {
    return Futures.immediateFuture(memcacheService.putIfUntouched(values, expiration));
  }

  @Override
  public Future<Boolean> putIfUntouched(Object key, IdentifiableValue oldValue, Object newValue) {
    return Futures.immediateFuture(memcacheService.putIfUntouched(key, oldValue, newValue));
  }

  @Override
  public Future<Boolean> putIfUntouched(Object key, IdentifiableValue oldValue, Object newValue,
      Expiration expires) {
    return Futures.immediateFuture(
        memcacheService.putIfUntouched(key, oldValue, newValue, expires));
  }
}
