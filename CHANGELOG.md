# Changelog
All notable changes to this project will be documented in this file.

## Unreleased:

### Changed

- Codec and ObjectCodec have been replaced with more specialized type classes: Encoder, ObjectEncoder
and Decoder.
- Instances of ObjectEncoder is no longer automatically provided for subtypes of scalajs.js.Object, they have
to be manually declared.

### Added

- Added support for the `IDBObjectStore.delete` operation
- Added instances of Encoder and Decoder for String, Boolean, numeric types and some collections
- Added instances of Encoder and Decoder for Option
- Added instances of Encoder and Decoder for java.time.Instant and java.time.LocalDate in a new module: scalajs-idb-java-time
- Instances of ObjectEncoder and Decoder can be derived automatically
- Encoder and ObjectEncoder are contravariant functors (they have a cats.Contravariant instance)
- Decoder is a functor (it has a cats.Functor instance)
- Added two new type classes: ObjectKeyEncoder and ObjectKeyDecoder
- Added instances of Encoder and Decoder for maps

## v0.1.0

The very first release of scalajs-idb

### Added

- Opening a database
- Defining an object store
- Transactions supporting the following operations:
  - get
  - getAll
  - put
  - add
- Two backends for interpreting transactions:
  - scala.concurrent.Future
  - cats.effect.IO
- The Codec and ObjectCodec type classes, with instances for all subtypes of js.Object and js.Any respectively.

    
