package com.google.api.server.spi.swagger;

import static com.google.common.truth.Truth.assertAbout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.truth.Fact;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import io.swagger.models.HttpMethod;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import io.swagger.validator.models.SchemaValidationError;
import io.swagger.validator.models.ValidationResponse;
import io.swagger.validator.services.ValidatorService;
import java.util.List;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.ComparisonFailure;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

public final class SwaggerSubject extends Subject {

  private final ObjectWriter witer = Json.mapper().writerWithDefaultPrettyPrinter();

  private final Swagger actual;

  public static SwaggerSubject assertThat(@NullableDecl Swagger swagger) {
    return assertAbout(swaggers()).that(swagger);
  }

  private static Factory<SwaggerSubject, Swagger> swaggers() {
    return SwaggerSubject::new;
  }

  private SwaggerSubject(FailureMetadata failureStrategy, @Nullable Object actual) {
    super(failureStrategy, actual);
    this.actual = actual instanceof Swagger ? (Swagger) actual : null;
  }

  void isValid() {
    validatesSchema();
    hasNoDuplicateOperations();
  }

  private void validatesSchema() {
    //TODO there is probably a better way to validate, to get errors like https://editor.swagger.io/
    // This validator will only validate against the JsonSchema, not check references for example 
    try {
      ValidationResponse validationResponse = new ValidatorService()
          .debugByContent(null, null, toString(actual));
      List<SchemaValidationError> schemaValidationMessages = validationResponse
          .getSchemaValidationMessages();
      if (schemaValidationMessages != null && !schemaValidationMessages.isEmpty()) {
        System.out.println("Swagger spec: " + toString(actual));
        throw new AssertionError("Swagger spec is not valid" +
            schemaValidationMessages.stream()
                .map(error -> "\nValidation error: " + toString(error))
                .collect(Collectors.joining()));
      }
    } catch (Exception e) {
      throw new AssertionError("Could not validate Swagger spec", e);
    }
  }

  private void hasNoDuplicateOperations() {
    Multimap<String, String> operationIds = HashMultimap.create();
    for (Entry<String, Path> pathEntry : actual.getPaths().entrySet()) {
      for (Entry<HttpMethod, Operation> opEntry : pathEntry.getValue().getOperationMap()
          .entrySet()) {
        operationIds
            .put(opEntry.getValue().getOperationId(), pathEntry.getKey() + "|" + opEntry.getKey());
      }
    }
    Set<String> duplicateOperationIds = Sets.newHashSet();
    for (Entry<String, Collection<String>> entry : operationIds.asMap().entrySet()) {
      if (entry.getValue().size() > 1) {
        System.out.println("Duplicate operation id: " + entry);
        duplicateOperationIds.add(entry.getKey());
      }
    }
    if (!duplicateOperationIds.isEmpty()) {
      failWithActual("Duplicates operations ids found", duplicateOperationIds);
    }
  }

  void isSameAs(Swagger expected) {
    checkEquality(expected);
    // Jackson preserves order when deserializing expected result, so we should
    // always output resource and security definitions in the same order
    compareMapOrdering("Security definition", actual, expected, Swagger::getSecurityDefinitions);
    compareMapOrdering("Model definition", actual, expected, Swagger::getDefinitions);
    compareMapOrdering("Path", actual, expected, Swagger::getPaths);
    compareMapOrdering("Parameter", actual, expected, Swagger::getParameters);
    compareMapOrdering("Response", actual, expected, Swagger::getResponses);
  }

  private void checkEquality(Swagger expected) {
    SwaggerGenerator.normalizeOperationParameters(expected);
    normalizeRequiredPropertyList(actual);
    normalizeRequiredPropertyList(expected);
    if (!Objects.equals(actual, expected)) {
      throw new ComparisonFailure("Swagger specs don't match",
          toString(expected), toString(actual));
    }
  }

  //ModelImpl.required is not "persisted", but gathered from properties
  private void normalizeRequiredPropertyList(Swagger swagger) {
    if (swagger.getDefinitions() != null) {
      swagger.getDefinitions().values().stream()
          .filter(Predicates.instanceOf(ModelImpl.class))
          .map(model -> (ModelImpl) model)
          .forEach(model -> model.setRequired(model.getRequired()));
    }
  }

  private void compareMapOrdering(String message, Swagger actual,
      Swagger expected, Function<Swagger, Map<String, ?>> mapFunction) {
    Map<String, ?> actualMap = mapFunction.apply(actual);
    Map<String, ?> expectedMap = mapFunction.apply(expected);
    if (expectedMap != null && actualMap != null) {
      Set<String> actualKeys = actualMap.keySet();
      Set<String> expectedKeys = expectedMap.keySet();
      if (!ImmutableList.copyOf(actualKeys).equals(ImmutableList.copyOf(expectedKeys))) {
        throw new ComparisonFailure(message + " orders don't match"
            + Fact.fact("\nExpected keys", expectedKeys).toString()
            + Fact.fact("\nActual keys", actualKeys).toString(),
            toString(expected), toString(actual));
      }
    }
  }

  private String toString(Object toJson) {
    try {
      return witer.writeValueAsString(toJson);
    } catch (JsonProcessingException e) {
      throw new AssertionError("Cannot create String representation", e);
    }
  }

}
