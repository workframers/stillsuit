# stillsuit

[![CircleCI](https://circleci.com/gh/workframers/stillsuit/tree/develop.svg?style=svg&circle-token=bdc3a82714767c0f8e0b7285d41fd6fffabe0d42)](https://circleci.com/gh/workframers/stillsuit/tree/develop)

stillsuit is a library that provides some facilities for
accessing [datomic](http://www.datomic.com/) databases
from the [lacinia](https://github.com/walmartlabs/lacinia)
GraphQL library.

It is designed to be used as a standalone library, but the
[catchpocket](https://github.com/workframers/catchpocket)
project can also be used in order to extract GraphQL
schema information from an existing datomic database.

For an example of using it, see the
[stillsuit-sample project](https://github.com/workframers/stillsuit-sample).

Note that because stillsuit relies heavily on the
[datomic Entity API](https://docs.datomic.com/on-prem/entities.html), it
only works with "On-Prem" datomic installations using the Peer libraries.
In particular, it is not currently usable with Datomic Cloud.

## Features

- Works on top of existing lacinia code and schemas
- Defines [scalar transformers](http://lacinia.readthedocs.io/en/latest/custom-scalars.html)
  for [datomic primitive types](https://docs.datomic.com/on-prem/schema.html#required-schema-attributes)
- Supports [GraphQL Enums](http://lacinia.readthedocs.io/en/latest/enums.html)
  for datomic attributes defined as keywords or
  [`:db/ident` enums](https://docs.datomic.com/on-prem/schema.html#enums)
- Defines lacinia [field resolver factories](http://lacinia.readthedocs.io/en/latest/resolve/attach.html#resolver-factories)
  to resolve datomic `:db.type/ref` references
- Defines a [default field resolver](http://lacinia.readthedocs.io/en/latest/resolve/overview.html#default-field-resolver)
  that translates between datomic `:namespaced/keywords` and lacinia `:graphql_identifiers`

The basic idea behind stillsuit is that you define a few top-level queries
that return datomic entities or lists of entities, set up your schema to
tell lacinia which `:db.type/ref` attributes point at which GraphQL types,
and stillsuit handles graph traversal and field lookup for you.

## Installation

Add this to project.clj, build.boot, or deps.edn:

```
[com.workframe/stillsuit "0.15.0"]
```

Note that stillsuit assumes that you have the datomic peer libraries in
your local maven repository.

## Documentation

The docs are sparse, but you can find a
[user manual](http://docs.workframe.com/stillsuit/current/manual/) and
[API documentation](http://docs.workframe.com/stillsuit/current/doc/) online.

There is a [slide deck here](http://docs.workframe.com/catchpocket/current/slides/)
discussion stillsuit and catchpocket.

## Usage

stillsuit takes as input a lacinia schema definition and a configuration map.
It will decorate the schema to add a few resolvers, queries, scalar transformers,
etc, and then optionally compile it for you.

### Using with catchpocket

stillsuit is usable with a manually-written schema file, but you
can also use the [catchpocket](https://github.com/workframers/catchpocket)
project to scan your datomic database and automatically generate large
parts of a schema file for you.

In the ideal scenario, catchpocket can generate all of your object
definitions for you, leaving only the top-level queries for you to write.

### Logging

`stillsuit` uses `clojure.tools.logging` over slf4j for logging (this is
also what the datomic libraries use).

### Why "stillsuit"?

In Frank Herbert's _Dune_ novels, a stillsuit is used to retain precious
water in the harsh desert environment of Arrakis.

# Contributing

Bug reports, feature ideas, and PRs are welcome! However, we might not be
a little slow to release merged PRs at times, depending on our own
release cycles.

We are especially looking for cases where stillsuit's assumptions about
a data-model are not correct.

# License

Copyright © 2018 Workframe, Inc.

Distributed under the Apache License, Version 2.0.

<!---
## TODO

- Investigate https://github.com/plexus/autodoc
-->
