# scalajs-idb, a statically-typed, idiomatic interface to IndexedDB

![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/dev.guillaumebogard/scalajs-idb-core_sjs1_3?color=brightgreen&label=version&server=https%3A%2F%2Fs01.oss.sonatype.org)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dev.guillaumebogard/scalajs-idb-core_sjs1_3?label=latest%20snapshot&server=https%3A%2F%2Fs01.oss.sonatype.org)
![License Apache 2.0](https://img.shields.io/badge/license-Apache.2.0-blue)
![Cats Friendly](https://img.shields.io/badge/-Cats%20Friendly-red)

scalajs-idb is a type-safe, idiomatic interface to 
[IndexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API) for Scala 3
and [Scala.js](https://www.scala-js.org/), with optional integration with
[Cats Effect](https://typelevel.org/cats-effect/).

This library aims at being less imperative than the original IndexedDB API: transactions and
database schemas in scalajs-idb are managed declaratively through simple data structures.

scalajs-idb provides a functional way to construct programs that rely on IndexedDB. Its
transaction system lets you write IndexedDB programs as pure descriptions and interpret them using
the "effect" of your choice, similarly to what [Doobie](https://tpolecat.github.io/doobie/) does 
with JDBC programs.

The schema system lets you manage object stores and indices without ever managing versions manually.

**This is still an early-stage project: some features of IndexedDB are still not supported and
breaking changes will be regularly introduced.**

## Installation

```scala
libraryDependencies ++= Seq(
  "dev.guillaumebogard" %%% "scalajs-idb-core" % "0.2.0"
  // Optional
  "dev.guillaumebogard" %%% "scalajs-idb-cats-effect" % "0.2.0",
  "dev.guillaumebogard" %%% "scalajs-idb-java-time" % "0.2.0"
)
```

## Usage

### Quick start with scala.concurrent.Future

```scala
// Import scalajs-idb
import dev.guillaumebogard.idb.api.*
import scala.concurrent.{Future, ExecutionContext}
import cats.implicits.given
import cats.data.NonEmptyList
import scalajs.js

given ec: ExecutionContext = ExecutionContext.global

// Define your data
case class Champion(val name: String, val position: String) derives ObjectEncoder, Decoder

val illaoi = Champion("Illaoi", "top")

// Define a store for your data type
// In this case, we're using "out-of line keys", 
// meaning we'll have to provide them explicitly
val champions = ObjectStore[Champion]("champions")

// A Schema describes what object stores and indicies should be
// created/deleted from your IndexedDB database
val schema = Schema().createObjectStore(championsStore)

// A Transaction is a pure description of an IndexedDB program,
// nothing is executed at this point
val transaction: Transaction[Boolean] = 
  for {
    key <- champions.put(illaoi, "favChampion".toKey)
    result <- heroes.get(key)
  } yield result == Some(illaoi)

// Once the database is open,
// `transact` can execute a Transaction[Boolean] and turn it into a Future[Boolean]
val res: Future[Boolean] = 
  Database
    .open[Future](Database.Name("LoL"), schema)
    .rethrow
    // Run a read-write transaction against our champions store
    .flatMap(_.readWrite(NonEmptyList.of(champions.name))(transaction))
```

### Transactions

scalajs-idb models IndexedDB operations, such as putting and retrieving keys, as pure
descriptions. A value of type `Transaction[A]` does nothing on its own, it simply represents
a sequence of instructions that can be interpreted by a [backend](#Backends).

Programs can be sequenced to build larger programs:

```scala
val users = ObjectStore[User]("users")
val userPreferences = ObjectStore[Preference]("config")

val getUser = users.get("john-doe".toKey)
val getPreferences = config.userPreferences.get("john-doe".toKey)

val getBoth =
  for {
    user <- getUser
    pref <- getPreferences 
  } yield (user, pref)
```

`Transaction[A]` is a [Monad](https://typelevel.org/cats/typeclasses/monad.html), meaning you can take
advantage of existing combinators to build your programs such as `mapN`, `traverse`, etc.

### Backends

A `dev.guillaumebogard.api.Backend[F[_]]` allows you to interpret IndexedDB programs
within an effect `F[_]`. If you have an implicit `Backend[F]` in scope, then you can:

- Use `dev.guillaumebogard.api.Database.open[F]` to open a database. The method returns
a `F[Database]`
- Use `Database#readOnly`, `Database#readWrite` or `Database.transact` to turn a `Transaction[A]`
into a `F[A]`

#### Future backend (included in `scalajs-idb-core`)

The only thing you need to be able to run `Database.open[scala.concurrent.Future]` is
an implicit `ExecutionContext` in the scope:

```scala
import dev.guillaumebogard.idb.api.*
import scala.concurrent.{Future, ExecutionContext}

given ec: ExecutionContext = ExecutionContext.global

val database: Future[Database] = 
  Database.open[Future](Database.Name("LoL"), schema)
```

#### Cats Effect backend (as part of `scalajs-idb-cats-effect`)

This library also provides a backend for any type `F[_]` with an implicit `cats.effect.Async[F]`.
This includes `cats.effect.IO`. All you have to do is import `dev.guillaumebogard.idb.cats.given`.

### Schema management

scalajs-idb lets you manage object stores and indices without manually managing database versions, using a declarative schema.

In the Javascript API, IndexedDB requires you to provide a version number when you *open* a database. The version number you provide
is checked against the last encountered version on the user's device to determine whether a schema upgrade is necessary. If the version you
provide is greater than what is stored on the device, a `upgradeneeded` event is fired, giving you a unique opportunity to create or delete
object stores and indices.

In scalajs-idb, when you open a database, instead of providing a version number, you provide a `dev.guillaumebogard.idb.api.Schema`, which is
an append-only list of schema operations that describe what your database should look like. Internally, scalajs-idb will increment the database's
version number for every operation in the schema, and automatically execute operations that have not yet been executed on the user's device.

Here's an example:

```scala
val schema = 
  Schema()                                 // Empty schema  - v1
    .createObjectStore(preferencesStores)  // Added a store - v2
    .createObjectStore(usersStores)        // Added a store - v3

Database.open[Future](Database.Name("my-db"), schema)
```
When you ask scalajs-idb to open a database with this schema, it will internally open an
IndexedDB database with version number 3, and handle the creation of your object stores in the `upgradeneeded`
callback automatically. When the `Future` resolves, the obtained database already has the required stores.

??????  **There is one very important rule with schemas: never delete an operation or swap operations. If you need to delete an object store
for example, don't remove the `createOperationStore` operation from the schema; instead, append a `deleteObjectStore` operation.**

### Encoders and Decoders

This library uses type classes to (de-)serialize Scala types into JS types that can be stored in an `ObjectStore`.
Each `ObjectStore` is associated with a type of values, and certain operations require you to an instance 
of `Encoder`, `ObjectEncoder` or `Decoder` for that type, depending on the operation.

To decide whether to use `Encoder` or `ObjectEncoder` you need to determine if you will be using *inline keys* or *out-of-line keys*:

- If your object stores uses *out-of-line keys* it means that you will be providing an explicit key with each write operation.
You can insert any value in it as long your value's type implements `Encoder`.
- If your object stores uses *inline keys*  it means the key associated with a given value can be derived from the value itself by
following a specific path inside the object.
You can insert any value in it as long your value's type implements `ObjectEncoder`.

This library provides instances for the most common Scala data types. Maps of type `Map[K, V]` can be encoded as long
as `K` implements `ObjectKeyEncoder` and `V` implements encoder.

A separate module provides bindings for the Java time API.

## License and code of conduct

This library is maintained by [@gbogard](https://github.com/gbogard) and licensed under the [Apache 2.0 License](../LICENSE). 
You are expected to follow the [Scala Code of Conduct](https://www.scala-lang.org/conduct/) when discussing the library.

## To-do list:

- Low level bindings:
  - `IDBFactory`:
    - [x] `open`
  - `IDBDatabase`:
    - [x] `createObjectStore`
    - [x] `transaction`
  - `IDBTransaction`:
    - [x] `objectStore`
  - `IDBObjectStore`
    - [x] `add`
    - [x] `put`
    - [x] `getAll`
    - [x] `delete`
- High-level bindings:
  - `ObjectStore`:
    - [x] `add`
    - [x] `put`
    - [x] `get`
    - [x] `getAll`
    - [x] `delete`
    - [ ] `openCursor`
- [x] Future Backend
- [x] Cats Effect IO Backend 
- Database schema management
  - [x] Declarative schema management
  - [x] Create object store
  - [x] Create object store with options
  - [ ] Delete object store
  - [ ] Create index
  - [ ] Delete index

- [x] Safe transactions using Free
- [x] Encoder / Decoder type class
- [ ] Github actions
- [ ] Add more examples
- [x] Document first version
- [x] Publish first version
- [x] Add a license and a code of conduct
