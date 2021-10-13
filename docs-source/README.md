# scalajs-idb, a statically-typed, idiomatic interface to IndexedDB (WIP)

Installation

```scala
libraryDependencies += "dev.guillaumebogard" %%% "scalajs-idb-core" % "@VERSION@"
```

### Roadmap:

- [ ] Low level bindings:
  - `IDBFactory`:
    - [x] `open`
  - `IDBDatabase`:
    - [x] `createObjectStore`
    - [x] `transaction`
    - [ ] `close`
  - `IDBTransaction`:
    - [x] `objectStore`
  - `IDBObjectStore`
    - [x] `add`
- [ ] Future bindings
- [ ] Cats Effect IO bindings 
- [ ] Database schema management
- [ ] Safe transactions using Free
