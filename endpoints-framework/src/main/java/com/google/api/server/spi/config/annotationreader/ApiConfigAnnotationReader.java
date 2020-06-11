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
package com.google.api.server.spi.config.annotationreader;

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.MethodHierarchyReader;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigSource;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.model.ApiClassConfig;
import com.google.api.server.spi.config.model.ApiClassConfig.MethodConfigMap;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiIssuerAudienceConfig;
import com.google.api.server.spi.config.model.ApiIssuerConfigs;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Reads annotations on endpoint classes to produce an api configuration.
 *
 * @author Eric Orth
 */
public class ApiConfigAnnotationReader implements ApiConfigSource {
  private final Map<String, Class<? extends Annotation>> annotationTypes;

  public ApiConfigAnnotationReader() throws ClassNotFoundException {
    this((new TypeLoader(ApiConfigAnnotationReader.class.getClassLoader())).getAnnotationTypes());
  }

  public ApiConfigAnnotationReader(Map<String, Class<? extends Annotation>> annotationTypes) {
    this.annotationTypes = annotationTypes;
  }

  @Override
  public void loadEndpointClass(ServiceContext serviceContext, Class<?> endpointClass,
      ApiConfig config) throws ApiConfigException {
    try {
      Annotation api = getDeclaredAnnotation(endpointClass, annotationTypes.get("Api"));
      Annotation apiClass = getDeclaredAnnotation(endpointClass, annotationTypes.get("ApiClass"));
      if (!readEndpointClass(config, endpointClass, api, apiClass, endpointClass)) {
        throw new ApiConfigException(endpointClass + " has no @Api annotation.");
      }
    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
      throw new ApiConfigException(e);
    }
  }

  @Override
  public void loadEndpointMethods(ServiceContext serviceContext, Class<?> endpointClass,
      ApiClassConfig.MethodConfigMap methodConfigMap) throws ApiConfigException {
    try {
      readEndpointMethods(endpointClass, methodConfigMap);
    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
      throw new ApiConfigException(e);
    }
  }

  @Override
  public boolean isStaticConfig(ApiConfig config) {
    return true;
  }

  @Nullable private Class<?> determineInheritanceSource(Class<?> endpointClass)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Class<?> inheritanceSource = null;

    // First attempt to follow any @ApiReference annotation.
    Annotation reference =
        getDeclaredAnnotation(endpointClass, annotationTypes.get("ApiReference"));
    if (reference != null) {
      inheritanceSource = getAnnotationProperty(reference, "value");
    }

    // Second try inheriting from a superclass.
    if (inheritanceSource == null) {
      inheritanceSource = endpointClass.getSuperclass();
    }

