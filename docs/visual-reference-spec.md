# Badcat visual reference baseline

The original screenshots are retained in the ChatGPT Library at:

`/坏坏猫客户端/界面参考图/`

| File | Purpose | Implementation notes |
|---|---|---|
| `01-首页参考.png` | Home screen composition | Blue brand header, centered Badcat wordmark, 小说 / 漫画 / 综合 selector directly below, wide rounded search field, white content surface, bottom navigation. |
| `02-书架封面状态标签参考.png` | Novel cover status | Small status chip pinned to the upper-right corner of the cover: 更新, 连载, 完结. |
| `03-搜索栏与搜索历史参考A.png` | Search field and history | Back affordance, content-type selector, rounded keyword field, clear-history action, compact rounded history chips with natural wrapping. |
| `04-搜索栏与搜索历史参考B.png` | Search field and expanded history layout | Search action aligned to the field; two-column history text and optional recommendation modules are visual references only. Do not fabricate AI search or hot-list data when no provider exists. |

## Behavior and data integrity

- Keep the home content-type selector immediately under the logo.
- The home search control and search-page control share the same persisted height preference.
- Search history is local, removable item-by-item by long-press, and clearable as a whole.
- Novel cover status must come from an explicit source `bookStatus` field. Normalize only recognizable values containing 更新, 连载, or 完结. Do not infer a completion state from the latest chapter title.
- Keep the original bottom-navigation icon artwork; animate the existing icon rather than replacing it with a different glyph.
- The screenshots are visual references, not evidence that the pictured remote services or recommendation feeds are implemented.
