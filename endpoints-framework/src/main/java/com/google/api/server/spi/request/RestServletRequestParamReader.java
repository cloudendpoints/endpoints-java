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

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.EndpointsContext;
import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.Strings;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.flogger.FluentLogger;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * A {@link ParamReader} which reads parameters from a JSON-REST request. That is, instead of just
 * looking in the JSON body, it also looks at path and query parameters. This is mainly for use
 * with {@link com.google.api.server.spi.EndpointsServlet}, and tries to emulate existing behavior
 * by stuffing path and query parameters into the main request body.
 */
public class RestServletRequestParamReader extends ServletRequestParamReader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final Splitter COMPOSITE_PATH_SPLITTER = Splitter.on(',');

  private final Map<String, String> rawPathParameters;
  private final Map<String, ApiParameterConfig> parameterConfigMap;

  public RestServletRequestParamReader(EndpointMethod method,
      EndpointsContext endpointsContext, ServletContext servletContext,
      ApiSerializationConfig serializationConfig, ApiMethodConfig methodConfig) {
    super(method, endpointsContext, servletContext, serializationConfig, methodConfig);
    this.rawPathParameters = endpointsContext.getRawPathParameters();
    ImmutableMap.Builder<String, ApiParameterConfig> builder = ImmutableMap.builder();
    for (ApiParameterConfig config : methodConfig.getParameterConfigs()) {
      if (config.getName() != null) {
        builder.put(config.getName(), config);
      }
    }
    this.parameterConfigMap = builder.build();
  }

  @Override
  public Object[] read() throws ServiceException {
    // Assumes input stream to be encoded in UTF-8
    // TODO: Take charset from content-type as encoding
    try {
      EndpointMethod method = getMethod();
      if (method.getParameterClasses().length == 0) {
        return new Object[0];
      }
      HttpServletRequest servletRequest = endpointsContext.getRequest();
      JsonNode node;
      // multipart/form-data requests can be used for requests which have no resource body. In
      // this case, each part represents a named parameter instead.
      if (ServletFileUpload.isMultipartContent(servletRequest)) {
        try {
          ServletFileUpload upload = new ServletFileUpload();
          FileItemIterator iter = upload.getItemIterator(servletRequest);
          ObjectNode obj = (ObjectNode) objectReader.createObjectNode();
          while (iter.hasNext()) {
            FileItemStream item = iter.next();
            if (item.isFormField()) {
              obj.put(item.getFieldName(), IoUtil.readStream(item.openStream()));
            } else {
              throw new BadRequestException("unable to parse multipart form field");
            }
          }
          node = obj;
        } catch (FileUploadException e) {
          throw new BadRequestException("unable to parse multipart request", e);
        }
      } else {
        String requestBody = IoUtil.readRequestBody(servletRequest);
        logger.atFine().log("requestBody=%s", requestBody);
        // Unlike the Lily protocol, which essentially always requires a JSON body to exist (due to
        // path and query parameters being injected into the body), bodies are optional here, so we
        // create an empty body and inject named parameters to make deserialize work.
        node = Strings.isEmptyOrWhitespace(requestBody) ? objectReader.createObjectNode()
            : objectReader.readTree(requestBody);
      }
      if (!node.isObject()) {
        throw new BadRequestException("expected a JSON object body");
      }
      ObjectNode body = (ObjectNode) node;
      Map<String, Class<?>> parameterMap = getParameterMap(method);
      // First add query parameters, then add path parameters. If the parameters already exist in
      // the resource, then the they aren't added to the body object. For compatibility reasons,
      // the order of precedence is resource field > query parameter > path parameter.
      for (Enumeration<?> e = servletRequest.getParameterNames(); e.hasMoreElements(); ) {
        String parameterName = (String) e.nextElement();
        if (!body.has(parameterName)) {
          Class<?> parameterClass = parameterMap.get(parameterName);
          ApiParameterConfig parameterConfig = parameterConfigMap.get(parameterName);
          if (parameterClass != null && parameterConfig.isRepeated()) {
            ArrayNode values = body.putArray(parameterName);
            for (String value : servletRequest.getParameterValues(parameterName)) {
              values.add(value);
            }
          } else {
            body.put(parameterName, servletRequest.getParameterValues(parameterName)[0]);
          }
        }
      }
      for (Entry<String, String> entry : rawPathParameters.entrySet()) {
        String parameterName = entry.getKey();
        Class<?> parameterClass = parameterMap.get(parameterName);
        if (parameterClass != null && !body.has(parameterName)) {
          if (parameterConfigMap.get(parameterName).isRepeated()) {
            ArrayNode values = body.putArray(parameterName);
            for (String value : COMPOSITE_PATH_SPLITTER.split(entry.getValue())) {
              values.add(value);
            }
          } else {
            body.put(parameterName, entry.getValue());
          }
        }
      }
      for (Entry<String, ApiParameterConfig> entry : parameterConfigMap.entrySet()) {
        if (!body.has(entry.getKey()) && entry.getValue().getDefaultValue() != null) {
          body.put(entry.getKey(), entry.getValue().getDefaultValue());
        }
      }
      return deserializeParams(body);
    } catch (InvalidFormatException e) {
      logger.atInfo().withCause(e).log("Unable to read request parameter(s)");
      throw translate(e);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
        | IOException e) {
      logger.atInfo().withCause(e).log("Unable to read request parameter(s)");
      throw new BadRequestException("Parse error", "parseError", e);
    }
  }
  
  private BadRequestException translate(InvalidFormatException e) {
    String messagePattern = "Invalid {0} value \"{1}\".{2}";
    String message;
    String reason = "parseError";
    if (e.getTargetType().isEnum()) {
      message = MessageFormat.format(messagePattern, "enum",
              e.getValue(),
              " Valid values are " + Arrays.toString(e.getTargetType().getEnumConstants())
      );
    } else if (isNumber(e.getTargetType())) {
      message = MessageFormat.format(messagePattern, "number", e.getValue(), "");
    } else if (isBoolean(e.getTargetType())) {
      message = MessageFormat.format(messagePattern,"boolean", e.getValue(), " Valid values are [true, false]");
    } else if (isDate(e.getTargetType())) {
      message = MessageFormat.format(messagePattern, "date", e.getValue(), "");
    } else {
      message = "Parse error";
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

  private static ImmutableMap<String, Class<?>> getParameterMap(EndpointMethod method)
      throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    ImmutableMap.Builder<String, Class<?>> builder = ImmutableMap.builder();
    List<String> parameterNames = getParameterNames(method);
    for (int i = 0; i < parameterNames.size(); i++) {
      String parameterName = parameterNames.get(i);
      if (parameterName != null) {
        builder.put(parameterName, method.getParameterClasses()[i]);
      }
    }
    return builder.build();
  }
}
