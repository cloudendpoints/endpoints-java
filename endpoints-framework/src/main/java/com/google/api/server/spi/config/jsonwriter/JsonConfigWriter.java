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
package com.google.api.server.spi.config.jsonwriter;

import com.google.api.server.spi.Constant;
import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigWriter;
import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.annotationreader.ApiAnnotationIntrospector;
import com.google.api.server.spi.config.model.ApiAuthConfig;
import com.google.api.server.spi.config.model.ApiCacheControlConfig;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiFrontendLimitsConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiNamespaceConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.config.validation.ApiConfigValidator;
import com.google.api.server.spi.config.validation.InvalidReturnTypeException;
import com.google.api.server.spi.config.validation.PropertyParameterNameConflictException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

/**
 * Writer of legacy Endpoints API configurations.
 */
public class JsonConfigWriter implements ApiConfigWriter {
  /**
   * Default deadline to set in lily adapters. Use a default deadline for Swarm APIs that is
   * slightly larger than the GAE request processing limits (60 seconds). This value is also used as
   * the Harpoon service deadline when we reach BEs outside Google.
   */
  private static final double DEFAULT_LILY_DEADLINE = 65.0;

  public static final String MAP_SCHEMA_NAME = "JsonMap";
  public static final String ANY_SCHEMA_NAME = "_any";

  private final TypeLoader typeLoader;
  private final ApiConfigValidator validator;

  private final ResourceSchemaProvider resourceSchemaProvider = new JacksonResourceSchemaProvider();

  public JsonConfigWriter() throws ClassNotFoundException {
    this(JsonConfigWriter.class.getClassLoader(), new ApiConfigValidator());
  }

  public JsonConfigWriter(ClassLoader classLoader, ApiConfigValidator validator)
      throws ClassNotFoundException {
    this.typeLoader = new TypeLoader(classLoader);

    this.validator = validator;
  }

  private static final ObjectMapper objectMapper = ObjectMapperUtil.createStandardObjectMapper();

  @Override
  public Map<ApiKey, String> writeConfig(Iterable<? extends ApiConfig> configs)
      throws ApiConfigException {
    Multimap<ApiKey, ? extends ApiConfig> apisByKey = Multimaps.index(configs,
        new Function<ApiConfig, ApiKey>() {
          @Override public ApiKey apply(ApiConfig config) {
            return config.getApiKey();
          }
        });

    // This *must* retain the order of apisByKey so the lily_java_api BUILD rule has predictable
    // output order.
    Map<ApiKey, String> results = Maps.newLinkedHashMap();
    for (ApiKey apiKey : apisByKey.keySet()) {
      Collection<? extends ApiConfig> apiConfigs = apisByKey.get(apiKey);
      validator.validate(apiConfigs);
      results.put(apiKey, generateForApi(apiConfigs));
    }
    return results;
  }

  @Override
  public String getFileExtension() {
    return "api";
  }

  private String generateForApi(Iterable<? extends ApiConfig> apiConfigs)
      throws ApiConfigException {
    ObjectNode root = objectMapper.createObjectNode();
    // First, generate api-wide configuration options, given any ApiConfig.
    ApiConfig apiConfig = Iterables.get(apiConfigs, 0);
    convertApi(root, apiConfig);
    convertApiAuth(root, apiConfig.getAuthConfig());
    convertApiFrontendLimits(root, apiConfig.getFrontendLimitsConfig());
    convertApiCacheControl(root, apiConfig.getCacheControlConfig());
    convertApiNamespace(root, apiConfig.getNamespaceConfig());
    // Next, generate config-specific configuration options,
    convertApiMethods(apiConfigs, root);
    return toString(root);
  }

