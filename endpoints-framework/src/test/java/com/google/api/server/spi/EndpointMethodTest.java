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
import static org.junit.Assert.fail;

import com.google.common.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Tests for {@link EndpointMethod}.
 */
@RunWith(JUnit4.class)
public class EndpointMethodTest {
  private static class RawList {
    @SuppressWarnings({"unused", "rawtypes"})
    public void foo(List raw) {}
  }

  private static class ParameterizedList {
    @SuppressWarnings("unused")
    public void foo(List<String> parameterized) {}
  }

  private class InnerClassList {
    @SuppressWarnings("unused")
    public void foo(List<String> parameterized) {}
  }

  private class ChildClassList extends RawList {}

  @Test
  public void testRawComparison() throws NoSuchMethodException, SecurityException {
    assertEquals(getListFoo(RawList.class).getResolvedMethodSignature(),
        getListFoo(ParameterizedList.class).getResolvedMethodSignature());
  }

  @Test
  public void testInnerClass() throws NoSuchMethodException, SecurityException {
    assertEquals(getListFoo(ParameterizedList.class).getResolvedMethodSignature(),
        getListFoo(InnerClassList.class).getResolvedMethodSignature());
  }

  @Test
  public void testMethodFromWrongClassChain() throws SecurityException, NoSuchMethodException {
    Class<?> clazz = ChildClassList.class;
    Method method = ParameterizedList.class.getMethod("foo", List.class);
    try {
      getEndpointMethod(clazz, method);
      fail(String.format("Method %s is not part of '%s's inheritance chain", clazz, method));
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test
  public void testMethodFromRightClassChain() throws SecurityException, NoSuchMethodException {
    Class<?> clazz = ChildClassList.class;
    Method method = RawList.class.getMethod("foo", List.class);
    getEndpointMethod(clazz, method);
  }

  @Test
  public void testToken_declaringClass() throws Exception {
    EndpointMethod.create(ChildClassList.class, RawList.class.getMethod("foo", List.class),
        TypeToken.of(RawList.class));
  }

  @Test
  public void testToken_chainedCreation() throws Exception {
    EndpointMethod.create(ChildClassList.class, RawList.class.getMethod("foo", List.class),
        TypeToken.of(ChildClassList.class).getSupertype(RawList.class));
  }

  @Test
  public void testToken_subClass() throws Exception {
    try {
      EndpointMethod.create(ChildClassList.class, RawList.class.getMethod("foo", List.class),
          TypeToken.of(ChildClassList.class));
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException expected) {}
  }

  @Test
  public void testWildcardParameter() throws Exception {
    class WildcardParameter {
      @SuppressWarnings("unused")
      public void foo(List<?> l) {}
    }

    try {
      EndpointMethod.create(WildcardParameter.class,
          WildcardParameter.class.getMethod("foo", List.class));
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException expected) {}
  }

  @Test
  public void testWildcardReturn() throws Exception {
    class WildcardReturn {
      @SuppressWarnings("unused")
      public List<?> foo() {
        return null;
      }
    }

    try {
      EndpointMethod.create(WildcardReturn.class, WildcardReturn.class.getMethod("foo"));
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException expected) {}
  }

  private EndpointMethod getEndpointMethod(Class<?> clazz, Method method) {
    return EndpointMethod.create(clazz, method);
  }

  private EndpointMethod getListFoo(Class<?> clazz)
      throws NoSuchMethodException, SecurityException {
    return getEndpointMethod(clazz, clazz.getMethod("foo", List.class));
  }
}
