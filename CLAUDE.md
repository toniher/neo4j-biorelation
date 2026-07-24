# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Neo4j **unmanaged extension** (server plugin) that exposes biological graph queries over HTTP. The graph holds three biomedical datasets — NCBI taxonomy, Gene Ontology (GO), and UniProt proteins with their GO annotations and IntAct interactions — and the extension answers relationship questions across them (shortest distance/path, lowest common ancestor, leaf nodes, annotation lookups).

This branch (`neo4j5`) targets **Neo4j 5.x (built against 5.26 LTS) / Java 17+** (see `pom.xml` `neo4j.version`). The `master` branch is the legacy Neo4j 3.5.x build. Key Neo4j 5 API facts that are load-bearing here:

- `Label.label()` / `RelationshipType.withName()` (the 3.5 `DynamicLabel` / `DynamicRelationshipType` are gone).
- Reads/writes go through the `Transaction` object: `tx.findNode()`, `tx.getNodeById()`, `tx.execute()`, and `tx.commit()` (not `tx.success()`, and not `db.*`).
- Entities are **transaction-scoped**: a `Node` is only valid inside the transaction that produced it. Every endpoint therefore opens **one** `try (Transaction tx = db.beginTx())` spanning find → path-find → JSON build, then `tx.commit()`.
- Path finding needs an `EvaluationContext`: `GraphAlgoFactory.shortestPath(new BasicEvaluationContext(tx, db), expander, depth)`. That is why `shortestDistance`/`shortestPathNodes` take `(Transaction tx, GraphDatabaseService db)`.
- JAX-RS imports are **`javax.ws.rs`** even on Neo4j 5: the server ships `jakarta.ws.rs:jakarta.ws.rs-api:2.1.x`, whose Java package is still `javax.ws.rs` (the package rename to `jakarta.ws.rs` only happened in the 3.0 API).
- The DB is obtained via `@Context DatabaseManagementService dbms` → `dbms.database("neo4j")` (constant `DATABASE_NAME` in `BioRelation`); `@Context GraphDatabaseService` injection no longer works.
- `getId()` / `getNodeById(long)` still work but are deprecated-for-removal; a future major will require element-id (`String`) APIs.

## Build

```bash
mvn clean package          # produces target/neo4j-biorelation-<version>.jar (shaded)
```

The `maven-shade-plugin` bundles minimal-json and commons-lang3 into the jar; `org.neo4j:neo4j` and `jakarta.ws.rs-api` are `provided` (supplied by the server) and are NOT bundled.

Deploy: copy the shaded jar to Neo4j `plugins/`, add to `conf/neo4j.conf`:

```
server.unmanaged_extension_classes=cat.cau.neo4j.biorelation.rest=/biodb
```

All endpoints are then served under the `/biodb` prefix (e.g. `http://localhost:7474/biodb/distance/tax/9606/10114`). APOC (core, matching the Neo4j version) is needed for the data-population step. There is no test suite wired up (`mvn test` runs nothing meaningful — see README TODO "Make tests work"); `neo4j-harness` is declared as a test dependency for when tests are added.

## Architecture — the extension (`src/main/java/cat/cau/neo4j/biorelation/rest/`)

Three classes, strict separation of concerns:

- **`BioRelation.java`** — JAX-RS resource. Every public method is an HTTP endpoint (`@GET` + `@Path`). Owns transaction lifecycle and JSON response building. Endpoints dispatch on a `type` path param that is either `go` or `tax`, which selects the node label (`GO` vs `TAXID`), the lookup property (`acc` string vs `taxid` int), and downstream formatting.
- **`BioRelationFunction.java`** — graph algorithms. `shortestDistance`, `shortestPathNodes`, `getAllLeafNodes`, `getLinkedNodes`, `getRootGO`/`getBaseGO`, `calcDistanceNodes`. This is where Cypher/traversal logic lives.
- **`BioRelationHelper.java`** — pure data massaging: `ArrayList<Node>` → `JsonArray`/`JsonObject`, plus set operations (`nonRedundantArrayNodeList`, `ancestorArrayNodeList`, `minArrayNodeList`, `allElementsTheSame`).

