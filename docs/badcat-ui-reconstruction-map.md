# Badcat UI reconstruction map

## Purpose and fidelity rule

The enhanced client must keep Badcat 1.5.0.5 as the host experience. Legado is an engine/data provider behind adapters; it must not dictate a new navigation model. Do not replace the original page hierarchy with generic development screens as the final UI.

The original APK remains an immutable, read-only reference. This document records resource/class evidence and visual requirements; it does not claim that the proprietary UI source code has been recovered or that any current screen has visual parity.

## Verified reference inventory

The original APK inspection records identify these key layouts:

| User-facing area | Original resource | Integration responsibility |
|---|---|---|
| Main shell and bottom navigation | `res/layout/activity_main.xml` | Keep the four-tab host navigation and its custom bottom toolbar |
| Home | `res/layout/fragment_home.xml` | Preserve home content structure and entry points |
| Search home | `res/layout/fragment_search.xml` | Keep search category tabs, search input, quick entries and navigation |
| Search input | `res/layout/activity_search.xml` | Retain the original search entry flow |
| Search results | `res/layout/activity_search_result.xml` | Keep filter/type control, keyword bar, source tabs and result list; replace data provider behind the view model |
| Book detail | `res/layout/activity_book_detail.xml` | Preserve detail presentation, favorite and “go read” actions |
| Chapter list | `res/layout/activity_chapter_lsit.xml`, `frag_chapterlist_catalog.xml`, `item_chapter_list.xml` | Adapt Legado chapters into the original catalog view |
| Favorites / shelf | `res/layout/activity_favorites.xml`, `activity_favorites_folder.xml` | Preserve shelf grouping, folders and book-card presentation |
| Reader | `res/layout/activity_base_read.xml` | Keep original reader host and its toolbar/drawer interaction where legally and technically possible |
| Reader settings | `res/layout/activity_read_setting.xml` | Map new engine settings without changing existing setting semantics |

Related class evidence includes `MainActivity`, `SearchActivity`, `SearchResultActivity`, `BookDetailActivity`, `ChapterListActivity`, `BaseReadActivity`, `GenuineBookReadView`, `LocalBookReadView` and `ReadConfig`. These names and resources are static evidence only; behavior must be verified against the reference screens and available code paths.

## Visual reference requirements

The four uploaded reference-screen descriptions establish these baselines:

1. **Reader:** warm beige background, dark large reading text, top reader toolbar, bottom chapter progress and catalog/source-switch/brightness/settings actions.
2. **Favorites:** white background, novel-only collection tab (no book-sticker tab), grouped shelf-card grid, bottom navigation for search/favorites/profile.
3. **Search home:** blue cat-themed header, novel/comic/all category switch, rounded search field, shortcut entries and four-item bottom navigation.
4. **Search results:** back action, type dropdown, keyword field, search button and multiple source tabs.

Treat exact colors, spacing, icon assets, typography and control states as values to measure from the actual reference images/resources before implementing them. Do not invent a “close enough” redesign and call it parity.

## Integration sequence

### UI shell first

1. Rebuild the main shell and bottom navigation.
2. Rebuild the search-home and search-results screens with fixture data.
3. Rebuild book detail and chapter catalog with fixture data.
4. Rebuild the favorites shelf and folder presentation.
5. Integrate the Legado adapter into the existing view models without moving controls or changing navigation.
6. Replace the temporary development launcher only after these screens have screenshot-based regression coverage.

### Reader compatibility

1. Keep online and local reader paths distinct until their data contracts are proven compatible.
2. Map Legado chapter content into the original reader's content model.
3. Preserve font size, line spacing, font, background, night/eye-care mode, brightness, orientation, auto-scroll, listening speed and page-turn preferences.
4. Keep chapter navigation, catalog drawer, source switching, progress and bookmarks in the original interaction flow.
5. Add new engine controls only as additive settings; do not overwrite existing preference keys.