    return inheritanceSource;
  }

  /**
   * Detect inheritance cycles using Floyd's cycle-finding algorithm
   * (See http://en.wikipedia.org/wiki/Cycle_detection#Tortoise_and_hare).
   *
   * @param endpointClass The class currently being parsed.  Used as the tortoise pointer for the
   *     cycle detection.
   * @param cycleCheck The hare pointer.  Either the last pointer returned by this function or
   *     {@code null} if an end has already been reached.
   * @return The new {@code cycleCheck} hare pointer or {@code null} if an end has already been
   *     reached.
   */
  @Nullable private Class<?> checkForInheritanceCycle(Class<?> endpointClass,
      @Nullable Class<?> cycleCheck) throws IllegalAccessException, InvocationTargetException,
          NoSuchMethodException, CyclicApiInheritanceException {
    for (int i = 0; cycleCheck != null && i < 2; ++i) {
      cycleCheck = determineInheritanceSource(cycleCheck);
    }

    if (endpointClass.equals(cycleCheck)) {
      throw new CyclicApiInheritanceException(endpointClass);
    }

    return cycleCheck;
  }

  @SuppressWarnings("unchecked")
  private boolean readEndpointClass(ApiConfig config, Class<?> endpointClass, Annotation api,
      Annotation apiClass, @Nullable Class<?> cycleCheck) throws NoSuchMethodException,
          IllegalAccessException, InvocationTargetException, CyclicApiInheritanceException {
    cycleCheck = checkForInheritanceCycle(endpointClass, cycleCheck);

    boolean hasAnnotation = api != null;
    Class<?> inheritanceSource = determineInheritanceSource(endpointClass);

    if (inheritanceSource != null) {
      Annotation superApi = getDeclaredAnnotation(inheritanceSource, annotationTypes.get("Api"));
      Annotation superApiClass =
          getDeclaredAnnotation(inheritanceSource, annotationTypes.get("ApiClass"));

      hasAnnotation |=
          readEndpointClass(config, inheritanceSource, superApi, superApiClass, cycleCheck);
    }

    if (api != null) {
      readApi(new ApiAnnotationConfig(config), api);
      readApiAuth(new ApiAuthAnnotationConfig(config.getAuthConfig()),
          getAnnotationProperty(api, "auth"));
      readApiFrontendLimits(new ApiFrontendLimitsAnnotationConfig(config.getFrontendLimitsConfig()),
          getAnnotationProperty(api, "frontendLimits"));
      readApiCacheControl(new ApiCacheControlAnnotationConfig(config.getCacheControlConfig()),
          getAnnotationProperty(api, "cacheControl"));
      readApiNamespace(new ApiNamespaceAnnotationConfig(config.getNamespaceConfig()),
          getAnnotationProperty(api, "namespace"));
      readSerializers(config.getSerializationConfig(),
          getAnnotationProperty(api, "transformers"));
    }

    if (apiClass != null) {
      readApiClass(new ApiClassAnnotationConfig(config.getApiClassConfig()), apiClass);
    }

    return hasAnnotation;
  }

  private void readApi(ApiAnnotationConfig config, Annotation api)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    config.setIsAbstractIfSpecified(getAnnotationProperty(api, "isAbstract"));

    config.setNameIfNotEmpty(getAnnotationProperty(api, "name"));
    config.setCanonicalNameIfNotEmpty(getAnnotationProperty(api, "canonicalName"));

    config.setVersionIfNotEmpty(getAnnotationProperty(api, "version"));
    config.setTitleIfNotEmpty(getAnnotationProperty(api, "title"));
    config.setDescriptionIfNotEmpty(getAnnotationProperty(api, "description"));
    config.setDocumentationLinkIfNotEmpty(getAnnotationProperty(api, "documentationLink"));
    config.setIsDefaultVersionIfSpecified(
        getAnnotationProperty(api, "defaultVersion"));
    config.setIsDiscoverableIfSpecified(
        getAnnotationProperty(api, "discoverable"));
    config.setUseDatastoreIfSpecified(
        getAnnotationProperty(api, "useDatastoreForAdditionalConfig"));

    config.setResourceIfNotEmpty(getAnnotationProperty(api, "resource"));
    config.setAuthLevelIfSpecified(getAnnotationProperty(api, "authLevel"));
    config.setScopesIfSpecified(getAnnotationProperty(api, "scopes"));
    config.setAudiencesIfSpecified(getAnnotationProperty(api, "audiences"));
    config.setIssuersIfSpecified(getIssuerConfigs(api));
    config.setIssuerAudiencesIfSpecified(getIssuerAudiences(api));
    config.setClientIdsIfSpecified(getAnnotationProperty(api, "clientIds"));
    config.setAuthenticatorsIfSpecified(
        this.<Class<? extends Authenticator>[]>getAnnotationProperty(api, "authenticators"));
    config.setApiKeyRequiredIfSpecified(
        this.getAnnotationProperty(api, "apiKeyRequired"));
    config.setApiLimitMetrics(
        this.getAnnotationProperty(api, "limitDefinitions"));
  }

  private ApiIssuerConfigs getIssuerConfigs(Annotation annotation)
      throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    return IssuerUtil.toConfig((ApiIssuer[]) getAnnotationProperty(annotation, "issuers"));
  }

  private ApiIssuerAudienceConfig getIssuerAudiences(Annotation annotation)
      throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    return IssuerUtil.toConfig(
        (ApiIssuerAudience[]) getAnnotationProperty(annotation, "issuerAudiences"));
  }

  private <T> T getAnnotationProperty(Annotation annotation, String name)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    @SuppressWarnings("unchecked")
    T value = (T) annotation.annotationType().getMethod(name).invoke(annotation);
    return value;
  }

  /**
   * Converts the auth config from the auth annotation. Subclasses may override
   * to add additional information to the auth config.
   */
  protected void readApiAuth(ApiAuthAnnotationConfig config, Annotation auth)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    config.setAllowCookieAuthIfSpecified(
        getAnnotationProperty(auth, "allowCookieAuth"));
    config.setBlockedRegionsIfNotEmpty(getAnnotationProperty(auth, "blockedRegions"));
  }

  private void readApiFrontendLimits(ApiFrontendLimitsAnnotationConfig config,
      Annotation frontendLimits) throws NoSuchMethodException, IllegalAccessException,
          InvocationTargetException {
    config.setUnregisteredUserQpsIfSpecified(
        getAnnotationProperty(frontendLimits, "unregisteredUserQps"));
    config.setUnregisteredQpsIfSpecified(
        getAnnotationProperty(frontendLimits, "unregisteredQps"));
    config.setUnregisteredDailyIfSpecified(
        getAnnotationProperty(frontendLimits, "unregisteredDaily"));

    readApiFrontendLimitRules(config,
        getAnnotationProperty(frontendLimits, "rules"));
  }

  private void readSerializers(
      ApiSerializationConfig config, Class<? extends Transformer<?, ?>>[] serializers) {
    for (Class<? extends Transformer<?, ?>> serializer : serializers) {
      config.addSerializationConfig(serializer);
    }
  }

  private void readApiCacheControl(ApiCacheControlAnnotationConfig config, Annotation cacheControl)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    config.setTypeIfNotEmpty(getAnnotationProperty(cacheControl, "type"));
    config.setMaxAgeIfSpecified(getAnnotationProperty(cacheControl, "maxAge"));
  }

  protected void readApiNamespace(ApiNamespaceAnnotationConfig config, Annotation namespace)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    config.setOwnerDomainIfNotEmpty(getAnnotationProperty(namespace, "ownerDomain"));
    config.setOwnerNameIfNotEmpty(getAnnotationProperty(namespace, "ownerName"));
    config.setPackagePathIfNotEmpty(getAnnotationProperty(namespace, "packagePath"));
  }

  private void readApiFrontendLimitRules(ApiFrontendLimitsAnnotationConfig config,
      Annotation[] rules) throws NoSuchMethodException, IllegalAccessException,
          InvocationTargetException {
    for (Annotation rule : rules) {
      String match = getAnnotationProperty(rule, "match");
      int qps = getAnnotationProperty(rule, "qps");
      int userQps = getAnnotationProperty(rule, "userQps");
      int daily = getAnnotationProperty(rule, "daily");
      String analyticsId = getAnnotationProperty(rule, "analyticsId");
      config.getConfig().addRule(match, qps, userQps, daily, analyticsId);
    }
  }

  private void readApiClass(ApiClassAnnotationConfig config, Annotation apiClass)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    config.setResourceIfNotEmpty(getAnnotationProperty(apiClass, "resource"));
    config.setAuthLevelIfSpecified(getAnnotationProperty(apiClass, "authLevel"));
    config.setScopesIfSpecified(getAnnotationProperty(apiClass, "scopes"));
    config.setAudiencesIfSpecified(getAnnotationProperty(apiClass, "audiences"));
    config.setIssuerAudiencesIfSpecified(getIssuerAudiences(apiClass));
    config.setClientIdsIfSpecified(getAnnotationProperty(apiClass, "clientIds"));
    config.setAuthenticatorsIfSpecified(
        this.<Class<? extends Authenticator>[]>getAnnotationProperty(apiClass, "authenticators"));
    config.setUseDatastoreIfSpecified(
        getAnnotationProperty(apiClass, "useDatastoreForAdditionalConfig"));
    config.setApiKeyRequiredIfSpecified(
        this.getAnnotationProperty(apiClass, "apiKeyRequired"));
  }

  private void readEndpointMethods(Class<?> endpointClass,
      ApiClassConfig.MethodConfigMap methodConfigMap)
      throws IllegalArgumentException, SecurityException, IllegalAccessException,
          InvocationTargetException, NoSuchMethodException {
    MethodHierarchyReader methodReader = new MethodHierarchyReader(endpointClass);
    Iterable<Collection<EndpointMethod>> methods = methodReader.getEndpointOverrides();

    for (Collection<EndpointMethod> overrides : methods) {
      readEndpointMethod(methodConfigMap, overrides,
          endpointClass.getAnnotation(Deprecated.class) != null);
    }
  }

  private void readEndpointMethod(MethodConfigMap methodConfigMap,
      Collection<EndpointMethod> overrides, boolean deprecated)
      throws IllegalArgumentException, SecurityException, IllegalAccessException,
          InvocationTargetException, NoSuchMethodException {
    Class<? extends Annotation> apiMethodClass = annotationTypes.get("ApiMethod");

    final EndpointMethod finalMethod = overrides.iterator().next();
    ApiMethodConfig methodConfig = methodConfigMap.getOrCreate(finalMethod);

    readMethodRequestParameters(finalMethod, methodConfig);

    // Process overrides in reverse order.
    for (EndpointMethod method : Lists.reverse(ImmutableList.copyOf(overrides))) {
      ApiMethodAnnotationConfig config = new ApiMethodAnnotationConfig(methodConfig);
      Annotation apiMethod = method.getMethod().getAnnotation(apiMethodClass);
      if (apiMethod != null) {
        readApiMethodInstance(config, apiMethod);
      }
      methodConfig.setDeprecated(deprecated 
          || method.getMethod().getAnnotation(Deprecated.class) != null);
    }
  }

  private void readApiMethodInstance(ApiMethodAnnotationConfig config, Annotation apiMethod)
      throws IllegalArgumentException, SecurityException, IllegalAccessException,
          InvocationTargetException, NoSuchMethodException {
    config.setNameIfNotEmpty(getAnnotationProperty(apiMethod, "name"));
    config.setDescriptionIfNotEmpty(getAnnotationProperty(apiMethod, "description"));
    config.setPathIfNotEmpty(getAnnotationProperty(apiMethod, "path"));
    config.setHttpMethodIfNotEmpty(getAnnotationProperty(apiMethod, "httpMethod"));
    config.setResponseStatus(getAnnotationProperty(apiMethod, "responseStatus"));
    config.setAuthLevelIfSpecified(getAnnotationProperty(apiMethod, "authLevel"));
    config.setScopesIfSpecified(getAnnotationProperty(apiMethod, "scopes"));
    config.setAudiencesIfSpecified(getAnnotationProperty(apiMethod, "audiences"));
    config.setIssuerAudiencesIfSpecified(getIssuerAudiences(apiMethod));
    config.setClientIdsIfSpecified(getAnnotationProperty(apiMethod, "clientIds"));
    config.setAuthenticatorsIfSpecified(
        this.<Class<? extends Authenticator>[]>getAnnotationProperty(apiMethod, "authenticators"));
    config.setIgnoredIfSpecified(getAnnotationProperty(apiMethod, "ignored"));
    config.setApiKeyRequiredIfSpecified(
        this.getAnnotationProperty(apiMethod, "apiKeyRequired"));
    config.setMetricCosts(
        getAnnotationProperty(apiMethod, "metricCosts"));
  }

  private void readMethodRequestParameters(EndpointMethod endpointMethod,
      ApiMethodConfig methodConfig) throws IllegalArgumentException, SecurityException,
          IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = endpointMethod.getMethod();
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    TypeToken<?>[] parameterTypes = endpointMethod.getParameterTypes();

    if (parameterAnnotations.length != parameterTypes.length) {
      throw new IllegalArgumentException();
    }

    for (int i = 0; i < parameterAnnotations.length; i++) {
      Annotation parameterName =
          AnnotationUtil.getNamedParameter(method, i, annotationTypes.get("Named"));
      Annotation description =
          AnnotationUtil.getParameterAnnotation(method, i, annotationTypes.get("Description"));
      Annotation nullable =
          AnnotationUtil.getNullableParameter(method, i, annotationTypes.get("Nullable"));
      Annotation defaultValue =
          AnnotationUtil.getParameterAnnotation(method, i, annotationTypes.get("DefaultValue"));
      readMethodRequestParameter(methodConfig, parameterName, description, nullable, defaultValue,
          parameterTypes[i]);
    }
  }

  private void readMethodRequestParameter(ApiMethodConfig methodConfig, Annotation parameterName,
      Annotation description, Annotation nullable, Annotation defaultValue, TypeToken<?> type)
      throws IllegalArgumentException, SecurityException, IllegalAccessException, 
      InvocationTargetException, NoSuchMethodException {
    String parameterNameString = null;
    if (parameterName != null) {
      parameterNameString = getAnnotationProperty(parameterName, "value");
    }
    String descriptionString = null;
    if (description != null) {
      descriptionString = getAnnotationProperty(description, "value");
    }
    String defaultValueString = null;
    if (defaultValue != null) {
      defaultValueString = getAnnotationProperty(defaultValue, "value");
    }

    ApiParameterConfig parameterConfig =
        methodConfig.addParameter(parameterNameString, descriptionString, nullable != null, 
            defaultValueString, type);

    Annotation apiSerializer =
        type.getRawType().getAnnotation(annotationTypes.get("ApiTransformer"));
    if (apiSerializer != null) {
      Class<? extends Transformer<?, ?>> serializer =
          getAnnotationProperty(apiSerializer, "value");
      parameterConfig.setSerializer(serializer);
    }

    if (parameterConfig.isRepeated()) {
      TypeToken<?> repeatedItemType = parameterConfig.getRepeatedItemType();
      apiSerializer =
          repeatedItemType.getRawType().getAnnotation(annotationTypes.get("ApiTransformer"));
      if (apiSerializer != null) {
        Class<? extends Transformer<?, ?>> repeatedItemSerializer =
            getAnnotationProperty(apiSerializer, "value");
        parameterConfig.setRepeatedItemSerializer(repeatedItemSerializer);
      }
    }
  }

  private static <A extends Annotation> A getDeclaredAnnotation(
      Class<?> clazz, Class<A> annotationClass) {

    for (Annotation annotation : clazz.getDeclaredAnnotations()) {
      if (annotation.annotationType().equals(annotationClass)) {
        return annotationClass.cast(annotation);
      }
    }
    return null;
  }
}