  /** Writes an object node as a string. */
  private String toString(ObjectNode node) throws ApiConfigException {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    } catch (IOException e) {
      throw new ApiConfigException(e);
    }
  }

  private void setNodePropertyNoConflict(
      ObjectNode node, String key, JsonNode value, String errorMessage) {
    if (!node.path(key).isMissingNode()) {
      throw new IllegalArgumentException(errorMessage);
    }
    node.set(key, value);
  }

  private void setNodePropertyNoConflict(ObjectNode node, String key, JsonNode value) {
    setNodePropertyNoConflict(node, key, value, "Multiple values for same key '" + key + "'");
  }

  private void convertApi(ObjectNode root, ApiConfig config) {
    root.put("extends", getParentApiFile());
    root.put("abstract", config.getIsAbstract());
    root.put("root", config.getRoot());
    root.put("name", config.getName());
    if (config.getCanonicalName() != null) {
      root.put("canonicalName", config.getCanonicalName());
    }
    root.put("version", config.getVersion());
    if (config.getTitle() != null) {
      root.put("title", config.getTitle());
    }
    if (config.getDescription() != null) {
      root.put("description", config.getDescription());
    }
    if (config.getDocumentationLink() != null) {
      root.put("documentation", config.getDocumentationLink());
    }
    root.put("defaultVersion", config.getIsDefaultVersion());
    ArrayNode discovery = objectMapper.createArrayNode();
    discovery.add(config.getIsDiscoverable() ? "PUBLIC" : "OFF");
    root.set("discovery", discovery);

    ObjectNode adapter = objectMapper.createObjectNode();
    adapter.put("bns", config.getBackendRoot());
    adapter.put("deadline", DEFAULT_LILY_DEADLINE);
    adapter.put("type", "lily");
    root.set("adapter", adapter);
  }

  /**
   * Returns the name of the file the config should extend. Subclasses may
   * override to use their own api file.
   */
  protected String getParentApiFile() {
    return "thirdParty.api";
  }

  /**
   * Converts the auth config from the auth annotation. Subclasses may override
   * to add additional information to the auth config.
   */
  private void convertApiAuth(ObjectNode root, ApiAuthConfig config) {
    ObjectNode authConfig = objectMapper.createObjectNode();

    authConfig.put("allowCookieAuth", config.getAllowCookieAuth());

    List<String> blockedRegions = config.getBlockedRegions();
    if (!blockedRegions.isEmpty()) {
      ArrayNode blockedRegionsNode = objectMapper.createArrayNode();
      for (String region : blockedRegions) {
        blockedRegionsNode.add(region);
      }
      authConfig.set("blockedRegions", blockedRegionsNode);
    }

    root.set("auth", authConfig);
  }

  private void convertApiFrontendLimits(ObjectNode root, ApiFrontendLimitsConfig config) {
    ObjectNode frontendLimitsConfig = objectMapper.createObjectNode();

    frontendLimitsConfig.put("unregisteredUserQps", config.getUnregisteredUserQps());
    frontendLimitsConfig.put("unregisteredQps", config.getUnregisteredQps());
    frontendLimitsConfig.put("unregisteredDaily", config.getUnregisteredDaily());

    convertApiFrontendLimitRules(frontendLimitsConfig, config.getRules());

    root.set("frontendLimits", frontendLimitsConfig);
  }

  private void convertApiCacheControl(ObjectNode root, ApiCacheControlConfig config) {
    ObjectNode cacheControlConfig = objectMapper.createObjectNode();
    cacheControlConfig.put("type", config.getType());
    cacheControlConfig.put("maxAge", config.getMaxAge());
    root.set("cacheControl", cacheControlConfig);
  }

  private void convertApiNamespace(ObjectNode root, ApiNamespaceConfig config) {
    if (!config.getOwnerDomain().isEmpty()) {
      root.put("ownerDomain", config.getOwnerDomain());
    }
    if (!config.getOwnerName().isEmpty()) {
      root.put("ownerName", config.getOwnerName());
    }
    if (!config.getPackagePath().isEmpty()) {
      root.put("packagePath", config.getPackagePath());
    }
  }

  private void convertApiFrontendLimitRules(ObjectNode frontendLimitsConfig,
      List<ApiFrontendLimitsConfig.FrontendLimitsRule> rules) {
    ArrayNode rulesConfig = objectMapper.createArrayNode();
    for (ApiFrontendLimitsConfig.FrontendLimitsRule rule : rules) {
      // TODO: Allow overriding individual rules based on same "match" field?
      ObjectNode ruleConfig = objectMapper.createObjectNode();
      ruleConfig.put("match", rule.getMatch());
      ruleConfig.put("qps", rule.getQps());
      ruleConfig.put("userQps", rule.getUserQps());
      ruleConfig.put("daily", rule.getDaily());
      ruleConfig.put("analyticsId", rule.getAnalyticsId());
      rulesConfig.add(ruleConfig);
    }
    frontendLimitsConfig.set("rules", rulesConfig);
  }

  private void convertApiMethods(Iterable<? extends ApiConfig> configs, ObjectNode root)
      throws IllegalArgumentException, SecurityException, ApiConfigException {
    ObjectNode methodsNode = objectMapper.createObjectNode();
    ObjectNode descriptorNode = objectMapper.createObjectNode();
    ObjectNode descriptorSchemasNode = objectMapper.createObjectNode();
    ObjectNode descriptorMethodsNode = objectMapper.createObjectNode();
    descriptorNode.set("schemas", descriptorSchemasNode);
    descriptorNode.set("methods", descriptorMethodsNode);

    for (ApiConfig config : configs) {
      convertApiMethods(methodsNode, descriptorSchemasNode, descriptorMethodsNode, config);
    }

    root.set("methods", methodsNode);
    root.set("descriptor", descriptorNode);
  }

  private void convertApiMethods(ObjectNode methodsNode, ObjectNode descriptorSchemasNode,
      ObjectNode descriptorMethodsNode, ApiConfig apiConfig)
      throws IllegalArgumentException, SecurityException, ApiConfigException {
    Map<EndpointMethod, ApiMethodConfig> methodConfigs = apiConfig.getApiClassConfig().getMethods();
    for (Map.Entry<EndpointMethod, ApiMethodConfig> methodConfig : methodConfigs.entrySet()) {
      if (!methodConfig.getValue().isIgnored()) {
        EndpointMethod endpointMethod = methodConfig.getKey();
        ApiMethodConfig config = methodConfig.getValue();
        convertApiMethod(methodsNode, descriptorSchemasNode, descriptorMethodsNode,
            endpointMethod, config, apiConfig);
      }
    }
  }

  private void convertApiMethod(ObjectNode methodsNode, ObjectNode descriptorSchemasNode,
      ObjectNode descriptorMethodsNode, EndpointMethod endpointMethod, ApiMethodConfig config,
      ApiConfig apiConfig)
      throws IllegalArgumentException, SecurityException, ApiConfigException {
    ObjectNode methodNode = objectMapper.createObjectNode();
    setNodePropertyNoConflict(
        methodsNode, config.getFullMethodName(), methodNode);

    methodNode.put("path", config.getPath());
    methodNode.put("httpMethod", config.getHttpMethod());

    methodNode.set("authLevel", objectMapper.convertValue(config.getAuthLevel(), JsonNode.class));
    methodNode.set("scopes", objectMapper.convertValue(
            AuthScopeExpressions.encode(config.getScopeExpression()), JsonNode.class));
    methodNode.set("audiences", objectMapper.convertValue(config.getAudiences(), JsonNode.class));
    methodNode.set("clientIds", objectMapper.convertValue(config.getClientIds(), JsonNode.class));
    methodNode.put("rosyMethod", config.getFullJavaName());

    ObjectNode descriptorMethodNode = objectMapper.createObjectNode();
    setNodePropertyNoConflict(
        descriptorMethodsNode, config.getFullJavaName(), descriptorMethodNode);

    convertMethodRequest(endpointMethod, methodNode, descriptorSchemasNode, descriptorMethodNode,
        config, apiConfig);
    convertMethodResponse(endpointMethod, methodNode, descriptorSchemasNode, descriptorMethodNode,
        config);
  }

  private void convertMethodRequest(EndpointMethod endpointMethod, ObjectNode apiMethodNode,
      ObjectNode descriptorSchemasNode, ObjectNode descriptorMethodNode, ApiMethodConfig config,
      ApiConfig apiConfig)
      throws IllegalArgumentException, SecurityException, ApiConfigException {
    ObjectNode requestNode = objectMapper.createObjectNode();

    convertMethodRequestParameters(endpointMethod, requestNode,
        descriptorSchemasNode, descriptorMethodNode, config, apiConfig);

    apiMethodNode.set("request", requestNode);
  }

  private void convertMethodRequestParameters(EndpointMethod endpointMethod, ObjectNode requestNode,
      ObjectNode descriptorSchemasNode, ObjectNode descriptorMethodNode, ApiMethodConfig config,
      ApiConfig apiConfig)
      throws IllegalArgumentException, SecurityException, ApiConfigException {
    ObjectNode parametersNode = objectMapper.createObjectNode();
    Method method = endpointMethod.getMethod();
    List<ApiParameterConfig> parameterConfigs = config.getParameterConfigs();

    for (ApiParameterConfig parameterConfig : parameterConfigs) {
      switch (parameterConfig.getClassification()) {
        case INJECTED:
          // Do nothing.
          break;
        case API_PARAMETER:
          convertSimpleParameter(parameterConfig, parametersNode);
          break;
        case RESOURCE:
          // Inserts resource in.
          convertComplexParameter(parameterConfig, method, descriptorSchemasNode,
              descriptorMethodNode, apiConfig, parameterConfigs);
          break;
        case UNKNOWN:
          throw new IllegalArgumentException("Unclassifiable parameter type found.");
      }
    }
    // Set API parameter types if needed.
    if (parametersNode.size() != 0) {
      requestNode.set("parameters", parametersNode);
    }
    // Sets request body to auto-template if Lily request portion is set..
    if (descriptorMethodNode.get("request") != null) {
      requestNode.put("body", "autoTemplate(backendRequest)");
      requestNode.put("bodyName", "resource");
    } else {
      requestNode.put("body", "empty");
    }
  }

  /*
   * This appends to the API file any type which can go into the methods.request.parameters
   * portion of the resulting .api file. This includes parameter types and any type, such as
   * enum or lists of simple types, which can be converted into simple types.
   */
  private void convertSimpleParameter(ApiParameterConfig config, ObjectNode parametersNode) {
    ObjectNode parameterNode = objectMapper.createObjectNode();

    Type type;
    if (config.isRepeated()) {
      parameterNode.put("repeated", true);
      type = config.getRepeatedItemSerializedType();
    } else {
      type = config.getSchemaBaseType();
    }

    if (config.isEnum()) {
      ObjectNode enumValuesNode = objectMapper.createObjectNode();
      for (Object enumConstant : ((Class<?>) type).getEnumConstants()) {
        ObjectNode enumNode = objectMapper.createObjectNode();
        enumValuesNode.set(enumConstant.toString(), enumNode);
      }
      parameterNode.set("enum", enumValuesNode);

      type = String.class;
    }

    parameterNode.put("type", typeLoader.getParameterTypes().get(type));
    parameterNode.put("required", !config.getNullable() && config.getDefaultValue() == null);

    // TODO: Try to find a way to move default value interpretation/conversion into the
    // general configuration code.
    String defaultValue = config.getDefaultValue();
    if (defaultValue != null) {
      Class<?> parameterClass = (Class<?>) type;
      try {
        objectMapper.convertValue(defaultValue, parameterClass);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(String.format(
            "'%s' is not a valid default value for type '%s'", defaultValue, type));
      }
      parameterNode.put("default", defaultValue);
    }

    parametersNode.set(config.getName(), parameterNode);
  }

  /*
   * This appends to the API file any type which can NOT go into the methods.request.parameters.
   * These will be present as Json schemas inside the Lily descriptor portion of the API file.
   */
  private void convertComplexParameter(ApiParameterConfig config, Method method,
      ObjectNode descriptorSchemasNode, ObjectNode descriptorMethodNode,
      ApiConfig apiConfig, List<ApiParameterConfig> parameterConfigs) throws ApiConfigException {
    Type type = config.getSchemaBaseType();
    ObjectNode requestTypeNode = objectMapper.createObjectNode();
    addTypeToNode(descriptorSchemasNode, type, null, requestTypeNode, apiConfig, parameterConfigs);

    setNodePropertyNoConflict(descriptorMethodNode, "request", requestTypeNode, "Method "
        + method.getDeclaringClass().getName() + "." + method.getName()
        + " cannot have multiple resource parameters");
  }

  private void convertMethodResponse(EndpointMethod serviceMethod, ObjectNode methodNode,
      ObjectNode descriptorSchemasNode, ObjectNode descriptorMethodNode,
      ApiMethodConfig config) throws ApiConfigException {
    ObjectNode responseNode = objectMapper.createObjectNode();
    methodNode.set("response", responseNode);
    if (methodHasResourceInResponse(serviceMethod)) {
      responseNode.put("body", "autoTemplate(backendResponse)");

      // TODO: Get from ApiMethodConfig.
      Type returnType = ApiAnnotationIntrospector.getSchemaType(
          serviceMethod.getReturnType(), config.getApiClassConfig().getApiConfig());
      descriptorMethodNode.set("response",
          convertMethodResponseType(descriptorSchemasNode, returnType, config));
    } else {
      // Void methods don't generate response sections in the descriptor
      responseNode.put("body", "empty");
    }
  }

  /**
   * Returns a node with the response object type, wrapping any arrays into a new Collection schema.
   */
  private ObjectNode convertMethodResponseType(ObjectNode descriptorSchemasNode, Type returnType,
      ApiMethodConfig config) throws ApiConfigException {
    ObjectNode returnTypeNode = objectMapper.createObjectNode();
    String responseTypeName =
        addTypeToNode(descriptorSchemasNode, returnType, null, returnTypeNode,
            config.getApiClassConfig().getApiConfig(), null);

    // TODO: Move to ApiConfigValidator once return type parsing is pulled out of the
    // writer.
    if (typeLoader.isSchemaType(returnType) || TypeLoader.isEnumType(returnType)) {
      throw new InvalidReturnTypeException(config, returnType);
    }

    if (TypeLoader.getArrayItemType(returnType) != null && returnType != byte[].class) {
      ObjectNode propertiesNode = objectMapper.createObjectNode();
      propertiesNode.set("items", returnTypeNode);

      ObjectNode arrayWrapperNode = objectMapper.createObjectNode();
      arrayWrapperNode.put("id", responseTypeName);
      arrayWrapperNode.put("type", "object");
      arrayWrapperNode.set("properties", propertiesNode);
      descriptorSchemasNode.set(responseTypeName, arrayWrapperNode);

      returnTypeNode = objectMapper.createObjectNode();
      returnTypeNode.put("$ref", responseTypeName);
    }
    return returnTypeNode;
  }

  private boolean methodHasResourceInResponse(EndpointMethod serviceMethod) {
    Type returnType = serviceMethod.getReturnType();
    return returnType != Void.TYPE && returnType != Void.class;
  }

  @VisibleForTesting
  String addTypeToSchema(ObjectNode schemasNode, Type type, ApiConfig apiConfig,
      List<ApiParameterConfig> parameterConfigs) throws ApiConfigException {
    return addTypeToSchema(schemasNode, type, null, apiConfig, parameterConfigs);
  }

  /**
   * Adds an arbitrary, non-array type to a given schema config.
   *
   * @param schemasNode the config to store the generated type schema
   * @param type the type from which to generate a schema
   * @param enclosingType for bean properties, the enclosing bean type, used for resolving type
   *        variables
   * @return the name of the schema generated from the type
   */
  @VisibleForTesting
  String addTypeToSchema(
      ObjectNode schemasNode, Type type, Type enclosingType, ApiConfig apiConfig,
      List<ApiParameterConfig> parameterConfigs) throws ApiConfigException {
    if (typeLoader.isSchemaType(type)) {
      return typeLoader.getSchemaTypes().get(type);
    } else if (type == Object.class) {
      if (!schemasNode.has(ANY_SCHEMA_NAME)) {
        ObjectNode anySchema = objectMapper.createObjectNode();
        anySchema.put("id", ANY_SCHEMA_NAME);
        anySchema.put("type", "any");
        schemasNode.set(ANY_SCHEMA_NAME, anySchema);
      }
      return ANY_SCHEMA_NAME;
    } else if (typeLoader.isMapType(type)) {
      if (!schemasNode.has(MAP_SCHEMA_NAME)) {
        ObjectNode mapSchema = objectMapper.createObjectNode();
        mapSchema.put("id", MAP_SCHEMA_NAME);
        mapSchema.put("type", "object");
        schemasNode.set(MAP_SCHEMA_NAME, mapSchema);
      }
      return MAP_SCHEMA_NAME;
    }

    // If we already have this schema defined, don't define it again!
    String typeName = apiConfig.getSimpleName(type);
    JsonNode existing = schemasNode.get(typeName);
    if (existing != null && existing.isObject()) {
      return typeName;
    }

    ObjectNode schemaNode = objectMapper.createObjectNode();
    if (type instanceof ParameterizedType) {
      Type collectionType = ((ParameterizedType) type).getRawType();

      // This check is necessary for parameterized types that have a specific schema format, e.g.
      // for Objectify Key types.
      if (typeLoader.isSchemaType(collectionType)) {
        return typeLoader.getSchemaTypes().get(collectionType);
      }

      if (typeLoader.getClassTypes().get("CollectionResponses").isAssignableFrom(
              (Class<?>) collectionType)) {
        // treat CollectionResponse or its subclass as a regular bean
        // TODO: Handle parameterized beans in general?
        addBeanTypeToSchema(
            schemasNode, apiConfig.getSimpleName(type), schemaNode, type, apiConfig,
            parameterConfigs);
      } else {
        throw new IllegalArgumentException("Parameterized type " + type + " not supported.");
      }
    } else if (type instanceof Class) {
      Class<?> c = (Class<?>) type;
      if (c.isEnum()) {
        schemasNode.set(typeName, schemaNode);
        schemaNode.put("id", typeName);
        schemaNode.put("type", "string");

        ArrayNode enumNode = objectMapper.createArrayNode();
        for (Object enumConstant : c.getEnumConstants()) {
          enumNode.add(enumConstant.toString());
        }
        schemaNode.set("enum", enumNode);
      } else {
        // JavaBean
        Type serializedType = ApiAnnotationIntrospector.getSchemaType(c, apiConfig);
        if (!c.equals(serializedType)) {
          return addTypeToSchema(schemasNode, serializedType, enclosingType, apiConfig,
              parameterConfigs);
        } else {
          addBeanTypeToSchema(schemasNode, typeName, schemaNode, c, apiConfig, parameterConfigs);
        }
      }
    } else {
      throw new IllegalArgumentException(String.format("Object type %s not supported.", type));
    }
    return typeName;
  }

  private void addBeanTypeToSchema(ObjectNode schemasNode, String typeName, ObjectNode schemaNode,
      Type type, ApiConfig apiConfig, List<ApiParameterConfig> parameterConfigs)
          throws ApiConfigException {
    schemasNode.set(typeName, schemaNode);
    schemaNode.put("id", typeName);
    schemaNode.put("type", "object");
    ObjectNode propertiesNode = objectMapper.createObjectNode();
    addBeanProperties(schemasNode, propertiesNode, type, apiConfig, parameterConfigs);
    schemaNode.set("properties", propertiesNode);
  }

  /**
   * Iterates over the given JavaBean class and adds the following to the given config object
   * (the value of "properties" of a schema object): "&lt;name&gt;": {"type": "&lt;type&gt;"}, where
   * "name" is the name of the JavaBean property and "type" the type of its value.
   */
  protected void addBeanProperties(ObjectNode schemasNode, ObjectNode node, Type beanType,
      ApiConfig apiConfig, List<ApiParameterConfig> parameterConfigs) throws ApiConfigException {
    // CollectionResponse<T> is treated as a bean but it is a parameterized type, too.
    Class<?> beanClass = beanType instanceof ParameterizedType ?
        (Class<?>) ((ParameterizedType) beanType).getRawType() : (Class<?>) beanType;
    ResourceSchema schema = resourceSchemaProvider.getResourceSchema(beanClass, apiConfig);
    for (Entry<String, ResourcePropertySchema> entry : schema.getProperties().entrySet()) {
      String propertyName = entry.getKey();
      validatePropertyName(propertyName, parameterConfigs);
      ObjectNode propertyNode = objectMapper.createObjectNode();
      Type propertyType = entry.getValue().getJavaType();
      if (propertyType != null) {
        addTypeToNode(schemasNode, propertyType, beanType, propertyNode,
            apiConfig, parameterConfigs);
        node.set(propertyName, propertyNode);
      }
    }
  }

  // TODO: When resource parsing is refactored out of the config writer, move this to the
  // validator.
  private static void validatePropertyName(String propertyName,
      List<ApiParameterConfig> parameterConfigs) throws ApiConfigException {
    // Special case to allow id as it is a common scenario for REST for the id parameter to be the
    // same id as the id property.  Thus conflict is unlikely.
    if (propertyName.equals("id")) {
      return;
    }

    if (parameterConfigs != null) {
      for (ApiParameterConfig parameter : parameterConfigs) {
        if (propertyName.equals(parameter.getName())) {
          throw new PropertyParameterNameConflictException(parameter);
        }
      }
    }
  }

  /**
   * Adds a schema for a type into an output node. For arrays, this generates nested schemas inline
   * for however many dimensions are necessary.
   *
   * @return an appropriate name for the schema if one isn't already assigned
   */
  protected String addTypeToNode(ObjectNode schemasNode, Type type, Type enclosingType,
      ObjectNode node, ApiConfig apiConfig, List<ApiParameterConfig> parameterConfigs)
          throws ApiConfigException {
    Type itemType = TypeLoader.getArrayItemType(type);

    if (typeLoader.getSchemaTypes().containsKey(type)) {
      String basicTypeName = typeLoader.getSchemaTypes().get(type);
      addElementTypeToNode(schemasNode, type, basicTypeName, node, apiConfig);
      return basicTypeName;
    } else if (itemType != null) {
      ObjectNode items = objectMapper.createObjectNode();
      node.put("type", "array");
      node.set(Constant.ITEMS, items);

      String itemTypeName = addTypeToNode(schemasNode, itemType, enclosingType, items, apiConfig,
          parameterConfigs);
      String arraySuffix = "Collection";
      StringBuilder sb = new StringBuilder(itemTypeName.length() + arraySuffix.length());
      sb.append(itemTypeName).append(arraySuffix);
      sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
      return sb.toString();
    } else if (type instanceof TypeVariable) {
      if (enclosingType instanceof ParameterizedType) {
        // TODO: Fix this if/when parameterized beans are supported.
        Type[] typeArgs = ((ParameterizedType) enclosingType).getActualTypeArguments();
        Type actualArg = typeArgs.length > 0 ? typeArgs[0] : null;
        return addTypeToNode(schemasNode, actualArg, null, node, apiConfig, parameterConfigs);
      } else {
        throw new IllegalArgumentException(
            String.format("Object type %s not supported.", type));
      }
    } else {
      String typeName = addTypeToSchema(schemasNode, type, enclosingType, apiConfig,
          parameterConfigs);
      addElementTypeToNode(schemasNode, type, typeName, node, apiConfig);
      return typeName;
    }
  }

  /**
   * Adds a basic (non-array) type to an output node, assuming the type has a corresponding schema
   * in the provided configuration if it will ever have one.
   */
  private void addElementTypeToNode(
      ObjectNode schemasNode, Type type, String typeName, ObjectNode node, ApiConfig apiConfig) {

    // This check works better than checking schemaTypes in the case of Map<K, V>
    if (schemasNode.has(typeName)) {
      node.put("$ref", typeName);
    } else {
      node.put("type", typeName);
      String format = schemaFormatForType(type, apiConfig);
      if (format != null) {
        node.put("format", format);
      }
    }
  }

  // If a type has a serializer installed, resolve down to target type of the serialization chain to
  // find the schema format.
  @Nullable
  private String schemaFormatForType(Type type, ApiConfig apiConfig) {
    Type serializedType = ApiAnnotationIntrospector.getSchemaType(type, apiConfig);
    if (!type.equals(serializedType)) {
      return schemaFormatForType(serializedType, apiConfig);
    }
    return typeLoader.getSchemaFormats().get(type);
 }
}
