# Reading engine contract

This directory defines a small, platform-neutral Kotlin port for the existing Badcat UI.

It is intentionally only an interface and data-model boundary. It does not yet contain:
- a Legado implementation,
- source-rule execution,
- Android persistence or networking,
- a reader UI,
- or a buildable APK.

The next step is to pin a Legado upstream revision and implement an adapter against the reviewed app-layer APIs. Keep this boundary stable so the original UI can remain the host.
