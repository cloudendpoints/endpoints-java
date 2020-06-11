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
package com.google.api.server.spi.config.validation;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiClass;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.config.model.Serializers;
import com.google.api.server.spi.testing.DefaultValueSerializer;
import com.google.api.server.spi.testing.DuplicateMethodEndpoint;
import com.google.api.server.spi.testing.PassAuthenticator;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * Tests for {@link ApiConfigValidator}.
 *
 * @author Eric Orth
 */
@RunWith(JUnit4.class)
public class ApiConfigValidatorTest {
  private ApiConfigValidator validator;

  private ApiConfig.Factory configFactory;
  private ApiConfigLoader configLoader;
  private ApiConfig config;

  @Before
  public void setUp() throws Exception {
    TypeLoader typeLoader = new TypeLoader(ApiConfigValidator.class.getClassLoader());
    SchemaRepository schemaRepository = new SchemaRepository(typeLoader);
    validator = new ApiConfigValidator(typeLoader, schemaRepository);
    configFactory = new ApiConfig.Factory();
    configLoader = new ApiConfigLoader();
    config = configLoader.loadConfiguration(ServiceContext.create(), TestEndpoint.class);
  }

  @Test
  public void testValidateConfig() throws Exception {
    validator.validate(config);
  }

  @Test
  public void testNamespaceValidation_emptyName() throws Exception {
    ApiConfig badNamespaceEmptyName = configFactory.copy(config);
    badNamespaceEmptyName.getNamespaceConfig().setOwnerDomain("domain");
    badNamespaceEmptyName.getNamespaceConfig().setOwnerName("");
    try {
      validator.validate(badNamespaceEmptyName);
      fail("Expected InvalidNamespaceException.");
    } catch (InvalidNamespaceException expected) {
    }
  }

  @Test
  public void testNamespaceValidation_emptyDomain() throws Exception {
    ApiConfig badNamespaceEmptyDomain = configFactory.copy(config);
    badNamespaceEmptyDomain.getNamespaceConfig().setOwnerDomain("");
    badNamespaceEmptyDomain.getNamespaceConfig().setOwnerName("name");
    try {
      validator.validate(badNamespaceEmptyDomain);
      fail("Expected InvalidNamespaceException.");
    } catch (InvalidNamespaceException expected) {
    }
  }

  @Test
  public void testNamespaceValidation_emptyPackage() throws Exception {
    ApiConfig validNamespaceEmptyPackage = configFactory.copy(config);
    validNamespaceEmptyPackage.getNamespaceConfig().setOwnerDomain("domain");
    validNamespaceEmptyPackage.getNamespaceConfig().setOwnerName("name");
    validator.validate(validNamespaceEmptyPackage);
  }

  @Test
  public void testNamespaceValidation_fullySpecified() throws Exception {
    ApiConfig validNamespaceFullySpecified = configFactory.copy(config);
    validNamespaceFullySpecified.getNamespaceConfig().setOwnerDomain("domain");
    validNamespaceFullySpecified.getNamespaceConfig().setOwnerName("name");
    validNamespaceFullySpecified.getNamespaceConfig().setPackagePath("package");
    validator.validate(validNamespaceFullySpecified);
  }

  @Test
  public void testNonuniqueRestSignatures() throws Exception {
    // Set the failWrapped() method to have the same path and httpMethod as getResultNoParams().
    config.getApiClassConfig().getMethods().get(
        methodToEndpointMethod(TestEndpoint.class.getMethod("failWrapped"))).setPath("noresults");
    config.getApiClassConfig().getMethods().get(
        methodToEndpointMethod(TestEndpoint.class.getMethod("failWrapped"))).setHttpMethod("GET");

    try {
      validator.validate(config);
      fail("Expected DuplicateRestPathException.");
    } catch (DuplicateRestPathException expected) {
    }
  }

  @Test
  public void testNonuniqueRestSignatures_multiClass() throws Exception {
    @Api
    class Foo {
      @ApiMethod(path = "path")
      public void foo() {}
    }
    ApiConfig config1 = configLoader.loadConfiguration(ServiceContext.create(), Foo.class);

    @Api
    class Bar {
      @ApiMethod(path = "path")
      public void bar() {}
    }
    ApiConfig config2 = configLoader.loadConfiguration(ServiceContext.create(), Bar.class);

    try {
      validator.validate(Lists.newArrayList(config1, config2));
      fail();
    } catch (DuplicateRestPathException expected) {
    }
  }

