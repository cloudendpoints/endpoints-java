package com.google.api.server.spi.config.model;

import com.google.api.server.spi.Constant;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.discovery.DiscoveryGenerator;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Stores a list of OAuth2 scopes with their corresponding descriptions.
 * Loads Google scopes from file googleScopeDescriptions.properties in same package.
 */
public class AuthScopeRepository {

  private static final ImmutableMap<String, String> GOOGLE_SCOPE_DESCRIPTIONS
      = loadScopeDescriptions("googleScopeDescriptions.properties");

  private static ImmutableMap<String, String> loadScopeDescriptions(String fileName) {
    try {
      Properties properties = new Properties();
      URL resourceFile = Resources.getResource(DiscoveryGenerator.class, fileName);
      InputStream inputStream = resourceFile.openStream();
      properties.load(inputStream);
      inputStream.close();
      return Maps.fromProperties(properties);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load scope descriptions from " + fileName, e);
    }
  }

  private final SortedMap<String, String> descriptionsByScope = new TreeMap<>();

  public AuthScopeRepository() {
    //userinfo.email should always be requested, as it is required for authentication
    add(AuthScopeExpressions.interpret(Constant.API_EMAIL_SCOPE));
  }

  public void add(AuthScopeExpression scopeExpression) {
    for (String scope : scopeExpression.getAllScopes()) {
      String description = MoreObjects.firstNonNull(GOOGLE_SCOPE_DESCRIPTIONS.get(scope), scope);
      descriptionsByScope.put(scope, description);
    }
  }

  /**
   * Returns the added scopes and their descriptions.
   * Unknown scopes will have the scope itself as description.
   *
   * @return a sorted map containing scopes as key, descriptions as value
   */
  public SortedMap<String, String> getDescriptionsByScope() {
    return descriptionsByScope;
  }

}
