# Badcat 1.5.0.5 immutable UI/reference baseline

## Input integrity

- APK: `坏坏猫_1.5.0.5.apk`
- Size: 24,608,277 bytes
- SHA-256: `a62c16d99547854ff313f229649762f2d7898b4e2e0dc66574df4c43fa62480e`
- ZIP entries: 3,177
- DEX files: `classes.dex` through `classes7.dex` plus two DEX payloads in assets.
- Companion DEX archive: `com.hhm.verticalsearch.zip`
- Companion archive SHA-256: `f747fa1c684ef568e3da1d857dbebdcbb7687e2db4c58a4390282948f5681d10`

The original APK is a read-only reference. Do not patch, resign, overwrite or attempt to bypass its native/signature checks.

## Screenshot-backed UI behavior to preserve

- **Search home:** blue cat-themed header/logo, three-way novel/comic/combined selector, rounded keyword search bar, shortcut row and four-item bottom navigation.
- **Search results:** back control, content-type selector, keyword field and search action; source/provider tabs are shown horizontally below the search row.
- **Favorites:** top filter, centered works/posts tabs, overflow menu, folder/work cards in a three-column grid and the original bottom navigation.
- **Reader:** warm beige page background, dark large Chinese text, top navigation and page actions; bottom progress scrubber and four primary actions for catalog, source switching, brightness and settings.

These observations come from user-supplied screenshots captured from the original app. They are visual acceptance references, not a full interaction specification.

## High-priority original resource paths

- Search: `res/layout/fragment_search.xml`, `res/layout/activity_search_result.xml`, `res/layout/fragment_search_result.xml`, `res/layout/head_view_search.xml`.
- Detail/catalog: `res/layout/activity_book_detail.xml`, `res/layout/activity_chapter_lsit.xml`, `res/layout/frag_chapterlist_catalog.xml`, `res/layout/item_chapter_list.xml`.
- Favorites: `res/layout/activity_favorites.xml`, `res/layout/activity_favorites_folder.xml`, `res/layout/fragment_favorites_novel.xml`.
- Reader: `res/layout/activity_base_read.xml`, `res/layout/activity_read_setting.xml`.
- Purification: `res/layout/activity_ad_block.xml`, `res/layout/item_setting_ad_block.xml`.
- Visual assets: `res/mipmap-xxhdpi/homepage_logo.png`, `res/mipmap-xxhdpi/homepage_search_icon.png`, `res/drawable-xxhdpi/reader_navbar_purify.png`, `res/drawable-xxhdpi/reader_purify_nomal.png`.

The APK's layout XML entries are compiled Android binary XML. They must be decoded and reviewed before reuse; they are not ordinary text XML that can be copied directly into a source resource directory.

## Known code/resource clues

- Reading: `com.reader.reader.base.BaseReadActivity`, `GenuineBookReadView`, `LocalBookReadView`, `ReadConfig`.
- Search/detail: `SearchActivity`, `SearchResultActivity`, `BookDetailActivity`, `ChapterListActivity`.
- Shelf: `FavoritesFolderActivity`, `DbBookShelf`, `DbBookCache`, `DbTxtCache`, `DbComicCache`.
- Purification: `SettingAdBlockActivity`, `sens.Pp.PPRule`, `sens.Source.SourceItem`, `sens.AdMark.AdMarkRuleItem`, `ADBlockUtils`, `AdBlockPresenter`.

DEX symbols and resource names are static evidence only. They do not provide the original full source project, server protocols or proof of exact runtime behavior.

## UI migration acceptance gate

Before claiming the original UI is preserved:
1. Decode the relevant layouts and drawable/color/string dependencies.
2. Rebuild each page in the new project with the same navigation, control order, labels and major geometry.
3. Compare screenshots at the same device density and system bar configuration.
4. Verify all existing user actions and navigation paths.
5. Keep the original APK installable and its data untouched alongside the new app.