  @Test
  public void testNonuniqueJavaNames() throws Exception {
    // Steal two overloaded methods from another class.
    EndpointMethod method1 = mock(EndpointMethod.class);
    when(method1.getMethod()).thenReturn(DuplicateMethodEndpoint.class.getMethod(
        "foo", String.class));
    doReturn(TestEndpoint.class).when(method1).getEndpointClass();
    when(method1.getReturnType()).thenReturn((TypeToken) TypeToken.of(Void.class));

    EndpointMethod method2 = mock(EndpointMethod.class);
    when(method2.getMethod()).thenReturn(DuplicateMethodEndpoint.class.getMethod(
        "foo", Integer.class));
    doReturn(TestEndpoint.class).when(method2).getEndpointClass();
    when(method2.getReturnType()).thenReturn((TypeToken) TypeToken.of(Void.class));

    config.getApiClassConfig().getMethods().getOrCreate(method1).setPath("fn1");
    config.getApiClassConfig().getMethods().getOrCreate(method2).setPath("fn2");

    try {
      validator.validate(config);
      fail("Expected OverloadedMethodException.");
    } catch (OverloadedMethodException expected) {
    }
  }

  @Test
  public void testInvalidMethodName() throws Exception {
    config.setName("testApi");
    config.getApiClassConfig().getMethods().get(
        methodToEndpointMethod(TestEndpoint.class.getMethod("failWrapped"))).setName(".");

    try {
      validator.validate(config);
      fail("Expected InvalidMethodNameException.");
    } catch (InvalidMethodNameException expected) {
    }
  }

  @Test
  public void testSerializerTypeMismatch() throws Exception {
    final class TestSerializer extends DefaultValueSerializer<String, Boolean> {}

    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .addParameter("param", null, false, null, TypeToken.of(Integer.class))
        .setSerializer(TestSerializer.class);

    try {
      validator.validate(config);
      fail("Expected WrongSerializerTypeException.");
    } catch (WrongTransformerTypeException expected) {
    }
  }

  @Test
  public void testSerializerTypeMismatch_repeatedItemSerializer() throws Exception {
    final class TestSerializer extends DefaultValueSerializer<String, Boolean> {}

    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .addParameter("param", null, false, null, TypeToken.of(Integer[].class))
        .setRepeatedItemSerializer(TestSerializer.class);

    try {
      validator.validate(config);
      fail("Expected WrongSerializerTypeException.");
    } catch (WrongTransformerTypeException expected) {
    }
  }

  @Test
  public void testMultipleSerializersInstalled() throws Exception {
    // TODO: The generic component of Comparable causes validation to miss certain error
    // cases like this.
    final class ComparableSerializer extends DefaultValueSerializer<Comparable<String>, Integer> {}
    final class CharSequenceSerializer extends DefaultValueSerializer<CharSequence, Long> {}
    config.getSerializationConfig().addSerializationConfig(ComparableSerializer.class);
    config.getSerializationConfig().addSerializationConfig(CharSequenceSerializer.class);

    List<Class<? extends Transformer<?, ?>>> serializerClasses = Serializers
        .getSerializerClasses(TypeToken.of(String.class), config.getSerializationConfig());
    assertThat(serializerClasses.size()).isEqualTo(2);
    
    try {
      validator.validate(config);
      fail("Expected MultipleTransformersException.");
    } catch (MultipleTransformersException expected) {
    }
  }

  @Test
  public void testMultipleSerializersInstalled_parentHasSerializer() throws Exception {
    final class ComparableSerializer extends DefaultValueSerializer<Comparable<?>, Integer> {}
    final class CharSequenceSerializer extends DefaultValueSerializer<CharSequence, Long> {}
    final class StringSerializer extends DefaultValueSerializer<String, Float> {}
    config.getSerializationConfig().addSerializationConfig(ComparableSerializer.class);
    config.getSerializationConfig().addSerializationConfig(CharSequenceSerializer.class);
    config.getSerializationConfig().addSerializationConfig(StringSerializer.class);

    validator.validate(config);
  }

