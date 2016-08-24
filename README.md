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

## Installing to local maven

To install test versions to Maven for easier dependency management, simply run:

    gradle install

## Contributing

Your contributions are welcome. Please follow the [contributor
guidelines](/CONTRIBUTING.md).
