# scalajs-idb, a statically-typed, idiomatic interface to IndexedDB (WIP)

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

### Roadmap:

- [ ] Low level bindings:
  - `IDBFactory`:
    - [ ] `open`
  - `IDBDatabase`:
    - [ ] `createObjectStore`
    - [ ] `transaction`
    - [ ] `close`
  - `IDBTransaction`:
    - [ ] `objectStore`
  - `IDBObjectStore`
    - [ ] `add`
- [ ] Future bindings
- [ ] Cats Effect IO bindings 
- [ ] Database schema management
- [ ] Safe resource acquisition/cleaning
- [ ] Safe transactions using Free
