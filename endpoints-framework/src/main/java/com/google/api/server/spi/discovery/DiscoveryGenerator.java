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
package com.google.api.server.spi.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Preconditions;
import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.Strings;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.Description;
import com.google.api.server.spi.config.annotationreader.ApiAnnotationIntrospector;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiNamespaceConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig.Classification;
import com.google.api.server.spi.config.model.FieldType;
import com.google.api.server.spi.config.model.Schema;
import com.google.api.server.spi.config.model.Schema.Field;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.config.model.AuthScopeRepository;
import com.google.api.server.spi.config.model.StandardParameters;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.DirectoryList.Items;
import com.google.api.services.discovery.model.DirectoryList.Items.Icons;
import com.google.api.services.discovery.model.JsonSchema;
import com.google.api.services.discovery.model.RestDescription;
import com.google.api.services.discovery.model.RestDescription.Auth;
import com.google.api.services.discovery.model.RestDescription.Auth.Oauth2;
import com.google.api.services.discovery.model.RestDescription.Auth.Oauth2.ScopesElement;
import com.google.api.services.discovery.model.RestMethod;
import com.google.api.services.discovery.model.RestMethod.Request;
import com.google.api.services.discovery.model.RestMethod.Response;
import com.google.api.services.discovery.model.RestResource;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.TypeToken;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Generates discovery documents without contacting the discovery generator service.
 */
public class DiscoveryGenerator {
  private static final Splitter DOT_SPLITTER = Splitter.on('.');
  private static final ObjectMapper objectMapper = ObjectMapperUtil.createStandardObjectMapper();
  private static final RestDescription REST_SKELETON = new RestDescription()
      .setBatchPath("batch")
      .setDescription("This is an API")
      .setDiscoveryVersion("v1")
      .setIcons(new RestDescription.Icons()
          .setX16("https://www.gstatic.com/images/branding/product/1x/googleg_16dp.png")
          .setX32("https://www.gstatic.com/images/branding/product/1x/googleg_32dp.png"))
      .setKind("discovery#restDescription")
      .setParameters(createStandardParameters())
      .setProtocol("rest");

  private final TypeLoader typeLoader;

  public DiscoveryGenerator(TypeLoader typeLoader) {
    this.typeLoader = typeLoader;
  }

  public Result writeDiscovery(Iterable<ApiConfig> configs, DiscoveryContext context) {
    return writeDiscovery(configs, context, new SchemaRepository(typeLoader));
  }

  public Result writeDiscovery(
      Iterable<ApiConfig> configs, DiscoveryContext context, SchemaRepository schemaRepository) {
    ImmutableListMultimap<ApiKey, ApiConfig> configsByKey = Multimaps.index(configs,
        new Function<ApiConfig, ApiKey>() {
          @Override public ApiKey apply(ApiConfig config) {
            return config.getApiKey();
          }
        });
    ImmutableMap.Builder<ApiKey, RestDescription> builder = ImmutableMap.builder();
    // "Default" API versions were determined automagically in legacy endpoints.
    // This version only allows to remove an API from default ones by adding
    // defaultVersion = AnnotationBoolean.FALSE to @Api
    ImmutableSet.Builder<ApiKey> preferred = ImmutableSet.builder();
    for (ApiKey apiKey : configsByKey.keySet()) {
      ImmutableList<ApiConfig> apiConfigs = configsByKey.get(apiKey);
      if (context.generateAll || apiConfigs.get(0).getIsDiscoverable()) {
        builder.put(apiKey, writeApi(apiKey, apiConfigs, context, schemaRepository));
        // last config takes precedence (same as writeApi)
        if (Iterables.getLast(apiConfigs).getIsDefaultVersion()) {
          preferred.add(apiKey);
        }
      }
    }
    ImmutableMap<ApiKey, RestDescription> discoveryDocs = builder.build();
    return Result.builder()
        .setDiscoveryDocs(discoveryDocs)
        .setDirectory(generateDirectory(discoveryDocs, preferred.build(), context))
        .build();
  }

