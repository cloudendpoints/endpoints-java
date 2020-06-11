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
package com.google.api.server.spi;

import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.ApiConfigWriter;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.jsonwriter.JsonConfigWriter;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig.SerializerConfig;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.config.validation.ApiConfigValidator;
import com.google.api.server.spi.discovery.CachingDiscoveryProvider;
import com.google.api.server.spi.discovery.DiscoveryGenerator;
import com.google.api.server.spi.discovery.LocalDiscoveryProvider;
import com.google.api.server.spi.discovery.ProxyingDiscoveryService;
import com.google.api.server.spi.request.ParamReader;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.ResultWriter;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import javax.annotation.Nullable;

/**
 * System service that execute service methods.
 */
public class SystemService {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String OAUTH_EXCEPTION_CLASS =
      "com.google.appengine.api.oauth.OAuthRequestException";

  public static final String MIME_JSON = "application/json; charset=UTF-8";

  public static final int DUPLICATE_SERVICE_REGISTER_COUNT = -1;

  private static final Predicate<ApiConfig> NON_INTERNAL_PREDICATE =
      new Predicate<ApiConfig>() {
        @Override
        public boolean apply(ApiConfig config) {
          return !ApiConfigLoader.INTERNAL_API_NAME.equals(config.getName());
        }
      };

  private static final Function<EndpointNode, ApiConfig> ENDPOINT_NODE_TO_API_CONFIG =
      new Function<EndpointNode, ApiConfig>() {
        @Override
        public ApiConfig apply(EndpointNode node) {
          return node.config;
        }
      };

  /**
   * Mapping from service class name to Objects. This object will map both the simple and full
   * name of classes to an instance of it, therefore there could be more than one object mapped
   * to a simple name (i.e. package.v1.Hello and package.v2.Hello both have simple name "Hello"
   */
  private final Map<String, List<Object>> servicesByName;
  private final ConcurrentMap<Object, EndpointNode> endpoints;
  private final Map<String, String> serviceApiVersions;
  private final Map<String, ApiSerializationConfig> serializationConfigs;
  private final Multimap<String, ApiConfig> initialConfigsByApi;

  private final ApiConfigLoader configLoader;
  private final ServiceContext serviceContext;
  private final ApiConfigWriter configWriter;
  private final boolean isIllegalArgumentBackendError;

  public static class EndpointNode {
    private final Object endpoint;
    private final ApiConfig config;
    private final Map<String, EndpointMethod> methods;

    EndpointNode(Object endpoint, ApiConfig config) {
      this.endpoint = endpoint;
      this.config = config;
      this.methods = new HashMap<String, EndpointMethod>();
    }

    public Object getEndpoint() {
      return endpoint;
    }

    public ApiConfig getConfig() {
      return config;
    }

    public boolean isExternalEndpoint() {
      return !ApiConfigLoader.INTERNAL_API_NAME.equals(config.getName());
    }

    public Map<String, EndpointMethod> getMethods() {
      return methods;
    }
  }

  /**
   * Constructs a {@link SystemService} and registers the provided services.
   *
   * @param configLoader The loader used to read annotation from service classes
   * @param appName The application's id
   * @param services The service classes to be registered
   */
  public SystemService(ApiConfigLoader configLoader, String appName, ApiConfigWriter configWriter,
      Object[] services, boolean isIllegalArgumentBackendError)
      throws ApiConfigException {
    this(configLoader, appName, configWriter, isIllegalArgumentBackendError);
    for (Object service : services) {
      registerService(service);
    }
  }

  /**
   * Constructs a {@link SystemService} with no registered services.
   *
   * @param configLoader The loader used to read annotation from service classes
   * @param appName The application's id
   */
  public SystemService(ApiConfigLoader configLoader, String appName, ApiConfigWriter configWriter,
      boolean isIllegalArgumentBackendError)
      throws ApiConfigException {
    this.servicesByName = new HashMap<String, List<Object>>();
    this.endpoints = new ConcurrentHashMap<Object, EndpointNode>();
    this.serviceApiVersions = new HashMap<String, String>();
    this.serializationConfigs = new HashMap<String, ApiSerializationConfig>();
    this.initialConfigsByApi = ArrayListMultimap.create();
    this.configLoader = configLoader;
    this.serviceContext = ServiceContext.create(appName, ServiceContext.DEFAULT_API_NAME);
    this.configWriter = configWriter;
    this.isIllegalArgumentBackendError = isIllegalArgumentBackendError;
  }

