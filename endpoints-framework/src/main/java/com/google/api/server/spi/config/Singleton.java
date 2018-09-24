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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.flogger.FluentLogger;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Annotation used with Authenticator and PeerAuthenticator to denote only one instance will be
 * created for optimization. Implementation must be thread safe. Without the annotation a new
 * (peer)authenticator instance will be created for each request.
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Singleton {

  /**
   * Instantiates instances of A, honoring the @{@link Singleton} contract.
   * Return a default instance when passed null values.
   */
  class Instantiator<A> {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private volatile Map<Class<? extends A>, A> instances = new HashMap<>();

    private final A defaultValue;

    private final Function<Class<? extends A>, A> instantiator
        = new Function<Class<? extends A>, A>() {
      @Override
      public A apply(Class<? extends A> clazz) {
        try {
          if (clazz.getAnnotation(Singleton.class) != null) {
            if (!instances.containsKey(clazz)) {
              instances.put(clazz, clazz.newInstance());
            }
            return instances.get(clazz);
          } else {
            return clazz.newInstance();
          }
        } catch (IllegalAccessException | InstantiationException e) {
          logger.atWarning().log("Could not instantiate: %s", clazz.getName());
          return null;
        }
      }
    };

    public Instantiator(A defaultValue) {
      this.defaultValue = defaultValue;
    }

    public A getInstanceOrDefault(Class<? extends A> clazz) {
      return clazz == null ? defaultValue : instantiator.apply(clazz);
    }

    public Iterable<A> getInstancesOrDefault(List<Class<? extends A>> classes) {
      return classes == null ? ImmutableList.of(defaultValue)
          : Iterables.filter(Iterables.transform(classes, instantiator),
              Predicates.notNull());
    }

  }

}
