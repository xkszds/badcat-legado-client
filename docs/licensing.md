# Legado licensing and distribution checklist

Legado is published under GPL-3.0. Before distributing a combined client that incorporates Legado code or creates a derivative/combined work, review the exact pinned upstream revision and satisfy the applicable GPL-3.0 obligations.

## Required project controls

- Record the exact upstream repository URL, commit SHA, retrieval date and license file for every incorporated Legado component.
- Preserve copyright notices, license texts and modification notices.
- Identify modified files and describe material modifications.
- Provide the complete corresponding source code for the covered combined work in the manner required by GPL-3.0.
- Include required build/install scripts and interface definition materials where applicable.
- Ensure recipients receive the license and no additional restrictions inconsistent with GPL-3.0.
- Review distribution of app stores, direct APKs and source archives separately with qualified counsel if needed.

## Current state

The repository pins Legado revision `330e606c4d57e08adc056ff8ab7a4132c784da9`. The Android CI workflow downloads the pinned Rhino JAR and 12 Rhino extension source files (including XML implementation classes) from that upstream revision into the build workspace; those downloaded files are not currently committed as ordinary repository source files. The resulting debug APK therefore includes upstream Legado/Rhino components. The project also carries the upstream GPL-3.0 license text under `third_party/legado/LICENSE`.

Before distributing any combined APK, verify the exact license and notice obligations for every included upstream component and dependency, publish complete corresponding source for the distributed build (including the workflow-retrieved files and modifications), and ensure the source archive/build instructions reproduce the artifact. This document is a compliance gate, not a legal opinion.

## Release must be blocked if

- The upstream revision or its license cannot be identified.
- Required corresponding source cannot be supplied.
- Third-party assets or dependencies have incompatible or unreviewed terms.
- A release artifact is built from source that is not represented by the published source archive.
