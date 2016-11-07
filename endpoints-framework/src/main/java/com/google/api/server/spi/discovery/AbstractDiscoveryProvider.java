package com.google.api.server.spi.discovery;

import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.response.NotFoundException;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

import java.util.logging.Logger;

/**
 * Base class for providing discovery data.
 */
abstract class AbstractDiscoveryProvider implements DiscoveryProvider {
  protected static final Logger logger =
      Logger.getLogger(AbstractDiscoveryProvider.class.getName());
  private static final Function<ApiConfig, ApiKey> CONFIG_TO_ROOTLESS_KEY =
      new Function<ApiConfig, ApiKey>() {
        @Override public ApiKey apply(ApiConfig config) {
          return new ApiKey(config.getName(), config.getVersion(), null /* root */);
        }
      };
  private final ImmutableList<ApiConfig> apiConfigs;
  private final ImmutableListMultimap<ApiKey, ApiConfig> configsByKey;

  AbstractDiscoveryProvider(ImmutableList<ApiConfig> apiConfigs) {
    this.apiConfigs = apiConfigs;
    this.configsByKey = FluentIterable.from(apiConfigs).index(CONFIG_TO_ROOTLESS_KEY);
  }

  ImmutableList<ApiConfig> getAllApiConfigs() {
    return apiConfigs;
  }

  ImmutableList<ApiConfig> getApiConfigs(String name, String version)
      throws NotFoundException {
    ApiKey key = new ApiKey(name, version, null /* root */);
    ImmutableList<ApiConfig> configs = configsByKey.get(key);
    if (configs.isEmpty()) {
      logger.info("No configuration found for name: " + name + ", version: " + version);
      throw new NotFoundException("Not Found");
    }
    return configs;
  }

  static Iterable<ApiConfig> rewriteConfigsWithRoot(Iterable<ApiConfig> configs,
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
}
