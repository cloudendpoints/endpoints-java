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
package com.google.api.server.spi.discovery;

import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.RestDescription;
import com.google.api.services.discovery.model.RpcDescription;

/**
 * An interface for generating discovery documents from API configurations.
 */
public interface DiscoveryProvider {
  /**
   * Gets a REST discovery document for an API.
   *
   * @throws NotFoundException if the API doesn't exist
   * @throws InternalServerErrorException an error takes place when getting the document
   */
  RestDescription getRestDocument(String root, String name, String version)
      throws NotFoundException, InternalServerErrorException;

  /**
   * Gets an RPC discovery document for an API.
   *
   * @throws NotFoundException if the API doesn't exist
   * @throws InternalServerErrorException an error takes place when getting the document
   */
  RpcDescription getRpcDocument(String root, String name, String version)
      throws NotFoundException, InternalServerErrorException;

  /**
   * Gets a list of REST discovery documents hosted by the current server. This method will never
   * return RPC discovery documents, as everything that uses online discovery uses the REST
   * discovery doc (API explorer, Python and JavaScript client, etc).
   *
   * @throws InternalServerErrorException
   */
  DirectoryList getDirectory(String root) throws InternalServerErrorException;
}
