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
package com.google.api.server.spi.swagger;

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.Strings;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.annotationreader.ApiAnnotationIntrospector;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiIssuerAudienceConfig;
import com.google.api.server.spi.config.model.ApiIssuerConfigs;
import com.google.api.server.spi.config.model.ApiIssuerConfigs.IssuerConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.config.model.ApiLimitMetricConfig;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiMetricCostConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.api.server.spi.config.model.FieldType;
import com.google.api.server.spi.config.model.Schema;
import com.google.api.server.spi.config.model.Schema.Field;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.config.validation.ApiConfigValidator;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.ByteArrayProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;

/**
 * Generates a {@link Swagger} object representing a set of {@link ApiConfig} objects.
 */
public class SwaggerGenerator {
  private static final Joiner COMMA_JOINER = Joiner.on(',');
  private static final String API_KEY = "api_key";
  private static final String API_KEY_PARAM = "key";
  private static final String MANAGEMENT_DEFINITIONS_KEY = "x-google-management";
  private static final String LIMITS_KEY = "limits";
  private static final String LIMIT_NAME_KEY = "name";
  private static final String LIMIT_METRIC_KEY = "metric";
  private static final String LIMIT_DISPLAY_NAME_KEY = "displayName";
  private static final String LIMIT_DEFAULT_LIMIT_KEY = "values";
  private static final String LIMIT_UNIT_KEY = "unit";
  private static final String LIMIT_PER_MINUTE_PER_PROJECT = "1/min/{project}";
  private static final String METRIC_NAME_KEY = "name";
  private static final String METRIC_VALUE_TYPE_KEY = "valueType";
  private static final String METRIC_VALUE_TYPE = "INT64";
  private static final String METRIC_KIND_KEY = "metricKind";
  private static final String METRIC_KIND = "GAUGE";
  private static final String METRICS_KEY = "metrics";
  private static final String QUOTA_KEY = "quota";