  /**
   * Registers a service class.  Only public methods in this class and all its superclasses, except
   * Object, are registered.  Two methods are not allowed to have the same name.  Registering a
   * different service with an existing name is a no-op.
   *
   * @param serviceClass is the class to start parsing endpoints
   * @param service Service object
   * @return number of service methods added, -1 on duplicate insertion
   * @throws ApiConfigException
   */
  public int registerService(Class<?> serviceClass, Object service) throws ApiConfigException {
    Preconditions.checkArgument(serviceClass.isInstance(service),
        "service is not an instance of " + serviceClass.getName());
    ApiConfig apiConfig = configLoader.loadConfiguration(serviceContext, serviceClass);
    return registerLoadedService(serviceClass, service, apiConfig);
  }

  public int registerService(Object service) throws ApiConfigException {
    Class<?> serviceClass = getServiceClass(service);
    return registerService(serviceClass, service);
  }

  private int registerLoadedService(Class<?> serviceClass, Object service, ApiConfig apiConfig)
      throws ApiConfigException {
    String fullName = serviceClass.getName();
    if (!servicesByName.containsKey(fullName)) {
      // TODO: The bit below uses two maps to store per-API serialization configurations.
      // The first map maps service names to an api-version key, and the second map maps the key to
      // a serialization config. This is currently required because the API information is not kept
      // outside of this method, but it would be nice to find a better way to clean this up.
      String api = apiConfig.getName() + "-" + apiConfig.getVersion();
      initialConfigsByApi.put(api, apiConfig);

      ApiSerializationConfig serializationConfig = serializationConfigs.get(api);
      if (serializationConfig == null) {
        serializationConfig = new ApiSerializationConfig();
      }
      for (SerializerConfig rule : apiConfig.getSerializationConfig().getSerializerConfigs()) {
        serializationConfig.addSerializationConfig(rule.getSerializer());
      }
      serializationConfigs.put(api, serializationConfig);

      registerServiceFromName(service, serviceClass.getSimpleName(), api);
      registerServiceFromName(service, fullName, api);

      updateEndpointConfig(service, apiConfig, null);
      return apiConfig.getApiClassConfig().getMethods().size();
    }
    return DUPLICATE_SERVICE_REGISTER_COUNT;
  }

  public <T> EndpointNode updateEndpointConfig(T endpoint, ApiConfig newConfig,
      @Nullable EndpointNode oldNode) {
    EndpointNode newNode = new EndpointNode(endpoint, newConfig);
    for (EndpointMethod method : newConfig.getApiClassConfig().getMethods().keySet()) {
      newNode.methods.put(method.getMethod().getName(), method);
    }

    if (oldNode == null) {
      endpoints.putIfAbsent(endpoint, newNode);
    } else {
      endpoints.replace(endpoint, oldNode, newNode);
    }

    return newNode;
  }

  /**
   * Registers a service class.  Only public methods in this class and all its superclasses, except
   * Object, are registered.  Two methods are not allowed to have the same name.  Registering a
   * different service with an existing name is a no-op.
   *
   * @param service Service object
   * @param name Name at which the service is to be registered
   */
  void registerServiceFromName(final Object service, String name, String api) {
    List<Object> services = servicesByName.get(name);
    // If it doesn't exist, create
    if (services == null) {
      services = new ArrayList<Object>() {{ add(service); }};
      servicesByName.put(name, services);
    } else {
      // TODO: Throw an exception if a collision exists, when we no longer need to
      // support simple names.
      services.add(service);
    }
    serviceApiVersions.put(name, api);
  }

  /**
   * Finds a service object with the {@code serviceName} or {@code null} if not found.
   *
   * @param serviceName either a full class name or a simple name of a service object
   * @param methodName the method name of a service object
   * @throws ServiceException when more than one service is mapped to the same {@code name} or
   *         when the named service does not exist
   */
  public EndpointMethod resolveService(String serviceName, String methodName)
      throws ServiceException {
    return getEndpointNode(serviceName).methods.get(methodName);
  }

  private ApiMethodConfig getMethodConfigFromNode(EndpointNode node, String methodName) {
    return node.config.getApiClassConfig().getMethods().get(node.methods.get(methodName));
  }

  /**
   * Gets the serialization configuration for the API corresponding to the named service.
   */
  public ApiSerializationConfig getSerializationConfig(String serviceName) {
    return serializationConfigs.get(serviceApiVersions.get(serviceName));
  }

  private EndpointNode getEndpointNode(String serviceName) throws ServiceException {
    Object service = findService(serviceName);
    EndpointNode node = endpoints.get(service);
    if (node == null) {
      throw new ServiceException(404, "service '" + serviceName + "' not found");
    } else {
      return node;
    }
  }

