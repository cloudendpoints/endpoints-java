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
package com.google.api.server.spi.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for API method configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiMethod {

  /** Constants of HTTP method names */
  public static class HttpMethod {
    /** An HTTP GET call. Used for retrieving resources. */
    public static final String GET = "GET";

    /** An HTTP POST call. Used for creating resources or custom methods. */
    public static final String POST = "POST";

    /** An HTTP PUT call. Used for updating resources. */
    public static final String PUT = "PUT";

    /** An HTTP DELETE call. Used for deleting resources. */
    public static final String DELETE = "DELETE";
  }

  /**
   * The name for this method in the .api file. This will automatically be
   * prefixed with {@code "<apiname>."} to create a unique name for the method.
   * If not set the method name will be derived from the Java method name.
   */
  String name() default "";

  /**
   * Description of this API method. This will be exposed in the discovery service to
   * describe your API method, and may also be used to generate documentation.
   */
  String description() default "";
  
  /**
   * The URI path to use to access this method. If not set a default will be
   * created based on the Java method name.
   */
  String path() default "";

  /**
   * The HTTP method to use to access this method. If not set a default will be
   * chosen based on the name of the method.
   */
  String httpMethod() default "";

  /**
   * The response status on success. If not set, the value is 200 or 204 if there is no content returned.
   */
  int responseStatus() default -1;

  /**
   * Set frontend auth level.
   */
  AuthLevel authLevel() default AuthLevel.UNSPECIFIED;

  /**
   * OAuth 2 scopes, one of which is required for calling this method.
   */
  String[] scopes() default {Api.UNSPECIFIED_STRING_FOR_LIST};

  /**
   * Audience for IdTokens.
   */
  String[] audiences() default {Api.UNSPECIFIED_STRING_FOR_LIST};

  /**
   * Audiences for individual issuers. This is only meant to be used with EspAuthenticator in
   * endpoints-framework-auth.
   */
  ApiIssuerAudience[] issuerAudiences() default {
      @ApiIssuerAudience(
          name = Api.UNSPECIFIED_STRING_FOR_LIST,
          audiences = {Api.UNSPECIFIED_STRING_FOR_LIST}
      )
  };

  /**
   * Client IDs allowed to call this method.
   */
  String[] clientIds() default {Api.UNSPECIFIED_STRING_FOR_LIST};

  /**
   * Custom authenticators used for authentication for this method.
   */
  Class<? extends Authenticator>[] authenticators() default {Authenticator.class};

  /**
   * Whether or not API method should be ignored.
   */
  AnnotationBoolean ignored() default AnnotationBoolean.UNSPECIFIED;

  /**
   * Whether or not an API key is required. This is used to output a Swagger specification and has
   * no effect unless used with endpoints-management-control-appengine.
   */
  AnnotationBoolean apiKeyRequired() default AnnotationBoolean.UNSPECIFIED;

  /**
   * A list of metric costs associated with this method.
   */
  ApiMetricCost[] metricCosts() default {};
}
