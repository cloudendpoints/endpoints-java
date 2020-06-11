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

import com.google.api.client.util.Strings;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ApiConfigInconsistency;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.model.ApiClassConfig;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiIssuerAudienceConfig;
import com.google.api.server.spi.config.model.ApiIssuerConfigs;
import com.google.api.server.spi.config.model.ApiIssuerConfigs.IssuerConfig;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiNamespaceConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig.Classification;
import com.google.api.server.spi.config.model.Schema;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.config.model.Serializers;
import com.google.api.server.spi.config.model.Types;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation provider for Swarm endpoint configurations.  Contains all SPI-level checks for what
 * constitutes a valid API or configuration.
 *
 * @author Eric Orth
 */
public class ApiConfigValidator {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  // The underscore is allowed in the API name because of very old legacy reasons, despite the
  // annotation documentation stating otherwise.
  private static final Pattern API_NAME_PATTERN = Pattern.compile(
      "^[a-z]+[A-Za-z0-9_]*$");

  private static final Pattern API_METHOD_NAME_PATTERN = Pattern.compile(
      "^\\w+(\\.\\w+)*$");

  private final TypeLoader typeLoader;
  private final SchemaRepository schemaRepository;

  public ApiConfigValidator(TypeLoader typeLoader, SchemaRepository schemaRepository) {
    this.typeLoader = typeLoader;
    this.schemaRepository = schemaRepository;
  }

  /**
   * Validates all configurations for a single API.  Makes sure the API-level configuration matches
   * for all classes and that the contained configuration is valid and can be turned into a *.api
   * file.  Only checks for swarm-specific validity.  Apiary FE may still dislike a config for its
   * own reasons.
   *
   * @throws ApiConfigInvalidException on any invalid API-wide configuration.
   * @throws ApiClassConfigInvalidException on any invalid API class configuration.
   * @throws ApiMethodConfigInvalidException on any invalid API method configuration.
   * @throws ApiParameterConfigInvalidException on any invalid API parameter configuration.
   */
  public void validate(Iterable<? extends ApiConfig> apiConfigs)
      throws ApiConfigInvalidException, ApiClassConfigInvalidException,
      ApiMethodConfigInvalidException, ApiParameterConfigInvalidException {
    if (Iterables.isEmpty(apiConfigs)) {
      return;
    }

    Map<String, ApiMethodConfig> restfulSignatures = Maps.newHashMap();

    Iterator<? extends ApiConfig> i = apiConfigs.iterator();
    ApiConfig first = i.next();
    validate(first, restfulSignatures);

    while (i.hasNext()) {
      ApiConfig config = i.next();
      Iterable<ApiConfigInconsistency<Object>> inconsistencies =
          config.getConfigurationInconsistencies(first);
      if (!Iterables.isEmpty(inconsistencies)) {
        throw new InconsistentApiConfigurationException(config, first, inconsistencies);
      }
      validate(config, restfulSignatures);
    }
  }

  /**
   * Makes sure the contained configuration is valid and can be turned into a *.api file.  Only
   * checks for swarm-specific validity.  Apiary FE may still dislike a config for its own reasons.
   *
   * @throws ApiClassConfigInvalidException on any invalid API class or API configuration.
   * @throws ApiMethodConfigInvalidException on any invalid API method configuration.
   * @throws ApiParameterConfigInvalidException on any invalid API parameter configuration.
   */
  public void validate(ApiConfig config) throws ApiClassConfigInvalidException,
      ApiMethodConfigInvalidException, ApiParameterConfigInvalidException,
      ApiConfigInvalidException {
    validate(config, new HashMap<String, ApiMethodConfig>());
  }

  private void validate(ApiConfig config, Map<String, ApiMethodConfig> restfulSignatures)
      throws ApiClassConfigInvalidException, ApiMethodConfigInvalidException,
      ApiParameterConfigInvalidException, ApiConfigInvalidException {
    validateApiConfig(config);
    validateThirdPartyAuth(config.getApiClassConfig());
    validateMethods(config.getApiClassConfig().getMethods(), restfulSignatures);
  }

