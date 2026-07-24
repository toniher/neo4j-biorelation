# Data population scripts

Bulk-loads the three source datasets — **NCBI taxonomy**, **Gene Ontology (GO)**
and **UniProt** proteins (with GO annotations from GOA, splice isoforms, and
IntAct interactions) — into a fresh Neo4j 5 database, producing the graph schema
the `biodb` extension queries.

These scripts target **Neo4j 5.x** (developed against 5.26 LTS). For the legacy
Neo4j 3.5 loader, see the `master` branch.

## How it works

Offline bulk import (`neo4j-admin database import full`) requires the database
**daemon to be stopped**. The scripts therefore run inside a Neo4j container that
is started with `tail -f /dev/null` (so the image is present but the server is
NOT running), driving the import from the outside via `docker exec` /
`singularity exec`.

Pipeline:

1. **`vars.sh`** — all source URLs, scratch paths, container name and options.
   Every other script sources it. Edit this first.
2. **`dockerrun.sh`** (or **`singularityrun.sh`**) — copies the `*.sh` / `*.py`
   scripts into the container's scratch dir and runs `steps.sh` inside it.
3. **`steps.sh`** — the master pipeline: downloads each dataset, transforms it
   into neo4j-admin import CSVs (TAB-delimited, `:ID`/`:START_ID`/`:END_ID`/`:TYPE`
   headers, `$` array delimiter) and runs a single
   `neo4j-admin database import full neo4j`.
4. **`indexes.sh`** — creates indexes/full-text indexes. Run this **after** the
   import, with the Neo4j **server running** (it talks to `cypher-shell`).
5. **`idmapping.sh`** — *optional*, off by default. APOC-based post-load of
   UniProt xref mappings.
6. **`import_mi_ontology.py`** — *optional*, off by default. Loads the Molecular
   Interaction (MI) controlled vocabulary over Bolt with py2neo (needs
   `requirements.txt` installed).

## Prerequisites

* Docker or Singularity/Apptainer.
* A Neo4j 5.x image — pin the version to match the extension build (5.26):
  `neo4j:5.26`.
* Substantial free disk in the scratch directory. `goa_uniprot_all` alone is
  many gigabytes uncompressed.
* For the optional `import_mi_ontology.py` only: `pip install -r requirements.txt`.
  The core pipeline (`steps.sh`) uses only the Python standard library.

## Usage

1. Start a Neo4j container with the daemon **not** running. Mount a data volume
   and the scratch dir (the scratch mount must match `SCRATCHDIR` in `vars.sh`,
   and the name must match `DOCKERNAME`):

   ```bash
   docker run -d --name neo4jbio \
       -p 7474:7474 -p 7687:7687 -p 7473:7473 \
       -v /data/neo4j:/data \
       -v /scratch/neo4j:/scratch \
       neo4j:5.26 tail -f /dev/null
   ```

2. Adjust variables in **`vars.sh`**. In particular:
   * `DOWNIFEXISTS=1` to (re)download the source files; `0` reuses whatever is
     already present in the scratch dirs.
   * `DOCKERNAME` / `SCRATCHDIR` must match the container above.
   * Source URLs use `https://` (NCBI/EBI/UniProt); adjust if you mirror them.

3. Run the import:

   ```bash
   ./dockerrun.sh          # or: ./singularityrun.sh
   ```

   `steps.sh` writes its intermediate CSVs under the scratch `moment` dir and
   finishes with the `neo4j-admin database import full neo4j` step. The import is
   run with `--overwrite-destination=true`, so it can be re-run over an existing
   `neo4j` database.

4. Start the Neo4j server (e.g. `docker exec neo4jbio neo4j start`, or restart
   the container without the `tail -f` override), then create the indexes:

   ```bash
   docker exec neo4jbio /scratch/indexes.sh
   ```

5. *(Optional)* run `idmapping.sh` and/or `import_mi_ontology.py` for xref and MI
   data.

## Notes

* The source URLs point at the *latest*/*current* releases, so a load is not
  reproducible across time — record the download date if provenance matters.
* Errors from `cypher-shell` in `indexes.sh` / `idmapping.sh` are appended to
  `$MOMENTDIR/syn.err`; check it if indexes look missing.

## Graph schema produced

* **Node labels**
  * `GO` — `id`, `acc`, `term_type`, `name`, `definition`
  * `TAXID` — `id`, `taxid`, `rank`, `scientific_name`, `name[]`
  * `MOL` — proteins/isoforms: `id`, `name`, `type`
  * `XREF` — *(optional, via `idmapping.sh`)* `id`, `source`
  * `MI` — *(optional, via `import_mi_ontology.py`)*
* **Relationships**
  * GO hierarchy: `is_a`, `part_of`, … (GO→GO)
  * `has_parent` (TAXID→TAXID)
  * `has_taxon` (MOL→TAXID)
  * `has_go` (MOL→GO; props `qualifier`, `ref`, `evidence`)
  * `isoform_of` (MOL→MOL)
  * `interacts_with` (MOL→MOL, from IntAct; props `method`, `intype`,
    `confidence`, `update`, `source`)
  * `has_xref` (MOL→XREF, optional)

Changing property names here will break the Java endpoints, which assume exactly
this schema.
