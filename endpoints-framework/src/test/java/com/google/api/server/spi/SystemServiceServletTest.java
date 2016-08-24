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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.request.Attribute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for {@link SystemServiceServlet}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SystemServiceServletTest {
  @Mock protected ServletConfig servletConfig;
  @Mock protected ServletContext servletContext;
  @Mock protected ApiMethodConfig methodConfig;

  private SystemServiceServlet servlet;
  private MockHttpServletRequest request;
  private Attribute attr;

  protected final ObjectMapper mapper = new ObjectMapper();

  private class ExceptionSystemServiceServlet extends SystemServiceServlet {
    private final ServiceException exception;

    public ExceptionSystemServiceServlet(ServiceException exception) {
      this.exception = exception;
    }

    @Override
    protected void execute(HttpServletRequest request, HttpServletResponse response,
        String serviceName, String methodName) throws ServiceException {
      throw exception;
    }
  }

  @Before
  public void setUp() throws Exception {
    servlet = new SystemServiceServlet();
    request = new MockHttpServletRequest();
    attr = Attribute.from(request);
  }

  @Test
  public void testExceptionHandling() throws Exception {
    int status = 123;
    ServiceException exception = new ServiceException(status, "error");
    request.setPathInfo("/Service.method");

    SystemServiceServlet servlet = new ExceptionSystemServiceServlet(exception);
    MockHttpServletResponse fakeResponse = new MockHttpServletResponse();
    try {
      servlet.doPost(request, fakeResponse);
    } catch (IOException e) {
      fail("should have turned all exceptions into a JSON error response");
    }
    assertEquals(SystemService.MIME_JSON, fakeResponse.getContentType());
    assertEquals(status, fakeResponse.getStatus());
    JsonNode node = mapper.readTree(fakeResponse.getContentAsString());
    assertNotNull(node);
    JsonNode errorNode = node.get("error_message");
    assertNotNull(errorNode);
    assertEquals("error", errorNode.asText());
  }
}
