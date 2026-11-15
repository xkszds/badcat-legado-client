# Third-party source attribution

## Legado Rhino runtime

- Project: Legado
- Repository: https://github.com/LegadoTeam/legado
- Revision: `330e606c4d57e08adc056ff8ab7a4132c784da9`
- Imported module: `modules/rhino`
- License: GPL-3.0-only / GNU General Public License version 3 (see `third_party/legado-rhino/LICENSE`)
- Local module: `legado-rhino/`

The runtime source is vendored from the pinned upstream revision. Its upstream copyright and source headers are retained where present. Local integration/build files are maintained by this repository.

## HtmlUnit Rhino dependency

- Artifact: `org.htmlunit:htmlunit-core-js:5.3.0-legado.4`
- Upstream packaging revision: `mgz0227/htmlunit-core-js@3eb5071cdca4a357119d1063a53d5f2d47984ed2`
- Upstream Rhino fork revision: `mgz0227/htmlunit-rhino-fork@76460c0312bfd351df6f2bb11168102cdb54170a`
- Licensing: MPL-2.0 for the Rhino-derived engine, with additional Apache-2.0 and third-party notices as recorded in `third_party/htmlunit-core-js-5.3.0-legado.4/`.
- Preserved records: `LICENSE.txt`, `LICENSE-core-js.txt`, `LICENSE-APACHE-2.0.txt`, `NOTICE.txt`, `NOTICE-tools.txt`, `SOURCE.md`, and `rhinoDiff.txt`.

The upstream Legado source files also contain original Oracle/Rhino file-level notices, including the stated GPL/classpath exception text. Those headers have been retained unchanged.
