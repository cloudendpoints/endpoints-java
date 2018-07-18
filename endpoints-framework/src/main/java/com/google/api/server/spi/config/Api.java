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
 * Annotation for API-wide configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Api {
  public static final int UNSPECIFIED_INT = Integer.MIN_VALUE;
  public static final String UNSPECIFIED_STRING_FOR_LIST = "_UNSPECIFIED_LIST_STRING_VALUE";

  /**
   * Frontend root URL, e.g. "https://example.appspot.com/_ah/api". All api
   * methods will be exposed below this path. This will default to
   * "https://yourapp.appspot.com/_ah/api".
   */
  @Deprecated
  String root() default "";

  /**
   * Name of the API, e.g. "guestbook". This is used as the prefix for all api
   * methods and paths. If not set a default of "myapi" will be used.
   */
  String name() default "";

  /**
   * Optional name, which indicates how an API name should be split into parts. This is useful in
   * generating better client library names. For example, the API name might be "myapi" and the
   * canonical name might be "My API", which indicates that the camel case name should be myApi
   * while the underscore name should be my_api.
   */
  String canonicalName() default "";

  /**
   * Version of the API, e.g. "v0.2". If not set a default of "v1" will be used.
   */
  String version() default "";

  /**
   * The title of an API. This is the title displayed by API Explorer.
   */
  String title() default "";

  /**
   * Description of this API. This will be exposed in the discovery service to
   * describe your API, and may also be used to generate documentation.
   */
  String description() default "";

  /**
   * A link to human readable documentation for the API. By default no link is is exposed in
   * discovery.
   */
  String documentationLink() default "";

  /**
   * Backend root URL, e.g. "https://example.appspot.com/_ah/spi". This is the root of all backend
   * method calls. This will default to "https://yourapp.appspot.com/_ah/spi". Non-secure http URLs
   * will be automatically converted to use https.
   */
  @Deprecated
  String backendRoot() default "";

  /**
   * Configures authentication information. See {@link ApiAuth} for details.
   */
  ApiAuth auth() default @ApiAuth;

  /**
   * Configures quota enforcement. See {@link ApiFrontendLimits} for details.
   */
  ApiFrontendLimits frontendLimits() default @ApiFrontendLimits;

  /**
   * Configures the Cache-Control header in the response. See
   * {@link ApiCacheControl} for details.
   */
  ApiCacheControl cacheControl() default @ApiCacheControl;

  /**
   * Set frontend auth level, applicable to all methods of the API unless overridden by
   * {@code @ApiClass#authLevel} or {@code @ApiMethod#authLevel}.
   */
  AuthLevel authLevel() default AuthLevel.UNSPECIFIED;

  /**
   * OAuth 2 scopes, one of which is required for calling this method, applicable to all methods
   * of the API unless overridden by {@code @ApiClass#scopes} or {@code @ApiMethod#scopes}.
   */
  String[] scopes() default {UNSPECIFIED_STRING_FOR_LIST};

  /**
   * Audience for IdTokens, applicable to all methods of the API unless overridden by
   * {@code @ApiClass#audiences} or {@code @ApiMethod#audiences}.
   */
  String[] audiences() default {UNSPECIFIED_STRING_FOR_LIST};

  /**
   * Custom JWT issuer configurations. This is only meant to be used with EspAuthenticator in
   * endpoints-framework-auth.
   */
  ApiIssuer[] issuers() default {@ApiIssuer(name = UNSPECIFIED_STRING_FOR_LIST)};

  /**
   * Audiences for individual issuers, applicable to all methods of the API unless overridden by
   * {@link ApiClass#issuerAudiences} or {@link ApiMethod#issuerAudiences}. This is only meant to
   * be used with EspAuthenticator in endpoints-framework-auth.
   */
  ApiIssuerAudience[] issuerAudiences() default {
      @ApiIssuerAudience(
          name = UNSPECIFIED_STRING_FOR_LIST,
          audiences = {UNSPECIFIED_STRING_FOR_LIST}
      )
  };

  /**
   * Client IDs allowed to call this method, applicable to all methods of the API unless overridden
   * by {@code @ApiClass#clientIds} or {@code @ApiMethod#clientIds}.
   */
  String[] clientIds() default {UNSPECIFIED_STRING_FOR_LIST};

  /**
   * Custom authenticators. Applies to all methods of the API unless overridden by
   * {@code @ApiClass#authenticators} or {@code @ApiMethod#authenticators}. See
   * {@link Authenticator}.
   */
  Class<? extends Authenticator>[] authenticators() default {Authenticator.class};

  /**
   * Custom peer authenticators. Applies to all methods of the API unless overridden by
   * {@code @ApiClass#peerAuthenticators} or {@code @ApiMethod#peerAuthenticators}. See
   * {@link PeerAuthenticator}.
   */
  Class<? extends PeerAuthenticator>[] peerAuthenticators() default {PeerAuthenticator.class};

  /**
   * {@code true} if this API configuration is used as the base for another. Should be {@code false}
   * for most situations.
   */
  AnnotationBoolean isAbstract() default AnnotationBoolean.UNSPECIFIED;

  /**
   * Marks this version of an API as the default version to use when a JSON-RPC call is executed
   * against the API.
   */
  AnnotationBoolean defaultVersion() default AnnotationBoolean.UNSPECIFIED;

  /**
   * The name (in plural) of the resource collection. It is used as a part of the RESTful path and
   * RPC method name. If it is left empty, a value is deduced from inspecting each endpoint method.
   */
  String resource() default "";

  /**
   * Custom transformers to be used for this API.
   */
  Class<? extends Transformer<?, ?>>[] transformers() default {};

  /**
   * {@code AnnotationBoolean.TRUE} to request that overriding configuration be loaded from the
   * appengine datastore for all API classes except for those classes that override using
   * {@code @ApiClass#useDatastoreForAdditionalConfig}.
   */
  // TODO: Make this work for a non-appengine environment.
  AnnotationBoolean useDatastoreForAdditionalConfig() default AnnotationBoolean.UNSPECIFIED;

  /**
   * Configures namespacing for generated clients. See {@link ApiNamespace} for details.
   */
  ApiNamespace namespace() default @ApiNamespace(ownerDomain = "", ownerName = "");

  /**
   * Whether or not the API is discoverable. This prevents the JavaScript client from working, and
   * by extension, the API Explorer.
   */
  AnnotationBoolean discoverable() default AnnotationBoolean.UNSPECIFIED;

  /**
   * Whether or not an API key is required. This is used to output a Swagger specification and has
   * no effect unless used with endpoints-management-control-appengine.
   */
  AnnotationBoolean apiKeyRequired() default AnnotationBoolean.UNSPECIFIED;

  /**
   * Rate limiting metric definitions that are used in this API.
   */
  ApiLimitMetric[] limitDefinitions() default {};
}
