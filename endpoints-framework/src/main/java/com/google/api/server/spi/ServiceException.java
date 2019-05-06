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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Generic service exception that, in addition to a status message, has a status code, and
 * optionally, response headers to return.
 */
public class ServiceException extends Exception {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  /** Reserved keywords, cannot be set as an extra field name. */
  public static final ImmutableList<String> EXTRA_FIELDS_RESERVED_NAMES = ImmutableList.of("domain", "message", "reason");

  protected final int statusCode;
  protected final String reason;
  protected final String domain;
  protected Level logLevel;
  private final Map<String, Object> extraFields = new HashMap<>();

  public ServiceException(int statusCode, String statusMessage) {
    super(statusMessage);

    this.statusCode = statusCode;
    this.reason = null;
    this.domain = null;
  }

  public ServiceException(int statusCode, Throwable cause) {
    super(cause);

    this.statusCode = statusCode;
    this.reason = null;
    this.domain = null;
  }

  public ServiceException(int statusCode, String statusMessage, Throwable cause) {
    super(statusMessage, cause);

    this.statusCode = statusCode;
    this.reason = null;
    this.domain = null;
  }

  public ServiceException(int statusCode, String statusMessage, String reason) {
    super(statusMessage);

    this.statusCode = statusCode;
    this.reason = reason;
    this.domain = null;
  }
  
  public ServiceException(int statusCode, String statusMessage, String reason, Throwable cause) {
    super(statusMessage, cause);
    
    this.statusCode = statusCode;
    this.reason = reason;
    this.domain = null;
  }

  public ServiceException(int statusCode, String statusMessage, String reason, String domain) {
    super(statusMessage);

    this.statusCode = statusCode;
    this.reason = reason;
    this.domain = domain;
  }
  
  public ServiceException(int statusCode, String statusMessage, String reason, String domain, 
      Throwable cause) {
    super(statusMessage, cause);
    
    this.statusCode = statusCode;
    this.reason = reason;
    this.domain = domain;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getReason() {
    return reason;
  }

  public String getDomain() {
    return domain;
  }

  public Map<String, String> getHeaders() {
    return null;
  }

  /**
   * Associates to this exception an extra field as a field name/value pair. If a field
   * with the same name was previously set, the old value is replaced by the specified
   * value.
   * @return this
   * @throws NullPointerException if {@code fieldName} is {@code null}.
   * @throws IllegalArgumentException if {@code fieldName} is one of the reserved field
   *         names {@link #EXTRA_FIELDS_RESERVED_NAMES}.
   */
  public ServiceException putExtraField(String fieldName, String value) {
    return putExtraFieldInternal(fieldName, value);
  }

  /**
   * Associates to this exception an extra field as a field name/value pair. If a field
   * with the same name was previously set, the old value is replaced by the specified
   * value.
   * @return this
   * @throws NullPointerException if {@code fieldName} is {@code null}.
   * @throws IllegalArgumentException if {@code fieldName} is one of the reserved field
   *         names {@link #EXTRA_FIELDS_RESERVED_NAMES}.
   */
  public ServiceException putExtraField(String fieldName, Boolean value) {
    return putExtraFieldInternal(fieldName, value);
  }

  /**
   * Associates to this exception an extra field as a field name/value pair. If a field
   * with the same name was previously set, the old value is replaced by the specified
   * value.
   * @return this
   * @throws NullPointerException if {@code fieldName} is {@code null}.
   * @throws IllegalArgumentException if {@code fieldName} is one of the reserved field
   *         names {@link #EXTRA_FIELDS_RESERVED_NAMES}.
   */
  public ServiceException putExtraField(String fieldName, Number value) {
    return putExtraFieldInternal(fieldName, value);
  }

  /**
   * Associates to this exception an extra field as a field name/value pair. If a field
   * with the same name was previously set, the old value is replaced by the specified
   * value.<br>
   * This unsafe version accepts any POJO as is:
   * <ul>
   * <li>the object should be serializable</li>
   * <li>no defensive copy nor conversion are made. So {@code value} should not be modified
   * or reused after the call of this method.</li>
   * </ul>
   * These constraints must be taken into consideration when overriding this method.
   * @return this
   * @throws NullPointerException if {@code fieldName} is {@code null}.
   * @throws IllegalArgumentException if {@code fieldName} is one of the reserved field
   *         names {@link #EXTRA_FIELDS_RESERVED_NAMES}.
   */
  protected ServiceException putExtraFieldUnsafe(String fieldName, Object value) {
    return putExtraFieldInternal(fieldName, value);
  }

  private ServiceException putExtraFieldInternal(String fieldName, Object value) {
    Preconditions.checkNotNull(fieldName);
    Preconditions.checkArgument(!EXTRA_FIELDS_RESERVED_NAMES.contains(fieldName), "The field name '%s' is reserved", fieldName);
    final Object previousValue = extraFields.put(fieldName, value);
    if (previousValue != null) {
      logger.atFine().log("Replaced extra field %s: %s => %s", fieldName, previousValue, value);
    }
    return this;
  }

  /**
   * Gets the extra fields. The extra fields are returned in an unmodifiable map,
   * each field name/value pair is a map entry. The map is empty if no extra field
   * has been added.
   */
  public final Map<String, Object> getExtraFields() {
    return Collections.unmodifiableMap(extraFields);
  }

  public Level getLogLevel() {
    return logLevel == null ? getDefaultLoggingLevel(statusCode) : logLevel;
  }

  private static Level getDefaultLoggingLevel(int statusCode) {
    return statusCode >= 500 ? Level.SEVERE : Level.INFO;
  }

  public static <T extends ServiceException> T withLogLevel(T exception, Level level) {
    exception.logLevel = level;
    return exception;
  }
}
