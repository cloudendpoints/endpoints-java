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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.common.truth.IntegerSubject;
import com.google.util.TestUtils;

import com.compat_tests.wax.Wax;
import com.compat_tests.wax.model.WaxDataItem;
import com.compat_tests.wax.model.WaxDataItemCollection;
import com.compat_tests.wax.model.WaxNewSessionRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * End-to-end tests using the Wax API.
 */
@RunWith(JUnit4.class)
public class WaxTest {
  private static final WaxDataItem ITEM_A = new WaxDataItem().setId("A").setName("Item A");
  private static final WaxDataItem ITEM_B = new WaxDataItem().setId("B").setName("Item B");
  private static final WaxDataItem ITEM_C = new WaxDataItem().setId("C").setName("Item C");
  private Wax wax;

  @Before
  public void setUp() {
    wax = TestUtils.configureApiClient(
        new Wax.Builder(new NetHttpTransport(), new JacksonFactory(), null)).build();
  }

  @Test
  public void newSession() throws IOException {
    String sessionId = createSession();
    // We just care that this doesn't return an error.
    wax.items().list(sessionId).execute();
  }

  @Test
  public void newSession_invalidSessionId() throws IOException {
    try {
      wax.sessions()
          .create(new WaxNewSessionRequest().setSessionName("").setDurationInMillis(1L))
          .execute();
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  @Test
  public void removeSession() throws IOException {
    String sessionId = createSession();
    // Make sure success is returned, but we don't care what the items are.
    wax.items().list(sessionId).execute();
    wax.sessions().remove(sessionId).execute();
    try {
      wax.items().list(sessionId);
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Test
  public void removeSession_invalidSession() throws IOException {
    try {
      wax.sessions().remove("not a real id").execute();
    } catch (GoogleJsonResponseException e) {
      // 500 is translated to 503 by default.
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }
  }

  @Test
  public void listSessionItems() throws IOException {
    String sessionId = createSession();
    WaxDataItemCollection items = wax.items().list(sessionId).execute();
    assertThat(stripMetadata(items.getItems())).containsExactly(ITEM_A, ITEM_B);
  }

  @Test
  public void listSessionItems_invalidSession() throws IOException {
    try {
      wax.items().list("not a real id").execute();
      fail("expected exception");
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Test
  public void addSessionItem() throws IOException {
    String sessionId = createSession();
    WaxDataItem item = wax.items().insert(sessionId, ITEM_C).execute();
    WaxDataItemCollection items = wax.items().list(sessionId).execute();
    assertThat(stripMetadata(item)).isEqualTo(ITEM_C);
    assertThat(stripMetadata(items.getItems())).containsExactly(ITEM_A, ITEM_B, ITEM_C);
  }

  @Test
  public void addSessionItem_invalidSession() throws IOException {
    try {
      wax.items().insert("not a real id", ITEM_C).execute();
      fail("expected exception");
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Test
  public void addSessionItem_invalidItem() throws IOException {
    try {
      String sessionId = createSession();
      wax.items().insert(sessionId, ITEM_A).execute();
      fail("expected exception");
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  @Test
  public void removeSessionItem() throws Exception {
    String sessionId = createSession();
    wax.items().delete(sessionId, ITEM_B.getId()).execute();
    WaxDataItemCollection items = wax.items().list(sessionId).execute();
    assertThat(stripMetadata(items.getItems())).containsExactly(ITEM_A);
  }

  @Test
  public void removeSessionItem_invalidSession() throws IOException {
    try {
      wax.items().delete("not a real id", ITEM_B.getId()).execute();
      fail("expected exception");
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Test
  public void removeSessionItem_invalidItem() throws IOException {
    try {
      String sessionId = createSession();
      wax.items().delete(sessionId, "not a real id").execute();
      fail("expected exception");
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Test
  public void updateSessionItem() throws IOException {
    String sessionId = createSession();
    WaxDataItem newItemB = new WaxDataItem().setId("B").setName("New Item B");
    WaxDataItem item = wax.items().update(sessionId, newItemB.getId(), newItemB).execute();
    WaxDataItemCollection items = wax.items().list(sessionId).execute();
    assertThat(stripMetadata(item)).isEqualTo(newItemB);
    assertThat(stripMetadata(items.getItems())).containsExactly(ITEM_A, newItemB);
  }

  @Test
  public void updateSessionItem_invalidSession() throws IOException {
    try {
      wax.items().update("not a real id", ITEM_B.getId(), ITEM_B).execute();
      fail("expected exception");
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Test
  public void updateSessionItem_invalidItem() throws IOException {
    try {
      String sessionId = createSession();
      wax.items().update(sessionId, ITEM_C.getId(), ITEM_C).execute();
      fail("expected exception");
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Test
  public void updateSessionItem_invalidItemIdChange() throws IOException {
    try {
      String sessionId = createSession();
      wax.items().update(sessionId, ITEM_B.getId(), ITEM_C).execute();
      fail("expected exception");
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  @Test
  public void getSessionItem() throws IOException {
    String sessionId = createSession();
    WaxDataItem item = wax.items().get(sessionId, ITEM_B.getId()).execute();
    assertThat(stripMetadata(item)).isEqualTo(ITEM_B);
  }

  @Test
  public void getSessionItem_invalidSession() throws IOException {
    try {
      wax.items().get("not a real id", ITEM_B.getId()).execute();
      fail("expected exception");
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Test
  public void getSessionItem_invalidItem() throws IOException {
    try {
      String sessionId = createSession();
      wax.items().get(sessionId, ITEM_C.getId()).execute();
      fail("expected exception");
    } catch (GoogleJsonResponseException e) {
      assertThatResponseCode(e).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private String createSession() throws IOException {
    return wax.sessions()
        .create(new WaxNewSessionRequest().setSessionName("test").setDurationInMillis(1L))
        .execute()
        .getNewSessionId();
  }

  private static IntegerSubject assertThatResponseCode(GoogleJsonResponseException e) {
    return assertThat(e.getStatusCode());
  }

  public List<WaxDataItem> stripMetadata(List<WaxDataItem> items) {
    for (WaxDataItem item : items) {
      stripMetadata(item);
    }
    return items;
  }

  public WaxDataItem stripMetadata(WaxDataItem item) {
    item.remove("kind");
    item.remove("etag");
    return item;
  }
}
