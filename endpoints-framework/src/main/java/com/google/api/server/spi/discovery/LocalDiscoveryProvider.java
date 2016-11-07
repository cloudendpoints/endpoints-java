package com.google.api.server.spi.discovery;

import com.google.api.server.spi.Strings;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.discovery.DiscoveryGenerator.DiscoveryContext;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.DirectoryList.Items;
import com.google.api.services.discovery.model.RestDescription;
import com.google.api.services.discovery.model.RpcDescription;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * A {@link DiscoveryProvider} which generates discovery documents locally.
 */
public class LocalDiscoveryProvider extends AbstractDiscoveryProvider {
  private static final String PLACEHOLDER_ROOT = "https://placeholder.appspot.com/_ah/api";
  private final DiscoveryGenerator generator;
  private final SchemaRepository repository;
  private Map<ApiKey, RestDescription> discoveryDocs;
  private DirectoryList directoryList;

  public LocalDiscoveryProvider(ImmutableList<ApiConfig> apiConfigs, DiscoveryGenerator generator,
      SchemaRepository repository) {
    super(apiConfigs);
    this.generator = generator;
    this.repository = repository;
  }

  @Override
  public RestDescription getRestDocument(String root, String name, String version)
      throws NotFoundException {
    ensureDiscoveryResult();
    RestDescription doc = discoveryDocs.get(new ApiKey(name, version, null /* root */));
    if (doc == null) {
      throw new NotFoundException("Not Found");
    }
    return replaceRoot(doc, root);
  }

  @Override
  public RpcDescription getRpcDocument(String root, String name, String version)
      throws NotFoundException {
    throw new NotFoundException("RPC discovery is no longer supported.");
  }

  @Override
  public DirectoryList getDirectory(String root) {
    ensureDiscoveryResult();
    return replaceRoot(directoryList, root);
  }

  private synchronized void ensureDiscoveryResult() {
    if (discoveryDocs == null) {
      DiscoveryGenerator.Result result = generator.writeDiscovery(
          getAllApiConfigs(), new DiscoveryContext().setApiRoot(PLACEHOLDER_ROOT), repository);
      directoryList = result.directory();
      ImmutableMap.Builder<ApiKey, RestDescription> builder = ImmutableMap.builder();
      for (Map.Entry<ApiKey, RestDescription> entry : result.discoveryDocs().entrySet()) {
        ApiKey rootedKey = entry.getKey();
        builder.put(
            new ApiKey(rootedKey.getName(), rootedKey.getVersion(), null /* root */),
            entry.getValue());
      }
      discoveryDocs = builder.build();
    }
  }

  private static RestDescription replaceRoot(RestDescription doc, String newRoot) {
    if (doc == null) {
      return null;
    }
    newRoot = Strings.stripTrailingSlash(newRoot);
    return doc.clone()
        .setBaseUrl(doc.getBaseUrl().replaceFirst(PLACEHOLDER_ROOT, newRoot))
        .setRootUrl(doc.getRootUrl().replaceFirst(PLACEHOLDER_ROOT, newRoot));
  }

  private static DirectoryList replaceRoot(DirectoryList directory, String newRoot) {
    if (directory == null) {
      return null;
    }
    newRoot = Strings.stripTrailingSlash(newRoot);
    directory = directory.clone();
    for (Items item : directory.getItems()) {
      item.setDiscoveryRestUrl(item.getDiscoveryRestUrl().replaceFirst(PLACEHOLDER_ROOT, newRoot));
    }
    return directory;
  }
}
