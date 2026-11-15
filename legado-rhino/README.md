# Vendored Legado Rhino runtime

This Android library contains the Rhino runtime source files copied from:

- Upstream: https://github.com/LegadoTeam/legado
- Exact revision: `330e606c4d57e08adc056ff8ab7a4132c784da9`
- Upstream module: `modules/rhino`
- License: GNU General Public License v3.0 (see [LICENSE](../third_party/legado-rhino/LICENSE))

The Kotlin sources under `src/main/java` are vendored upstream files. They are kept in the `com.script` namespace and have not intentionally been modified. The dependency versions in this module match the upstream module's version catalog at the pinned revision.

## Important integration boundary

Adding this module makes the upstream Rhino engine available to the Android build; it does **not** yet enable Legado book-source JavaScript. The application must still supply a reviewed host adapter and the complete Legado-specific bindings/URL analysis semantics. Until that adapter is implemented and tested, the app's default `UnsupportedSourceScriptRuntime` remains fail-closed. Do not expose arbitrary Android or Java objects to scripts.

## License compliance

This is a GPL-3.0 component. Any distribution of a combined application must comply with GPL-3.0, including providing the corresponding source code and license notices as required. Review the upstream license and the project's distribution model before publishing binaries.