Conventions that recur across endpoints:
- `type` = `"go"` or `"tax"` toggles label/property/proptype everywhere; keep the two branches in sync when editing.
- Multi-ID list endpoints (`/common/...`, `/rels/...`) split the path segment on `-` (`list.split("-",-1)`).
- **LCA** is computed by walking each node's shortest path to root, then intersecting paths (the nested-loop `detectMatch` block, duplicated in `getCommonGO` and `getCommonTax`).
- `acc2 == "root"` is a special sentinel: taxonomy root → `"1"`, GO root → looked up via `getRootGO`/`getBaseGO` per term_type.
- `/rels/{type}/{list}/{method}` supports methods `all` (default), `nonred`, `ancestor`, and a majority-vote fallback.

## Architecture — data population (`populate/`)

Bulk-loads the three datasets into a fresh Neo4j DB. This is a separate concern from the extension and runs against a **Dockerized/Singularity Neo4j with the daemon NOT running** (started with `tail -f /dev/null`), because offline bulk import requires a stopped database.

Flow:
1. Edit **`vars.sh`** — all source URLs, scratch paths, `DOCKERNAME`, and `DOWNIFEXISTS` (set to `1` to (re)download source files) live here. Every other script sources it.
2. Run **`dockerrun.sh`** (or **`singularityrun.sh`**) — copies `*.sh`/`*.py` into the container's scratch dir and execs **`steps.sh`** inside it.
3. **`steps.sh`** is the master pipeline: downloads GO / NCBI taxonomy / GOA / UniProt isoforms / IntAct, transforms each into import-format CSVs (TAB-delimited, `:ID`/`:START_ID`/`:END_ID`/`:TYPE` headers, `$` array delimiter), then runs one **`neo4j-admin database import full neo4j`** (Neo4j 5 syntax; `--skip-bad-relationships`, `--nodes=Label=file`).
4. **`indexes.sh`** — creates indexes/fulltext indexes **after** import (server running), using Neo4j 5 DDL: `CREATE INDEX … FOR (n:Label) ON (n.prop)` and `CREATE FULLTEXT INDEX … FOR ()-[r:TYPE]-() ON EACH [r.prop]`.
5. **`idmapping.sh`** — optional, APOC-based post-load of UniProt xref mappings (`apoc.periodic.iterate` / `apoc.load.csv`).

The Python scripts (`generateGOnodes.py`, `generateGOrels.py`, `prepareTaxNodesAndRels.py`, `mapGOidsInGOA.py`, `processIsoformsNames.py`, `filterIntactReduced.py`, `import_mi_ontology.py`) are single-purpose CSV transformers invoked by `steps.sh`; `populate/requirements.txt` pins their deps.

### Graph schema produced

- Node labels: `GO` (props `id`, `acc`, `term_type`, `name`, `definition`), `TAXID` (`id`, `taxid`, `rank`, `scientific_name`), `MOL` (proteins/isoforms: `id`, `name`, `type`), plus optional `XREF`.
- Relationships: GO hierarchy (`is_a`, `part_of`, …), taxonomy parent links, `has_taxon` (MOL→TAXID), `has_go` (MOL→GO, props `qualifier`/`ref`/`evidence`), `isoform_of` (MOL→MOL), `interacts_with` (MOL→MOL, from IntAct), `has_xref`.

The extension's queries assume exactly this schema — changing property names in the population scripts (e.g. `acc`, `taxid`, `term_type`, `scientific_name`) will break the Java endpoints and vice versa.

## Deployment images

- **`docker/centos/neo4j-install/`** — builds the biorelation image on a Java-17+ base (`eclipse-temurin:21-jdk`), installs Neo4j 5.x, clones+builds the `neo4j5` branch, drops the shaded jar + APOC (core) into `plugins/`. Versions are `ARG`s at the top of the `Dockerfile` (keep `NEO4J_BIORELATION_VERSION` in sync with `pom.xml` and `NEO4J_APOC_VERSION` matched to `NEO4J_VERSION`). The shaded jar means minimal-json no longer needs separate copying.
- **`singularity/`** — `Singularity` bootstraps from the Docker image above; `README.md` has the `singularity build` / `instance start` invocation; `neo4j.conf` (a minimal Neo4j 5 config using `server.*`/`db.*` keys) is bind-mounted in.
