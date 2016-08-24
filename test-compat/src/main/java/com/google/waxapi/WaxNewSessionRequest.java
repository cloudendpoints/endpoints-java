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

/**
 * An object representing the request of an API call to create a session.
 */
public class WaxNewSessionRequest {
  private String sessionName;
  private Long durationInMillis;

  public WaxNewSessionRequest() {
  }

  public WaxNewSessionRequest(String sessionName, Long durationInMillis) {
    this.sessionName = sessionName;
    this.durationInMillis = durationInMillis;
  }

  public String getSessionName() {
    return sessionName;
  }

  public void setSessionName(String sessionName) {
    this.sessionName = sessionName;
  }

  public Long getDurationInMillis() {
    return durationInMillis;
  }

  public void setDurationInMillis(long durationInMillis) {
    this.durationInMillis = durationInMillis;
  }
}
