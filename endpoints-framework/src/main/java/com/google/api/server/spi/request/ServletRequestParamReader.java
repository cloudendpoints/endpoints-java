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

import com.google.api.server.spi.ConfiguredObjectMapper;
import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.annotationreader.AnnotationUtil;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.config.model.StandardParameters;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.appengine.api.datastore.Blob;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Reads parameters from an {@link HttpServletRequest}.
 */
public class ServletRequestParamReader extends AbstractParamReader {

  private static final Logger logger = Logger.getLogger(ServletRequestParamReader.class.getName());

  private static final Set<SimpleModule> READER_MODULES;

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

    SimpleModule blobModule =
        new SimpleModule("blobModule", new Version(1, 0, 0, null, null, null));
    blobModule.addDeserializer(Blob.class, new BlobDeserializer());
    modules.add(blobModule);

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

  protected Object[] deserializeParams(JsonNode node) throws IOException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException {
    EndpointMethod method = getMethod();
    Class<?>[] paramClasses = method.getParameterClasses();
    Type[] paramTypes = method.getParameterTypes();
    Object[] params = new Object[paramClasses.length];
    List<String> parameterNames = getParameterNames(method);
    for (int i = 0; i < paramClasses.length; i++) {
      Type type = paramTypes[i];
      Class<?> clazz = paramClasses[i];
      if (clazz == User.class) {
        // User type parameter requires no Named annotation (ignored if present)
        params[i] = getUser();
        logger.log(Level.FINE, "deserialize: User injected into param[{0}]", i);
      } else if (clazz == com.google.appengine.api.users.User.class) {
        // User type parameter requires no Named annotation (ignored if present)
        params[i] = getAppEngineUser();
        logger.log(Level.FINE, "deserialize: App Engine User injected into param[{0}]", i);
      } else if (clazz == HttpServletRequest.class) {
        // HttpServletRequest type parameter requires no Named annotation (ignored if present)
        params[i] = servletRequest;
        logger.log(Level.FINE, "deserialize: HttpServletRequest injected into param[{0}]", i);
      } else if (clazz == ServletContext.class) {
        // ServletContext type parameter requires no Named annotation (ignored if present)
        params[i] = servletContext;
        logger.log(Level.FINE, "deserialize: ServletContext {0} injected into param[{1}]",
            new Object[] {params[i], i});
      } else {
        String name = parameterNames.get(i);
        if (Strings.isNullOrEmpty(name)) {
          params[i] = (node == null) ? null : objectReader.forType(clazz).readValue(node);
          logger.log(Level.FINE, "deserialize: {0} {1} injected into unnamed param[{2}]",
              new Object[]{clazz, params[i], i});
        } else if (StandardParameters.isStandardParamName(name)) {
          params[i] = getStandardParamValue(node, name);
        } else {
          JsonNode nodeValue = node.get(name);
          if (nodeValue == null) {
            params[i] = null;
          } else {
            // Check for collection type
            if (Collection.class.isAssignableFrom(clazz) && type instanceof ParameterizedType) {
              params[i] = deserializeCollection(clazz, (ParameterizedType) type, nodeValue);
            } else {
              params[i] = objectReader.forType(clazz).readValue(nodeValue);
            }
          }
          logger.log(Level.FINE, "deserialize: {0} {1} injected into param[{2}] named {3}",
              new Object[] {clazz, params[i], i, name});
        }
      }
    }
    return params;
  }

  @VisibleForTesting
  User getUser() {
    return Auth.from(servletRequest).authenticate();
  }

  @VisibleForTesting
  com.google.appengine.api.users.User getAppEngineUser() {
    return Auth.from(servletRequest).authenticateAppEngineUser();
  }

  private Object getStandardParamValue(JsonNode body, String paramName) {
    if (!StandardParameters.isStandardParamName(paramName)) {
      throw new IllegalArgumentException("paramName");
    } else if (StandardParameters.USER_IP.equals(paramName)) {
      return servletRequest.getRemoteAddr();
    } else if (StandardParameters.PRETTY_PRINT.equals(paramName)) {
      return StandardParameters.shouldPrettyPrint(servletRequest);
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
      com.google.api.client.util.DateTime date =
          new com.google.api.client.util.DateTime(jsonParser.readValueAs(String.class));
      return new Date(date.getValue());
    }
  }

  private static class DateAndTimeDeserializer extends JsonDeserializer<DateAndTime> {
    @Override
    public DateAndTime deserialize(JsonParser jsonParser, DeserializationContext context)
        throws IOException {
      return DateAndTime.parseRfc3339String(jsonParser.readValueAs(String.class));
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
        return new SimpleDate(year, month, day);
      } else {
        throw new IllegalArgumentException(
            "String is not an RFC3339 formated date (yyyy-mm-dd): " + value);
      }
    }
  }

  private static class BlobDeserializer extends JsonDeserializer<Blob> {
    @Override
    public Blob deserialize(JsonParser jsonParser, DeserializationContext context)
        throws IOException {
      return new Blob(jsonParser.getBinaryValue());
    }
  }

  protected final HttpServletRequest servletRequest;
  private final ServletContext servletContext;
  protected final ObjectReader objectReader;

  public ServletRequestParamReader(EndpointMethod method, HttpServletRequest servletRequest,
      ServletContext servletContext, ApiSerializationConfig serializationConfig) {
    super(method);

    this.servletRequest = servletRequest;
    this.servletContext = servletContext;

    LinkedHashSet<SimpleModule> modules = new LinkedHashSet<>();
    modules.addAll(READER_MODULES);
    this.objectReader = ConfiguredObjectMapper
        .builder()
        .apiSerializationConfig(serializationConfig)
        .addRegisteredModules(modules)
        .build()
        .reader();
  }

  @Override
  public Object[] read() throws BadRequestException {
    // Assumes input stream to be encoded in UTF-8
    // TODO: Take charset from content-type as encoding
    try {
      String requestBody = IoUtil.readStream(servletRequest.getInputStream());
      logger.log(Level.FINE, "requestBody=" + requestBody);
      if (requestBody == null || requestBody.trim().isEmpty()) {
        return new Object[0];
      }
      JsonNode node = objectReader.readTree(requestBody);
      return deserializeParams(node);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
        | IOException e) {
      throw new BadRequestException(e);
    }
  }
}
