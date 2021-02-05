[![Build Status](https://travis-ci.org/cloudendpoints/endpoints-java.svg?branch=master)](https://travis-ci.org/cloudendpoints/endpoints-java)
[![codecov](https://codecov.io/gh/cloudendpoints/endpoints-java/branch/master/graph/badge.svg)](https://codecov.io/gh/cloudendpoints/endpoints-java)

# Endpoints Java Framework

The Endpoints Java Framework aims to be a simple solution to assist in creation
of RESTful web APIs in Java. This repository provides several artifacts, all
in the `com.google.endpoints` group:

1.  `endpoints-framework`: The core framework, required for all applications
    building Endpoints apps.
2.  `endpoints-framework-guice`: An extension for configuring Endpoints using
    Guice.
3.  `endpoints-framework-tools`: Tools for generating discovery documents,
    Swagger documents, and client libraries.

The main documents for consuming Endpoints can be found at
https://cloud.google.com/endpoints/docs/frameworks/java

## Installing to local Maven

To install test versions to Maven for easier dependency management, simply run:

    gradle install

## Migrating from the legacy Endpoints framework

This release replaces the old `appengine-endpoints` artifact. You should replace
the dependency with the `endpoints-framework` artifact from the
`com.google.endpoints` group. In Maven, the new dependency looks like this:

    <dependency>
      <groupId>com.google.endpoints</groupId>
      <artifactId>endpoints-framework</artifactId>
      <version>2.2.0</version>
    </dependency>

In Gradle, the new dependency looks like this:

    compile group: 'com.google.endpoints', name: 'endpoints-framework', version: '2.0.14'

You also need to update your `web.xml`. Simply replace all instances of
`SystemServiceServlet` with `EndpointsServlet` and replace `/_ah/spi/*` with
`/_ah/api/*`. The new Endpoints configuration should look something like this:

    <servlet>
      <servlet-name>EndpointsServlet</servlet-name>
      <servlet-class>com.google.api.server.spi.EndpointsServlet</servlet-class>
      <init-param>
        <param-name>services</param-name>
        <param-value>com.example.Endpoint1,com.example.Endpoint2</param-value>
      </init-param>
      <init-param>
        <param-name>restricted</param-name>
        <param-value>false</param-value>
      </init-param>
    </servlet>
    <servlet-mapping>
      <servlet-name>EndpointsServlet</servlet-name>
      <url-pattern>/_ah/api/*</url-pattern>
    </servlet-mapping>

## Repackaging dependencies

The new version of the Endpoints framework does not repackage its dependencies
to hide them. If you run into dependency conflicts and need to do so, we
recommend using the Maven Shade plugin or Gradle Shadow plugin. Full
instructions on doing so are on the [wiki][1].

## Contributing

Your contributions are welcome. Please follow the [contributor
guidelines](/CONTRIBUTING.md).

[1]: https://github.com/cloudendpoints/endpoints-java/wiki/Vendoring-dependencies