  private RestDescription writeApi(ApiKey apiKey, Iterable<ApiConfig> apiConfigs,
      DiscoveryContext context, SchemaRepository schemaRepo) {
    // The first step is to scan all methods and try to extract a base path, aka a common prefix
    // for all methods. This prefix must end in a slash and can't contain any path parameters.
    String servicePath = computeApiServicePath(apiConfigs);
    String basePath = context.basePath + "/" + servicePath;
    RestDescription doc = REST_SKELETON.clone()
        .setBasePath(basePath)
        .setBaseUrl(context.getApiRoot() + "/" + servicePath)
        .setId(apiKey.getName() + ":" + apiKey.getVersion())
        .setName(apiKey.getName())
        .setRootUrl(context.getApiRoot() + "/")
        .setServicePath(servicePath)
        .setVersion(apiKey.getVersion());

    final AuthScopeRepository scopeRepo = new AuthScopeRepository();

    for (ApiConfig config : apiConfigs) {
      // API descriptions should be identical across all configs, but the last one will take
      // precedence here if there happens to be divergence.
      if (config.getDescription() != null) {
        doc.setDescription(config.getDescription());
      }
      if (config.getTitle() != null) {
        doc.setTitle(config.getTitle());
      }
      if (config.getDocumentationLink() != null) {
        doc.setDocumentationLink(config.getDocumentationLink());
      }
      if (config.getNamespaceConfig() != null) {
        ApiNamespaceConfig namespaceConfig = config.getNamespaceConfig();
        if (!Strings.isEmptyOrWhitespace(namespaceConfig.getOwnerName())) {
          doc.setOwnerName(namespaceConfig.getOwnerName());
        }
        if (!Strings.isEmptyOrWhitespace(namespaceConfig.getOwnerDomain())) {
          doc.setOwnerDomain(namespaceConfig.getOwnerDomain());
        }
        if (!Strings.isEmptyOrWhitespace(namespaceConfig.getPackagePath())) {
          doc.setPackagePath(namespaceConfig.getPackagePath());
        }
      }
      if (config.getCanonicalName() != null) {
        doc.setCanonicalName(config.getCanonicalName());
      }
      scopeRepo.add(config.getScopeExpression());
      for (ApiMethodConfig methodConfig : config.getApiClassConfig().getMethods().values()) {
        if (!methodConfig.isIgnored()) {
          writeApiMethod(config, servicePath, doc, methodConfig, schemaRepo, scopeRepo);
        }
      }
    }

    Map<String, ScopesElement> scopeElements = new LinkedHashMap<>();
    for (Entry<String, String> entry : scopeRepo.getDescriptionsByScope().entrySet()) {
      scopeElements.put(entry.getKey(), new ScopesElement().setDescription(entry.getValue()));
    }
    doc.setAuth(new Auth().setOauth2(new Oauth2().setScopes(scopeElements)));

    List<Schema> schemas = schemaRepo.getAllSchemaForApi(apiKey);
    if (!schemas.isEmpty()) {
      Map<String, JsonSchema> docSchemas = Maps.newTreeMap();
      for (Schema schema : schemas) {
        docSchemas.put(schema.name(), convertToDiscoverySchema(schema));
      }
      doc.setSchemas(docSchemas);
    }
    return doc;
  }

  private void writeApiMethod(ApiConfig config, String servicePath, RestDescription doc,
      ApiMethodConfig methodConfig, SchemaRepository schemaRepo, AuthScopeRepository scopeRepo) {
    List<String> parts = DOT_SPLITTER.splitToList(methodConfig.getFullMethodName());
    Map<String, RestMethod> methods = getMethodMapFromDoc(doc, parts);
    Map<String, JsonSchema> parameters = convertMethodParameters(methodConfig);
    AuthScopeExpression scopeExpression = methodConfig.getScopeExpression();
    RestMethod method = new RestMethod()
        .setDescription(methodConfig.getDescription())
        .setHttpMethod(methodConfig.getHttpMethod())
        .setId(methodConfig.getFullMethodName())
        .setPath(methodConfig.getCanonicalPath().substring(servicePath.length()))
        .setScopes(AuthScopeExpressions.encodeMutable(scopeExpression));
    scopeRepo.add(scopeExpression);
    List<String> parameterOrder = computeParameterOrder(methodConfig);
    if (!parameterOrder.isEmpty()) {
      method.setParameterOrder(parameterOrder);
    }
    if (!parameters.isEmpty()) {
      method.setParameters(parameters);
    }
    ApiParameterConfig requestParamConfig = getAndCheckMethodRequestResource(methodConfig);
    if (requestParamConfig != null) {
      TypeToken<?> requestType = requestParamConfig.getSchemaBaseType();
      Schema schema = schemaRepo.getOrAdd(requestType, config);
      method.setRequest(new Request().set$ref(schema.name()).setParameterName("resource"));
    }
    if (methodConfig.hasResourceInResponse()) {
      TypeToken<?> returnType =
          ApiAnnotationIntrospector.getSchemaType(methodConfig.getReturnType(), config);
      Schema schema = schemaRepo.getOrAdd(returnType, config);
      method.setResponse(new Response().set$ref(schema.name()));
    }
    methods.put(parts.get(parts.size() - 1), method);
  }