  private void validateApiConfig(ApiConfig config) throws InvalidNamespaceException,
    InvalidApiNameException, InvalidIssuerValueException.ForApi {
    validateApiName(config);
    validateNamespaceConfig(config.getNamespaceConfig(), config.getApiClassConfig());
    validateThirdPartyAuth(config);
  }

  private void validateApiName(ApiConfig config) throws InvalidApiNameException {
    if (!API_NAME_PATTERN.matcher(config.getName()).matches()) {
      throw new InvalidApiNameException(config, config.getName());
    }
  }

  private void validateNamespaceConfig(ApiNamespaceConfig config, ApiClassConfig apiConfig)
      throws InvalidNamespaceException {
    boolean allUnspecified =
        config.getOwnerDomain().isEmpty() && config.getOwnerName().isEmpty()
        && config.getPackagePath().isEmpty();
    boolean ownerFullySpecified =
        !config.getOwnerDomain().isEmpty() && !config.getOwnerName().isEmpty();

    // Either everything must be fully unspecified or owner domain/name must both be specified.
    if (!allUnspecified && !ownerFullySpecified) {
      throw new InvalidNamespaceException(apiConfig);
    }
  }

  private void validateThirdPartyAuth(ApiConfig config) throws InvalidIssuerValueException.ForApi {
    String error = checkIssuers(config.getIssuers());
    if (error != null) {
      throw new InvalidIssuerValueException.ForApi(config, error);
    }
    error = checkIssuerAudiences(config.getIssuers(), config.getIssuerAudiences());
    if (error != null) {
      throw new InvalidIssuerValueException.ForApi(config, error);
    }
  }

  private void validateThirdPartyAuth(ApiClassConfig config)
      throws InvalidIssuerValueException.ForApiClass {
    String error = checkIssuerAudiences(config.getApiConfig().getIssuers(),
        config.getIssuerAudiences());
    if (error != null) {
      throw new InvalidIssuerValueException.ForApiClass(config, error);
    }
  }

  private void validateMethods(ApiClassConfig.MethodConfigMap configMap,
      Map<String, ApiMethodConfig> restfulSignatures)
      throws ApiClassConfigInvalidException, ApiMethodConfigInvalidException,
      ApiParameterConfigInvalidException {
    Map<String, ApiMethodConfig> javaMethodNames = Maps.newHashMap();
    for (ApiMethodConfig methodConfig : configMap.values()) {
      if (!methodConfig.isIgnored()) {
        validateRestSignatureUnique(methodConfig, restfulSignatures);
        validateBackendMethodNameUnique(methodConfig, javaMethodNames);
        validateMethod(methodConfig);
        validateResourceAndFieldNames(methodConfig);
      }
    }
  }

  private void validateRestSignatureUnique(ApiMethodConfig methodConfig,
      Map<String, ApiMethodConfig> restfulSignatures) throws DuplicateRestPathException {
    String restSignature = methodConfig.getRestfulSignature();
    ApiMethodConfig seenMethod = restfulSignatures.get(restSignature);
    if (seenMethod == null) {
      restfulSignatures.put(restSignature, methodConfig);
    } else {
      throw new DuplicateRestPathException(methodConfig.getApiClassConfig(),
          restSignature, methodConfig.getName(), seenMethod.getName());
    }
  }

  private void validateBackendMethodNameUnique(ApiMethodConfig methodConfig,
      Map<String, ApiMethodConfig> javaMethodNames) throws OverloadedMethodException {
    String javaName = methodConfig.getFullJavaName();
    ApiMethodConfig seenMethod = javaMethodNames.get(javaName);
    if (seenMethod == null) {
      javaMethodNames.put(javaName, methodConfig);
    } else {
      throw new OverloadedMethodException(methodConfig.getApiClassConfig(), javaName,
          methodConfig.getName(), seenMethod.getName());
    }
  }