### Regression gates

- Screenshot comparisons for all four reference screens and each major state (normal, selected, empty, loading, error).
- Navigation tests for home → search → results → detail → catalog → reader and favorites → reader.
- Reader tests for previous/next chapter, catalog drawer, progress restore, bookmarks, text settings and theme settings.
- Verify that unsupported Legado rule behavior is reported explicitly and does not crash or silently return misleading content.
- Test on a physical Android device before claiming runtime or visual parity.

## Current implementation status

- CI debug APK packaging: verified on GitHub Actions run #43707974288.
- Temporary Android launcher and development-only source screens: present.
- Original Badcat UI layouts/views wired into the new app: not yet implemented.
- Screenshot parity, original reader integration, and physical-device regression: not yet verified.
- Full Legado `AnalyzeRule` / `AnalyzeUrl` runtime: not yet implemented.

## Evidence and safety boundaries

- APK layout/class inventories and the four-screen visual description are recorded in the project research notes.
- Static class names, layout names and DEX strings do not prove full runtime semantics.
- Do not remove original native libraries or protection-related components based solely on names. Do not bypass signatures, integrity checks or access controls.
- Do not claim an APK can replace the installed original app. A separately signed test build needs an isolated package identity/device and must not overwrite user data.
- Preserve the original APK and research inputs unchanged.


## Modern implementation, same experience

The old application is not a code template. Rebuild its visible behavior with modern Kotlin/Android components while treating the screenshots and observed flows as acceptance criteria.

- Compose is preferred for new screen implementation and state-driven UI, with interoperability for existing Android Views/WebView where necessary.
- Preserve the original visual language rather than accepting default Material 3 colors, shapes, typography or spacing.
- Use ViewModel + StateFlow for screen state, repositories for data access and the existing engine adapter for Legado operations.
- Migrate existing SharedPreferences data deliberately before moving structured entities to Room or settings to DataStore.
- Keep rendering and UI independent from source parsing, network requests and replacement rules.
- Add screenshot tests for layout fidelity and UI tests for actions/navigation. A screen is not complete merely because it compiles.
- Reuse original assets only where their use is authorized and technically appropriate; otherwise recreate equivalent assets without changing their visual role.


## 2026-11-16 implementation update: first home-shell pass

Implemented in `MainActivity.kt`:
- Search content types are now `小说 / 漫画 / 综合`, and the selected type is passed into the existing source-search route.
- The home shortcut strip now has the original three roles and labels: `排行榜 / 今日抽奖 / 赚取现金`; they are deliberately presentation-only until their service/API contracts are verified. They do not launch unrelated development tools.
- Bottom navigation now uses the original four labels: `搜索 / 收藏夹 / 喵星 / 我的`.
- Favorites routes to the existing shelf. `我的` routes to a dedicated tools hub with working entries for source catalog, reading history and replacement rules.
- `喵星` has its own explicit destination shell. It currently states that the original service contract is not verified; no fabricated community feed is shown.
- The old home-page row of four development shortcuts was removed.

This is a structural first pass, not screenshot parity. The current home remains a programmatic Android View layout; a Compose migration is not yet implemented. Exact spacing, icon artwork, shortcut actions and the original bottom-navigation icons still require screenshot/device comparison and verified service behavior.


## 2026-11-16 follow-up: home simplification and search screen

User-directed home adjustments:
- Removed the entire home shortcut area (including the former ranking, lottery and cash-entry shortcuts).
- Bottom navigation now contains only `搜索 / 收藏夹 / 我的`; the former `喵星` destination is removed from the visible navigation.