  @SuppressWarnings("unchecked")  // Deliberate bad cast for testing.
  public void testMissingSerializerInterface() throws Exception {
    final class TestSerializer {}

    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .addParameter("param", null, false, null, TypeToken.of(Integer.class))
        .setSerializer((Class<? extends Transformer<?, ?>>) (Class<?>) TestSerializer.class);

    try {
      validator.validate(config);
      fail("Expected NoSerializerInterfaceException.");
    } catch (NoTransformerInterfaceException expected) {
    }
  }

  @SuppressWarnings("unchecked")  // Deliberate bad cast for testing.
  public void testMissingSerializerInterface_repeatedItemSerializer() throws Exception {
    final class TestSerializer {}

    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .addParameter("param", null, false, null, TypeToken.of(Integer[].class))
        .setRepeatedItemSerializer(
            (Class<? extends Transformer<?, ?>>) (Class<?>) TestSerializer.class);

    try {
      validator.validate(config);
      fail("Expected NoSerializerInterfaceException.");
    } catch (NoTransformerInterfaceException expected) {
    }
  }

  @Test
  public void testCollectionOfArrays() throws Exception {
    class Foo {
      @SuppressWarnings("unused")
      public void foo(List<String[]> l) {}
    }
    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .addParameter("param", null, false, null,
            TypeToken.of(
                Foo.class.getDeclaredMethod("foo", List.class).getGenericParameterTypes()[0]));

    try {
      validator.validate(config);
      fail("Expected NestedCollectionException.");
    } catch (NestedCollectionException expected) {
    }
  }

  @Test
  public void testArraysOfCollections() throws Exception {
    class Foo {
      @SuppressWarnings("unused")
      public void foo(List<String>[] l) {}
    }
    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .addParameter("param", null, false, null,
            TypeToken.of(
                Foo.class.getDeclaredMethod("foo", List[].class).getGenericParameterTypes()[0]));

    try {
      validator.validate(config);
      fail("Expected NestedCollectionException.");
    } catch (NestedCollectionException expected) {
    }
  }

  @Test
  public void testCollectionOfCollections() throws Exception {
    class Foo {
      @SuppressWarnings("unused")
      public void foo(List<List<String>> l) {}
    }
    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .addParameter("param", null, false, null,
            TypeToken.of(
                Foo.class.getDeclaredMethod("foo", List.class).getGenericParameterTypes()[0]));

    try {
      validator.validate(config);
      fail("Expected NestedCollectionException.");
    } catch (NestedCollectionException expected) {
    }
  }

  @Test
  public void testArrayOfArraysParameter() throws Exception {
    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .addParameter("param", null, false, null, TypeToken.of(Integer[][].class));

    try {
      validator.validate(config);
      fail("Expected NestedCollectionException.");
    } catch (NestedCollectionException expected) {
    }
  }

  @Test
  public void testUnknownParameterType() throws Exception {
    final class Foo<T> {
      @SuppressWarnings("unused")
      public void foo(T t) {}
    }
    TypeToken<?> unknownType = TypeToken.of(
        Foo.class.getDeclaredMethod("foo", Object.class).getGenericParameterTypes()[0]);

    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .addParameter("param", null, false, null, unknownType);

    try {
      validator.validate(config);
      fail("Expected GenericTypeException.");
    } catch (GenericTypeException expected) {
    }
  }

  @Test
  public void testDifferentApisWithSameApiWideConfig() throws Exception {
    @Api(name = "testApi", version = "v1", resource = "foo")
    final class Test1 {}
    ApiConfig config1 = configLoader.loadConfiguration(ServiceContext.create(), Test1.class);

    @Api(name = "testApi", version = "v1", resource = "foo")
    @ApiClass(resource = "bar")
    final class Test2 {}
    ApiConfig config2 = configLoader.loadConfiguration(ServiceContext.create(), Test2.class);

    validator.validate(Lists.newArrayList(config1, config2));
  }

