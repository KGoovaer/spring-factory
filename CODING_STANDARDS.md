# Coding standards

Starter rules. Grow this file every time review catches something: each rule
should name the incident that created it.

## Deep modules, small public API

Behaviour goes in a package with a small public API (package-private
everything else), reachable through one entry point a test can call.
Controllers parse, delegate and map to a response; they hold no rules.

## Parse untrusted input at the edge

Request bodies get a validated DTO (`jakarta.validation`) before the values
reach a service. Parse once at the boundary; after that, trust the type.

## Fail with exceptions, not return codes

A service that cannot continue throws a domain exception. One
`@RestControllerAdvice` maps them to HTTP responses. Controllers have one
success path.

## Check permission in the service, not only the controller

Method-level authorization (`@PreAuthorize`) or an explicit check in the
service. A controller guard alone does not protect other callers.

## Tests go through the module under test

Arrange state through the module's public API, not by writing to repositories
directly. Prefer real objects and Testcontainers over mocks of your own code;
mock only at system boundaries.

## Money is an integer or BigDecimal, never a double
