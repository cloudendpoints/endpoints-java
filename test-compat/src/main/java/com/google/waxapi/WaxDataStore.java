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

import java.util.List;

/**
 * Interface to a Wax API backing store.
 */
public interface WaxDataStore {
  /**
   * Gets all {@link WaxDataItem} objects associated with a session.
   *
   * @param sessionId the id of the session to fetch
   * @return all {@link WaxDataItem} objects associated with the session
   * @throws InvalidSessionException if the session doesn't exist in the store
   */
  List<WaxDataItem> list(String sessionId) throws InvalidSessionException;

  /**
   * Gets a single {@link WaxDataItem} by id within a session.
   *
   * @param sessionId the id of the session to fetch
   * @param itemId the id of the item to fetch
   * @return the requested {@link WaxDataItem}, or null if the session doesn't have an item with
   *         the specified id
   * @throws InvalidSessionException if the session doesn't exist in the store
   */
  WaxDataItem get(String sessionId, String itemId) throws InvalidSessionException;

  /**
   * Adds a {@link WaxDataItem} to a session.
   *
   * @param sessionId the id of the session to fetch
   * @param item
   * @return the inserted item
   * @throws InvalidSessionException if the session doesn't exist in the store
   * @throws InvalidWaxDataItemException if the session already has a {@link WaxDataItem} with the
   *         requested id
   */
  WaxDataItem insert(String sessionId, WaxDataItem item)
      throws InvalidSessionException, InvalidWaxDataItemException;

  /**
   * Deletes a {@link WaxDataItem} by id within a session.
   *
   * @param sessionId the id of the session to fetch
   * @param itemId the id of the item to delete
   * @return whether or not an item was found and deleted
   * @throws InvalidSessionException if the session doesn't exist in the store
   */
  boolean delete(String sessionId, String itemId) throws InvalidSessionException;

  /**
   * Updates a {@link WaxDataItem} by id within a session.
   *
   * @param sessionId the id of the session to fetch
   * @param itemId the id of the {@link WaxDataItem} to update
   * @param newItem the data to update the {@link WaxDataItem} with
   * @return the {@link WaxDataItem} after updating, or null if the session doesn't have an item
   *         with the specified id
   * @throws InvalidSessionException if the session doesn't exist in the store
   * @throws InvalidWaxDataItemException if the item's id would be changed by the update
   */
  WaxDataItem update(String sessionId, String itemId, WaxDataItem newItem)
      throws InvalidSessionException, InvalidWaxDataItemException;

  /**
   * Creates a session that starts with a given prefix. The returned id has no constraints other
   * than this, but the recommended format is ${prefix}-${timeInMillis} for compatibility with
   * the Rosy Wax API. The new session must be created with at least two {@link WaxDataItem}
   * objects associated with it in the implementation of this method.
   *
   * @param prefix the requested prefix for the session id
   * @param durationInMillis the duration that the session should last
   * @return a uniquely identifying session id
   */
  String createSession(String prefix, Long durationInMillis);

  /**
   * Delete a session given its id.
   *
   * @param sessionId the id of the session to delete
   * @throws InvalidSessionException if the session doesn't exist in the store
   */
  void deleteSession(String sessionId) throws InvalidSessionException;
}
