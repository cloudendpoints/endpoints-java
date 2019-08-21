[![Build Status](https://api.travis-ci.org/AODocs/endpoints-java.svg?branch=master)](https://travis-ci.org/AODocs/endpoints-java)
[![codecov](https://codecov.io/gh/AODocs/endpoints-java/branch/master/graph/badge.svg)](https://codecov.io/gh/AODocs/endpoints-java)

# Endpoints Java Framework

The Endpoints Java Framework aims to be a simple solution to assist in creation
of RESTful web APIs in Java. This repository provides several artifacts, all
in the `com.aodocs.endpoints` group:

1.  `endpoints-framework`: The core framework, required for all applications
    building Endpoints apps.
2.  `endpoints-framework-all`: Same as above, but with repackaged dependencies.
3.  `endpoints-framework-guice`: An extension for configuring Endpoints using
    Guice.
4.  `endpoints-framework-tools`: Tools for generating discovery documents,
    Swagger documents, and client libraries.

The main documents for consuming Endpoints can be found at
https://cloud.google.com/endpoints/docs/frameworks/java

## Installing to local Maven

To install test versions to Maven for easier dependency management, simply run:

    gradle install
    
## Additions to the original project

These are the most notable additions to
[the original project by Google](https://github.com/cloudendpoints/endpoints-java), currently
inactive: 
- Allow adding [arbitrary data](https://github.com/AODocs/endpoints-java/pull/20) to generic errors
- [Improve errors](https://github.com/AODocs/endpoints-java/pull/30) on malformed JSON
- Generated Swagger spec is [compatible](https://github.com/AODocs/endpoints-java/pull/34) with 
[Cloud Endpoints Portal ](https://cloud.google.com/endpoints/docs/frameworks/dev-portal-overview)
([and](https://github.com/AODocs/endpoints-java/pull/38) 
[other](https://github.com/AODocs/endpoints-java/pull/36) 
[improvements](https://github.com/AODocs/endpoints-java/pull/37))

Check 
[closed PRs](https://github.com/AODocs/endpoints-java/pulls?q=is%3Apr+sort%3Aupdated-desc+is%3Aclosed)
for all additions.

## Contributing

Your contributions are welcome. Please follow the [contributor guidelines](/CONTRIBUTING.md).