  private void validateResourceAndFieldNames(ApiMethodConfig methodConfig)
      throws PropertyParameterNameConflictException {
    for (ApiParameterConfig parameterConfig : methodConfig.getParameterConfigs()) {
      if (parameterConfig.getClassification() == Classification.RESOURCE) {
        Schema schema = schemaRepository.getOrAdd(
            parameterConfig.getSchemaBaseType(), methodConfig.getApiConfig());
        Set<String> fieldNames = schema.fields().keySet();
        for (ApiParameterConfig parameter : methodConfig.getParameterConfigs()) {
          if (parameter.getClassification() == Classification.API_PARAMETER &&
              !"id".equals(parameter.getName()) && fieldNames.contains(parameter.getName())) {
            log.atWarning().log("Parameter %s conflicts with a resource field name. This may "
                + "result in unexpected values in the request.", parameter.getName());
          }
        }
      }
    }
  }

  private void validateMethod(ApiMethodConfig config) throws ApiMethodConfigInvalidException,
      ApiParameterConfigInvalidException {
    if (!API_METHOD_NAME_PATTERN.matcher(config.getName()).matches()) {
      throw new InvalidMethodNameException(config, config.getName());
    }

    validateNullaryConstructor(config.getAuthenticators(), config, "custom authenticator");

    Set<String> parameterNames = Sets.newHashSet();
    for (ApiParameterConfig parameter : config.getParameterConfigs()) {
      validateParameter(parameter, parameterNames, config.getPathParameters());
    }
    validateThirdPartyAuth(config);

    TypeToken<?> returnType = config.getReturnType();
    if (typeLoader.isSchemaType(returnType) || Types.isEnumType(returnType)) {
      throw new InvalidReturnTypeException(config, returnType);
    }

    int responseStatus = config.getResponseStatus();
    if (responseStatus != ApiMethodConfig.RESPONSE_STATUS_UNSET && (responseStatus < 200 || responseStatus > 299)) {
      throw new InvalidResponseStatusException(config, responseStatus);
    }
  }

  private void validateThirdPartyAuth(ApiMethodConfig config)
      throws InvalidIssuerValueException.ForApiMethod {
    String error = checkIssuerAudiences(config.getApiClassConfig().getApiConfig().getIssuers(),
        config.getIssuerAudiences());
    if (error != null) {
      throw new InvalidIssuerValueException.ForApiMethod(config, error);
    }
  }

  private void validateNullaryConstructor(List<?> classes, ApiMethodConfig config,
      String description) throws ApiMethodConfigInvalidException {
    if (classes == null) {
      return;
    }
    for (Object clazz : classes) {
      assert clazz instanceof Class<?>;
      boolean nullaryFound = false;
      for (Constructor<?> constructor : ((Class<?>) clazz).getConstructors()) {
        if (isConstructorPublicNullary(constructor)) {
          nullaryFound = true;
          break;
        }
      }
      if (!nullaryFound) {
        throw new InvalidConstructorException(((Class<?>) clazz), config, description);
      }
    }
  }

  private static boolean isConstructorPublicNullary(Constructor<?> constructor) {
    return constructor.getParameterTypes().length == 0
        && (constructor.getModifiers() & Modifier.PUBLIC) != 0;
  }

