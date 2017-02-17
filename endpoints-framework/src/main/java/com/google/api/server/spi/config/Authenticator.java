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

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.common.User;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface for Endpoints authenticators.  To create a custom authenticator, implement and assign
 * your authenticator class in the {@code authenticator} configuration property.
 */
// TODO: Figure out some way to pass in configuration to the custom authenticator similar
// (but more generialized) to how the current Google authenticator takes scopes, client ids, etc.
// Maybe some configured string that either gets passed into the constructor or the authenticate
// method.
// Or maybe this is good enough already.  Just create a separate Authenticator class for each
// configuration you want.
public interface Authenticator {
  /**
   * Authenticates the user from {@code request}.
   *
   * @return The authenticated user or null if there is no auth or auth has failed.
   */
  User authenticate(HttpServletRequest request) throws ServiceException;
}
