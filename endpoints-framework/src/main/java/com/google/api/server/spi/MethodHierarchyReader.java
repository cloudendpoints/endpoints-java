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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * Utility to take a class and create maps of methods usable for endpoints and related data.
 *
 * <p>On the first call, parses and stores the full hierarchy information.  The cached full
 * hierarchy is then used for all subsequent calls.  Returned iterables, maps, and lists are copied
 * out without reference to the original full hierarchy information to allow the caller to keep just
 * the needed portion of information and release the full hierarchy along with this class.
 *
 * @author Eric Orth
 */
public class MethodHierarchyReader {
  private final Class<?> endpointClass;
  // Map from method signatures to a list of method overrides for that signature (ordered
  // subclass -> superclass).
  private ListMultimap<EndpointMethod.ResolvedSignature, EndpointMethod> endpointMethods;

  /**
   * Constructs a {@code MethodHierarchyReader} for the given class type.
   *
   * @param endpointClass Must be a concrete type (not abstract or interface).
   */
  public MethodHierarchyReader(Class<?> endpointClass) {
    Preconditions.checkArgument(
        !Modifier.isAbstract(endpointClass.getModifiers())
            && !Modifier.isInterface(endpointClass.getModifiers()),
        "A concrete class is expected, but got %s.", endpointClass.getName());

    this.endpointClass = endpointClass;
  }

  private void readMethodHierarchyIfNecessary() {
    if (endpointMethods == null) {
      ImmutableListMultimap.Builder<EndpointMethod.ResolvedSignature, EndpointMethod> builder =
          ImmutableListMultimap.builder();
      buildServiceMethods(builder, TypeToken.of(endpointClass));
      endpointMethods = builder.build();
    }
  }

  /**
   * Returns the final leaf subclass version of a method.
   *
   * @param overrides A list of method overrides ordered subclass -> superclass.
   */
  private EndpointMethod getLeafMethod(List<EndpointMethod> overrides) {
    // Because the list is ordered subclass -> superclass, index 0 will always contain the leaf
    // subclass implementation.
    return overrides.get(0);
  }

  /**
   * Returns {@link ListMultimap#asMap multimap.asMap()}, with its type
   * corrected from {@code Map<K, Collection<V>>} to {@code Map<K, List<V>>}.
   */
  // Copied from com.google.common.collect.Multimaps.  We can't use the actual method from
  // that class as appengine build magic gives us an older version of guava that doesn't yet have
  // this method.
  // TODO: Switch to Multimaps.asMap() once it becomes available in appengine.
  @SuppressWarnings("unchecked")
  // safe by specification of ListMultimap.asMap()
  private static <K, V> Map<K, List<V>> asMap(ListMultimap<K, V> multimap) {
    return (Map<K, List<V>>) (Map<K, ?>) multimap.asMap();
  }

  /**
   * Returns a collection of public service methods defined by the class and its super classes.
   * Bridge methods are ignored.  Only the final leaf subclass version of each method is included.
   */
  public Iterable<Method> getLeafMethods() {
    readMethodHierarchyIfNecessary();
    ImmutableList.Builder<Method> builder = ImmutableList.builder();
    for (List<EndpointMethod> overrides : asMap(endpointMethods).values()) {
      builder.add(getLeafMethod(overrides).getMethod());
    }
    return builder.build();
  }

  /**
   * Returns a collection of public service methods defined by the class and its super classes.
   * Bridge methods are ignored.  Only the final leaf subclass version of each method is included.
   * Methods are stored in the EndpointMethod container.
   */
  public Iterable<EndpointMethod> getLeafEndpointMethods() {
    readMethodHierarchyIfNecessary();
    ImmutableList.Builder<EndpointMethod> builder = ImmutableList.builder();
    for (List<EndpointMethod> overrides : asMap(endpointMethods).values()) {
      builder.add(getLeafMethod(overrides));
    }
    return builder.build();
  }

  /**
   * Returns a collection of public service methods defined by the class and its super classes.
   * Bridge methods are ignored.  For each method, all valid method implementations are included,
   * ordered subclass to superclass.
   */
  public Iterable<List<Method>> getMethodOverrides() {
    readMethodHierarchyIfNecessary();
    ImmutableList.Builder<List<Method>> builder = ImmutableList.builder();
    for (List<EndpointMethod> overrides : asMap(endpointMethods).values()) {
      ImmutableList.Builder<Method> methodBuilder = ImmutableList.builder();
      for (EndpointMethod method : overrides) {
        methodBuilder.add(method.getMethod());
      }
      builder.add(methodBuilder.build());
    }
    return builder.build();
  }

  /**
   * Returns a collection of public service methods defined by the class and its super classes.
   * Bridge methods are ignored.  For each method, all valid method implementations are included,
   * ordered subclass to superclass.  Methods are stored in the EndpointMethod container.
   */
  public Iterable<List<EndpointMethod>> getEndpointOverrides() {
    readMethodHierarchyIfNecessary();
    return asMap(endpointMethods).values();
  }

  /**
   * Returns a mapping of public service methods defined by the class and its super classes,
   * keyed by method name.  Bridge methods are ignored.  Only the final leaf subclass version of
   * each method is included.
   */
  public Map<String, Method> getNameToLeafMethodMap() {
    readMethodHierarchyIfNecessary();
    ImmutableMap.Builder<String, Method> builder = ImmutableMap.builder();
    for (List<EndpointMethod> overrides : asMap(endpointMethods).values()) {
      Method leafMethod = getLeafMethod(overrides).getMethod();
      builder.put(leafMethod.getName(), leafMethod);
    }
    return builder.build();
  }

  /**
   * Returns a mapping of public service methods defined by the class and its super classes,
   * keyed by method name.  Bridge methods are ignored.  All valid method implementations are
   * included, ordered subclass to superclass.
   */
  public ListMultimap<String, EndpointMethod> getNameToEndpointOverridesMap() {
    readMethodHierarchyIfNecessary();
    ImmutableListMultimap.Builder<String, EndpointMethod> builder = ImmutableListMultimap.builder();
    for (List<EndpointMethod> overrides : asMap(endpointMethods).values()) {
      builder.putAll(getLeafMethod(overrides).getMethod().getName(), overrides);
    }
    return builder.build();
  }

  /**
   * Recursively builds a map from method_names to methods.  Method types will be resolved as much
   * as possible using {@code serviceType}.
   *
   * @param serviceType is the class object being inspected for service methods
   */
  private void buildServiceMethods(
      ImmutableListMultimap.Builder<EndpointMethod.ResolvedSignature, EndpointMethod> builder,
      TypeToken<?> serviceType) {
    for (TypeToken<?> typeToken : serviceType.getTypes().classes()) {
      Class<?> serviceClass = typeToken.getRawType();
      if (Object.class.equals(serviceClass)) {
        return;
      }
      for (Method method : serviceClass.getDeclaredMethods()) {
        if (!isServiceMethod(method)) {
          continue;
        }

        EndpointMethod currentMethod = EndpointMethod.create(endpointClass, method, typeToken);
        builder.put(currentMethod.getResolvedMethodSignature(), currentMethod);
      }
    }
  }

  @VisibleForTesting
  static boolean isServiceMethod(Method method) {
    int modifiers = method.getModifiers();
    return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && !method.isBridge();
  }
}
