# UI reference baseline (2026-11-17)

The four comparison screenshots are retained in the ChatGPT Library at `/坏坏猫客户端/界面参考图/`. The library README records each image and the comparison rules.

## Home
- Blue brand header with the Badcat wordmark.
- The content-type row is directly under the wordmark: 小说 / 漫画 / 综合.
- A white, fully rounded search field sits beneath the type row; its height and top spacing remain user-configurable.
- The reference shows three compact shortcut tiles below the header and a centered server-error/empty state with a blue retry action.
- Retry opens the dedicated search surface so source loading can be attempted again; the app must not fabricate a home feed when no real feed API/data exists.

## Book cover status
- Place a compact badge at the top-right of the cover.
- Allowed labels are 更新, 连载, 完结, only when explicitly provided by source metadata.
- The shared `BookStatusBadge` is used in search results and favorites; status/cover metadata is persisted with the favorite and refreshed from book detail.

## Search
- Keep the back affordance, content-type selector, keyword input and search action on one row.
- Search history appears immediately below the bar, with a heading and clear-history action.
- History entries are arranged in two columns, single-line ellipsized, tap to search and long-press to remove.
- Do not show a “no enabled sources” warning while the user is simply viewing search history.
- The fourth screenshot also contains an AI-search banner and hot-list cards. Those must remain absent until a real supported service and data contract are integrated; a visual imitation with fake results is not acceptable.

## Acceptance
Compare layout hierarchy, relative spacing, colors, rounded corners, badge placement and keyboard-safe behavior against the saved images. Code/CI validation is not equivalent to real-device visual acceptance.
