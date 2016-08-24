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
package com.google.api.server.spi.response;

import java.util.Collection;

/**
 * Collection response to be returned by an endpoint implementation method that wants to support
 * pagination (and other metadata about returned collection in the future).  Endpoint
 * implementation method would do something like this to use it:
 * <p>
 * <code>
 * public CollectionResponse&lt;Greeting&gt; list(@Named("pageToken") String pageToken) {
 *   List&lt;Greeting&gt; greetings = new ArrayList&lt;Greeting&gt;();
 *   // use pageToken to fill greetings with next page of items
 *   return CollectionResponse.&lt;Greeting&gt;builder()
 *       .setItems(greetings)
 *       .setNextPageToken("newPageToken")
 *       .build();
 * }
 * </code>
 */
public class CollectionResponse<T> {

  /**
   * Builder for {@link CollectionResponse}.
   */
  public static class Builder<T> {

    private Collection<T> items;
    private String nextPageToken;

    public Builder<T> setItems(Collection<T> items) {
      this.items = items;
      return this;
    }

    public Builder<T> setNextPageToken(String nextPageToken) {
      this.nextPageToken = nextPageToken;
      return this;
    }

    public CollectionResponse<T> build() {
      return new CollectionResponse<T>(items, nextPageToken);
    }
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  private final Collection<T> items;
  private final String nextPageToken;

  protected CollectionResponse(Collection<T> items, String nextPageToken) {
    this.items = items;
    this.nextPageToken = nextPageToken;
  }

  public String getNextPageToken() {
    return nextPageToken;
  }

  public Collection<T> getItems() {
    return items;
  }
}
