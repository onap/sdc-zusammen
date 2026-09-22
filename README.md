# zusammen

This is an ONAP-maintained fork of five upstream Amdocs Zusammen Maven reactors, vendored
as sibling subdirectories under one aggregator `pom.xml` and republished under the
`org.onap.sdc.zusammen` / `org.onap.sdc.zusammen.plugin` coordinates. Zusammen is the collaborative
versioned-document store that SDC's catalog backend uses to persist and diff VSP/service
model content.

## Provenance

Each subdirectory is an unmodified import of the given upstream branch/commit; only the
poms were touched (groupId, version, and the build-plugin fixes described in each import
commit's message).

| Subdirectory | Upstream repo | Branch | Commit |
|---|---|---|---|
| `zusammen-commons` | `open-amdocs/zusammen-commons` | `release/1.0.3` | `c389e67603cac468f91031f84a58c1801dfe320a` |
| `zusammen` | `open-amdocs/zusammen` | `release/1.0.2` | `25617f1c95fe067bc6671e2f101ffcce1ba7f447` |
| `zusammen-metadata-cassandra` | `open-amdocs/zusammen-metadata-cassandra` | `release/1.0.3` | `55571888422a9f8a93548451b6030b9c44ecd8c8` |
| `zusammen-collaborative-cassandra` | `open-amdocs/zusammen-collaborative-cassandra` | `release/1.0.3` | `27c2fc62cd27cd52d679570c66c7311655143392` |
| `zusammen-search-elastic` | `open-amdocs/zusammen-search-elastic` | `release/1.0.0` | `71eb8be6c7c8734302cd5b794ddcd3753e669775` |

## Standing rule: do not rename the Java packages

All imported sources keep their upstream `com.amdocs.zusammen.*` package names. This is
deliberate and must not change: SDC and other consumers resolve classes by that package,
so renaming it would turn what should be a pure coordinate migration into a source rewrite
that every downstream consumer would have to follow. Only the Maven `groupId`/`artifactId`
coordinates move to `org.onap.sdc.zusammen[.plugin]`.

## Coordinates SDC consumes

SDC pulls exactly seven artifacts out of this fork:

- `org.onap.sdc.zusammen:zusammen-datatypes`
- `org.onap.sdc.zusammen:zusammen-adaptor-inbound-api`
- `org.onap.sdc.zusammen:zusammen-adaptor-inbound-impl`
- `org.onap.sdc.zusammen:zusammen-commons-utils`
- `org.onap.sdc.zusammen.plugin:zusammen-collaboration-cassandra-plugin`
- `org.onap.sdc.zusammen.plugin:zusammen-state-store-cassandra-plugin`
- `org.onap.sdc.zusammen.plugin:zusammen-search-index-empty-plugin`

## Build

Requires **JDK 11**. The reactor does not build on JDK 21: the vintage `mockito-all`
dependency and TestNG mass-skip and fail there.

`version.properties` at the repo root is the version source of truth; the LF stage job
stamps the release from it.

## Traps for the next person here

- The directory `zusammen-metadata-cassandra/` produces the artifact
  `zusammen-state-store-cassandra-plugin` — the directory name and the artifact name don't
  match, which can mislead a search into concluding the artifact is missing.
- `zusammen-search-index-elasticsearch-plugin` is present under `zusammen-search-elastic/`
  but deliberately commented out of that reactor's `<modules>` (it pulls Elasticsearch
  transport 5.1.1 and log4j-core 2.7, and nothing in ONAP uses it) — it is imported for
  future modernisation, not built or published.
- Upstream artifactIds are preserved verbatim, including generic ones — this is why ONAP
  ends up publishing an artifact literally called `amdocs-zusammen`.
