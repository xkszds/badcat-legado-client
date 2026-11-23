

#### Runtime-3.2f.47 user-pause enforcement — both four-source suites disabled

- The four-source pause applies to both the Android public-source E2E class `PublicLegadoSourcesEndToEndInstrumentedTest` and the separate hardcoded four-source uploaded-corpus Python smoke probe documented in .42.
- The Android class is excluded with the AndroidJUnitRunner `notClass` argument; the Python step and its report-upload step have now been removed from subsequent workflow runs. The Python script and historical reports remain untouched.
- Single-source unrelated smoke probes remain independent; no four-source descriptors, rules, fixtures, or source execution are changed.
