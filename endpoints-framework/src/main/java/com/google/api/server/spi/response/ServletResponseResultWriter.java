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
package com.google.api.server.spi.response;

import com.google.api.server.spi.ConfiguredObjectMapper;
import com.google.api.server.spi.Constant;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.SystemService;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.appengine.api.datastore.Blob;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import com.google.common.io.CountingOutputStream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * Writes a result to a servlet response.
 */
public class ServletResponseResultWriter implements ResultWriter {

  @VisibleForTesting
  protected static final Set<SimpleModule> WRITER_MODULES;

  static {
    Set<SimpleModule> modules = new LinkedHashSet<>();
    modules.add(getWriteLongAsStringModule());
    modules.add(getWriteDateAsStringModule());
    modules.add(getWriteDateAndTimeAsStringModule());
    modules.add(getWriteSimpleDateAsStringModule());
    try {
      // Attempt to load the Blob class, which may not exist outside of App Engine Standard.
      ServletResponseResultWriter.class.getClassLoader()
          .loadClass("com.google.appengine.api.datastore.Blob");
      modules.add(getWriteBlobAsBase64Module());
    } catch (ClassNotFoundException e) {
      // Ignore this error, since we can function without the Blob deserializer.
    }
    WRITER_MODULES = Collections.unmodifiableSet(modules);
  }

  private final HttpServletResponse servletResponse;
  private final ObjectWriter objectWriter;
  private final ObjectWriter errorObjectWriter;
  private final boolean addContentLength;

  public ServletResponseResultWriter(
      HttpServletResponse servletResponse, ApiSerializationConfig serializationConfig,
      boolean prettyPrint, boolean addContentLength) {
    this(servletResponse,  ConfiguredObjectMapper.builder()
            .apiSerializationConfig(serializationConfig)
            .addRegisteredModules(WRITER_MODULES)
            .build().writer(), prettyPrint, addContentLength);
  }

  public ServletResponseResultWriter(
      HttpServletResponse servletResponse, ObjectWriter objectWriter,
      boolean prettyPrint, boolean addContentLength) {
    this.servletResponse = servletResponse;
    if (prettyPrint) {
      objectWriter = objectWriter.with(new EndpointsPrettyPrinter());
    }
    this.objectWriter = configureWriter(objectWriter);
    this.errorObjectWriter = objectWriter;
    this.addContentLength = addContentLength;
  }

  /**
   * Override to add additional behavior, like partial response, etc.
   *
   * @param objectWriter the standard object writer
   * @return a configured writer (might be wrapped)
   */
  protected ObjectWriter configureWriter(ObjectWriter objectWriter) {
    return objectWriter;
  }

  @Override
  public void write(Object response, int status) throws IOException {
    write(status, null, ResponseUtil.wrapCollection(response), false);
  }

  @Override
  public void writeError(ServiceException e) throws IOException {
    Map<String, String> errors = new HashMap<>();
    errors.put(Constant.ERROR_MESSAGE, e.getMessage());
    write(e.getStatusCode(), e.getHeaders(), errors, true);
  }

  protected void write(int status, Map<String, String> headers, Object content, boolean isError) throws IOException {
    
    // write response status code
    servletResponse.setStatus(status);

    // write response headers
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        servletResponse.addHeader(entry.getKey(), entry.getValue());
      }
    }

    // write response body
    ObjectWriter writer = isError ? errorObjectWriter: objectWriter;
    if (content != null) {
      servletResponse.setContentType(SystemService.MIME_JSON);
      if (addContentLength) {
        CountingOutputStream counter = new CountingOutputStream(ByteStreams.nullOutputStream());
        writer.writeValue(counter, content);
        servletResponse.setContentLength((int) counter.getCount());
      }
      writer.writeValue(servletResponse.getOutputStream(), content);
    }
  }

  private static SimpleModule getWriteLongAsStringModule() {
    JsonSerializer<Long> longSerializer = new JsonSerializer<Long>() {
      @Override
      public void serialize(Long value, JsonGenerator jgen, SerializerProvider provider)
          throws IOException {
        jgen.writeString(value.toString());
      }
    };
    SimpleModule writeLongAsStringModule = new SimpleModule("writeLongAsStringModule",
        new Version(1, 0, 0, null, null, null));
    writeLongAsStringModule.addSerializer(Long.TYPE, longSerializer);  // long (primitive)
    writeLongAsStringModule.addSerializer(Long.class, longSerializer); // Long (class)
    return writeLongAsStringModule;
  }

  private static SimpleModule getWriteDateAndTimeAsStringModule() {
    JsonSerializer<DateAndTime> dateAndTimeSerializer = new JsonSerializer<DateAndTime>() {
      @Override
      public void serialize(DateAndTime value, JsonGenerator jgen, SerializerProvider provider)
          throws IOException {
        jgen.writeString(value.toRfc3339String());
      }
    };
    SimpleModule writeDateAsStringModule = new SimpleModule("writeDateAsStringModule",
        new Version(1, 0, 0, null, null, null));
    writeDateAsStringModule.addSerializer(DateAndTime.class, dateAndTimeSerializer);
    return writeDateAsStringModule;
  }

  private static SimpleModule getWriteSimpleDateAsStringModule() {
    JsonSerializer<SimpleDate> simpleDateSerializer = new JsonSerializer<SimpleDate>() {
      @Override
      public void serialize(SimpleDate value, JsonGenerator jgen, SerializerProvider provider)
          throws IOException {
        jgen.writeString(
            String.format("%04d-%02d-%02d", value.getYear(), value.getMonth(), value.getDay()));
      }
    };
    SimpleModule writeSimpleDateAsModule = new SimpleModule("writeSimpleDateAsModule",
        new Version(1, 0, 0, null, null, null));
    writeSimpleDateAsModule.addSerializer(SimpleDate.class, simpleDateSerializer);
    return writeSimpleDateAsModule;
  }


  private static SimpleModule getWriteDateAsStringModule() {
    JsonSerializer<Date> dateSerializer = new JsonSerializer<Date>() {
      @Override
      public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider)
          throws IOException {
        jgen.writeString(new com.google.api.client.util.DateTime(value).toStringRfc3339());
      }
    };
    SimpleModule writeDateAsStringModule = new SimpleModule("writeDateAsStringModule",
        new Version(1, 0, 0, null, null, null));
    writeDateAsStringModule.addSerializer(Date.class, dateSerializer);
    return writeDateAsStringModule;
  }

  private static SimpleModule getWriteBlobAsBase64Module() {
    JsonSerializer<Blob> dateSerializer = new JsonSerializer<Blob>() {
      @Override
      public void serialize(Blob value, JsonGenerator jgen, SerializerProvider provider)
          throws IOException {
        byte[] bytes = value.getBytes();
        jgen.writeBinary(bytes, 0, bytes.length);
      }
    };
    SimpleModule writeBlobAsBase64Module = new SimpleModule("writeBlobAsBase64Module",
        new Version(1, 0, 0, null, null, null));
    writeBlobAsBase64Module.addSerializer(Blob.class, dateSerializer);
    return writeBlobAsBase64Module;
  }

}
