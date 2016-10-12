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
 * Annotation for configuration specific to an API class.
 *
 * @author Eric Orth
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiClass {
  /**
   * The name (in plural) of the resource collection. It is used as a part of the RESTful path and
   * RPC method name. If it is left empty, a value is deduced from inspecting each endpoint method.
   */
  // TODO: Make this less REST-specific.
  String resource() default "";

  /**
   * Set frontend auth level, applicable to all methods of the API unless overridden by
   * {@code @ApiMethod#authLevel}.
   */
  AuthLevel authLevel() default AuthLevel.UNSPECIFIED;

  /**
   * OAuth 2 scopes, one of which is required for calling this method, applicable to all methods of
   * the API class unless overridden by {@code @ApiMethod#scopes}.
   */
  String[] scopes() default {Api.UNSPECIFIED_STRING_FOR_LIST};

  /**
   * Audience for IdTokens, applicable to all methods of the API class unless overridden by
   * {@code @ApiMethod#audiences}.
   */
  String[] audiences() default {Api.UNSPECIFIED_STRING_FOR_LIST};

  /**
   * Audiences for individual issuers, applicable to all methods of the API unless overridden by
   * {@link ApiMethod#issuerAudiences}. This is only meant to be used with EspAuthenticator in
   * endpoints-framework-auth.
   */
  ApiIssuerAudience[] issuerAudiences() default {
      @ApiIssuerAudience(
          name = Api.UNSPECIFIED_STRING_FOR_LIST,
          audiences = {Api.UNSPECIFIED_STRING_FOR_LIST}
      )
  };

  /**
   * Client IDs allowed to call this method, applicable to all methods of the API class unless
   * overridden by {@code @ApiMethod#clientIds}.
   */
  String[] clientIds() default {Api.UNSPECIFIED_STRING_FOR_LIST};

  /**
   * Custom authenticators, applicable to all methods of the API class unless
   * overridden by {@code @ApiMethod#authenticators}.
   */
  Class<? extends Authenticator>[] authenticators() default {Authenticator.class};

  /**
   * Custom peer authenticators, applicable to all methods of the API class unless overridden by
   * {@code @ApiMethod#peerAuthenticators}.
   */
  Class<? extends PeerAuthenticator>[] peerAuthenticators() default {PeerAuthenticator.class};

  /**
   * {@code AnnotationBoolean.TRUE} to request that overriding configuration be loaded from the
   * appengine datastore.
   */
  AnnotationBoolean useDatastoreForAdditionalConfig() default AnnotationBoolean.UNSPECIFIED;

  /**
   * Whether or not an API key is required. This is used to output a Swagger specification and has
   * no effect unless used with endpoints-management-control-appengine.
   */
  AnnotationBoolean apiKeyRequired() default AnnotationBoolean.UNSPECIFIED;
}
