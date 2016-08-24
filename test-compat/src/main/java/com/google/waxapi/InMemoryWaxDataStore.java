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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An in memory data store for Wax.
 */
public class InMemoryWaxDataStore implements WaxDataStore {
  private Map<String, Map<String, WaxDataItem>> sessions =
      new HashMap<String, Map<String, WaxDataItem>>();

  @Override
  public List<WaxDataItem> list(String sessionId) throws InvalidSessionException {
    return copyItems(getSession(sessionId).values());
  }

  @Override
  public WaxDataItem get(String sessionId, String itemId) throws InvalidSessionException {
    Map<String, WaxDataItem> session = getSession(sessionId);
    return copyItem(session.get(itemId));
  }

  @Override
  public WaxDataItem insert(String sessionId, WaxDataItem item)
      throws InvalidSessionException, InvalidWaxDataItemException {
    Map<String, WaxDataItem> session = getSession(sessionId);
    if (session.containsKey(item.getId())) {
      throw new InvalidWaxDataItemException(item.getId());
    }
    session.put(item.getId(), copyItem(item));
    return copyItem(item);
  }

  @Override
  public boolean delete(String sessionId, String itemId) throws InvalidSessionException {
    Map<String, WaxDataItem> session = getSession(sessionId);
    return session.remove(itemId) != null;
  }

  @Override
  public WaxDataItem update(String sessionId, String itemId, WaxDataItem newItem)
      throws InvalidSessionException, InvalidWaxDataItemException {
    Map<String, WaxDataItem> session = getSession(sessionId);
    if (!session.containsKey(itemId)) {
      return null;
    }
    if (!itemId.equals(newItem.getId())) {
      throw new InvalidWaxDataItemException(newItem.getId());
    }
    session.put(itemId, copyItem(newItem));
    return copyItem(newItem);
  }

  @Override
  public String createSession(String prefix, Long durationInMillis) {
    WaxSession session = WaxSession.createSession(prefix, durationInMillis);
    String sessionId = session.getSessionId();
    Map<String, WaxDataItem> items = new HashMap<String, WaxDataItem>();

    for (WaxDataItem item : session.getItems()) {
      items.put(item.getId(), item);
    }
    sessions.put(sessionId, items);
    return sessionId;
  }

  @Override
  public void deleteSession(String sessionId) throws InvalidSessionException {
    if (sessions.remove(sessionId) == null) {
      throw new InvalidSessionException(sessionId);
    }
  }

  private Map<String, WaxDataItem> getSession(String sessionId) throws InvalidSessionException {
    Map<String, WaxDataItem> session = sessions.get(sessionId);
    if (session != null) {
      return session;
    }
    throw new InvalidSessionException(sessionId);
  }

  private List<WaxDataItem> copyItems(Collection<WaxDataItem> items) {
    if (items == null) {
      return null;
    }
    List<WaxDataItem> newItems = new ArrayList<WaxDataItem>(items.size());
    for (WaxDataItem item : items) {
      newItems.add(copyItem(item));
    }
    return newItems;
  }

  private WaxDataItem copyItem(WaxDataItem item) {
    if (item == null) {
      return null;
    }
    return new WaxDataItem(item.getId(), item.getName(), item.getBlobOfData());
  }
}