  private List<String> computeParameterOrder(ApiMethodConfig methodConfig) {
    ImmutableSortedSet.Builder<String> queryParamBuilder = ImmutableSortedSet.naturalOrder();
    Collection<String> pathParameters = methodConfig.getPathParameters();
    for (ApiParameterConfig parameterConfig : methodConfig.getParameterConfigs()) {
      if (parameterConfig.getClassification() == Classification.API_PARAMETER
          && !pathParameters.contains(parameterConfig.getName())
          && !parameterConfig.getNullable()) {
        queryParamBuilder.add(parameterConfig.getName());
      }
    }
    List<String> order = new ArrayList<>(pathParameters);
    order.addAll(queryParamBuilder.build());
    return order;
  }

  private JsonSchema convertToDiscoverySchema(Schema schema) {
    JsonSchema docSchema = new JsonSchema()
        .setId(schema.name())
        .setType(schema.type());
    if (!schema.fields().isEmpty()) {
      Map<String, JsonSchema> fields = Maps.newLinkedHashMap();
      for (Field f : schema.fields().values()) {
        fields.put(f.name(), convertToDiscoverySchema(f));
      }
      docSchema.setProperties(fields);
    }
    if (schema.mapValueSchema() != null) {
      docSchema.setAdditionalProperties(convertToDiscoverySchema(schema.mapValueSchema()));
    }
    docSchema.setDescription(schema.description());
    if (!schema.enumValues().isEmpty()) {
      docSchema.setEnum(new ArrayList<>(schema.enumValues()));
      docSchema.setEnumDescriptions(new ArrayList<>(schema.enumDescriptions()));
    }
    return docSchema;
  }

  private JsonSchema convertToDiscoverySchema(Field f) {
    if (f.schemaReference() != null) {
      return new JsonSchema()
          .setDescription(f.description())
          .set$ref(f.schemaReference().get().name());
    }
    JsonSchema fieldSchema = new JsonSchema()
        .setType(f.type().getDiscoveryType())
        .setDescription(f.description())
        .setFormat(f.type().getDiscoveryFormat());
    if (f.type() == FieldType.ARRAY) {
      fieldSchema.setItems(convertToDiscoverySchema(f.arrayItemSchema()));
    }
    return fieldSchema;
  }

  private ApiParameterConfig getAndCheckMethodRequestResource(ApiMethodConfig methodConfig) {
    ApiParameterConfig config = null;
    for (ApiParameterConfig parameterConfig : methodConfig.getParameterConfigs()) {
      if (parameterConfig.getClassification() == Classification.RESOURCE) {
        if (config != null) {
          throw new IllegalArgumentException(String.format(
              "Method %s can't have multiple resource parameters", methodConfig.getFullJavaName()));
        }
        config = parameterConfig;
      }
    }
    return config;
  }

  /**
   * Gets the correct map in a {@link RestDescription} to add a method to, based on its name. This
   * is slightly complicated due to the fact that methods can exist outside of resources and on the
   * {@link RestDescription} object itself.
   */
  private Map<String, RestMethod> getMethodMapFromDoc(RestDescription doc, List<String> parts) {
    if (parts.size() == 2) {
      if (doc.getMethods() == null) {
        doc.setMethods(new TreeMap<String, RestMethod>());
      }
      return doc.getMethods();
    }
    RestResource resource = null;
    Map<String, RestResource> resources = doc.getResources();
    if (resources == null) {
      resources = new TreeMap<>();
      doc.setResources(resources);
    }
    for (int i = 1; i < parts.size() - 1; i++) {
      String part = parts.get(i);
      if (resources == null) {
        resources = new TreeMap<>();
        resource.setResources(resources);
      }
      resource = resources.get(part);
      if (resource == null) {
        resource = new RestResource();
        resources.put(part, resource);
      }
      resources = resource.getResources();
    }
    if (resource.getMethods() == null) {
      resource.setMethods(new TreeMap<String, RestMethod>());
    }
    return resource.getMethods();
  }

