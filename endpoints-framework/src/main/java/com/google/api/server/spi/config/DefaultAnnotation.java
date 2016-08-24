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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Class used for creation of a default instance of an annotation.
 */
public class DefaultAnnotation {

  private static class Handler implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      return method.getDefaultValue();
    }
  }

  /**
   * Creates an instance of an annotation with all properties set to their default values.
   * @param annotation Class of annotation to create default instance for
   */
  @SuppressWarnings("unchecked")
  public static <A extends Annotation> A of(Class<A> annotation) {
    return (A) Proxy.newProxyInstance(
        annotation.getClassLoader(), new Class<?>[] {annotation}, new Handler());
  }
}
