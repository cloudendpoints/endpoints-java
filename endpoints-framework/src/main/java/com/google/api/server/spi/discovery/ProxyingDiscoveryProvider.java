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

import com.google.api.server.spi.Client;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigWriter;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.services.discovery.Discovery;
import com.google.api.services.discovery.model.ApiConfigs;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.RestDescription;
import com.google.api.services.discovery.model.RpcDescription;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides discovery information by proxying to the v1.0 discovery service.
 */
public class ProxyingDiscoveryProvider implements DiscoveryProvider {
  private static final Logger logger = Logger.getLogger(ProxyingDiscoveryProvider.class.getName());
  private static final Function<ApiConfig, ApiKey> CONFIG_TO_ROOTLESS_KEY =
      new Function<ApiConfig, ApiKey>() {
        @Override public ApiKey apply(ApiConfig config) {
          return new ApiKey(config.getName(), config.getVersion(), null /* root */);
        }
      };

  private final ImmutableList<ApiConfig> apiConfigs;
  private final ImmutableListMultimap<ApiKey, ApiConfig> configsByKey;
  private final ApiConfigWriter configWriter;
  private final Discovery discovery;

  public ProxyingDiscoveryProvider(
      ImmutableList<ApiConfig> apiConfigs, ApiConfigWriter configWriter) {
    this(apiConfigs, configWriter, createDiscovery());
  }

  public ProxyingDiscoveryProvider(
      ImmutableList<ApiConfig> apiConfigs, ApiConfigWriter configWriter, Discovery discovery) {
    this.apiConfigs = apiConfigs;
    this.configsByKey = FluentIterable.from(apiConfigs).index(CONFIG_TO_ROOTLESS_KEY);
    this.configWriter = configWriter;
    this.discovery = discovery;
  }

  @Override
  public RestDescription getRestDocument(String root, String name, String version)
      throws NotFoundException, InternalServerErrorException {
    try {
      ApiKey key = new ApiKey(name, version, null /* root */);
      ImmutableList<ApiConfig> configs = configsByKey.get(key);
      if (configs.isEmpty()) {
        logger.info("No configuration found for name: " + name + ", version: " + version);
        throw new NotFoundException("Not Found");
      }
      return discovery.apis()
          .generateRest(new com.google.api.services.discovery.model.ApiConfig().setConfig(
              getApiConfigStringWithRoot(configs, root))).execute();
    } catch (IOException | ApiConfigException e) {
      logger.log(Level.SEVERE, "Could not generate or cache discovery doc", e);
      throw new InternalServerErrorException("Internal Server Error", e);
    }
  }

  @Override
  public RpcDescription getRpcDocument(String root, String name, String version)
      throws NotFoundException, InternalServerErrorException {
    try {
      ApiKey key = new ApiKey(name, version, null /* root */);
      ImmutableList<ApiConfig> configs = configsByKey.get(key);
      if (configs.isEmpty()) {
        logger.info("No configuration found for name: " + name + ", version: " + version);
        throw new NotFoundException("Not Found");
      }
      return discovery.apis()
          .generateRpc(new com.google.api.services.discovery.model.ApiConfig().setConfig(
              getApiConfigStringWithRoot(configs, root))).execute();
    } catch (IOException | ApiConfigException e) {
      logger.log(Level.SEVERE, "Could not generate or cache discovery doc", e);
      throw new InternalServerErrorException("Internal Server Error", e);
    }
  }

  @Override
  public DirectoryList getDirectory(String root) throws InternalServerErrorException {
    try {
      Map<ApiKey, String> configStrings =
          configWriter.writeConfig(rewriteConfigsWithRoot(apiConfigs, root));
      ApiConfigs configs = new ApiConfigs();
      configs.setConfigs(Lists.newArrayList(configStrings.values()));
      return discovery.apis().generateDirectory(configs).execute();
    } catch (IOException | ApiConfigException e) {
      logger.log(Level.SEVERE, "Could not generate or cache directory", e);
      throw new InternalServerErrorException("Internal Server Error", e);
    }
  }

  private String getApiConfigStringWithRoot(ImmutableList<ApiConfig> configs, final String root)
      throws InternalServerErrorException, ApiConfigException {
    Map<ApiKey, String> configMap = configWriter.writeConfig(rewriteConfigsWithRoot(configs, root));
    if (configMap.size() != 1) {
      logger.severe("config generation yielded more than one API");
      throw new InternalServerErrorException("Internal Server Error");
    }
    return Iterables.getFirst(configMap.values(), null);
  }

  private static Iterable<ApiConfig> rewriteConfigsWithRoot(Iterable<ApiConfig> configs,
      String root) {
    ApiConfig.Factory factory = new ApiConfig.Factory();
    return FluentIterable.from(configs).transform(new RootRemapperFunction(root, factory));
  }

  private static class RootRemapperFunction implements Function<ApiConfig, ApiConfig> {
    private final String root;
    private final ApiConfig.Factory factory;

    RootRemapperFunction(String root, ApiConfig.Factory factory) {
      this.root = root;
      this.factory = factory;
    }

    @Override public ApiConfig apply(ApiConfig input) {
      ApiConfig copy = factory.copy(input);
      copy.setRoot(root);
      return copy;
    }
  }

  private static Discovery createDiscovery() {
    Client client = Client.getInstance();
    return new Discovery.Builder(client.getHttpTransport(), client.getJsonFactory(),
        null /* httpRequestInitializer */) .build();
  }
}