  private Map<String, JsonSchema> convertMethodParameters(ApiMethodConfig methodConfig) {
    Map<String, JsonSchema> parameters = Maps.newLinkedHashMap();
    Collection<String> pathParameters = methodConfig.getPathParameters();
    for (ApiParameterConfig parameterConfig : methodConfig.getParameterConfigs()) {
      if (parameterConfig.getClassification() == Classification.API_PARAMETER) {
        parameters.put(
            parameterConfig.getName(),
            convertMethodParameter(
                parameterConfig,
                pathParameters.contains(parameterConfig.getName())));
      }
    }
    return parameters;
  }

  private JsonSchema convertMethodParameter(
      ApiParameterConfig parameterConfig, boolean isPathParameter) {
    JsonSchema schema = new JsonSchema();
    TypeToken<?> type;
    if (parameterConfig.isRepeated()) {
      schema.setRepeated(true);
      type = parameterConfig.getRepeatedItemSerializedType();
    } else {
      type = parameterConfig.getSchemaBaseType();
    }

    if (parameterConfig.isEnum()) {
      List<String> enumValues = Lists.newArrayList();
      List<String> enumDescriptions = Lists.newArrayList();
      for (java.lang.reflect.Field field : type.getRawType().getFields()) {
        if (field.isEnumConstant()) {
          enumValues.add(field.getName());
          Description description = field.getAnnotation(Description.class);
          enumDescriptions.add(description == null ? "" : description.value());
        }
      }
      schema.setEnum(enumValues);
      schema.setEnumDescriptions(enumDescriptions);

      type = TypeToken.of(String.class);
    }

    schema.setType(typeLoader.getSchemaType(type));
    schema.setFormat(FieldType.fromType(type).getDiscoveryFormat());
    if (!parameterConfig.getNullable() && parameterConfig.getDefaultValue() == null) {
      schema.setRequired(true);
    }
    // TODO: Try to find a way to move default value interpretation/conversion into the
    // general configuration code.
    String defaultValue = parameterConfig.getDefaultValue();
    if (defaultValue != null) {
      Class<?> parameterClass = type.getRawType();
      try {
        objectMapper.convertValue(defaultValue, parameterClass);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(String.format(
            "'%s' is not a valid default value for type '%s'", defaultValue, type));
      }
      schema.setDefault(defaultValue);
    }

    if (isPathParameter) {
      schema.setLocation("path");
    } else {
      schema.setLocation("query");
    }
    if (parameterConfig.getDescription() != null) {
      schema.setDescription(parameterConfig.getDescription());
    }
    return schema;
  }

  private String computeApiServicePath(Iterable<ApiConfig> apiConfigs) {
    CommonPathPrefixBuilder builder = new CommonPathPrefixBuilder();
    for (ApiConfig apiConfig : apiConfigs) {
      for (ApiMethodConfig methodConfig : apiConfig.getApiClassConfig().getMethods().values()) {
        builder.addPath(methodConfig.getCanonicalPath());
      }
    }
    return builder.getCommonPrefix();
  }

  private DirectoryList generateDirectory(Map<ApiKey, RestDescription> discoveryDocs,
      ImmutableSet<ApiKey> preferred, DiscoveryContext context) {
    DirectoryList directory = new DirectoryList()
        .setDiscoveryVersion("v1")
        .setKind("discovery#directoryList");
    List<Items> items = Lists.newArrayList();
    for (Map.Entry<ApiKey, RestDescription> entry : discoveryDocs.entrySet()) {
      RestDescription doc = entry.getValue();
      String relativePath = "/apis/" + doc.getName() + "/" + doc.getVersion() + "/rest";
      items.add(new Items()
          .setDescription(doc.getDescription())
          .setDiscoveryLink("." + relativePath)
          .setDiscoveryRestUrl(context.getApiRoot() + "/discovery/v1" + relativePath)
          .setIcons(new Icons()
                .setX16("https://www.gstatic.com/images/branding/product/1x/googleg_16dp.png")
                .setX32("https://www.gstatic.com/images/branding/product/1x/googleg_32dp.png"))
          .setId(doc.getName() + ":" + doc.getVersion())
          .setKind("discovery#directoryItem")
          .setName(doc.getName())
          .setPreferred(preferred.contains(entry.getKey()))
          .setTitle(doc.getTitle())
          .setVersion(doc.getVersion())
          .setDocumentationLink(doc.getDocumentationLink()));
    }
    return directory.setItems(items);
  }

