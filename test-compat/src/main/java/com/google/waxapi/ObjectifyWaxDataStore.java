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

import com.googlecode.objectify.ObjectifyService;

import java.util.Iterator;
import java.util.List;

/**
 * A data store based on App Engine Datastore via the Objectify library.
 */
public class ObjectifyWaxDataStore implements WaxDataStore {

  @Override
  public List<WaxDataItem> list(String sessionId) throws InvalidSessionException {
    WaxSession session = getSession(sessionId);
    return session.getItems();
  }

  @Override
  public WaxDataItem get(String sessionId, String itemId) throws InvalidSessionException {
    WaxSession session = getSession(sessionId);
    for (WaxDataItem item : session.getItems()) {
      if (itemId.equals(item.getId())) {
        return item;
      }
    }
    return null;
  }

  @Override
  public WaxDataItem insert(String sessionId, WaxDataItem item)
      throws InvalidSessionException, InvalidWaxDataItemException {
    WaxSession session = getSession(sessionId);
    for (WaxDataItem existingItem : session.getItems()) {
      if (existingItem.getId().equals(item.getId())) {
        throw new InvalidWaxDataItemException(item.getId());
      }
    }
    session.getItems().add(item);
    ObjectifyService.ofy().save().entities(session).now();
    return item;
  }

  @Override
  public boolean delete(String sessionId, String itemId) throws InvalidSessionException {
    WaxSession session = getSession(sessionId);
    Iterator<WaxDataItem> iter = session.getItems().iterator();
    while (iter.hasNext()) {
      if (itemId.equals(iter.next().getId())) {
        iter.remove();
        ObjectifyService.ofy().save().entities(session).now();
        return true;
      }
    }
    return false;
  }

  @Override
  public WaxDataItem update(String sessionId, String itemId, WaxDataItem newItem)
      throws InvalidSessionException, InvalidWaxDataItemException {
    if (!itemId.equals(newItem.getId())) {
      // TODO: I think this is a facility provided by Apiary in the non-Swarm case, but
      // unsure if it's supported in Swarm.
      throw new InvalidWaxDataItemException(newItem.getId());
    }
    WaxSession session = getSession(sessionId);
    for (int i = 0; i < session.getItems().size(); i++) {
      WaxDataItem item = session.getItems().get(i);
      if (itemId.equals(item.getId())) {
        session.getItems().set(i, newItem);
        ObjectifyService.ofy().save().entities(session).now();
        return newItem;
      }
    }
    return null;
  }

  @Override
  public String createSession(String prefix, Long durationInMillis) {
    WaxSession session = WaxSession.createSession(prefix, durationInMillis);
    ObjectifyService.ofy().save().entities(session).now();
    return session.getSessionId();
  }

  @Override
  public void deleteSession(String sessionId) throws InvalidSessionException {
    WaxSession session = getSession(sessionId);
    ObjectifyService.ofy().delete().type(WaxSession.class).ids(sessionId).now();
  }

  private WaxSession getSession(String sessionId) throws InvalidSessionException {
    WaxSession session = ObjectifyService.ofy().load().type(WaxSession.class).id(sessionId).now();
    if (session != null) {
      return session;
    }
    throw new InvalidSessionException(sessionId);
  }
}
