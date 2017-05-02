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
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigSource;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.ApiLimitMetric;
import com.google.api.server.spi.config.ApiMetricCost;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.model.ApiClassConfig;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiIssuerAudienceConfig;
import com.google.api.server.spi.config.model.ApiIssuerConfigs;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
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
          (Annotation) getAnnotationProperty(api, "auth"));
      readApiFrontendLimits(new ApiFrontendLimitsAnnotationConfig(config.getFrontendLimitsConfig()),
          (Annotation) getAnnotationProperty(api, "frontendLimits"));
      readApiCacheControl(new ApiCacheControlAnnotationConfig(config.getCacheControlConfig()),
          (Annotation) getAnnotationProperty(api, "cacheControl"));
      readApiNamespace(new ApiNamespaceAnnotationConfig(config.getNamespaceConfig()),
          (Annotation) getAnnotationProperty(api, "namespace"));
      readSerializers(config.getSerializationConfig(),
          (Class<? extends Transformer<?, ?>>[]) getAnnotationProperty(api, "transformers"));
    }

    if (apiClass != null) {
      readApiClass(new ApiClassAnnotationConfig(config.getApiClassConfig()), apiClass);
    }

    return hasAnnotation;
  }

  private void readApi(ApiAnnotationConfig config, Annotation api)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    config.setIsAbstractIfSpecified((AnnotationBoolean) getAnnotationProperty(api, "isAbstract"));
    config.setRootIfNotEmpty((String) getAnnotationProperty(api, "root"));

    config.setNameIfNotEmpty((String) getAnnotationProperty(api, "name"));
    config.setCanonicalNameIfNotEmpty((String) getAnnotationProperty(api, "canonicalName"));

    config.setVersionIfNotEmpty((String) getAnnotationProperty(api, "version"));
    config.setTitleIfNotEmpty((String) getAnnotationProperty(api, "title"));
    config.setDescriptionIfNotEmpty((String) getAnnotationProperty(api, "description"));
    config.setDocumentationLinkIfNotEmpty((String) getAnnotationProperty(api, "documentationLink"));
    config.setIsDefaultVersionIfSpecified(
        (AnnotationBoolean) getAnnotationProperty(api, "defaultVersion"));
    config.setIsDiscoverableIfSpecified(
        (AnnotationBoolean) getAnnotationProperty(api, "discoverable"));
    config.setUseDatastoreIfSpecified(
        (AnnotationBoolean) getAnnotationProperty(api, "useDatastoreForAdditionalConfig"));

    config.setBackendRootIfNotEmpty((String) getAnnotationProperty(api, "backendRoot"));

    config.setResourceIfNotEmpty((String) getAnnotationProperty(api, "resource"));
    config.setAuthLevelIfSpecified((AuthLevel) getAnnotationProperty(api, "authLevel"));
    config.setScopesIfSpecified((String[]) getAnnotationProperty(api, "scopes"));
    config.setAudiencesIfSpecified((String[]) getAnnotationProperty(api, "audiences"));
    config.setIssuersIfSpecified(getIssuerConfigs(api));
    config.setIssuerAudiencesIfSpecified(getIssuerAudiences(api));
    config.setClientIdsIfSpecified((String[]) getAnnotationProperty(api, "clientIds"));
    config.setAuthenticatorsIfSpecified(
        this.<Class<? extends Authenticator>[]>getAnnotationProperty(api, "authenticators"));
    config.setPeerAuthenticatorsIfSpecified(this
        .<Class<? extends PeerAuthenticator>[]>getAnnotationProperty(api, "peerAuthenticators"));
    config.setApiKeyRequiredIfSpecified(
        (AnnotationBoolean) this.getAnnotationProperty(api, "apiKeyRequired"));
    config.setApiLimitMetrics(
        (ApiLimitMetric[]) this.getAnnotationProperty(api, "limitDefinitions"));
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
        (AnnotationBoolean) getAnnotationProperty(auth, "allowCookieAuth"));
    config.setBlockedRegionsIfNotEmpty((String[]) getAnnotationProperty(auth, "blockedRegions"));
  }

  private void readApiFrontendLimits(ApiFrontendLimitsAnnotationConfig config,
      Annotation frontendLimits) throws NoSuchMethodException, IllegalAccessException,
          InvocationTargetException {
    config.setUnregisteredUserQpsIfSpecified(
        (Integer) getAnnotationProperty(frontendLimits, "unregisteredUserQps"));
    config.setUnregisteredQpsIfSpecified(
        (Integer) getAnnotationProperty(frontendLimits, "unregisteredQps"));
    config.setUnregisteredDailyIfSpecified(
        (Integer) getAnnotationProperty(frontendLimits, "unregisteredDaily"));

    readApiFrontendLimitRules(config,
        (Annotation[]) getAnnotationProperty(frontendLimits, "rules"));
  }

  private void readSerializers(
      ApiSerializationConfig config, Class<? extends Transformer<?, ?>>[] serializers) {
    for (Class<? extends Transformer<?, ?>> serializer : serializers) {
      config.addSerializationConfig(serializer);
    }
  }

  private void readApiCacheControl(ApiCacheControlAnnotationConfig config, Annotation cacheControl)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    config.setTypeIfNotEmpty((String) getAnnotationProperty(cacheControl, "type"));
    config.setMaxAgeIfSpecified((Integer) getAnnotationProperty(cacheControl, "maxAge"));
  }

  protected void readApiNamespace(ApiNamespaceAnnotationConfig config, Annotation namespace)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    config.setOwnerDomainIfNotEmpty((String) getAnnotationProperty(namespace, "ownerDomain"));
    config.setOwnerNameIfNotEmpty((String) getAnnotationProperty(namespace, "ownerName"));
    config.setPackagePathIfNotEmpty((String) getAnnotationProperty(namespace, "packagePath"));
  }

  private void readApiFrontendLimitRules(ApiFrontendLimitsAnnotationConfig config,
      Annotation[] rules) throws NoSuchMethodException, IllegalAccessException,
          InvocationTargetException {
    for (Annotation rule : rules) {
      String match = getAnnotationProperty(rule, "match");
      int qps = (Integer) getAnnotationProperty(rule, "qps");
      int userQps = (Integer) getAnnotationProperty(rule, "userQps");
      int daily = (Integer) getAnnotationProperty(rule, "daily");
      String analyticsId = getAnnotationProperty(rule, "analyticsId");
      config.getConfig().addRule(match, qps, userQps, daily, analyticsId);
    }
  }

  private void readApiClass(ApiClassAnnotationConfig config, Annotation apiClass)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    config.setResourceIfNotEmpty((String) getAnnotationProperty(apiClass, "resource"));
    config.setAuthLevelIfSpecified((AuthLevel) getAnnotationProperty(apiClass, "authLevel"));
    config.setScopesIfSpecified((String[]) getAnnotationProperty(apiClass, "scopes"));
    config.setAudiencesIfSpecified((String[]) getAnnotationProperty(apiClass, "audiences"));
    config.setIssuerAudiencesIfSpecified(getIssuerAudiences(apiClass));
    config.setClientIdsIfSpecified((String[]) getAnnotationProperty(apiClass, "clientIds"));
    config.setAuthenticatorsIfSpecified(
        this.<Class<? extends Authenticator>[]>getAnnotationProperty(apiClass, "authenticators"));
    config.setPeerAuthenticatorsIfSpecified(this.<
        Class<? extends PeerAuthenticator>[]>getAnnotationProperty(apiClass, "peerAuthenticators"));
    config.setUseDatastoreIfSpecified(
        (AnnotationBoolean) getAnnotationProperty(apiClass, "useDatastoreForAdditionalConfig"));
    config.setApiKeyRequiredIfSpecified(
        (AnnotationBoolean) this.getAnnotationProperty(apiClass, "apiKeyRequired"));
  }

  private void readEndpointMethods(Class<?> endpointClass,
      ApiClassConfig.MethodConfigMap methodConfigMap)
      throws IllegalArgumentException, SecurityException, IllegalAccessException,
          InvocationTargetException, NoSuchMethodException {
    MethodHierarchyReader methodReader = new MethodHierarchyReader(endpointClass);
    Iterable<List<EndpointMethod>> methods = methodReader.getEndpointOverrides();

    for (List<EndpointMethod> overrides : methods) {
      readEndpointMethod(methodConfigMap, overrides);
    }
  }

  private void readEndpointMethod(ApiClassConfig.MethodConfigMap methodConfigMap,
      List<EndpointMethod> overrides)
      throws IllegalArgumentException, SecurityException, IllegalAccessException,
          InvocationTargetException, NoSuchMethodException {
    Class<? extends Annotation> apiMethodClass = annotationTypes.get("ApiMethod");

    final EndpointMethod finalMethod = overrides.get(0);
    ApiMethodConfig methodConfig = methodConfigMap.getOrCreate(finalMethod);

    readMethodRequestParameters(finalMethod, methodConfig);

    // Process overrides in reverse order.
    for (EndpointMethod method : Lists.reverse(overrides)) {
      Annotation apiMethod = method.getMethod().getAnnotation(apiMethodClass);
      if (apiMethod != null) {
        readApiMethodInstance(new ApiMethodAnnotationConfig(methodConfig), apiMethod);
      }
    }
  }

  private void readApiMethodInstance(ApiMethodAnnotationConfig config, Annotation apiMethod)
      throws IllegalArgumentException, SecurityException, IllegalAccessException,
          InvocationTargetException, NoSuchMethodException {
    config.setNameIfNotEmpty((String) getAnnotationProperty(apiMethod, "name"));
    config.setDescriptionIfNotEmpty((String) getAnnotationProperty(apiMethod, "description"));
    config.setPathIfNotEmpty((String) getAnnotationProperty(apiMethod, "path"));
    config.setHttpMethodIfNotEmpty((String) getAnnotationProperty(apiMethod, "httpMethod"));
    config.setAuthLevelIfSpecified((AuthLevel) getAnnotationProperty(apiMethod, "authLevel"));
    config.setScopesIfSpecified((String[]) getAnnotationProperty(apiMethod, "scopes"));
    config.setAudiencesIfSpecified((String[]) getAnnotationProperty(apiMethod, "audiences"));
    config.setIssuerAudiencesIfSpecified(getIssuerAudiences(apiMethod));
    config.setClientIdsIfSpecified((String[]) getAnnotationProperty(apiMethod, "clientIds"));
    config.setAuthenticatorsIfSpecified(
        this.<Class<? extends Authenticator>[]>getAnnotationProperty(apiMethod, "authenticators"));
    config.setPeerAuthenticatorsIfSpecified(this.<
        Class<? extends PeerAuthenticator>[]>getAnnotationProperty(apiMethod,
        "peerAuthenticators"));
    config.setIgnoredIfSpecified((AnnotationBoolean) getAnnotationProperty(apiMethod, "ignored"));
    config.setApiKeyRequiredIfSpecified(
        (AnnotationBoolean) this.getAnnotationProperty(apiMethod, "apiKeyRequired"));
    config.setMetricCosts(
        (ApiMetricCost[]) getAnnotationProperty(apiMethod, "metricCosts"));
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
