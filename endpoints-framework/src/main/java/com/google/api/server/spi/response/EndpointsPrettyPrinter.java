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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;

/**
 * A {@link com.fasterxml.jackson.core.PrettyPrinter} that mimics legacy formatting.
 */
public class EndpointsPrettyPrinter extends DefaultPrettyPrinter {
  public EndpointsPrettyPrinter() {
    this(1);
  }

  public EndpointsPrettyPrinter(int spacesPerIndent) {
    SpaceIndenter indenter = new SpaceIndenter(spacesPerIndent);
    indentArraysWith(indenter);
    indentObjectsWith(indenter);
  }

  @Override
  public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException {
    jg.writeRaw(": ");
  }

  @Override
  public EndpointsPrettyPrinter createInstance() {
    return this;
  }

  private static class SpaceIndenter implements Indenter {
    private final String indent;

    SpaceIndenter(int spacesPerIndent) {
      StringBuilder builder = new StringBuilder(spacesPerIndent);
      for (int i = 0; i < spacesPerIndent; i++) {
        builder.append(' ');
      }
      indent = builder.toString();
    }

    @Override
    public void writeIndentation(JsonGenerator jg, int level) throws IOException {
      jg.writeRaw('\n');
      for (int i = 0; i < level; i++) {
        jg.writeRaw(indent);
      }
    }

    @Override
    public boolean isInline() {
      return false;
    }
  }
}
