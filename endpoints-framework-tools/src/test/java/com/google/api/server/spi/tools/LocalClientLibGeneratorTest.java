/*
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
package com.google.api.server.spi.tools;

import static org.junit.Assume.assumeTrue;

import static com.google.api.server.spi.tools.LocalClientLibGenerator.GENERATOR_EXECUTABLE;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.tools.testing.FakeClientLibGenerator;

public class LocalClientLibGeneratorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  private LocalClientLibGenerator generator;
  private String discoveryDoc;

  @Before
  public void init() throws Exception {
    generator = new LocalClientLibGenerator();
    discoveryDoc = IoUtil.readStream(FakeClientLibGenerator.class.getResourceAsStream("fake-discovery-doc-rest.json"));
  }

  @Test
  public void testJavaCodeGeneration() throws Exception {
    assumeTrue(isToolInstalled());

    File destinationDir = tmpFolder.newFolder("destination");
    generator.generateClientLib(discoveryDoc, "java", null, null, destinationDir);
    File com = new File(destinationDir, "com");
    Assert.assertTrue(com.isDirectory());
    File google = new File(com, "google");
    Assert.assertTrue(google.isDirectory());
    File client = new File(google, "client");
    Assert.assertTrue(client.isDirectory());
    File guestbook = new File(client, "guestbook");
    Assert.assertTrue(guestbook.isDirectory());
    File v1 = new File(guestbook, "v1");
    Assert.assertTrue(guestbook.isDirectory());
    File model = new File(v1, "model");
    Assert.assertTrue(model.isDirectory());
    File guestbookJava = new File(v1, "Guestbook.java");
    Assert.assertTrue(guestbookJava.isFile());
    File guestbookRequestJava = new File(v1, "GuestbookRequest.java");
    Assert.assertTrue(guestbookRequestJava.isFile());
    File guestbookRequestInitializerJava = new File(v1, "GuestbookRequestInitializer.java");
    Assert.assertTrue(guestbookRequestInitializerJava.isFile());
    File greetingJava = new File(model, "Greeting.java");
    Assert.assertTrue(greetingJava.isFile());
  }

  @Test
  public void testDestinationDirectoryCreation() throws Exception {
    assumeTrue(isToolInstalled());

    File destinationParent = tmpFolder.newFolder("destination");
    File dir1 = new File(destinationParent, "d1");
    File destinationDir = new File(dir1, "d2");
    Assert.assertFalse(dir1.exists());
    Assert.assertFalse(destinationDir.exists());
    generator.generateClientLib(discoveryDoc, "java", null, null, destinationDir);
    Assert.assertTrue(dir1.exists());
    Assert.assertTrue(destinationDir.exists());
  }

  @Test
  public void testInvalidDiscoveryFile_fails() throws Exception {
    assumeTrue(isToolInstalled());

    expectedException.expect(IOException.class);

    discoveryDoc = IoUtil.readStream(FakeClientLibGenerator.class.getResourceAsStream("fake-api-config.json"));
    File destinationDir = tmpFolder.newFolder("destination");
    generator.generateClientLib(discoveryDoc, "java", null, null, destinationDir);
  }

  @Test
  public void testDestinationIsFile_fails() throws Exception {
    assumeTrue(isToolInstalled());

    expectedException.expect(IOException.class);

    File destinationFile = tmpFolder.newFile("destination");
    generator.generateClientLib(discoveryDoc, "java", null, null, destinationFile);
  }

  @Test
  public void testLanguageUnsupported() throws Exception {
    assumeTrue(isToolInstalled());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unsupported language: python");

    generator.generateClientLib(discoveryDoc, "python", null, null, tmpFolder.getRoot());
  }

  /**
   * Checks if the generator is installed.
   */
  private boolean isToolInstalled() {
    try {
      Runtime.getRuntime().exec(GENERATOR_EXECUTABLE);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