  private void validateParameter(ApiParameterConfig parameter, Set<String> parameterNames,
      Collection<String> pathParameters) throws ApiParameterConfigInvalidException {
    try {
      validateParameterSerializers(parameter, parameter.getSerializers(), parameter.getType());
      validateParameterSerializers(parameter, parameter.getRepeatedItemSerializers(),
          parameter.getRepeatedItemType());
    } catch (IllegalStateException e) {
      // TODO: Switch to something less fragile for this error type.  I'm not comfortable
      // trusting that any ISE coming out of the serializer code will always be the multiple
      // serializer error that it is right now.  And it shouldn't be up to any code outside of the
      // validator to decide the error message.  Ideal would be that the error originates
      // here, so maybe the getSerializer chain should be changed to getSerilizer*s* with an
      // exception thrown here for multiple results.
      throw new ApiParameterConfigInvalidException(parameter, e.getMessage());
    }

    TypeToken<?> type;
    if (parameter.isRepeated()) {
      type = parameter.getRepeatedItemSerializedType();
      if (Types.isArrayType(type)) {
        throw new NestedCollectionException(parameter, type);
      }
    } else {
      type = parameter.getSchemaBaseType();
    }

    switch (parameter.getClassification()) {
      case INJECTED:
        // No classification-specific validation for injected parameters.
        break;
      case API_PARAMETER:
        validateApiParameter(parameter, parameterNames, pathParameters, type);
        break;
      case RESOURCE:
        validateResourceParameter(parameter, type);
        break;
      case UNKNOWN:
        // Unknown types are never allowed.
        throw new GenericTypeException(parameter);
      default:
        throw new AssertionError("Unrecognized parameter classification: "
            + parameter.getClassification());
    }
  }

  private void validateParameterSerializers(ApiParameterConfig config,
      List<Class<? extends Transformer<?, ?>>> serializers, TypeToken<?> parameterType)
      throws ApiParameterConfigInvalidException {
    if (serializers.isEmpty()) {
      return;
    }

    if (serializers.size() > 1) {
      throw new MultipleTransformersException(config, serializers);
    }

    TypeToken<?> sourceType = Serializers.getSourceType(serializers.get(0));
    TypeToken<?> serializedType = Serializers.getTargetType(serializers.get(0));

    if (sourceType == null || serializedType == null) {
      throw new NoTransformerInterfaceException(config, serializers.get(0));
    }

    if (!sourceType.isSupertypeOf(parameterType)) {
      throw new WrongTransformerTypeException(config, serializers.get(0), parameterType,
          sourceType);
    }
  }

  private void validateApiParameter(ApiParameterConfig parameter,
      Set<String> parameterNames, Collection<String> pathParameters, TypeToken<?> type)
      throws ApiParameterConfigInvalidException {
    if (parameter.getName() == null) {
      throw new MissingParameterNameException(parameter, type);
    }
    if (!parameterNames.add(parameter.getName())) {
      throw new DuplicateParameterNameException(parameter);
    }
    if ((parameter.getNullable() || parameter.getDefaultValue() != null)
        && pathParameters.contains(parameter.getName())) {
      throw new InvalidParameterAnnotationsException(parameter);
    }
  }

  private void validateResourceParameter(ApiParameterConfig parameter, TypeToken<?> type)
      throws ApiParameterConfigInvalidException {
    if (parameter.isRepeated()) {
      throw new CollectionResourceException(parameter, parameter.getRepeatedItemSerializedType(),
          parameter.getSchemaBaseType());
    }
    if (parameter.getName() != null) {
      throw new NamedResourceException(parameter, type);
    }
  }

  private static String checkIssuers(ApiIssuerConfigs issuers) {
    if (!issuers.isSpecified()) {
      return null;
    }
    for (IssuerConfig issuer : issuers.asMap().values()) {
      if (Strings.isNullOrEmpty(issuer.getName())) {
        return "issuer name cannot be blank";
      } else if (Strings.isNullOrEmpty(issuer.getIssuer())) {
        return "issuer '" + issuer.getName() + "' cannot have a blank issuer value";
      }
    }
    return null;
  }

  private static String checkIssuerAudiences(
      ApiIssuerConfigs issuerConfigs, ApiIssuerAudienceConfig issuerAudiences) {
    if (!issuerAudiences.isEmpty()) {
      return null;
    }
    for (Map.Entry<String, Collection<String>> entry : issuerAudiences.asMap().entrySet()) {
      if (!issuerConfigs.hasIssuer(entry.getKey())) {
        return "cannot specify audiences for unknown issuer '" + entry.getKey() + "'";
      }
      for (String audience : entry.getValue()) {
        if (Strings.isNullOrEmpty(audience)) {
          return "issuer '" + entry.getKey() + "' cannot have null or blank audiences";
        }
      }
    }
    return null;
  }
}