Search-screen implementation:
- Replaced the development Spinner + utility-button layout with a user-facing hierarchy: back/type selector, rounded keyword field, horizontal source tabs, copyright disclaimer, result list and bottom pagination.
- Source tabs are populated from enabled Legado sources. “全部” searches all eligible sources; each individual tab filters to one source.
- Search still calls the existing `ReadingEngine.searchPage`, deduplicates by source ID + book URL, and opens the existing book-detail flow.
- Empty/loading/error states are explicit. No legacy channel names or service endpoints are fabricated; the tab labels reflect actual imported sources.
- The content-type selector cycles through `小说 / 漫画 / 综合` and retains the selected type in the screen state. Actual source-side content-type filtering is not yet implemented.

This remains an initial View-based reconstruction. Exact original visual matching, source-side type semantics, race-safe cancellation for overlapping searches, and physical-device verification remain open.


## 2026-11-16 follow-up: favorites and personal center

User-directed changes:
- Favorites no longer exposes a book-sticker/notes category. The collection is labeled `小说`; folder organization remains available.
- Removed the entire `猫咪福利社` section from the personal center.
- Renamed the personal-center entry from `书贴管理` to `书签管理`.
- Added a central bookmark manager backed by the same `badcat_reading_progress` preference keys used by the reader. It lists saved chapter positions, opens the corresponding chapter at the saved position, and can delete individual bookmarks.
- New reader bookmarks also persist book/chapter labels for the manager. Existing bookmarks without labels remain readable using a URL-derived chapter fallback.

The personal center currently shows only implemented routes: reading history, bookmark manager, source catalog and replacement rules. No fake download, messaging, sync, welfare or community actions are displayed. Visual fidelity to the screenshot and physical-device verification remain open.


## 2026-11-16 follow-up: bookmark metadata integrity

- Reader bookmark metadata now records the stable chapter title rather than reusing the transient status label. This prevents a later save after a bookmark jump from overwriting the chapter title with a status message.
- New bookmarks record a save timestamp; the manager sorts newest first and removes timestamp metadata when a bookmark is deleted.
- Existing bookmarks without timestamps remain visible and sort after timestamped entries; existing book/title fallbacks remain intact.
- The saved scroll offset continues to be restored after the chapter document finishes loading. This is code-path verification only; WebView scroll restoration still requires physical-device testing with long chapters and different display settings.


## 2026-11-16 follow-up: reader navigation shell

- Replaced the development reader's always-visible multi-row button cluster with a compact top bar (back, chapter title, tools menu), chapter status, WebView reading area, and bottom previous/progress/next navigation.
- Chapter progress seeks through the loaded table of contents. The tools menu retains font size, line spacing, background, CSS profiles, chapter catalog, save bookmark, and jump-to-bookmark actions.
- Existing source-engine loading, reading history, per-chapter scroll persistence, and bookmark position restoration remain in the reader activity.
- This is a structural first pass, not a pixel-perfect reproduction of the original reader; no device rendering or touch-accuracy validation has been performed yet.


## 2026-11-16 follow-up: reader position slider

- The bottom reader slider now reflects the current WebView scroll percentage within the loaded chapter, rather than the chapter's ordinal position in the TOC.
- Dragging the slider seeks within the current chapter; previous/next buttons and the chapter catalog remain responsible for chapter navigation.
- Progress is recalculated on WebView scroll and after document load/scroll restoration. Stored pixel scroll offsets remain the persistence source, avoiding a second conflicting progress store.
- Physical-device validation is still required for very long chapters, font/CSS changes, and WebView content-height recalculation.


## 2026-11-16 follow-up: book detail cover and introduction

- Book detail now reads the cover URL from the engine's `BookDetail.coverUrl` field and loads it off the main thread.
- Cover requests accept only HTTP(S), use bounded connect/read timeouts, reject declared or streamed payloads above 2 MiB, validate image dimensions, and decode a downsampled bitmap. Failed or unavailable covers retain a local placeholder.
- The introduction is collapsed to four lines by default and can be tapped to expand/collapse.
- The book detail/catalog screen is still a structural reconstruction. Catalog filtering/sorting, original screenshot parity, and physical-device verification remain open.
