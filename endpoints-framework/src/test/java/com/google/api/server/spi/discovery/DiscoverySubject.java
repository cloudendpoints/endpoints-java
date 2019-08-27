package com.google.api.server.spi.discovery;

import static com.google.common.truth.Truth.assertAbout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.api.services.discovery.model.RestDescription;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import io.swagger.util.Json;
import java.util.Objects;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.ComparisonFailure;

public final class DiscoverySubject extends Subject {

  private final ObjectWriter writer = Json.mapper().writerWithDefaultPrettyPrinter();

  private final RestDescription actual;

  public static DiscoverySubject assertThat(@NullableDecl RestDescription swagger) {
    return assertAbout(discoveries()).that(swagger);
  }

  private static Factory<DiscoverySubject, RestDescription> discoveries() {
    return DiscoverySubject::new;
  }

  private DiscoverySubject(FailureMetadata failureStrategy, @Nullable Object actual) {
    super(failureStrategy, actual);
    this.actual = actual instanceof RestDescription ? (RestDescription) actual : null;
  }

  void isSameAs(RestDescription expected) {
    checkEquality(expected);
  }

  private void checkEquality(RestDescription expected) {
    if (!Objects.equals(actual, expected)) {
      throw new ComparisonFailure("Discovery specs don't match",
          toString(expected), toString(actual));
    }
  }

  private String toString(RestDescription expected) {
    try {
      return writer.writeValueAsString(expected);
    } catch (JsonProcessingException e) {
      throw new AssertionError("Cannot create String representation for specs", e);
    }
  }

}
