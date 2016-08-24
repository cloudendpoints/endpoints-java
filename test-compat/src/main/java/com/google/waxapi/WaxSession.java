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

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.ArrayList;
import java.util.List;

/**
 * A Wax session object for use with {@link ObjectifyWaxDataStore}.
 */
@Entity
public class WaxSession {
  /**
   * Ids for the {@link WaxDataItem} objects added to each new session.
   */
  static final String[] DEFAULT_SESSION_ITEM_IDS = {"A", "B"};

  private static final long DEFAULT_WAX_SESSION_DURATION_MS = 300000; // 5 minutes

  @Id private String sessionId;

  private List<WaxDataItem> items;

  private long expirationTimeMillis;

  public WaxSession() {
  }

  public WaxSession(String sessionId) {
    this(sessionId, DEFAULT_WAX_SESSION_DURATION_MS, new ArrayList<WaxDataItem>());
  }

  public WaxSession(String sessionId, long expirationTimeMillis, List<WaxDataItem> items) {
    this.sessionId = sessionId;
    this.expirationTimeMillis = expirationTimeMillis;
    this.items = items;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public long getExpirationTimeMillis() {
    return expirationTimeMillis;
  }

  public void setExpirationTimeMillis(long expirationTimeMillis) {
    this.expirationTimeMillis = expirationTimeMillis;
  }

  public List<WaxDataItem> getItems() {
    return items;
  }

  public void setItems(List<WaxDataItem> items) {
    this.items = items;
  }

  public static WaxSession createSession(String sessionPrefix, Long durationMs) {
    if (durationMs == null) {
      durationMs = DEFAULT_WAX_SESSION_DURATION_MS;
    }

    long currentTime = System.currentTimeMillis();
    String sessionId = sessionPrefix + "-" + currentTime;
    List<WaxDataItem> items = new ArrayList<WaxDataItem>(2);
    for (String id : DEFAULT_SESSION_ITEM_IDS) {
      items.add(new WaxDataItem(id, "Item " + id, null));
    }
    WaxSession session = new WaxSession(sessionId, currentTime + durationMs, items);
    return session;
  }
}
