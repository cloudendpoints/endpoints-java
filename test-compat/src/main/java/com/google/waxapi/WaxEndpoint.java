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
package com.google.waxapi;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.common.base.Strings;

import java.util.List;

import javax.inject.Named;

/**
 * A Swarm SPI used for end-to-end testing.
 */
@Api(
    name = "wax",
    description = "Endpoints system integration testing backend API",
    namespace = @ApiNamespace(ownerDomain = "compat-tests.com", ownerName = "Compat Tests"))
public class WaxEndpoint {
  private WaxDataStore store;

  public WaxEndpoint() {
    this(new ObjectifyWaxDataStore());
  }

  public WaxEndpoint(WaxDataStore store) {
    this.store = store;
  }

  /**
   * Responds with list of {@link WaxDataItem} for the {@link WaxSession} specified in the
   * request.
   *
   * @throws NotFoundException if the session doesn't exist
   */
  @ApiMethod(
      name = "items.list",
      path = "sessions/{sessionId}/items")
  public List<WaxDataItem> list(@Named("sessionId") String sessionId) throws NotFoundException {
    try {
      return store.list(sessionId);
    } catch (InvalidSessionException e) {
      throw new NotFoundException(e.getMessage());
    }
  }

  /**
   * Responds with the {@link WaxDataItem} resource requested.
   *
   * @throws NotFoundException if the session doesn't exist or has no {@link WaxDataItem} with the
   *         requested id
   */
  @ApiMethod(
      name = "items.get",
      path = "sessions/{sessionId}/items/{itemId}")
  public WaxDataItem get(@Named("sessionId") String sessionId, @Named("itemId") String itemId)
      throws NotFoundException {
    try {
      WaxDataItem item = store.get(sessionId, itemId);
      if (item != null) {
        return item;
      }
      throw new NotFoundException("Invalid itemId: " + itemId);
    } catch (InvalidSessionException e) {
      throw new NotFoundException(e.getMessage());
    }
  }

  /**
   * Creates a new {@link WaxDataItem} resource and adds it to a {@link WaxSession} if it does not
   * already exist.
   *
   * @throws NotFoundException if the session doesn't exist
   * @throws BadRequestException if the requested item id already exists in the session
   */
  @ApiMethod(
      name = "items.insert",
      path = "sessions/{sessionId}/items")
  public WaxDataItem insert(@Named("sessionId") String sessionId, WaxDataItem request)
      throws NotFoundException, BadRequestException {
    try {
      return store.insert(sessionId, request);
    } catch (InvalidSessionException e) {
      throw new NotFoundException(e.getMessage());
    } catch (InvalidWaxDataItemException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  /**
   * Removes {@link WaxDataItem} resource from the specified {@link WaxSession}.
   *
   * @throws NotFoundException if the session or item id doesn't exist
   */
  @ApiMethod(
      name = "items.delete",
      path = "sessions/{sessionId}/items/{itemId}")
  public void delete(
      @Named("sessionId") String sessionId, @Named("itemId") String itemId)
          throws NotFoundException {
    try {
      if (!store.delete(sessionId, itemId)) {
        throw new NotFoundException("Invalid itemId: " + itemId);
      }
    } catch (InvalidSessionException e) {
      throw new NotFoundException(e.getMessage());
    }
  }

  /**
   * Replaces an existing {@link WaxDataItem} resource with the one from the request. Reports an
   * error to the client if the resource requested to be updated does not exist.
   *
   * @return the updated {@link WaxDataItem}
   * @throws NotFoundException if the session doesn't exist or has no {@link WaxDataItem} with the
   *         requested id
   * @throws BadRequestException if the request object id differs from the id to update
   */
  @ApiMethod(
      name = "items.update",
      path = "sessions/{sessionId}/items/{itemId}")
  public WaxDataItem update(
      @Named("sessionId") String sessionId, @Named("itemId") String itemId, WaxDataItem request)
          throws NotFoundException, BadRequestException {
    try {
      WaxDataItem item = store.update(sessionId, itemId, request);
      if (item != null) {
        return item;
      }
      throw new NotFoundException("Invalid itemId: " + itemId);
    } catch (InvalidSessionException e) {
      throw new NotFoundException(e.getMessage());
    } catch (InvalidWaxDataItemException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  /**
   * Creates a new session from {@link WaxNewSessionRequest}.
   *
   * @return {@link WaxNewSessionResponse} with the created session id
   * @throws InternalServerErrorException if the session creation failed
   * @throws BadRequestException if the requested session name is bad
   */
  @ApiMethod(
      name = "sessions.create",
      path = "newsession",
      httpMethod = HttpMethod.POST)
  public WaxNewSessionResponse createSession(WaxNewSessionRequest request)
      throws InternalServerErrorException, BadRequestException {
    if (Strings.isNullOrEmpty(request.getSessionName())) {
      throw new BadRequestException("Name must be non-empty");
    }
    String sessionId =
        store.createSession(request.getSessionName(), request.getDurationInMillis());
    if (sessionId != null) {
      return new WaxNewSessionResponse(sessionId);
    }
    throw new InternalServerErrorException("Error while adding session");
  }

  /**
   * Remove an existing session.
   * <p>
   * Clients that create sessions without a duration (will last forever) will need to call this
   * method on their own to clean up the session.
   *
   * @return {@link WaxRemoveSessionResponse} with the deleted session id
   * @throws InternalServerErrorException if the session deletion failed
   */
  @ApiMethod(
      name = "sessions.remove",
      path = "removesession",
      httpMethod = HttpMethod.POST)
  public WaxRemoveSessionResponse removeSession(@Named("sessionId") String sessionId)
      throws InternalServerErrorException {
    try {
      store.deleteSession(sessionId);
      return new WaxRemoveSessionResponse(sessionId);
    } catch (InvalidSessionException e) {
      throw new InternalServerErrorException(e.getMessage());
    }
  }
}