  private static final Converter<String, String> CONVERTER =
      CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);
  private static final Joiner JOINER = Joiner.on("").skipNulls();
  private static final ImmutableMap<Type, String> TYPE_TO_STRING_MAP =
      ImmutableMap.<java.lang.reflect.Type, String>builder()
          .put(String.class, "string")
          .put(Boolean.class, "boolean")
          .put(Boolean.TYPE, "boolean")
          .put(Integer.class, "integer")
          .put(Integer.TYPE, "integer")
          .put(Long.class, "integer")
          .put(Long.TYPE, "integer")
          .put(Float.class, "number")
          .put(Float.TYPE, "number")
          .put(Double.class, "number")
          .put(Double.TYPE, "number")
          .put(byte[].class, "string")
          .put(SimpleDate.class, "string")
          .put(DateAndTime.class, "string")
          .put(Date.class, "string")
          .build();
  private static final ImmutableMap<Type, String> TYPE_TO_FORMAT_MAP =
      ImmutableMap.<java.lang.reflect.Type, String>builder()
          .put(Integer.class, "int32")
          .put(Integer.TYPE, "int32")
          .put(Long.class, "int64")
          .put(Long.TYPE, "int64")
          .put(Float.class, "float")
          .put(Float.TYPE, "float")
          .put(Double.class, "double")
          .put(Double.TYPE, "double")
          .put(byte[].class, "byte")
          .put(SimpleDate.class, "date")
          .put(DateAndTime.class, "date-time")
          .put(Date.class, "date-time")
          .build();
  private static final ImmutableMap<FieldType, Class<? extends Property>> FIELD_TYPE_TO_PROPERTY_CLASS_MAP =
      ImmutableMap.<FieldType, Class<? extends Property>>builder()
          .put(FieldType.BOOLEAN, BooleanProperty.class)
          .put(FieldType.BYTE_STRING, ByteArrayProperty.class)
          .put(FieldType.DATE, DateProperty.class)
          .put(FieldType.DATE_TIME, DateTimeProperty.class)
          .put(FieldType.DOUBLE, DoubleProperty.class)
          .put(FieldType.FLOAT, FloatProperty.class)
          .put(FieldType.INT8, IntegerProperty.class)
          .put(FieldType.INT16, IntegerProperty.class)
          .put(FieldType.INT32, IntegerProperty.class)
          .put(FieldType.INT64, LongProperty.class)
          .put(FieldType.STRING, StringProperty.class)
          .build();

  private static final Function<ApiConfig, ApiKey> CONFIG_TO_ROOTLESS_KEY =
      new Function<ApiConfig, ApiKey>() {
        @Override
        public ApiKey apply(ApiConfig config) {
          return new ApiKey(config.getName(), config.getVersion(), null /* root */);
        }
      };

  public Swagger writeSwagger(Iterable<ApiConfig> configs, boolean writeInternal,
      SwaggerContext context) throws ApiConfigException {
    try {
      TypeLoader typeLoader = new TypeLoader(SwaggerGenerator.class.getClassLoader());
      SchemaRepository repo = new SchemaRepository(typeLoader);
      GenerationContext genCtx = new GenerationContext();
      genCtx.validator = new ApiConfigValidator(typeLoader, repo);
      genCtx.writeInternal = writeInternal;
      genCtx.schemata = new SchemaRepository(typeLoader);
      return writeSwagger(configs, context, genCtx);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private Swagger writeSwagger(Iterable<ApiConfig> configs, SwaggerContext context,
      GenerationContext genCtx)
      throws ApiConfigException {
    ImmutableListMultimap<ApiKey, ? extends ApiConfig> configsByKey = FluentIterable.from(configs)
        .index(CONFIG_TO_ROOTLESS_KEY);
    Swagger swagger = new Swagger()
        .produces("application/json")
        .consumes("application/json")
        .scheme(context.scheme)
        .host(context.hostname)
        .basePath(context.basePath)
        .info(new Info()
            .title(context.hostname)
            .version(context.docVersion));
    for (ApiKey apiKey : configsByKey.keySet()) {
      writeApi(apiKey, configsByKey.get(apiKey), swagger, genCtx);
    }
    writeQuotaDefinitions(swagger, genCtx);
    return swagger;
  }

  private void writeQuotaDefinitions(Swagger swagger, GenerationContext genCtx) {
    if (!genCtx.limitMetrics.isEmpty()) {
      Map<String, List<Map<String, Object>>> quotaDefinitions = new HashMap<>();
      List<Map<String, Object>> limits = new ArrayList<>();
      List<Map<String, Object>> metrics = new ArrayList<>();
      for (ApiLimitMetricConfig limitMetric : genCtx.limitMetrics.values()) {
        metrics.add(ImmutableMap.<String, Object>builder()
            .put(METRIC_NAME_KEY, limitMetric.name())
            .put(METRIC_VALUE_TYPE_KEY, METRIC_VALUE_TYPE)
            .put(METRIC_KIND_KEY, METRIC_KIND)
            .build());
        ImmutableMap.Builder<String, Object> limitBuilder = ImmutableMap.<String, Object>builder()
            .put(LIMIT_NAME_KEY, limitMetric.name())
            .put(LIMIT_METRIC_KEY, limitMetric.name())
            .put(LIMIT_DEFAULT_LIMIT_KEY, ImmutableMap.of("STANDARD", limitMetric.limit()))
            .put(LIMIT_UNIT_KEY, LIMIT_PER_MINUTE_PER_PROJECT);
        if (limitMetric.displayName() != null && !"".equals(limitMetric.displayName())) {
          limitBuilder.put(LIMIT_DISPLAY_NAME_KEY, limitMetric.displayName());
        }
        limits.add(limitBuilder.build());
      }
      quotaDefinitions.put(LIMITS_KEY, limits);
      swagger.setVendorExtension(MANAGEMENT_DEFINITIONS_KEY,
          ImmutableMap.of(METRICS_KEY, metrics, QUOTA_KEY, quotaDefinitions));
    }
  }

  private void writeApi(ApiKey apiKey, ImmutableList<? extends ApiConfig> apiConfigs,
      Swagger swagger, GenerationContext genCtx)
      throws ApiConfigException {
    // TODO: This may result in duplicate validations in the future if made available online
    genCtx.validator.validate(apiConfigs);
    for (ApiConfig apiConfig : apiConfigs) {
      for (ApiLimitMetricConfig limitMetric : apiConfig.getApiLimitMetrics()) {
        addNonConflictingApiLimitMetric(genCtx.limitMetrics, limitMetric);
      }
      writeApiClass(apiConfig, swagger, genCtx);
    }
    List<Schema> schemas = genCtx.schemata.getAllSchemaForApi(apiKey);
    for (Schema schema : schemas) {
      if (schema.enumValues().isEmpty()) {
        getOrCreateDefinitionMap(swagger).put(schema.name(), convertToSwaggerSchema(schema));
      }
    }
  }

  private void addNonConflictingApiLimitMetric(
      Map<String, ApiLimitMetricConfig> limitMetrics, ApiLimitMetricConfig limitMetric)
      throws ApiConfigException {
    if (limitMetric.equals(limitMetrics.get(limitMetric.name()))) {
      throw new ApiConfigException(String.format(
          "Multiple limit metric definitions found for metric %s. Metric definitions must have "
              + "unique names for all APIs included in the OpenAPI document, or they must have "
              + "identical definitions.", limitMetric.name()));
    }
    limitMetrics.put(limitMetric.name(), limitMetric);
  }

  private void writeApiClass(ApiConfig apiConfig, Swagger swagger,
      GenerationContext genCtx) throws ApiConfigException {
    Map<EndpointMethod, ApiMethodConfig> methodConfigs = apiConfig.getApiClassConfig().getMethods();
    for (Map.Entry<EndpointMethod, ApiMethodConfig> methodConfig : methodConfigs.entrySet()) {
      if (!methodConfig.getValue().isIgnored()) {
        ApiMethodConfig config = methodConfig.getValue();
        writeApiMethod(config, apiConfig, swagger, genCtx);
      }
    }
  }

  private void writeApiMethod(
      ApiMethodConfig methodConfig, ApiConfig apiConfig, Swagger swagger, GenerationContext genCtx)
      throws ApiConfigException {
    Path path = getOrCreatePath(swagger, methodConfig);
    Operation operation = new Operation();
    operation.setOperationId(getOperationId(apiConfig, methodConfig));
    operation.setDescription(methodConfig.getDescription());
    Collection<String> pathParameters = methodConfig.getPathParameters();
    for (ApiParameterConfig parameterConfig : methodConfig.getParameterConfigs()) {
      switch (parameterConfig.getClassification()) {
        case API_PARAMETER:
          boolean isPathParameter = pathParameters.contains(parameterConfig.getName());
          SerializableParameter parameter =
              isPathParameter ? new PathParameter() : new QueryParameter();
          parameter.setName(parameterConfig.getName());
          parameter.setDescription(parameterConfig.getDescription());
          boolean required = isPathParameter || (!parameterConfig.getNullable()
              && parameterConfig.getDefaultValue() == null);
          if (parameterConfig.isRepeated()) {
            TypeToken<?> t = parameterConfig.getRepeatedItemSerializedType();
            parameter.setType("array");
            Property p = getSwaggerArrayProperty(t);
            if (parameterConfig.isEnum()) {  // TODO: Not sure if this is the right check
              ((StringProperty) p).setEnum(getEnumValues(t));
            }
            parameter.setItems(p);
          } else if (parameterConfig.isEnum()) {
            parameter.setType("string");
            parameter.setEnum(getEnumValues(parameterConfig.getType()));
            parameter.setRequired(required);
          } else {
            parameter.setType(
                TYPE_TO_STRING_MAP.get(parameterConfig.getSchemaBaseType().getType()));
            parameter.setFormat(
                TYPE_TO_FORMAT_MAP.get(parameterConfig.getSchemaBaseType().getType()));
            parameter.setRequired(required);
          }
          operation.parameter(parameter);
          break;
        case RESOURCE:
          TypeToken<?> requestType = parameterConfig.getSchemaBaseType();
          Schema schema = genCtx.schemata.getOrAdd(requestType, apiConfig);
          BodyParameter bodyParameter = new BodyParameter();
          bodyParameter.setName("body");
          bodyParameter.setSchema(new RefModel(schema.name()));
          operation.addParameter(bodyParameter);
          break;
        case UNKNOWN:
          throw new IllegalArgumentException("Unclassifiable parameter type found.");
        case INJECTED:
          break;  // Do nothing, these are synthetic and created by the framework.
      }
    }
    Response response = new Response().description("A successful response");
    if (methodConfig.hasResourceInResponse()) {
      TypeToken<?> returnType =
          ApiAnnotationIntrospector.getSchemaType(methodConfig.getReturnType(), apiConfig);
      Schema schema = genCtx.schemata.getOrAdd(returnType, apiConfig);
      response.setSchema(new RefProperty(schema.name()));
    }
    operation.response(200, response);
    writeAuthConfig(swagger, methodConfig, operation);
    if (methodConfig.isApiKeyRequired()) {
      List<Map<String, List<String>>> security = operation.getSecurity();
      // Loop through each existing security requirement for this method, which is currently just a
      // JWT config id, and add an API key requirement to it. If there are currently no new
      // security requirements, add a new one for just the API key.
      if (security != null) {
        for (Map<String, List<String>> securityEntry : security) {
          securityEntry.put(API_KEY, ImmutableList.<String>of());
        }
      } else {
        operation.addSecurity(API_KEY, ImmutableList.<String>of());
      }
      Map<String, SecuritySchemeDefinition> definitions = swagger.getSecurityDefinitions();
      if (definitions == null || !definitions.containsKey(API_KEY)) {
        swagger.securityDefinition(API_KEY, new ApiKeyAuthDefinition(API_KEY_PARAM, In.QUERY));
      }
    }
    path.set(methodConfig.getHttpMethod().toLowerCase(), operation);
    addDefinedMetricCosts(genCtx.limitMetrics, operation, methodConfig.getMetricCosts());
  }

  private void writeAuthConfig(Swagger swagger, ApiMethodConfig methodConfig, Operation operation)
      throws ApiConfigException {
    ApiIssuerAudienceConfig issuerAudiences = methodConfig.getIssuerAudiences();
    boolean issuerAudiencesIsEmpty = !issuerAudiences.isSpecified() || issuerAudiences.isEmpty();
    List<String> legacyAudiences = methodConfig.getAudiences();
    boolean legacyAudiencesIsEmpty = legacyAudiences == null || legacyAudiences.isEmpty();
    if (issuerAudiencesIsEmpty && legacyAudiencesIsEmpty) {
      return;
    }
    if (!issuerAudiencesIsEmpty) {
      for (String issuer : issuerAudiences.getIssuerNames()) {
        ImmutableSet<String> audiences = issuerAudiences.getAudiences(issuer);
        IssuerConfig issuerConfig = methodConfig.getApiConfig().getIssuers().getIssuer(issuer);
        String fullIssuer = addNonConflictingSecurityDefinition(swagger, issuerConfig, audiences);
        operation.addSecurity(fullIssuer, ImmutableList.<String>of());
      }
    }
    if (!legacyAudiencesIsEmpty) {
      ImmutableSet<String> legacyAudienceSet = ImmutableSet.copyOf(legacyAudiences);
      String fullIssuer = addNonConflictingSecurityDefinition(
          swagger, ApiIssuerConfigs.GOOGLE_ID_TOKEN_ISSUER, legacyAudienceSet);
      String fullAltIssuer = addNonConflictingSecurityDefinition(
          swagger, ApiIssuerConfigs.GOOGLE_ID_TOKEN_ISSUER_ALT, legacyAudienceSet);
      operation.addSecurity(fullIssuer, ImmutableList.<String>of());
      operation.addSecurity(fullAltIssuer, ImmutableList.<String>of());
    }
  }

  private void addDefinedMetricCosts(Map<String, ApiLimitMetricConfig> limitMetrics,
      Operation operation, List<ApiMetricCostConfig> metricCosts) throws ApiConfigException {
    if (!metricCosts.isEmpty()) {
      Map<String, Integer> costs = new HashMap<>();
      for (ApiMetricCostConfig cost : metricCosts) {
        if (!limitMetrics.containsKey(cost.name())) {
          throw new ApiConfigException(String.format(
              "Could not add a metric cost for metric '%s'. The limit metric must be "
                  + "defined at the API level.", cost.name()));
        }
        costs.put(cost.name(), cost.cost());
      }
      operation.setVendorExtension("x-google-quota", ImmutableMap.of("metricCosts", costs));
    }
  }

  private Model convertToSwaggerSchema(Schema schema) {
    ModelImpl docSchema = new ModelImpl().type("object");
    Map<String, Property> fields = Maps.newLinkedHashMap();
    if (!schema.fields().isEmpty()) {
      for (Field f : schema.fields().values()) {
        fields.put(f.name(), convertToSwaggerProperty(f));
      }
      docSchema.setProperties(fields);
    }
    if (schema.mapValueSchema() != null) {
      docSchema.setAdditionalProperties(convertToSwaggerProperty(schema.mapValueSchema()));
    }
    return docSchema;
  }

  private Property convertToSwaggerProperty(Field f) {
    Property p = null;
    Class<? extends Property> propertyClass = FIELD_TYPE_TO_PROPERTY_CLASS_MAP.get(f.type());
    if (propertyClass != null) {
      try {
        p = propertyClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
        //cannot happen, as Property subclasses are guaranteed to have a default constructor
      }
    } else {
      if (f.type() == FieldType.OBJECT) {
        p = new RefProperty(f.schemaReference().get().name());
      } else if (f.type() == FieldType.ARRAY) {
        p = new ArrayProperty(convertToSwaggerProperty(f.arrayItemSchema()));
      } else if (f.type() == FieldType.ENUM) {
        p = new StringProperty()._enum(getEnumValues(f.schemaReference().type()));
      }
    }
    if (p == null) {
      throw new IllegalArgumentException("could not convert field " + f);
    }
    //the spec explicitly disallows description on $ref
    if (!(p instanceof RefProperty)) {
      p.description(f.description());
    }
    return p;
  }

  private static String getOperationId(ApiConfig apiConfig, ApiMethodConfig methodConfig) {
    return FluentIterable.of(apiConfig.getName(), apiConfig.getVersion(),
        apiConfig.getResource(), apiConfig.getApiClassConfig().getResource(),
        methodConfig.getEndpointMethodName()).transform(CONVERTER).join(JOINER);
  }

  private static Property getSwaggerArrayProperty(TypeToken<?> typeToken) {
    Class<?> type = typeToken.getRawType();
    if (type == String.class) {
      return new StringProperty();
    } else if (type == Boolean.class || type == Boolean.TYPE) {
      return new BooleanProperty();
    } else if (type == Integer.class || type == Integer.TYPE) {
      return new IntegerProperty();
    } else if (type == Long.class || type == Long.TYPE) {
      return new LongProperty();
    } else if (type == Float.class || type == Float.TYPE) {
      return new FloatProperty();
    } else if (type == Double.class || type == Double.TYPE) {
      return new DoubleProperty();
    } else if (type == byte[].class) {
      return new ByteArrayProperty();
    } else if (type.isEnum()) {
      return new StringProperty();
    }
    throw new IllegalArgumentException("invalid property type");
  }

  private Path getOrCreatePath(Swagger swagger, ApiMethodConfig methodConfig) {
    String pathStr = "/" + methodConfig.getCanonicalPath();
    Path path = swagger.getPath(pathStr);
    if (path == null) {
      path = new Path();
      swagger.path(pathStr, path);
    }
    return path;
  }

  private static List<String> getEnumValues(TypeToken<?> t) {
    List<String> values = Lists.newArrayList();
    for (Object value : t.getRawType().getEnumConstants()) {
      values.add(value.toString());
    }
    return values;
  }

  private static SecuritySchemeDefinition toScheme(
      IssuerConfig issuerConfig, ImmutableSet<String> audiences) {
    OAuth2Definition tokenDef = new OAuth2Definition().implicit("");
    tokenDef.setVendorExtension("x-google-issuer", issuerConfig.getIssuer());
    if (!com.google.common.base.Strings.isNullOrEmpty(issuerConfig.getJwksUri())) {
      tokenDef.setVendorExtension("x-google-jwks_uri", issuerConfig.getJwksUri());
    }
    tokenDef.setVendorExtension("x-google-audiences", COMMA_JOINER.join(audiences));
    return tokenDef;
  }

  private Map<String, Model> getOrCreateDefinitionMap(Swagger swagger) {
    Map<String, Model> definitions = swagger.getDefinitions();
    if (definitions == null) {
      definitions = new LinkedHashMap<>();
      swagger.setDefinitions(definitions);
    }
    return definitions;
  }

  private static Map<String, SecuritySchemeDefinition> getOrCreateSecurityDefinitionMap(
      Swagger swagger) {
    Map<String, SecuritySchemeDefinition> securityDefinitions = swagger.getSecurityDefinitions();
    if (securityDefinitions == null) {
      securityDefinitions = new LinkedHashMap<>();
      swagger.setSecurityDefinitions(securityDefinitions);
    }
    return securityDefinitions;
  }

  private static String addNonConflictingSecurityDefinition(
      Swagger swagger, IssuerConfig issuerConfig, ImmutableSet<String> audiences)
      throws ApiConfigException {
    Map<String, SecuritySchemeDefinition> securityDefinitions =
        getOrCreateSecurityDefinitionMap(swagger);
    String issuerPlusHash = String.format("%s-%x", issuerConfig.getName(), audiences.hashCode());
    SecuritySchemeDefinition existingDef = securityDefinitions.get(issuerConfig.getName());
    SecuritySchemeDefinition newDef = toScheme(issuerConfig, audiences);
    if (existingDef != null && !existingDef.equals(newDef)) {
      throw new ApiConfigException(
          "Multiple conflicting definitions found for issuer " + issuerConfig.getName());
    }
    swagger.securityDefinition(issuerPlusHash, newDef);
    return issuerPlusHash;
  }

  public static class SwaggerContext {
    private Scheme scheme = Scheme.HTTPS;
    private String hostname = "myapi.appspot.com";
    private String basePath = "/_ah/api";
    private String docVersion = "1.0.0";

    public SwaggerContext setApiRoot(String apiRoot) {
      try {
        URL url = new URL(apiRoot);
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

    public SwaggerContext setScheme(String scheme) {
      this.scheme = "http".equals(scheme) ? Scheme.HTTP : Scheme.HTTPS;
      return this;
    }

    public SwaggerContext setHostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    public SwaggerContext setBasePath(String basePath) {
      this.basePath = basePath;
      return this;
    }

    public SwaggerContext setDocVersion(String docVersion) {
      this.docVersion = docVersion;
      return this;
    }
  }

  private static class GenerationContext {
    private final Map<String, ApiLimitMetricConfig> limitMetrics = new TreeMap<>();
    private ApiConfigValidator validator;
    private boolean writeInternal;
    private SchemaRepository schemata;
  }
}
