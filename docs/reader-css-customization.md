# Reader CSS customization

The development reader exposes a local CSS profile editor from the reading controls. Profiles can be exported to a JSON file or imported through Android's document picker; imports are appended without overwriting existing profiles. Import files are limited to 8 MiB, 100 profiles, and 120 child selectors per profile, with bounded field lengths. The reader renderer collects at most 12,000 enabled child-selector rules across all profiles (the combined 100 × 120 profile/rule maximum) and caps emitted CSS at 1,000,000 characters. Stored profile JSON is limited to 8 MiB: the editor rejects any save or import that would exceed this aggregate limit, and the renderer independently rejects oversized stored data without modifying it. Rules are applied in profile order and then child-selector list order; invalid selectors or empty/blocked declarations are skipped and do not consume the render budget. Imported selectors are syntax-validated before any profile is saved. The editor enforces the same profile/rule counts and rejects malformed selectors before saving. Profiles are stored on-device in the app's private preferences and are independent of book-source rules.

## Document structure

Chapter text is escaped and rendered as local HTML with JavaScript, file access, and content access disabled. The stable reader elements are:

- `.reader-root`: reader content wrapper
- `.reader-content`: chapter content container
- `.reader-paragraph`: each paragraph split from the parsed chapter text

Examples:

```css
.reader-content {
  font-family: serif;
}

.reader-paragraph {
  text-indent: 2em;
  line-height: 1.9;
}

.reader-paragraph:nth-child(2) {
  color: #666666;
  font-size: 20px;
}
```

Comma-separated selector groups are supported. Each group is scoped independently to the reader root, so a rule such as `.reader-paragraph, .reader-content` cannot accidentally apply its second selector outside the reader. Commas inside bracket values or functional selectors are not treated as group separators.

Enter the selector in the selector field and declarations (without braces) in the multiline CSS field. Unbalanced parentheses/brackets, unclosed quotes, and empty comma-separated selector groups are rejected instead of being passed to the WebView. The editor includes a local preview button that renders sample paragraphs with the current selector and CSS declarations (unsaved changes are previewed without applying them to the chapter). The profile manager can preview all rules in a profile together, preserving their saved order, so interacting selectors and cascade precedence can be checked before returning to the reader. It also includes quick-insert presets for the reader root, content container, all paragraphs, first paragraph, second paragraph, and last paragraph. Each child selector can have its own display name and note. Rules can be duplicated as a starting point for a related selector, then renamed and edited independently. Copying a child selector respects the per-profile 120-rule limit. The editor validates selector syntax and bounds declaration size; unsupported properties and active/external CSS values are discarded by the sanitizer. Entire profiles can also be duplicated; the copy receives a new ID and is disabled by default so it cannot unexpectedly change the reading page. A built-in default reading-style profile can be added without replacing existing profiles. The conflict checker reports identical selectors across enabled profiles; it is a duplicate-selector warning, not a complete CSS cascade or semantic-overlap analyzer. Rules can be moved up or down; their saved order controls the order in which matching declarations are emitted, so later matching declarations normally win when specificity is equal. The `.reader-root` selector targets the outer reader wrapper directly.

## Safety and current limits

- Only a bounded selector character set and a CSS property allowlist are accepted.
- External URLs, JavaScript, imports, and active content are not permitted in custom CSS.
- The renderer currently styles the normalized chapter text, not the source site's original DOM. It therefore exposes reader-generated paragraph/container selectors rather than every original website element.
- Complex selectors and arbitrary CSS at-rules are not supported.
- This feature changes the development reader's content presentation only; it does not alter the original Badcat APK.


Imported profiles preserve an explicit Boolean enabled flag; if that field is absent, the profile defaults to disabled to avoid unexpectedly activating incomplete backup data. Import also rejects fields whose JSON types are incorrect instead of coercing numbers or text into names, selectors, CSS declarations, enabled states, or rule collections.
