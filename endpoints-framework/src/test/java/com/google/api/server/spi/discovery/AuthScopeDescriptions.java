package com.google.api.server.spi.discovery;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import javax.annotation.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Fetches up-to-date auth scope descriptions from https://developers.google.com/identity/protocols/googlescopes.
 * Use this to update the scopeDescriptions.properties.
 * Uses Jsoup from the appengine-api-stubs package (used as test dependency).
 */
public class AuthScopeDescriptions {

  private static final String GOOGLESCOPES_URL = "https://developers.google.com/identity/protocols/googlescopes";

  //short scopes first, then order alphabetically
  private static final Ordering<String> SCOPE_ORDERING = Ordering.compound(
      ImmutableList.of(
          new Ordering<String>() {
            @Override
            public int compare(@Nullable String left, @Nullable String right) {
              return Boolean.compare(left.startsWith("https"), right.startsWith("https"));
            }
          },
          Ordering.<String>natural()
      )
  );

  public static void main(String[] args) throws Exception {
    new AuthScopeDescriptions().print(System.out);
  }

  private Map<String, String> descriptionsByScope = new LinkedHashMap<>();

  private AuthScopeDescriptions() throws Exception {
    final Document document = Jsoup.parse(new URL(GOOGLESCOPES_URL), 10000);
    final Elements scopes = document.select("table.responsive tr:has(td)");
    for (Element scope : scopes) {
      final Elements cells = scope.select("td");
      descriptionsByScope.put(cells.get(0).text(), cells.get(1).text());
    }
  }

  private void print(OutputStream out) throws IOException {
    //sorted properties
    Properties properties = new Properties() {
      public Enumeration keys() {
        Enumeration keysEnum = super.keys();
        Vector<String> keyList = new Vector<>();
        while (keysEnum.hasMoreElements()) {
          keyList.add((String) keysEnum.nextElement());
        }
        Collections.sort(keyList, SCOPE_ORDERING);
        return keyList.elements();
      }
    };
    for (Entry<String, String> entry : descriptionsByScope.entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue());
    }
    properties.store(out, "Source: " + GOOGLESCOPES_URL);
  }

}
