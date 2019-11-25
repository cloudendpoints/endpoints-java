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
package com.google.api.server.spi.request;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.server.spi.ConfiguredObjectMapper;
import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.EndpointsContext;
import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.config.annotationreader.AnnotationUtil;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.config.model.StandardParameters;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.appengine.api.datastore.Blob;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.common.reflect.TypeToken;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.stream.Collectors;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Reads parameters from an {@link HttpServletRequest}.
 */
public class ServletRequestParamReader extends AbstractParamReader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final Set<SimpleModule> READER_MODULES;
  private static final String APPENGINE_USER_CLASS_NAME = "com.google.appengine.api.users.User";

  static {
    Set<SimpleModule> modules = new LinkedHashSet<>();
    SimpleModule dateModule =
        new SimpleModule("dateModule", new Version(1, 0, 0, null, null, null));
    dateModule.addDeserializer(Date.class, new DateDeserializer());
    modules.add(dateModule);

    SimpleModule simpleDateModule =
        new SimpleModule("simpleDateModule", new Version(1, 0, 0, null, null, null));
    simpleDateModule.addDeserializer(SimpleDate.class, new SimpleDateDeserializer());
    modules.add(simpleDateModule);

    SimpleModule dateAndTimeModule =
        new SimpleModule("dateAndTimeModule", new Version(1, 0, 0, null, null, null));
    dateAndTimeModule.addDeserializer(DateAndTime.class, new DateAndTimeDeserializer());
    modules.add(dateAndTimeModule);

    try {
      // Attempt to load the Blob class, which may not exist outside of App Engine Standard.
      ServletRequestParamReader.class.getClassLoader()
          .loadClass("com.google.appengine.api.datastore.Blob");
      SimpleModule blobModule =
          new SimpleModule("blobModule", new Version(1, 0, 0, null, null, null));
      blobModule.addDeserializer(Blob.class, new BlobDeserializer());
      modules.add(blobModule);
    } catch (ClassNotFoundException e) {
      // Ignore this error, since we can function without the Blob deserializer.
    }

    READER_MODULES = Collections.unmodifiableSet(modules);
  }

  protected static List<String> getParameterNames(EndpointMethod endpointMethod)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    List<String> parameterNames = endpointMethod.getParameterNames();
    if (parameterNames == null) {
      Method method = endpointMethod.getMethod();
      parameterNames = new ArrayList<>();
      for (int parameter = 0; parameter < method.getParameterTypes().length; parameter++) {
        Annotation annotation = AnnotationUtil.getNamedParameter(method, parameter, Named.class);
        if (annotation == null) {
          parameterNames.add(null);
        } else {
          parameterNames.add((String) annotation.getClass().getMethod("value").invoke(annotation));
        }
      }
      endpointMethod.setParameterNames(parameterNames);
    }
    return parameterNames;
  }

  protected Object[] deserializeParams(JsonNode body, JsonNode parameters) throws IOException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException, ServiceException {
    EndpointMethod method = getMethod();
    Class<?>[] paramClasses = method.getParameterClasses();
    TypeToken<?>[] paramTypes = method.getParameterTypes();
    Object[] params = new Object[paramClasses.length];
    List<String> parameterNames = getParameterNames(method);
    for (int i = 0; i < paramClasses.length; i++) {
      TypeToken<?> type = paramTypes[i];
      Class<?> clazz = paramClasses[i];
      if (User.class.isAssignableFrom(clazz)) {
        // User type parameter requires no Named annotation (ignored if present)
        User user = getUser();
        if (user == null && methodConfig != null
            && methodConfig.getAuthLevel() == AuthLevel.REQUIRED) {
          throw new UnauthorizedException("Valid user credentials are required.");
        }
        if (user == null || clazz.isAssignableFrom(user.getClass())) {
          params[i] = user;
          logger.atFine().log("deserialize: User injected into param[%d]", i);
        } else {
          logger.atWarning().log(
              "deserialize: User object of type %s is not assignable to %s. User will be null.",
              user.getClass().getName(), clazz.getName());
        }
      } else if (APPENGINE_USER_CLASS_NAME.equals(clazz.getName())) {
        // User type parameter requires no Named annotation (ignored if present)
        com.google.appengine.api.users.User appEngineUser = getAppEngineUser();
        if (appEngineUser == null && methodConfig != null
            && methodConfig.getAuthLevel() == AuthLevel.REQUIRED) {
          throw new UnauthorizedException("Valid user credentials are required.");
        }
        params[i] = appEngineUser;
        logger.atFine().log("deserialize: App Engine User injected into param[%d]", i);
      } else if (clazz == HttpServletRequest.class) {
        // HttpServletRequest type parameter requires no Named annotation (ignored if present)
        params[i] = endpointsContext.getRequest();
        logger.atFine().log("deserialize: HttpServletRequest injected into param[%d]", i);
      } else if (clazz == ServletContext.class) {
        // ServletContext type parameter requires no Named annotation (ignored if present)
        params[i] = servletContext;
        logger.atFine().log("deserialize: ServletContext %s injected into param[%d]",
            params[i], i);
      } else {
        String name = parameterNames.get(i);
        if (Strings.isNullOrEmpty(name)) {
          params[i] = (body == null) ? null : objectReader.forType(clazz).readValue(body);
          logger.atFine().log("deserialize: %s %s injected into unnamed param[%d]",
              clazz, params[i], i);
        } else if (StandardParameters.isStandardParamName(name)) {
          params[i] = getStandardParamValue(parameters, name);
        } else {
          JsonNode nodeValue = parameters.get(name);
          if (nodeValue == null) {
            params[i] = null;
          } else {
            // Check for collection type
            if (Collection.class.isAssignableFrom(clazz)
                && type.getType() instanceof ParameterizedType) {
              params[i] =
                  deserializeCollection(clazz, (ParameterizedType) type.getType(), nodeValue);
            } else {
              params[i] = objectReader.forType(clazz).readValue(nodeValue);
            }
          }
          if (params[i] == null && isRequiredParameter(method, i)) {
            throw new BadRequestException("null value for parameter '" + name + "' not allowed");
          }
          logger.atFine().log("deserialize: %s %s injected into param[%d] named {%s}",
              clazz, params[i], i, name);
        }
      }
    }
    return params;
  }

  private boolean isRequiredParameter(EndpointMethod method, int i) {
    return AnnotationUtil.getNullableParameter(method.getMethod(), i, Nullable.class) == null
        || method.getParameterTypes()[i].isPrimitive();
  }

  @VisibleForTesting
  User getUser() throws ServiceException {
    return Auth.from(endpointsContext.getRequest()).authenticate();
  }

  @VisibleForTesting
  com.google.appengine.api.users.User getAppEngineUser() throws ServiceException {
    return Auth.from(endpointsContext.getRequest()).authenticateAppEngineUser();
  }

  private Object getStandardParamValue(JsonNode body, String paramName) {
    if (!StandardParameters.isStandardParamName(paramName)) {
      throw new IllegalArgumentException("paramName");
    } else if (StandardParameters.USER_IP.equals(paramName)) {
      return endpointsContext.getRequest().getRemoteAddr();
    } else if (StandardParameters.PRETTY_PRINT.equals(paramName)) {
      return StandardParameters.shouldPrettyPrint(endpointsContext);
    }
    JsonNode value = body.get(paramName);
    if (value == null && StandardParameters.ALT.equals(paramName)) {
      return "json";
    }
    return value != null ? value.asText() : null;
  }

  private <T> Collection<T> deserializeCollection(Class<?> clazz, ParameterizedType collectionType,
      JsonNode nodeValue) throws IOException {
    @SuppressWarnings("unchecked")
    Class<? extends Collection<T>> collectionClass = (Class<? extends Collection<T>>) clazz;
    @SuppressWarnings("unchecked")
    Class<T> paramClass =
        (Class<T>) EndpointMethod.getClassFromType(collectionType.getActualTypeArguments()[0]);
    @SuppressWarnings("unchecked")
    Class<T[]> arrayClazz = (Class<T[]>) Array.newInstance(paramClass, 0).getClass();
    Collection<T> collection =
        objectReader.forType(collectionClass).readValue(objectReader.createArrayNode());
    if (nodeValue != null) {
      T[] array = objectReader.forType(arrayClazz).readValue(nodeValue);
      collection.addAll(Arrays.asList(array));
    }
    return collection;
  }

  private static class DateDeserializer extends JsonDeserializer<Date> {
    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext context)
        throws IOException {
      String value = jsonParser.readValueAs(String.class);
      try {
        com.google.api.client.util.DateTime date = new com.google.api.client.util.DateTime(value);
        return new Date(date.getValue());
      } catch (NumberFormatException e) {
        throw InvalidFormatException.from(jsonParser, e.getMessage(), value, Date.class);
      }
    }
  }

  private static class DateAndTimeDeserializer extends JsonDeserializer<DateAndTime> {
    @Override
    public DateAndTime deserialize(JsonParser jsonParser, DeserializationContext context)
        throws IOException {
      String value = jsonParser.readValueAs(String.class);
      try {
        return DateAndTime.parseRfc3339String(value);
      } catch (IllegalArgumentException e) {
        throw InvalidFormatException.from(jsonParser, e.getMessage(), value, DateAndTime.class);
      }
    }
  }

  private static class SimpleDateDeserializer extends JsonDeserializer<SimpleDate> {
    Pattern pattern = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");

    @Override
    public SimpleDate deserialize(JsonParser jsonParser, DeserializationContext context)
        throws IOException {
      String value = jsonParser.readValueAs(String.class).trim();
      Matcher matcher = pattern.matcher(value);
      if (matcher.find()) {
        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        try {
          return new SimpleDate(year, month, day);
        } catch (IllegalArgumentException e) {
          throw buildException(jsonParser, value);
        }
      } else {
        throw buildException(jsonParser, value);
      }
    }
    
    private InvalidFormatException buildException(JsonParser jsonParser, String value) {
      return InvalidFormatException.from(jsonParser,
              "String is not an RFC3339 formated date (yyyy-mm-dd)",
              value,
              SimpleDate.class);
    }
  }

  private static class BlobDeserializer extends JsonDeserializer<Blob> {
    @Override
    public Blob deserialize(JsonParser jsonParser, DeserializationContext context)
        throws IOException {
      return new Blob(jsonParser.getBinaryValue());
    }
  }

  protected final EndpointsContext endpointsContext;
  private final ServletContext servletContext;
  protected final ObjectReader objectReader;
  protected final ApiMethodConfig methodConfig;

  public ServletRequestParamReader(
      EndpointMethod method,
      EndpointsContext endpointsContext,
      ServletContext servletContext,
      ApiSerializationConfig serializationConfig,
      ApiMethodConfig methodConfig) {
    super(method);

    this.methodConfig = methodConfig;
    this.endpointsContext = endpointsContext;
    this.servletContext = servletContext;

    LinkedHashSet<SimpleModule> modules = new LinkedHashSet<>();
    modules.addAll(READER_MODULES);
    this.objectReader = ConfiguredObjectMapper
        .builder()
        .apiSerializationConfig(serializationConfig)
        .addRegisteredModules(modules)
        .build()
        .reader()
        .with(Base64Variants.MIME_NO_LINEFEEDS);
  }

  @Override
  public Object[] read() throws ServiceException {
    // Assumes input stream to be encoded in UTF-8
    // TODO: Take charset from content-type as encoding
    try {
      String requestBody = IoUtil.readStream(endpointsContext.getRequest().getInputStream());
      logger.atFine().log("requestBody=%s", requestBody);
      if (requestBody == null || requestBody.trim().isEmpty()) {
        return new Object[0];
      }
      JsonNode node = objectReader.readTree(requestBody);
      if (!node.isObject()) {
        throw new BadRequestException("expected a JSON object body");
      }
      //this convention comes from gapi.client to separate params and body
      JsonNode resource = node.get("resource");
      ((ObjectNode) node).remove("resource");
      return deserializeParams(resource, node);
    } catch (MismatchedInputException e) {
      logger.atInfo().withCause(e).log("Unable to read request parameter(s)");
      throw translateJsonException(e);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
        | IOException e) {
      throw new BadRequestException(e);
    }
  }

  BadRequestException translateJsonException(MismatchedInputException e) {
    String reason = "parseError";
    String message = "Parse error for ";
    List<Reference> path = e.getPath();
    if (path.isEmpty()) {
      //query / path parameter name can't be retrieved from the error
      message += "a parameter";
    } else {
      String fieldPath = path.stream().map(Reference::getFieldName)
          .collect(Collectors.joining("."));
      message += "field '" + fieldPath + "'";
    }
    Class<?> targetType = e.getTargetType();
    message += " of type '" + targetType.getSimpleName() + "'";
    String messagePattern = ": invalid {0} value \"{1}\".{2}";
    if (e instanceof InvalidFormatException) {
      Object value = ((InvalidFormatException) e).getValue();
      if (targetType.isEnum()) {
        message += MessageFormat.format(messagePattern, "enum",
            value,
            " Valid values are " + Arrays.toString(targetType.getEnumConstants())
        );
      } else if (isNumber(targetType)) {
        message += MessageFormat.format(messagePattern, "number", value, "");
      } else if (isBoolean(targetType)) {
        message += MessageFormat.format(messagePattern,"boolean", value, " Valid values are [true, false]");
      } else if (isDate(targetType)) {
        message += MessageFormat.format(messagePattern, "date", value, "");
      }
    }

    return new BadRequestException(message, reason, e);
  }

  private boolean isBoolean(Class<?> clazz) {
    return Boolean.class.equals(clazz) || boolean.class.equals(clazz);
  }

  private boolean isDate(Class<?> clazz) {
    return Date.class.isAssignableFrom(clazz)
        || DateAndTime.class.equals(clazz)
        || SimpleDate.class.equals(clazz);
  }

  private boolean isNumber(Class<?> clazz) {
    return Number.class.isAssignableFrom(clazz)
        || byte.class.equals(clazz)
        || int.class.equals(clazz)
        || long.class.equals(clazz)
        || float.class.equals(clazz)
        || double.class.equals(clazz);
  }
}
