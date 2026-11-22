# Required binary dependency

This Maven coordinate is the custom Legado fork, not the stock Maven Central artifact:

`org.htmlunit:htmlunit-core-js:5.3.0-legado.4`

Upstream source revision: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9`

Upstream artifact path:
`third_party/maven/org/htmlunit/htmlunit-core-js/5.3.0-legado.4/htmlunit-core-js-5.3.0-legado.4.jar`

The upstream POM metadata is preserved here. The exact upstream JAR has size **1,900,654 bytes** and Git blob SHA-1 `d240cdd66f5269ed33da53cde7e4e690cee6e162` at that revision. The binary JAR is not yet present in this repository because the available GitHub text-file integration cannot transfer binary blobs. Do not substitute stock `5.3.0`: Legado's Rhino source relies on fork-specific behavior/features. The Android build remains blocked until the exact upstream JAR is added and its SHA-256 is verified against the pinned upstream artifact.

