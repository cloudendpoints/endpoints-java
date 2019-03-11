package com.google.api.server.spi;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Boolean.TRUE;

import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.UnauthorizedException;

import java.util.Map;
import java.util.logging.Level;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ServiceExceptionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testWithLogLevel() {
    UnauthorizedException ex = new UnauthorizedException("");
    assertThat(ex.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(ServiceException.withLogLevel(ex, Level.WARNING).getLogLevel())
        .isEqualTo(Level.WARNING);
  }

  @Test
  public void testExtraFields() {
    UnauthorizedException ex = new UnauthorizedException("");
    ex.putExtraField("isAdmin", TRUE)
      .putExtraField("userId", Integer.valueOf(12))
      .putExtraField("userName", "John Doe");
    Map<String, Object> extraFields = ex.getExtraFields();
    assertThat(extraFields.size()).isEqualTo(3);
    assertThat(extraFields.get("isAdmin")).isEqualTo(TRUE);
    assertThat(extraFields.get("userId")).isEqualTo(12);
    assertThat(extraFields.get("userName")).isEqualTo("John Doe");
  }

  @Test(expected = NullPointerException.class)
  public void testExtraFields_nameNull() {
    new BadRequestException("").putExtraField(null, "value not null");
  }

  @Test
  public void testExtraFields_valueNull_allowed() {
    UnauthorizedException ex = new UnauthorizedException("");
    ex.putExtraField("isAdmin", (String) null);
    Map<String, Object> extraFields = ex.getExtraFields();
    assertThat(extraFields.size()).isEqualTo(1);
    assertThat(extraFields.get("isAdmin")).isNull();
  }

  @Test
  public void testExtraFields_overrideValue_keepLast() {
    UnauthorizedException ex = new UnauthorizedException("");
    ex.putExtraField("isAdmin", "YES");
    ex.putExtraField("isAdmin", TRUE);
    Map<String, Object> extraFields = ex.getExtraFields();
    assertThat(extraFields.size()).isEqualTo(1);
    assertThat(extraFields.get("isAdmin")).isEqualTo(TRUE);
  }

  @Test
  public void testExtraFields_ReservedNameDomain_forbidden() {
    assertExtraFields_ReservedName_forbidden("domain");
  }

  @Test
  public void testExtraFields_ReservedNameMessage_forbidden() {
    assertExtraFields_ReservedName_forbidden("message");
  }

  @Test
  public void testExtraFields_ReservedNameReason_forbidden() {
    assertExtraFields_ReservedName_forbidden("reason");
  }

  private void assertExtraFields_ReservedName_forbidden(String fieldName) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The field name '" + fieldName + "' is reserved");

    new ConflictException("Fails", "no extra " + fieldName).putExtraField(fieldName, "some other " + fieldName);
  }
}
