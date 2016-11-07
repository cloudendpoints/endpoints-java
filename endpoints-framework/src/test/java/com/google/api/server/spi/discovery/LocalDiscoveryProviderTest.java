package com.google.api.server.spi.discovery;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.discovery.DiscoveryGenerator.DiscoveryContext;
import com.google.api.server.spi.discovery.DiscoveryGenerator.Result;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.DirectoryList.Items;
import com.google.api.services.discovery.model.RestDescription;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for {@link LocalDiscoveryProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LocalDiscoveryProviderTest {
  private static final String ROOT = "https://root.appspot.com/api";
  private static final String NAME = "foo";
  private static final String VERSION = "v1";

  @Mock private DiscoveryGenerator generator;
  @Mock private SchemaRepository repository;
  private LocalDiscoveryProvider provider;

  @Before
  public void setUp() {
    provider = new LocalDiscoveryProvider(ImmutableList.<ApiConfig>of(), generator, repository);
    when(generator.writeDiscovery(
        anyListOf(ApiConfig.class), any(DiscoveryContext.class), eq(repository)))
        .thenReturn(Result.builder().setDiscoveryDocs(
            ImmutableMap.of(new ApiKey(NAME, VERSION, null /* root */), getPlaceholderDoc()))
            .setDirectory(getPlaceholderDirectory())
            .build());
  }

  @Test
  public void getRestDocument() throws Exception {
    RestDescription doc = provider.getRestDocument(ROOT, NAME, VERSION);
    assertThat(doc.getBaseUrl()).isEqualTo("https://root.appspot.com/api/root/v1/");
    assertThat(doc.getRootUrl()).isEqualTo("https://root.appspot.com/api/");
  }

  @Test
  public void getRestDocument_NotFoundException() {
    try {
      provider.getRestDocument(ROOT, NAME, "notfound");
      fail("expected NotFoundException");
    } catch (NotFoundException expected) {
      // expected
    }
  }

  @Test
  public void getRpcDocument() {
    try {
      provider.getRpcDocument(ROOT, NAME, VERSION);
      fail("expected NotFoundException");
    } catch (NotFoundException expected) {
      // expected
    }
  }

  @Test
  public void getDirectory() throws Exception {
    DirectoryList directory = provider.getDirectory(ROOT);
    assertThat(directory.getItems().get(0).getDiscoveryRestUrl())
        .isEqualTo("https://root.appspot.com/api/discovery/v1/apis/foo/v1/rest");
  }

  private static RestDescription getPlaceholderDoc() {
    return new RestDescription()
        .setBaseUrl("https://placeholder.appspot.com/_ah/api/root/v1/")
        .setRootUrl("https://placeholder.appspot.com/_ah/api/");
  }

  private static DirectoryList getPlaceholderDirectory() {
    return new DirectoryList()
        .setItems(Lists.newArrayList(new Items()
            .setDiscoveryRestUrl(
                "https://placeholder.appspot.com/_ah/api/discovery/v1/apis/foo/v1/rest")));
  }
}