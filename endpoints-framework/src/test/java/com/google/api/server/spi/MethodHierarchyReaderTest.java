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

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.server.spi.testing.SimpleOverloadEndpoint;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.api.server.spi.testing.TestEndpointSuperclass;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link MethodHierarchyReader}.
 *
 * @author Eric Orth
 */
@RunWith(JUnit4.class)
public class MethodHierarchyReaderTest {
  private MethodHierarchyReader methodReader;

  @Before
  public void setUp() {
    methodReader = new MethodHierarchyReader(TestEndpoint.class);
  }

  private static class TestHelper {
    private boolean[] seen;

    public TestHelper(int numMethodsFound) {
      int numExpectedMethods = TestEndpoint.ExpectedMethod.class.getEnumConstants().length - 1;
      assertEquals(numExpectedMethods, numMethodsFound);

      seen = new boolean[numExpectedMethods];
      Arrays.fill(seen, false);
    }

    public void verifyAllMethodsSeen() {
      for (int i = 0; i < seen.length; ++i) {
        assertTrue("Could not find expected method: " + i, seen[i]);
      }
    }

    private void verifySingleMethod(Method method) {
      TestEndpoint.ExpectedMethod expectedMethod =
          TestEndpoint.ExpectedMethod.fromName(method.getName());
      seen[expectedMethod.ordinal()] = true;

      assertWithMessage("Did not recognize method: " + method.getName())
          .that(TestEndpoint.ExpectedMethod.UNKNOWN)
          .isNotEqualTo(expectedMethod);

      assertEquals("Wrong class recognized for method: " + method,
          expectedMethod.declaringClass, method.getDeclaringClass());
    }
  }

  private void verifyOverrides(Method method, Collection<EndpointMethod> overrides) {
    TestEndpoint.ExpectedMethod expectedMethod =
        TestEndpoint.ExpectedMethod.fromName(method.getName());

    if (expectedMethod.isOverride) {
      assertEquals("Wrong number of overrides for method: " + method.getName(), 2,
          overrides.size());
      assertEquals("Overridden " + method.getName() + " is wrong class.",
          TestEndpointSuperclass.class, Iterables.get(overrides, 1).getMethod().getDeclaringClass());
    } else {
      assertEquals("Wrong number of overrides for method: " + method.getName(), 1,
          overrides.size());
    }
  }

  @Test
  public void testGetLeafMethods() {
    Iterable<Method> methods = methodReader.getLeafMethods();

    TestHelper helper = new TestHelper(Iterables.size(methods));
    for (Method method : methods) {
      helper.verifySingleMethod(method);
    }
    helper.verifyAllMethodsSeen();
  }

  @Test
  public void testGetEndpointOverrides() {
    Iterable<Collection<EndpointMethod>> methods = methodReader.getEndpointOverrides();

    TestHelper helper = new TestHelper(Iterables.size(methods));
    for (Collection<EndpointMethod> overrides : methods) {
      Method method = overrides.iterator().next().getMethod();
      helper.verifySingleMethod(method);
      verifyOverrides(method, overrides);
    }
    helper.verifyAllMethodsSeen();
  }

  @Test
  public void testGetNameToLeafMethodMap() {
    Map<String, Method> methods = methodReader.getNameToLeafMethodMap();

    TestHelper helper = new TestHelper(methods.size());
    for (Map.Entry<String, Method> method : methods.entrySet()) {
      assertEquals(method.getValue().getName(), method.getKey());
      helper.verifySingleMethod(method.getValue());
    }
    helper.verifyAllMethodsSeen();
  }

  @Test
  public void testGetNameToEndpointOverridesMap() {
    ListMultimap<String, EndpointMethod> methods = methodReader.getNameToEndpointOverridesMap();

    TestHelper helper = new TestHelper(methods.keySet().size());
    for (String methodName : methods.keySet()) {
      List<EndpointMethod> overrides = methods.get(methodName);
      Method method = overrides.get(0).getMethod();
      assertEquals(method.getName(), methodName);
      helper.verifySingleMethod(method);
      verifyOverrides(method, overrides);
    }
    helper.verifyAllMethodsSeen();
  }

  @Test
  public void testIsServiceMethod_normal() throws Exception {
    Method normal = TestEndpoint.class.getMethod("getDate", Date.class);
    assertTrue(MethodHierarchyReader.isServiceMethod(normal));
  }

  @Test
  public void testIsServiceMethod_normalOverride() throws Exception {
    Method override = TestEndpoint.class.getMethod("overrideMethod1");
    assertTrue(MethodHierarchyReader.isServiceMethod(override));
  }

  @Test
  public void testIsServiceMethod_isBridge() throws Exception {
    Method bridgeMethod = TestEndpoint.class.getMethod("overrideMethod", Object.class);
    assertFalse(MethodHierarchyReader.isServiceMethod(bridgeMethod));
  }

  @Test
  public void testIsServiceMethod_isStatic() throws Exception {
    Method staticMethod = TestEndpoint.class.getMethod("staticMethod");
    assertFalse(MethodHierarchyReader.isServiceMethod(staticMethod));
  }

  @Test
  public void testReturnsAreCopies_getLeafMethods() {
    Iterable<Method> methods = methodReader.getLeafMethods();
    Iterable<Method> methods2 = methodReader.getLeafMethods();

    assertNotSame(methods, methods2);
    assertEquals(methods, methods2);
  }

  @Test
  public void testReturnsAreCopies_getNameToLeafMethodMap() {
    Map<String, Method> methods = methodReader.getNameToLeafMethodMap();
    Map<String, Method> methods2 = methodReader.getNameToLeafMethodMap();

    assertNotSame(methods, methods2);
    assertEquals(methods, methods2);
  }

  @Test
  public void testReturnsAreCopies_getNameToEndpointOverridesMap() {
    ListMultimap<String, EndpointMethod> methods = methodReader.getNameToEndpointOverridesMap();
    ListMultimap<String, EndpointMethod> methods2 = methodReader.getNameToEndpointOverridesMap();

    assertNotSame(methods, methods2);
    assertEquals(methods, methods2);
  }

  @Test
  public void testOverloads() throws Exception {
    methodReader = new MethodHierarchyReader(SimpleOverloadEndpoint.class);
    Iterable<Method> methods = methodReader.getLeafMethods();

    boolean foundInt = false;
    boolean foundString = false;
    for (Method method : methods) {
      if (method.equals(SimpleOverloadEndpoint.class.getMethod("foo", Integer.class))) {
        foundInt = true;
      } else if (method.equals(
          SimpleOverloadEndpoint.class.getSuperclass().getMethod("foo", String.class))) {
        foundString = true;
      } else {
        fail("Unexpected method: " + method);
      }
    }

    assertTrue(foundInt);
    assertTrue(foundString);
  }
}
