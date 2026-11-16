# Reader CSS customization

The development reader exposes a local CSS profile editor from the reading controls. Profiles are stored on-device in the app's private preferences and are independent of book-source rules.

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

Enter the selector in the selector field and declarations (without braces) in the CSS field. The editor includes a local preview button that renders sample paragraphs with the current selector and CSS declarations (unsaved changes are previewed without applying them to the chapter). The profile manager can preview all rules in a profile together, preserving their saved order, so interacting selectors and cascade precedence can be checked before returning to the reader. It also includes quick-insert presets for the reader root, content container, all paragraphs, first paragraph, second paragraph, and last paragraph. Each child selector can have its own display name and note. Rules can be moved up or down; their saved order controls the order in which matching declarations are emitted, so later matching declarations normally win when specificity is equal. The `.reader-root` selector targets the outer reader wrapper directly.

## Safety and current limits

- Only a bounded selector character set and a CSS property allowlist are accepted.
- External URLs, JavaScript, imports, and active content are not permitted in custom CSS.
- The renderer currently styles the normalized chapter text, not the source site's original DOM. It therefore exposes reader-generated paragraph/container selectors rather than every original website element.
- Complex selectors and arbitrary CSS at-rules are not supported.
- This feature changes the development reader's content presentation only; it does not alter the original Badcat APK.
