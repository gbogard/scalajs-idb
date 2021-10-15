# scalajs-idb, a statically-typed, idiomatic interface to IndexedDB

scalajs-idb is a type-safe, idiomatic interface to 
[IndexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API) for Scala 3
and [Scala.js](https://www.scala-js.org/), with optional integration with
[Cats Effect](https://typelevel.org/cats-effect/).

scalajs-idb provides a functional way to construct programs that rely on IndexedDB. Its
transaction system lets you write IndexedDB programs as pure descriptions and interpret them using
the "effect" of your choice, similarly to what [Doobie](https://tpolecat.github.io/doobie/) does 
with JDBC programs.

## Installation

```scala
libraryDependencies ++= Seq(
  "dev.guillaumebogard" %%% "scalajs-idb-core" % "0.1.0"
  // Optional
  "dev.guillaumebogard" %%% "scalajs-idb-cats-effect" % "0.1.0",
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

val championsStoreName = ObjectStore.Name("champions")

// A Schema describes what object stores and indicies should be
// created/deleted from your IndexedDB database
val schema = Schema().createObjectStore(championsStoreName)

// Transaction is a pure description of an IndexedDB program,
// nothing is executed at this point
val transaction: Transaction[Boolean] = 
  for {
    heroes <- Transaction.getObjectStore(championsStoreName)
    _ <- heroes.put("Illaoi": js.Any, Some(Key("favChampion")))
    illaoi <- heroes.get(Key("favChampion"))
  } yield illaoi == Some("Illaoi")

val res: Future[Boolean] = 
  Database
    .open[Future](Database.Name("LoL"), schema)
    .rethrow
    .flatMap(_.readWrite(NonEmptyList.of(championsStoreName))(transaction))
```

### Transactions

scalajs-idb models IndexedDB operations, such as putting and retrieving keys, as pure
descriptions. A value of type `Transaction[A]` does nothing on its own, it simply represents
a sequence of instructions that can be interpreted by a [backend](#Backends).

Programs can be sequenced to build larger programs:

```scala
val getUser =
  for {
    store <- Transaction.getObjectStore(ObjectStore.Name("users"))
    user  <- store.get(Key("john-doe"))
  } yield user

val getPreferences =
  for {
    store <- Transaction.getObjectStore(ObjectStore.Name("config"))
    pref  <- store.get(Key("userPreferences"))
  } yield pref

val getBoth =
  for {
    user <- getUser
    pref <- getPreferences 
  } yield (user, pref)
```

`Transaction[A]` is a [Monad](https://typelevel.org/cats/typeclasses/monad.html), meaning you can
advantage of existing combinators to build your programs such as `mapN`, `traverse`, etc.

### Backends

A `dev.guillaumebogard.api.Backend[F[_]]` allows you to interpret IndexedDB programs
within an effect `F[_]`. If you have an implicit `Backend[F]` in scope, then you can:

- Use `dev.guillaumebogard.api.Database.open[F]` to open a database. The method returns
a `F[Either[Throwable, Database]]`
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

// TODO

### Schema management

// TODO

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
- High-level bindings:
  - `ObjectStore`:
    - [ ] `add`
    - [x] `put`
    - [x] `get`
- [x] Future Backend
- [ ] Cats Effect IO Backend 
- Database schema management
  - [x] Declarative schema management
  - [x] Create object store
  - [ ] Delete object store
  - [ ] Create index
  - [ ] Delete index

- [x] Safe transactions using Free
- [ ] Get / Put type class
- [ ] Github actions
- [ ] Document first version
- [ ] Publish first version
- [ ] Add a license and a code of conduct
