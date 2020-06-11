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

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonErrorContainer;
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
import com.google.api.server.spi.config.model.ApiMethodConfig.ErrorResponse;
import com.google.api.server.spi.config.model.ApiMetricCostConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.api.server.spi.config.model.AuthScopeRepository;
import com.google.api.server.spi.config.model.FieldType;
import com.google.api.server.spi.config.model.Schema;
import com.google.api.server.spi.config.model.Schema.Field;
import com.google.api.server.spi.config.model.Schema.SchemaReference;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.config.model.Types;
import com.google.api.server.spi.config.validation.ApiConfigValidator;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.net.UrlEscapers;
import com.google.common.reflect.TypeToken;
import io.swagger.models.ExternalDocs;
import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.RefResponse;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.ByteArrayProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.refs.RefType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.text.StrSubstitutor;

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
  //expected "additionalProperties: true" for free-from objects is not possible with Java API
  //using an object property with empty properties is semantically identical
  private static final ObjectProperty FREE_FORM_PROPERTY = new ObjectProperty()
      .properties(Collections.emptyMap());
  //some well-known types should be inlined to avoid polluting model namespace
  private static final ImmutableSet<String> INLINED_MODEL_NAMES = ImmutableSet.of(
      GoogleJsonError.class.getSimpleName(), GoogleJsonError.ErrorInfo.class.getSimpleName()
  );
  
  private static final Function<ApiConfig, ApiKey> CONFIG_TO_ROOTLESS_KEY =
      config -> new ApiKey(config.getName(), config.getVersion(), null /* root */);

  public Swagger writeSwagger(Iterable<ApiConfig> configs, SwaggerContext context)
      throws ApiConfigException {
    try {
      TypeLoader typeLoader = new TypeLoader(SwaggerGenerator.class.getClassLoader());
      SchemaRepository repo = new SchemaRepository(typeLoader);
      GenerationContext genCtx = new GenerationContext();
      genCtx.validator = new ApiConfigValidator(typeLoader, repo);
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
            .title(context.title != null ? context.title : context.hostname)
            .description(context.description)
            .version(context.docVersion)
            //TODO contact, license, termsOfService could be configured
        );
    if (!Strings.isEmptyOrWhitespace(context.apiName)) {
      swagger.vendorExtension("x-google-api-name", context.apiName);
    }
    for (ApiKey apiKey : configsByKey.keySet()) {
      writeApi(apiKey, configsByKey.get(apiKey), swagger, context, genCtx);
    }
    checkEquivalentPaths(swagger);
    combineCommonParameters(swagger, context);
    //TODO could also combine common responses
    normalizeOperationParameters(swagger);
    writeQuotaDefinitions(swagger, genCtx);
    return swagger;
  }

  /*
    A generated spec might have "equivalent" paths like this:
    - POST /myapi/v1/foo/{id}
    - GET /myapi/v1/foo/{fooId}
    This is valid for the Discovery format, but won't work on Swagger.
   */
  private void checkEquivalentPaths(Swagger swagger) {
    List<Entry<String, List<String>>> duplicatePaths = swagger.getPaths().keySet().stream()
        .collect(Collectors.groupingBy(path -> path.replaceAll("\\{[^}]+}", "{%}")))
        .entrySet().stream()
        .filter(entry -> entry.getValue().size() > 1)
        .collect(Collectors.toList());
    if (!duplicatePaths.isEmpty()) {
      throw new IllegalStateException("Equivalent paths found:" + duplicatePaths.stream()
          .map(entry -> String.format("\n%s -> %s", entry.getKey(), entry.getValue()))
          .collect(Collectors.joining()));
    }
  }

  /*
   * Swagger library will set parameters to empty by default. We force them to be null.
   * If not empty, makes sure the body is always last.
   */
  public static void normalizeOperationParameters(Swagger swagger) {
    swagger.getPaths().values().stream()
        .flatMap(path -> path.getOperations().stream())
        .forEach(operation -> {
          List<Parameter> parameters = operation.getParameters();
          if (parameters != null && parameters.isEmpty()) {
            operation.setParameters(null);
          }
        });
  }

  private void combineCommonParameters(Swagger swagger, SwaggerContext context) {
    if (!context.extractCommonParametersAsRefs && !context.combineCommonParametersInSamePath) {
      return;
    }
    
    Map<String, Multiset<Parameter>> paramNameCounter = new LinkedHashMap<>();
    Multimap<Parameter, Path> specLevelParameters = HashMultimap.create();
    Map<Path, Multimap<Parameter, Operation>> pathLevelParameters = Maps.newHashMap();
    
    //collect parameters on all operations
    swagger.getPaths().values().forEach(path -> {
      Multimap<Parameter, Operation> parameters = HashMultimap.create();
      path.getOperations().forEach(operation -> {
        operation.getParameters().forEach(parameter -> {
          Multiset<Parameter> counter = Optional
              .ofNullable(paramNameCounter.get(getRefName(parameter)))
              .orElse(HashMultiset.create());
          counter.add(parameter);
          paramNameCounter.put(getRefName(parameter), counter);
          specLevelParameters.put(parameter, path);
          parameters.put(parameter, operation);
        });
        pathLevelParameters.put(path, parameters);
      });
    });
    
    if (context.extractCommonParametersAsRefs) {
      //combine common spec-level params (only if more than one path)
      specLevelParameters.asMap().forEach((parameter, paths) -> {
        //parameters used in more than one path are replaced
        if (paths.size() > 1) {
          //if multiple params are named the same, only replace the one with the most occurrences
          //TODO add param name suffix depending on "in" and "required" values to deduplicate
          Multiset<Parameter> paramCounter = Multisets
              .copyHighestCountFirst(paramNameCounter.get(getRefName(parameter)));
          if (paramCounter.iterator().next().equals(parameter)) {
            addGlobalParameter(swagger, parameter);
            swagger.getPaths().values().forEach(path -> path.getOperations()
                .forEach(operation -> replaceParameterByRef(operation.getParameters(), parameter)
            ));
            pathLevelParameters.values().forEach(pathParameters -> pathParameters.removeAll(parameter));
          }
        }
      });
    }
    
      //combine remaining common path-level params
    pathLevelParameters.forEach((path, parameterMap) -> {
      parameterMap.asMap().forEach((parameter, operations) -> {
        //if parameter is used in all operations on this path, move it to path level
        boolean combined = false;
        if (context.combineCommonParametersInSamePath 
            && operations.size() == path.getOperations().size()) {
          path.addParameter(parameter);
          operations.forEach(operation -> operation.getParameters().remove(parameter));
          combined = true;
        }
        //if parameter is more than once in this path but was not extracted before, extract as ref
        if (context.extractCommonParametersAsRefs && operations.size() > 1 && !combined) {
          addGlobalParameter(swagger, parameter);
          operations.forEach(operation ->
              replaceParameterByRef(operation.getParameters(), parameter));
        }
      });
    });
  }

  private void addGlobalParameter(Swagger swagger, Parameter parameter) {
    if (swagger.getParameters() == null) {
      swagger.setParameters(new TreeMap<>());
    }
    swagger.addParameter(getRefName(parameter), parameter);
  }

  private void replaceParameterByRef(List<Parameter> opParameters, Parameter parameter) {
    int index = opParameters.indexOf(parameter);
    if (index != -1) {
      opParameters.add(index,
          new RefParameter(getFullRef(RefType.PARAMETER, getRefName(parameter))));
      opParameters.remove(parameter);
    }
  }

  private String getRefName(Parameter parameter) {
    String suffix = "_" + parameter.getIn() + "_parameter";
    return parameter.getName() + suffix;
  }

  private void writeQuotaDefinitions(Swagger swagger, GenerationContext genCtx) {
    if (!genCtx.limitMetrics.isEmpty()) {
      Map<String, List<Map<String, Object>>> quotaDefinitions = new HashMap<>();
      List<Map<String, Object>> limits = new ArrayList<>();
      List<Map<String, Object>> metrics = new ArrayList<>();
      for (ApiLimitMetricConfig limitMetric : genCtx.limitMetrics.values()) {
        Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
            .put(METRIC_NAME_KEY, limitMetric.name())
            .put(METRIC_VALUE_TYPE_KEY, METRIC_VALUE_TYPE)
            .put(METRIC_KIND_KEY, METRIC_KIND);
        if (!Strings.isEmptyOrWhitespace(limitMetric.displayName())) {
          builder.put(LIMIT_DISPLAY_NAME_KEY, limitMetric.displayName());
        }
        metrics.add(builder.build());
        limits.add(ImmutableMap.<String, Object>builder()
            .put(LIMIT_NAME_KEY, limitMetric.name())
            .put(LIMIT_METRIC_KEY, limitMetric.name())
            .put(LIMIT_DEFAULT_LIMIT_KEY, ImmutableMap.of("STANDARD", limitMetric.limit()))
            .put(LIMIT_UNIT_KEY, LIMIT_PER_MINUTE_PER_PROJECT).build());
      }
      quotaDefinitions.put(LIMITS_KEY, limits);
      swagger.setVendorExtension(MANAGEMENT_DEFINITIONS_KEY,
          ImmutableMap.of(METRICS_KEY, metrics, QUOTA_KEY, quotaDefinitions));
    }
  }

  private void writeApi(ApiKey apiKey, ImmutableList<? extends ApiConfig> apiConfigs,
      Swagger swagger, SwaggerContext context, GenerationContext genCtx)
      throws ApiConfigException {
    // TODO: This may result in duplicate validations in the future if made available online
    genCtx.validator.validate(apiConfigs);
    for (ApiConfig apiConfig : apiConfigs) {
      for (ApiLimitMetricConfig limitMetric : apiConfig.getApiLimitMetrics()) {
        addNonConflictingApiLimitMetric(genCtx.limitMetrics, limitMetric);
      }
      writeApiClass(apiConfig, swagger, context, genCtx);
      swagger.tag(getTag(apiConfig, context));
    }
    List<Schema> schemas = genCtx.schemata.getAllSchemaForApi(apiKey);
    for (Schema schema : schemas) {
      //enum, maps and some explicitly listed models should be inlined
      if (isEnumModel(schema) || isMapModel(schema) || isInlinedModel(schema)) {
        continue;
      }
      getOrCreateDefinitionMap(swagger).put(schema.name(), convertToSwaggerSchema(schema));
    }
  }

  private boolean isEnumModel(Schema schema) {
    return !schema.enumValues().isEmpty();
  }

  private boolean isMapModel(Schema schema) {
    return SchemaRepository.isJsonMapSchema(schema) || schema.mapValueSchema() != null;
  }

  private boolean isInlinedModel(Schema schema) {
    return INLINED_MODEL_NAMES.contains(schema.name());
  }

  private Tag getTag(ApiConfig apiConfig, SwaggerContext context) {
    Tag tag = new Tag().name(getTagName(apiConfig, context));
    String description = apiConfig.getDescription();
    if (!Strings.isEmptyOrWhitespace(description)) {
      tag.description(description);
    }
    String documentationLink = apiConfig.getDocumentationLink();
    if (!Strings.isEmptyOrWhitespace(documentationLink)) {
      tag.externalDocs(new ExternalDocs().url(documentationLink));
    }
    return tag;
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

  private void writeApiClass(ApiConfig apiConfig, Swagger swagger, SwaggerContext context,
      GenerationContext genCtx) throws ApiConfigException {
    Map<EndpointMethod, ApiMethodConfig> methodConfigs = apiConfig.getApiClassConfig().getMethods();
    for (Map.Entry<EndpointMethod, ApiMethodConfig> methodConfig : methodConfigs.entrySet()) {
      if (!methodConfig.getValue().isIgnored()) {
        ApiMethodConfig config = methodConfig.getValue();
        writeApiMethod(config, apiConfig, swagger, context, genCtx);
      }
    }
  }

  private void writeApiMethod(ApiMethodConfig methodConfig, ApiConfig apiConfig, Swagger swagger,
      SwaggerContext context, GenerationContext genCtx) throws ApiConfigException {
    Path path = getOrCreatePath(swagger, methodConfig);
    Operation operation = new Operation()
      .operationId(getOperationId(apiConfig, methodConfig, context))
      .tags(Collections.singletonList(getTagName(apiConfig, context)))
      .description(methodConfig.getDescription())
      .deprecated(methodConfig.isDeprecated() ? true : null);
    Collection<String> pathParameters = methodConfig.getPathParameters();
    for (ApiParameterConfig parameterConfig : methodConfig.getParameterConfigs()) {
      boolean isPathParameter = pathParameters.contains(parameterConfig.getName());
      switch (parameterConfig.getClassification()) {
        case API_PARAMETER:
          AbstractSerializableParameter parameter =
              isPathParameter ? new PathParameter() : new QueryParameter();
          parameter.name(parameterConfig.getName()).description(parameterConfig.getDescription());
          String defaultValue = parameterConfig.getDefaultValue();
          if (!Strings.isEmptyOrWhitespace(defaultValue)) {
            parameter.setDefaultValue(defaultValue);
          }
          boolean required = isPathParameter || (!parameterConfig.getNullable()
              && defaultValue == null);
          if (parameterConfig.isRepeated()) {
            TypeToken<?> t = parameterConfig.getRepeatedItemSerializedType();
            parameter.type("array")
                //RestServletRequestParamReader uses "," as a separator for repeated path params 
                // => csv, but reads multiple occurrences of query parameters => multi
                .collectionFormat(isPathParameter ? "csv" : "multi");
            Property p = getSwaggerArrayProperty(t);
            if (parameterConfig.isEnum()) {  // TODO: Not sure if this is the right check
              ((StringProperty) p)._enum(getEnumValues(t));
            }
            parameter.items(p);
          } else if (parameterConfig.isEnum()) {
            parameter.type("string")
                ._enum(getEnumValues(parameterConfig.getType()))
                .required(required);
          } else {
            parameter.type(
                TYPE_TO_STRING_MAP.get(parameterConfig.getSchemaBaseType().getType()))
                .format(
                    TYPE_TO_FORMAT_MAP.get(parameterConfig.getSchemaBaseType().getType()))
                .required(required);
          }
          operation.parameter(parameter);
          break;
        case RESOURCE:
          TypeToken<?> requestType = parameterConfig.getSchemaBaseType();
          Schema schema = genCtx.schemata.getOrAdd(requestType, apiConfig);
          BodyParameter bodyParameter = new BodyParameter()
              .name(schema.name())
              .description(parameterConfig.getDescription())
              .schema(getSchema(schema));
          bodyParameter.setRequired(true);
          operation.addParameter(bodyParameter);
          break;
        case UNKNOWN:
          throw new IllegalArgumentException("Unclassifiable parameter type found.");
        case INJECTED:
          break;  // Do nothing, these are synthetic and created by the framework.
      }
    }
    Response response = new Response().description("A successful response");
    int responseCode = methodConfig.getEffectiveResponseStatus();
    if (methodConfig.hasResourceInResponse()) {
      TypeToken<?> returnType =
          ApiAnnotationIntrospector.getSchemaType(methodConfig.getReturnType(), apiConfig);
      Schema schema = genCtx.schemata.getOrAdd(returnType, apiConfig);
      response.responseSchema(getSchema(schema))
        .description("A " + schema.name() + " response");
    }
    operation.response(responseCode, response);

    boolean addGoogleJsonErrorAsDefaultResponse = context.addGoogleJsonErrorAsDefaultResponse;
    boolean addErrorCodesForServiceExceptions = context.addErrorCodesForServiceExceptions;
    if (addGoogleJsonErrorAsDefaultResponse || addErrorCodesForServiceExceptions) {
      //add error response model only if necessary
      if (addErrorCodesForServiceExceptions) {
        //add error code specific to the exceptions thrown by the method
        List<ErrorResponse> errorCodes = methodConfig.getErrorReponses();
        for (ErrorResponse error : errorCodes) {
          operation.response(error.code, 
              getOrCreateErrorModelRef(swagger, apiConfig, genCtx, error.name, error.description));
        }
      }
      if (addGoogleJsonErrorAsDefaultResponse) {
        //add GoogleJsonError as the default response
        operation.defaultResponse(
            getOrCreateErrorModelRef(swagger, apiConfig, genCtx, null,null));
      }
    }
    
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

  private RefResponse getOrCreateErrorModelRef(Swagger swagger, ApiConfig apiConfig, 
      GenerationContext genCtx, String name, String description) {
    Model schema = getSchema(genCtx.schemata
        .getOrAdd(TypeToken.of(GoogleJsonErrorContainer.class), apiConfig));
    if (swagger.getResponses() == null) {
      swagger.setResponses(new TreeMap<>());
    }
    String ref = Optional.ofNullable(name).orElse("DefaultError");
    swagger.response(ref, new Response()
            .description(Optional.ofNullable(description).orElse("A failed response"))
            .responseSchema(schema));
    return new RefResponse(getFullRef(RefType.RESPONSE, ref));
  }

  private Model getSchema(Schema schema) {
    if (SchemaRepository.isJsonMapSchema(schema)) {
      return new ModelImpl().additionalProperties(FREE_FORM_PROPERTY);
    }
    Field mapField = schema.mapValueSchema();
    if (mapField != null) {
      return new ModelImpl().additionalProperties(convertToSwaggerProperty(mapField));
    }
    return new RefModel(getFullRef(RefType.DEFINITION, schema.name()));
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
    ImmutableList<String> scopes = ImmutableList
        .copyOf(methodConfig.getScopeExpression().getAllScopes());
    if (!issuerAudiencesIsEmpty) {
      for (String issuer : issuerAudiences.getIssuerNames()) {
        ImmutableSet<String> audiences = issuerAudiences.getAudiences(issuer);
        IssuerConfig issuerConfig = methodConfig.getApiConfig().getIssuers().getIssuer(issuer);
        List<String> requiredScopes = issuerConfig.isUseScopesInAuthFlow() ? scopes :
            Collections.emptyList();
        String fullIssuer = addNonConflictingSecurityDefinition(swagger, issuerConfig, audiences, 
            requiredScopes);
        operation.addSecurity(fullIssuer, requiredScopes);
      }
    }
    if (!legacyAudiencesIsEmpty) {
      ImmutableSet<String> legacyAudienceSet = ImmutableSet.copyOf(legacyAudiences);
      String fullIssuer = addNonConflictingSecurityDefinition(
          swagger, ApiIssuerConfigs.GOOGLE_ID_TOKEN_ISSUER, legacyAudienceSet, scopes);
      String fullAltIssuer = addNonConflictingSecurityDefinition(
          swagger, ApiIssuerConfigs.GOOGLE_ID_TOKEN_ISSUER_ALT, legacyAudienceSet, scopes);
      operation.addSecurity(fullIssuer, scopes);
      operation.addSecurity(fullAltIssuer, scopes);
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
    String description = schema.description();
    if (!Strings.isEmptyOrWhitespace(description)) {
      docSchema.description(description);
    }
    if (!schema.fields().isEmpty()) {
      Map<String, Property> fields = new TreeMap<>();
      for (Field f : schema.fields().values()) {
        fields.put(f.name(), convertToSwaggerProperty(f));
      }
      docSchema.setProperties(fields);
    }
    //map schema should be inlined, but handling anyway 
    Field mapValueSchema = schema.mapValueSchema();
    if (mapValueSchema != null) {
      docSchema.setAdditionalProperties(convertToSwaggerProperty(mapValueSchema));
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
      SchemaReference schemaReference = f.schemaReference();
      if (f.type() == FieldType.OBJECT) {
        Schema schema = schemaReference.get();
        if (isInlinedModel(schema)) {
          p = inlineObjectProperty(schemaReference);
        } else if (isMapModel(schema)) {
          p = inlineMapProperty(schemaReference);
        } else {
          String name = schema.name();
          p = new RefProperty(getFullRef(RefType.DEFINITION, name));
        }
      } else if (f.type() == FieldType.ARRAY) {
        p = new ArrayProperty(convertToSwaggerProperty(f.arrayItemSchema()));
      } else if (f.type() == FieldType.ENUM) {
        p = new StringProperty()._enum(getEnumValues(schemaReference.type()));
      }
    }
    if (p == null) {
      throw new IllegalArgumentException("could not convert field " + f);
    }
    //the spec explicitly disallows description on $ref
    if (!(p instanceof RefProperty)) {
      p.description(f.description());
      if (f.required() != null) {
        p.setRequired(f.required());
      }
    }
    return p;
  }

  private Property inlineObjectProperty(SchemaReference schemaReference) {
    Schema schema = schemaReference.get();
    Map<String, Property> properties = Maps
        .transformValues(schema.fields(), this::convertToSwaggerProperty);
    return new ObjectProperty(ImmutableMap.copyOf(properties));
  }

  private MapProperty inlineMapProperty(SchemaReference schemaReference) {
    Schema schema = schemaReference.get();
    Field mapField = schema.mapValueSchema();
    if (SchemaRepository.isJsonMapSchema(schema)
      || mapField == null) { //map field should not be null for non-JsonMap schema, handling anyway
      return new MapProperty(FREE_FORM_PROPERTY);
    }
    return new MapProperty(convertToSwaggerProperty(mapField));
  }

  private static String getTagName(ApiConfig apiConfig, SwaggerContext context) {
    return NamingContext.build(apiConfig, null).resolve(context.tagTemplate);
  }

  private String getFullRef(RefType type, String name) {
    return type.getInternalPrefix() + UrlEscapers.urlFormParameterEscaper().escape(name);
  }

  private static String getOperationId(ApiConfig apiConfig, ApiMethodConfig methodConfig, SwaggerContext context) {
    return NamingContext.build(apiConfig, methodConfig).resolve(context.operationIdTemplate);
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
      ByteArrayProperty property = new ByteArrayProperty();
      //this will add a base64 pattern to the property
      return PropertyBuilder.build(property.getType(), property.getFormat(),
          Collections.emptyMap());
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
      if (swagger.getPaths() == null) {
        swagger.setPaths(new TreeMap<>());
      }
      swagger.path(pathStr, path);
    }
    return path;
  }

  private static List<String> getEnumValues(TypeToken<?> t) {
    return new ArrayList<>(Types.getEnumValuesAndDescriptions((TypeToken<Enum<?>>) t).keySet());
  }

  private static OAuth2Definition toScheme(
      IssuerConfig issuerConfig, ImmutableSet<String> audiences) {
    OAuth2Definition tokenDef = new OAuth2Definition()
        .implicit(issuerConfig.getAuthorizationUrl());
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
      definitions = new TreeMap<>();
      swagger.setDefinitions(definitions);
    }
    return definitions;
  }

  private static Map<String, SecuritySchemeDefinition> getOrCreateSecurityDefinitionMap(
      Swagger swagger) {
    Map<String, SecuritySchemeDefinition> securityDefinitions = swagger.getSecurityDefinitions();
    if (securityDefinitions == null) {
      securityDefinitions = new TreeMap<>();
      swagger.setSecurityDefinitions(securityDefinitions);
    }
    return securityDefinitions;
  }

  private static String addNonConflictingSecurityDefinition(Swagger swagger,
      IssuerConfig issuerConfig, ImmutableSet<String> audiences, List<String> scopes)
      throws ApiConfigException {
    Map<String, SecuritySchemeDefinition> securityDefinitions =
        getOrCreateSecurityDefinitionMap(swagger);
    String issuerPlusHash = String.format("%s-%x", issuerConfig.getName(), audiences.hashCode());
    OAuth2Definition newDef = toScheme(issuerConfig, audiences);
    SecuritySchemeDefinition existingDef = securityDefinitions.get(issuerPlusHash);
    if (existingDef != null) {
      checkExistingDefinition(issuerConfig.getName(), newDef, existingDef);
    }
    OAuth2Definition def = existingDef != null ? (OAuth2Definition) existingDef : newDef;
    scopes.forEach(scope -> def.addScope(scope, AuthScopeRepository.getDescription(scope)));
    swagger.securityDefinition(issuerPlusHash, def);
    return issuerPlusHash;
  }

  private static void checkExistingDefinition(String defName, OAuth2Definition newDef,
      SecuritySchemeDefinition existingDef) throws ApiConfigException {
    if (!(existingDef instanceof OAuth2Definition)) {
      throw new ApiConfigException(
          "Conflicting definition types found for issuer " + defName);
    }
    OAuth2Definition existingOAuth2Def = (OAuth2Definition) existingDef;
    boolean propertiesMatchExceptScope = Stream.<Function<OAuth2Definition, Object>>of(
        OAuth2Definition::getType, OAuth2Definition::getAuthorizationUrl,
        OAuth2Definition::getFlow, OAuth2Definition::getTokenUrl,
        OAuth2Definition::getDescription, OAuth2Definition::getVendorExtensions)
        .allMatch(getter -> Objects.equals(getter.apply(existingOAuth2Def), getter.apply(newDef)));
    if (!propertiesMatchExceptScope) {
      throw new ApiConfigException(
          "Conflicting OAuth2 definitions found for issuer " + defName);
    }
  }

  public static class SwaggerContext {
    public static final String DEFAULT_TAG_TEMPLATE = "${apiName}:${apiVersion}${.Resource}";
    public static final String DEFAULT_OPERATION_ID_TEMPLATE = "${apiName}:${apiVersion}${.Resource}${.method}";
    
    private Scheme scheme = Scheme.HTTPS;
    private String hostname = "myapi.appspot.com";
    private String basePath = "/_ah/api";
    private String docVersion = "1.0.0";
    private String title;
    private String description;
    private String apiName;
    private String tagTemplate = DEFAULT_TAG_TEMPLATE;
    private String operationIdTemplate = DEFAULT_OPERATION_ID_TEMPLATE;
    private boolean addGoogleJsonErrorAsDefaultResponse;
    private boolean addErrorCodesForServiceExceptions;
    private boolean extractCommonParametersAsRefs;
    private boolean combineCommonParametersInSamePath;

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

    public SwaggerContext setTitle(String title) {
      this.title = title;
      return this;
    }

    public SwaggerContext setDescription(String description) {
      this.description = description;
      return this;
    }

    public SwaggerContext setApiName(String apiName) {
      this.apiName = apiName;
      return this;
    }

    public SwaggerContext setTagTemplate(String tagTemplate) {
      this.tagTemplate = tagTemplate;
      return this;
    }

    public SwaggerContext setOperationIdTemplate(String operationIdTemplate) {
      this.operationIdTemplate = operationIdTemplate;
      return this;
    }

    public SwaggerContext setAddGoogleJsonErrorAsDefaultResponse(boolean addGoogleJsonErrorAsDefaultResponse) {
      this.addGoogleJsonErrorAsDefaultResponse = addGoogleJsonErrorAsDefaultResponse;
      return this;
    }

    public SwaggerContext setAddErrorCodesForServiceExceptions(boolean addErrorCodesForServiceExceptions) {
      this.addErrorCodesForServiceExceptions = addErrorCodesForServiceExceptions;
      return this;
    }

    public SwaggerContext setExtractCommonParametersAsRefs(boolean extractCommonParametersAsRefs) {
      this.extractCommonParametersAsRefs = extractCommonParametersAsRefs;
      return this;
    }

    public SwaggerContext setCombineCommonParametersInSamePath(boolean combineCommonParametersInSamePath) {
      this.combineCommonParametersInSamePath = combineCommonParametersInSamePath;
      return this;
    }
  }

  private static class GenerationContext {
    private final Map<String, ApiLimitMetricConfig> limitMetrics = new TreeMap<>();
    private ApiConfigValidator validator;
    private SchemaRepository schemata;
  }

  /**
   * A template mechanism based on Apache Commons lang's StrSubstitutor (placeholder syntax is "${var}).
   * 
   * The following variables are available on API and API method contexts:
   * - apiName
   * - apiVersion
   * - resource (might be null for API or method context)
   * - method (is null when working in a method context)
   * 
   * Each variable comes with following variants:
   * - Uppercased variants (if "${apiName}" is "myApi", "${ApiName}" will be "MyAPi"
   * - Prefixed with "-",":" or "." (only chars that are safe for use in Swagger tags for Endpoints Portal)
   * - Prefixed variants should be used for nullable vars: "${.resource}" will be empty if the resource var is null, but will be ".myResource" if resource is "myResource"
   * - Prefixed variants also come in uppercased flavors ("${.Resource}" will be ".MyResource" if resource var is "myResource")
   */
  private static class NamingContext {

    private static final Converter<String, String> UPPER
        = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);
    private final Map<String, String> values = new HashMap<>();
    private final String prefixes;

    private static NamingContext build(ApiConfig apiConfig, ApiMethodConfig methodConfig) {
      String resource = apiConfig.getApiClassConfig().getResource();
      String method = methodConfig != null ? methodConfig.getEndpointMethodName() : null;
      return new NamingContext("-:.")
          .put("apiName", apiConfig.getName())
          .put("apiVersion", apiConfig.getVersion())
          .put("resource", resource)
          .put("method", method);
    }

    NamingContext(String prefixes) {
      this.prefixes = prefixes;
    }

    NamingContext put(String key, String value) {
      value = com.google.common.base.Strings.nullToEmpty(value);
      values.put(key, value);
      values.put(UPPER.convert(key), UPPER.convert(value));
      for (char c : prefixes.toCharArray()) {
        values.put(c + key, value.isEmpty() ? "" : c + value);
        values.put(c + UPPER.convert(key), value.isEmpty() ? "" : c + UPPER.convert(value));
      }
      return this;
    }

    String resolve(String template) {
      return new StrSubstitutor(values).replace(template);
    }

  }
  
}
