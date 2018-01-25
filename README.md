# stillsuit

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

## Installation

Add this to project.clj, build.boot, or deps.edn:

```
[com.workframe/stillsuit "0.1.0-SNAPSHOT"]
```

## Usage

stillsuit takes as input a lacinia schema definition and a configuration map.
It will decorate the schema to add a few resolvers, queries, scalar transformers,
etc, and then optionally compile it for you.

### Using with catchpocket

stillsuit should be usable with a manually-written schema file, but you
can also use the catchpocket project to inspect your datomic database and
automatically generate large parts of a schema file for you.

In the ideal scenario, catchpocket can generate all of your object
definitions for you, leaving only the top-level queries for you to write.

### Logging

`stillsuit` uses `clojure.tools.logging` over slf4j for logging (this is
also what the datomic libraries use).

### Why "stillsuit"?

In Frank Herbert's _Dune_ novels, a stillsuit is used to retain precious
water in the harsh desert environment of Arrakis.
