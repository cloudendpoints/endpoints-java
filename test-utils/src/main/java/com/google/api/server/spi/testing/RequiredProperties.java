package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class RequiredProperties {
    public String getUndefined() {
      return null;
    }
    @ApiResourceProperty
    public String apiResourceProperty_undefined() {
      return null;
    }
    @ApiResourceProperty(required = AnnotationBoolean.TRUE)
    public String apiResourceProperty_required() {
      return "";
    }
    @ApiResourceProperty(required = AnnotationBoolean.FALSE)
    public String apiResourceProperty_not_required() {
      return null;
    }
    @Nullable
    public String getNullable() {
      return null;
    }
    @Nonnull
    public String getNonnull() {
      return "";
    }
    @ApiResourceProperty(required = AnnotationBoolean.TRUE) @Nullable
    public String getPriority1() {
      return "";
    }
    @Nonnull @Nullable
    public String getPriority2() {
      return "";
    }
    @ApiResourceProperty(required = AnnotationBoolean.FALSE) @Nonnull
    public String getPriority3() {
      return null;
    }
  }