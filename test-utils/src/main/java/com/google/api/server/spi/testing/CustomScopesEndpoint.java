package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;

@Api(
    name = "customScopes",
    version = "v1",
    scopes = {
        "openid", //short scope with description
        "https://www.googleapis.com/auth/drive", //long scope with description
        "doesnotexist" //should not find a description
    })
public class CustomScopesEndpoint {
    @ApiMethod(scopes = "https://mail.google.com/")
    public Foo foo() {
        return null;
    }
    @ApiMethod(scopes = "cloud-platform")
    public Bar bar() {
        return null;
    }
}