  /**
   * Finds a service object with the {@code name}
   *
   * @throws ServiceException when more than one service is mapped to the same {@code name} or
   *         when the named service does not exist
   */
  public Object findService(String name) throws ServiceException {
    List<Object> services = this.servicesByName.get(name);
    if (services == null || services.isEmpty()) {
      throw new ServiceException(404, "service '" + name + "' not found");
    } else if (services.size() > 1) {
      // Build exception for the ambiguous case.
      Class<?> clazz = getServiceClass(services.get(0));
      Preconditions.checkState(name.equals(clazz.getSimpleName()),
          "Only requested simple class names should result in a collision.");
      StringBuilder builder = new StringBuilder(
          "Two or more Endpoint classes are mapped to the same service name (").append(name)
          .append("):");
      for (Object service : services) {
        builder.append(' ').append(getServiceClass(service).getName());
      }
      throw new ServiceException(500, builder.toString());
    } else {
      Object service = services.get(0);
      logger.atFine().log("%s => %s", name, services.get(0));
      return service;
    }
  }

  /**
   * Finds a method object with the given {@code methodName} on the {@code service} object.
   *
   * @throws ServiceException if method does not exist
   */
  public Method findServiceMethod(Object service, String methodName) throws ServiceException {
    EndpointNode endpointNode = service == null ? null : endpoints.get(service);
    if (endpointNode != null) {
      EndpointMethod method = endpointNode.methods.get(methodName);
      if (method != null) {
        logger.atFine().log("serviceMethod=%s", method.getMethod());
        return method.getMethod();
      }
    }
    throw new ServiceException(404, "method '" + service + "." + methodName + "' not found");
  }

  /**
   * Invokes a {@code method} on a {@code service} given a {@code paramReader} to read parameters
   * and a {@code resultWriter} to write result.
   */
  public void invokeServiceMethod(Object service, Method method, int status, ParamReader paramReader,
      ResultWriter resultWriter) throws IOException {

    try {
      Object[] params = paramReader.read();
      logger.atFine().log("params=%s (String)", Arrays.toString(params));
      Object response = method.invoke(service, params);
      resultWriter.write(response, status);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      logger.atSevere().withCause(e).log("exception occurred while calling backend method");
      resultWriter.writeError(new BadRequestException(e));
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      Level level = Level.INFO;
      if (cause instanceof ServiceException) {
        resultWriter.writeError((ServiceException) cause);
      } else if (cause instanceof IllegalArgumentException) {
        resultWriter.writeError(
            isIllegalArgumentBackendError
                ? new InternalServerErrorException(cause) : new BadRequestException(cause));
      } else if (isOAuthRequestException(cause.getClass())) {
        resultWriter.writeError(new UnauthorizedException(cause));
      } else if (cause.getCause() != null && cause.getCause() instanceof ServiceException) {
        ServiceException serviceException = (ServiceException) cause.getCause();
        level = serviceException.getLogLevel();
        resultWriter.writeError(serviceException);
      } else {
        level = Level.SEVERE;
        resultWriter.writeError(new InternalServerErrorException(cause));
      }
      logger.at(level).withCause(cause).log("exception occurred while calling backend method");
    } catch (ServiceException e) {
      logger.at(e.getLogLevel()).withCause(e)
          .log("exception occurred while calling backend method");
      resultWriter.writeError(e);
    }
  }

  /**
   * Generates wire-format configuration for all loaded APIs.
   * @return A map from {@link ApiKey}s to wire-formatted configuration strings.
   */
  public Map<ApiKey, String> getApiConfigs() throws ApiConfigException {
    return configWriter.writeConfig(
        FluentIterable.from(endpoints.values())
            .transform(ENDPOINT_NODE_TO_API_CONFIG)
            .filter(NON_INTERNAL_PREDICATE));
  }

  public ImmutableList<EndpointNode> getEndpoints() {
    return ImmutableList.copyOf(endpoints.values());
  }

  private void validateRegisteredServices(ApiConfigValidator validator) throws ApiConfigException {
    for (String api : initialConfigsByApi.keySet()) {
      validator.validate(initialConfigsByApi.get(api));
    }
  }