  @AutoValue
  public abstract static class Result {
    public abstract DirectoryList directory();
    public abstract ImmutableMap<ApiKey, RestDescription> discoveryDocs();

    static Builder builder() {
      return new AutoValue_DiscoveryGenerator_Result.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setDirectory(DirectoryList directory);
      public abstract Builder setDiscoveryDocs(ImmutableMap<ApiKey, RestDescription> discoveryDocs);
      public abstract Result build();
    }
  }

  public static class DiscoveryContext {
    private String scheme = "https";
    private String hostname = "myapi.appspot.com";
    private String basePath = "/_ah/api";
    private boolean generateAll = true;

    public String getApiRoot() {
      return scheme + "://" + hostname + basePath;
    }

    public DiscoveryContext setApiRoot(String apiRoot) {
      try {
        URL url = new URL(Strings.stripTrailingSlash(apiRoot));
        hostname = url.getHost();
        if (("http".equals(url.getProtocol()) && url.getPort() != 80 && url.getPort() != -1)
            || ("https".equals(url.getProtocol()) && url.getPort() != 443 && url.getPort() != -1)) {
          hostname += ":" + url.getPort();
        }
        basePath = Strings.stripTrailingSlash(url.getPath());
        setScheme(url.getProtocol());
        return this;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
    }

    public DiscoveryContext setScheme(String scheme) {
      Preconditions.checkArgument("http".equals(scheme) || "https".equals(scheme),
          "scheme must be http or https");
      this.scheme = scheme;
      return this;
    }

    public DiscoveryContext setHostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    public DiscoveryContext setBasePath(String basePath) {
      this.basePath = Strings.stripTrailingSlash(basePath);
      return this;
    }

    /**
     * Returns whether or not APIs with discoverable set to false should be generated.
     */
    public DiscoveryContext setGenerateAll(boolean generateAll) {
      this.generateAll = generateAll;
      return this;
    }
  }

  private static Map<String, JsonSchema> createStandardParameters() {
    TreeMap<String, JsonSchema> params = new TreeMap<>();
    params.put(StandardParameters.ALT, new JsonSchema()
        .setDefault("json")
        .setDescription("Data format for the response.")
        .setEnum(Lists.newArrayList("json"))
        .setEnumDescriptions(
            Lists.newArrayList("Responses with Content-Type of application/json"))
        .setLocation("query")
        .setType("string"));
    params.put(StandardParameters.FIELDS, new JsonSchema()
        .setDescription(
            "Selector specifying which fields to include in a partial response.")
        .setLocation("query")
        .setType("string"));
    params.put(StandardParameters.KEY, new JsonSchema()
        .setDescription("API key. Your API key identifies your project and provides you with "
            + "API access, quota, and reports. Required unless you provide an OAuth 2.0 "
            + "token.")
        .setLocation("query")
        .setType("string"));
    params.put(StandardParameters.OAUTH_TOKEN, new JsonSchema()
        .setDescription("OAuth 2.0 token for the current user.")
        .setLocation("query")
        .setType("string"));
    params.put(StandardParameters.PRETTY_PRINT, new JsonSchema()
        .setDefault("true")
        .setDescription("Returns response with indentations and line breaks.")
        .setLocation("query")
        .setType("boolean"));
    params.put(StandardParameters.QUOTA_USER, new JsonSchema()
        .setDescription("Available to use for quota purposes for server-side applications. "
            + "Can be any arbitrary string assigned to a user, but should not exceed 40 "
            + "characters. Overrides userIp if both are provided.")
        .setLocation("query")
        .setType("string"));
    params.put(StandardParameters.USER_IP, new JsonSchema()
        .setDescription("IP address of the site where the request originates. Use this if "
            + "you want to enforce per-user limits.")
        .setLocation("query")
        .setType("string"));
    return params;
  }
}
