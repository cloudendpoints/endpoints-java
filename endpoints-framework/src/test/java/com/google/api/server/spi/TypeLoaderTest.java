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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

/**
 * Tests for {@link TypeLoader}.
 * @author Eric Orth
 */
@RunWith(JUnit4.class)
public class TypeLoaderTest {
  private TypeLoader typeLoader;

  @Before
  public void setUp() throws Exception {
    typeLoader = new TypeLoader();
  }

  @Test
  public void testIsInjectedType() {
    assertTrue(typeLoader.isInjectedType(TypeToken.of(HttpServletRequest.class)));
    assertFalse(typeLoader.isInjectedType(TypeToken.of(String.class)));
  }

  @Test
  public void testIsSchemaType() {
    assertTrue(typeLoader.isSchemaType(TypeToken.of(Boolean.class)));
    assertFalse(typeLoader.isSchemaType(TypeToken.of(HashMap.class)));
  }

  @Test
  public void testIsParameterType() {
    assertTrue(typeLoader.isParameterType(TypeToken.of(Integer.class)));
    assertFalse(typeLoader.isParameterType(TypeToken.of(Short.class)));
  }
}