  private static boolean isOAuthRequestException(Class<?> clazz) {
    while (Object.class != clazz) {
      if (OAUTH_EXCEPTION_CLASS.equals(clazz.getName())) {
        return true;
      }
      clazz = clazz.getSuperclass();
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<? super T> getServiceClass(T service) {
    Class<?> clazz = service.getClass();
    Enhancers[] enhancers = Enhancers.values();
    for (int i = 0; i < enhancers.length; ++i) {
      if (enhancers[i].matches(clazz)) {
        clazz = clazz.getSuperclass();
        i = 0;
      }
    }
    return (Class<? super T>) clazz;
  }

  private enum Enhancers {
    GUICE("$$EnhancerByGuice$$"),
    NONE(null);

    private final String enhancerSubstring;

    Enhancers(String enhancerSubstring) {
      this.enhancerSubstring = enhancerSubstring;
    }

    public boolean matches(Class<?> clazz) {
      return enhancerSubstring != null && clazz.getSimpleName().contains(enhancerSubstring);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * A {@link SystemService} builder which encapsulates common logic for building its dependencies.
   */
  public static class Builder {
    private ApiConfigLoader configLoader;
    private TypeLoader typeLoader;
    private ApiConfigValidator configValidator;
    private String appName;
    private ApiConfigWriter configWriter;
    private boolean isIllegalArgumentBackendError;
    private boolean enableDiscoveryService;
    private Map<Class<?>, Object> services = Maps.newLinkedHashMap();
    private SchemaRepository schemaRepository;

    public Builder withDefaults(ClassLoader classLoader) throws ClassNotFoundException {
      setStandardConfigLoader(classLoader);
      setAppName(new BackendProperties().getApplicationId());
      typeLoader = new TypeLoader(classLoader);
      isIllegalArgumentBackendError = false;
      enableDiscoveryService = false;
      setConfigWriter(new JsonConfigWriter(typeLoader, configValidator));
      schemaRepository = new SchemaRepository(typeLoader);
      setConfigValidator(new ApiConfigValidator(typeLoader, schemaRepository));
      return this;
    }

    public Builder setStandardConfigLoader(ClassLoader classLoader)
        throws ClassNotFoundException {
      TypeLoader typeLoader = new TypeLoader(classLoader);
      ApiConfigAnnotationReader annotationReader =
          new ApiConfigAnnotationReader(typeLoader.getAnnotationTypes());

      this.configLoader =
          new ApiConfigLoader(new ApiConfig.Factory(), typeLoader, annotationReader);
      return this;
    }

    public Builder setConfigValidator(ApiConfigValidator configValidator) {
      this.configValidator = configValidator;
      return this;
    }

    public Builder setAppName(String appName) {
      this.appName = appName;
      return this;
    }

    public Builder setConfigWriter(ApiConfigWriter configWriter) {
      this.configWriter = configWriter;
      return this;
    }

    public Builder setIllegalArgumentIsBackendError(boolean isIllegalArgumentBackendError) {
      this.isIllegalArgumentBackendError = isIllegalArgumentBackendError;
      return this;
    }

    public Builder setDiscoveryServiceEnabled(boolean enableDiscoveryService) {
      this.enableDiscoveryService = enableDiscoveryService;
      return this;
    }

    public Builder addService(Class<?> serviceClass, Object service) {
      this.services.put(serviceClass, service);
      return this;
    }

    public SystemService build() throws ApiConfigException {
      Preconditions.checkNotNull(configLoader, "configLoader");
      Preconditions.checkNotNull(configValidator, "configValidator");
      Preconditions.checkNotNull(configWriter, "configWriter");
      SystemService systemService = new SystemService(configLoader, appName, configWriter,
          isIllegalArgumentBackendError);
      for (Entry<Class<?>, Object> entry : services.entrySet()) {
        systemService.registerService(entry.getKey(), entry.getValue());
      }
      // Discovery must come last so it can initialize correctly.
      if (enableDiscoveryService) {
        ProxyingDiscoveryService discoveryService = new ProxyingDiscoveryService();
        systemService.registerService(discoveryService);
        discoveryService.initialize(
            new CachingDiscoveryProvider(new LocalDiscoveryProvider(
                getApiConfigs(systemService), new DiscoveryGenerator(typeLoader),
                schemaRepository)));
      }
      systemService.validateRegisteredServices(configValidator);
      return systemService;
    }

    private ImmutableList<ApiConfig> getApiConfigs(SystemService systemService) {
      ApiConfig.Factory factory = new ApiConfig.Factory();
      ImmutableList.Builder<ApiConfig> builder =
          ImmutableList.builder();
      for (EndpointNode node : systemService.getEndpoints()) {
        if (node.isExternalEndpoint()) {
          builder.add(factory.copy(node.getConfig()));
        }
      }
      return builder.build();
    }
  }
}