  @Test
  public void testInconsistentApiWideConfig() throws Exception {
    @Api(name = "testApi", version = "v1", resource = "foo")
    final class Test1 {}
    ApiConfig config1 = configLoader.loadConfiguration(ServiceContext.create(), Test1.class);

    @Api(name = "testApi", version = "v1", resource = "bar")
    final class Test2 {}
    ApiConfig config2 = configLoader.loadConfiguration(ServiceContext.create(), Test2.class);

    try {
      validator.validate(Lists.newArrayList(config1, config2));
      fail("Expected InconsistentApiConfigurationException.");
    } catch (InconsistentApiConfigurationException expected) {
    }
  }

  @Test
  public void testApiWideConfigWithInvalidApiName() throws Exception {
    @Api(name = "TestApi", version = "v1", resource = "bar")
    final class Test {}
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Test.class);

    try {
      validator.validate(config);
      fail("Expected InvalidApiNameException.");
    } catch (InvalidApiNameException expected) {
    }
  }

  @Test
  public void testApiMethodConfigWithApiMethodNameContainingSpecialCharacter() throws Exception {
    @Api(name = "testApi", version = "v1", resource = "bar")
    final class Test {
      @ApiMethod(name = "Api.Test#Method")
      public void test() {
      }
    }

    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Test.class);

    try {
      validator.validate(config);
      fail("Expected InvalidMethodNameException.");
    } catch (InvalidMethodNameException expected) {
    }
  }

  @Test
  public void testApiMethodConfigWithApiMethodNameContainingContinuousDots() throws Exception {
    @Api(name = "testApi", version = "v1", resource = "bar")
    final class Test {
      @ApiMethod(name = "TestApi..testMethod")
      public void test() {
      }
    }

    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Test.class);

    try {
      validator.validate(config);
      fail("Expected InvalidMethodNameException.");
    } catch (InvalidMethodNameException expected) {
    }
  }

  @Test
  public void testApiMethodConfigWithApiMethodNameContainingStartingDot() throws Exception {
    @Api(name = "testApi", version = "v1", resource = "bar")
    final class Test {
      @ApiMethod(name = ".Api.TestMethod")
      public void test() {
      }
    }

    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Test.class);

    try {
      validator.validate(config);
      fail("Expected InvalidMethodNameException.");
    } catch (InvalidMethodNameException expected) {
    }
  }

  @Test
  public void testApiMethodConfigWithApiMethodNameContainingEndingDot() throws Exception {
    @Api(name = "testApi", version = "v1", resource = "bar")
    final class Test {
      @ApiMethod(name = "Api.TestMethod.")
      public void test() {
      }
    }

    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Test.class);

    try {
      validator.validate(config);
      fail("Expected InvalidMethodNameException.");
    } catch (InvalidMethodNameException expected) {
    }
  }

  @Test
  public void testApiMethodConfigWithApiMethodResponseStatusInformal() throws Exception {
    @Api(name = "testApi", version = "v1", resource = "bar")
    final class Test {
      @ApiMethod(responseStatus = 103)
      public void test() {
      }
    }

    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Test.class);

    try {
      validator.validate(config);
      fail("Expected InvalidResponseStatusException.");
    } catch (InvalidResponseStatusException expected) {
      assertThat(expected.getMessage()).contains("103");
    }
  }

  @Test
  public void testApiMethodConfigWithApiMethodResponseStatusRedirection() throws Exception {
    @Api(name = "testApi", version = "v1", resource = "bar")
    final class Test {
      @ApiMethod(responseStatus = 300)
      public void test() {
      }
    }

    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Test.class);

    try {
      validator.validate(config);
      fail("Expected InvalidResponseStatusException.");
    } catch (InvalidResponseStatusException expected) {
      assertThat(expected.getMessage()).contains("300");
    }
  }

  @Test
  public void testApiMethodConfigWithApiMethodResponseStatusCreated() throws Exception {
    @Api(name = "testApi", version = "v1", resource = "bar")
    final class Test {
      @ApiMethod(responseStatus = 201)
      public void test() {
      }
    }

    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Test.class);
    validator.validate(config);
  }

  @Test
  public void testValidateAuthenticator() throws Exception {
    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .setAuthenticators(
            ImmutableList.<Class<? extends Authenticator>>of(PassAuthenticator.class));

    validator.validate(config);
  }

  @Test
  public void testValidateAuthenticator_noNullary() throws Exception {
    final class InvalidAuthenticator implements Authenticator {
      @SuppressWarnings("unused")
      public InvalidAuthenticator(int x) {}

      @SuppressWarnings("unused")
      @Override
      public User authenticate(HttpServletRequest request) {
        return null;
      }
    }

    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .setAuthenticators(
            ImmutableList.<Class<? extends Authenticator>>of(InvalidAuthenticator.class));

    try {
      validator.validate(config);
      fail();
    } catch (InvalidConstructorException expected) {
      assertTrue(expected.getMessage().contains("Invalid custom authenticator"));
      assertTrue(expected.getMessage().endsWith(
          "InvalidAuthenticator. It must have a public nullary constructor."));
    }
  }

  @Test
  public void testValidateAuthenticator_privateNullary() throws Exception {
    final class InvalidAuthenticator implements Authenticator {
      @SuppressWarnings("unused")
      private InvalidAuthenticator() {}

      @SuppressWarnings("unused")
      @Override
      public User authenticate(HttpServletRequest request) {
        return null;
      }
    }

    config.getApiClassConfig().getMethods()
        .get(methodToEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams")))
        .setAuthenticators(
            ImmutableList.<Class<? extends Authenticator>>of(InvalidAuthenticator.class));

    try {
      validator.validate(config);
      fail();
    } catch (InvalidConstructorException expected) {
      assertTrue(expected.getMessage().contains("Invalid custom authenticator"));
      assertTrue(expected.getMessage().endsWith(
          "InvalidAuthenticator. It must have a public nullary constructor."));
    }
  }

  @Test
  public void testValidateMethods_ignoredMethod() throws Exception {
    final class Bean {

    }
    @Api
    final class Endpoint {
      @ApiMethod(ignored = AnnotationBoolean.TRUE)
      public void thisShouldBeIgnored(Bean resource1, Bean resource2) {
      }
    }
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Endpoint.class);
    // If this passes validation, then no error will be thrown. Otherwise, the validator will
    // complain that the method has two resources.
    validator.validate(config);
  }

  @Test
  public void testInvalidIssuerValueException_invalidIssuerName() throws Exception {
    @Api(issuers = {@ApiIssuer})
    final class Endpoint { }
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Endpoint.class);
    try {
      validator.validate(config);
    } catch (InvalidIssuerValueException.ForApi e) {
      assertThat(e.getMessage()).contains("issuer name cannot be blank");
    }
  }

  @Test
  public void testInvalidIssuerValueException_invalidIssuerValue() throws Exception {
    @Api(issuers = {@ApiIssuer(name = "issuer")})
    final class Endpoint { }
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Endpoint.class);
    try {
      validator.validate(config);
    } catch (InvalidIssuerValueException.ForApi e) {
      assertThat(e.getMessage()).contains("issuer 'issuer' cannot have a blank issuer value");
    }
  }

  @Test
  public void testInvalidIssuerValueException_issuerNotFound() throws Exception {
    @Api(issuerAudiences = {@ApiIssuerAudience(name = "nope", audiences = {"aud"})})
    final class Endpoint { }
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Endpoint.class);
    try {
      validator.validate(config);
    } catch (InvalidIssuerValueException.ForApi e) {
      assertThat(e.getMessage()).contains("cannot specify audiences for unknown issuer 'nope'");
    }
  }

  @Test
  public void testInvalidIssuerValueException_invalidAudience() throws Exception {
    @Api(
        issuers = {@ApiIssuer(name = "issuer", issuer = "iss")},
        issuerAudiences = {@ApiIssuerAudience(name = "issuer", audiences = {""})})
    final class Endpoint { }
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), Endpoint.class);
    try {
      validator.validate(config);
    } catch (InvalidIssuerValueException.ForApi e) {
      assertThat(e.getMessage()).contains("issuer 'issuer' cannot have null or blank audiences");
    }
  }

  private EndpointMethod methodToEndpointMethod(Method method) {
    return EndpointMethod.create(method.getDeclaringClass(), method);
  }
}
