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

import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.config.model.ApiSerializationConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests of {@link CollectionResponse}.
 */
@RunWith(JUnit4.class)
public class CollectionResponseTest {

  private static class Bean {
    @SuppressWarnings("unused")
    public int getDummy() {
      return 0;
    }
  }

  @Test
  public void testCollectionResponse() throws IOException {
    MockHttpServletResponse servletResponse = new MockHttpServletResponse();
    ServletResponseResultWriter writer = new ServletResponseResultWriter(
        servletResponse, (ApiSerializationConfig) null, false, false);
    writer.write(getBeans(2), HttpServletResponse.SC_OK);

    ObjectNode json = new ObjectMapper().readValue(
        servletResponse.getContentAsString(), ObjectNode.class);
    assertThat(json.path("items")).hasSize(2);
    assertThat(json.path("nextPageToken").asText()).isEqualTo("next");
  }

  private CollectionResponse<Bean> getBeans(int beanCount) {
    List<Bean> beans = new ArrayList<Bean>();
    for (int i = 0; i < beanCount; i++) {
      beans.add(new Bean());
    }
    return CollectionResponse.<Bean>builder()
        .setItems(beans)
        .setNextPageToken("next")
        .build();
  }
}
