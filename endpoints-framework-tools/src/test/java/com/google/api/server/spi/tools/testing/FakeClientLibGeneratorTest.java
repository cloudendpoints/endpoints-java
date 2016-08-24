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
package com.google.api.server.spi.tools.testing;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;

/**
 * Tests for {@link FakeClientLibGenerator}.
 */
@RunWith(JUnit4.class)
public class FakeClientLibGeneratorTest {
  @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Test
  public void testGenerateClientLib() throws IOException {
    File destFile =
        File.createTempFile("my-client-lib", ".jar", tmpFolder.getRoot().getAbsoluteFile());
    new FakeClientLibGenerator().generateClientLib("", "java", null, null, destFile);
    assertTrue(destFile.exists());
  }
}
