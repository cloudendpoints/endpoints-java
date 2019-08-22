package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;


@Api(name = "specialChars", version = "v1")
public class SpecialCharsEndpoint {

  public static class Requestù {
  }
  
  public static class Responseµ {
  }
  
  //checks escaping of param reference
  @ApiMethod(path = "paramSpecialChar1")
  public Responseµ paramSpecialChar1(@Named("µ") @Nullable Integer µ, Requestù requestù) {
    return null;
  }

  @ApiMethod(path = "paramSpecialChar2")
  public Responseµ paramSpecialChar2(@Named("µ") @Nullable Integer µ) {
    return null;
  }
}
