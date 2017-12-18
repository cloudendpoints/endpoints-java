package com.google.api.server.spi;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.response.UnauthorizedException;
import java.util.logging.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ServiceExceptionTest {
  @Test
  public void testWithLogLevel() {
    UnauthorizedException ex = new UnauthorizedException("");
    assertThat(ex.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(ServiceException.withLogLevel(ex, Level.WARNING).getLogLevel())
        .isEqualTo(Level.WARNING);
  }
}