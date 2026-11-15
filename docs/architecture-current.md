# 当前客户端结构图（实现状态）

> 本图按仓库当前实现整理。虚线表示尚未完成的原版能力追踪，不代表已迁移。

```mermaid
flowchart TB
    UI[Android 客户端 UI]
    MAIN[MainActivity]
    RULEUI[Legado 替换规则页]
    BOOKUI[书籍 / 目录 / 阅读页面]
    MAIN --> RULEUI
    MAIN --> BOOKUI

    subgraph ENGINE[书源与阅读引擎]
      SOURCE[书源模型与 JSON Codec]
      SEARCH[搜索解析]
      INFO[书籍详情解析]
      TOC[目录解析与翻页]
      CONTENT[章节内容与分页服务]
      CSS[CSS 规则解析器]
      COMBINE[规则组合处理]
      SOURCE --> SEARCH
      SOURCE --> INFO
      SOURCE --> TOC
      SOURCE --> CONTENT
      SEARCH --> CSS
      INFO --> CSS
      TOC --> CSS
      CONTENT --> CSS
      CSS --> COMBINE
    end

    subgraph RULES[Legado 替换规则子系统]
      CATALOG[本地规则目录 / JSON Codec]
      SELECT[范围与启用状态筛选]
      REPLACE[正文 / 标题替换]
      RHINO[受限 Rhino @js 执行器]
      CATALOG --> SELECT --> REPLACE --> RHINO
    end

    HTTP[HTTP 网关 / URL 模板解析 / 安全 URL 校验]
    BOOKUI --> ENGINE
    RULEUI --> CATALOG
    CONTENT --> REPLACE
    SEARCH --> HTTP
    INFO --> HTTP
    TOC --> HTTP
    CONTENT --> HTTP

    EVIDENCE[原版 APK 静态证据]
    PENDING[原版广告净化 / AdMark / PPRule 方法级语义待确认]
    EVIDENCE -.-> PENDING
```

## 已实现的主要路径

- Android 页面可进入书源阅读流程与 Legado 替换规则验证页面。
- 书源 JSON 编解码、受限 CSS 搜索/目录/章节解析及 HTTP 请求路径已建立。
- 替换规则包含本地 JSON 导入导出、范围筛选、字面量替换与受限 Rhino 执行入口。
- 搜索结果、目录章节链接和目录翻页链接统一限制为无凭据的 HTTP(S) URL；相对链接按响应 URL 解析。

## 当前明确未完成

- 完整 Legado AnalyzeRule 语法、XPath/JSONPath 全覆盖、完整书源 JS/网络请求运行时。
- 原版坏坏猫净化逻辑的真实方法级迁移（WebView 注入、AdMark 服务、PPRule 执行语义尚未验证）。
- 原版 APK 的 UI 逐页还原、真机全流程验收与最终发布 APK。
