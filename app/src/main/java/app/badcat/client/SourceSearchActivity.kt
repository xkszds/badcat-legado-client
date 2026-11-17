package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.badcat.client.runtime.BadcatReadingEngineFactory
import app.badcat.engine.BookCandidate
import app.badcat.engine.BookSource
import app.badcat.engine.EngineErrorCode
import app.badcat.engine.EngineResult
import app.badcat.engine.ReadingEngine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

/**
 * User-facing search screen around the existing Legado ReadingEngine.
 * The original search page's hierarchy is retained: back/type row, keyword
 * field, source/channel strip, disclaimer, then results/empty state.
 */
class SourceSearchActivity : Activity() {
    companion object {
        const val EXTRA_INITIAL_QUERY = "initial_query"
        const val EXTRA_CONTENT_TYPE = "content_type"
        const val EXTRA_FOCUS_SEARCH = "focus_search"
        private const val SEARCH_HISTORY_PREFS = "badcat_search_history"
        private const val SEARCH_HISTORY_KEY = "queries_v1"
        private const val MAX_SEARCH_HISTORY = 20
    }

    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val blue = Color.rgb(55, 137, 222)
    private val darkMode by lazy { getSharedPreferences("badcat_appearance", MODE_PRIVATE).getString("appearance_mode", "light") == "dark" }
    private val ink get() = if (darkMode) Color.rgb(235, 237, 240) else Color.rgb(48, 55, 66)
    private val muted get() = if (darkMode) Color.rgb(165, 172, 182) else Color.rgb(135, 145, 158)
    private val page get() = if (darkMode) Color.rgb(25, 28, 33) else Color.rgb(247, 249, 252)
    private val card get() = if (darkMode) Color.rgb(38, 42, 49) else Color.WHITE
    private lateinit var engine: ReadingEngine
    private lateinit var keywordInput: EditText
    private lateinit var searchButton: TextView
    private lateinit var previousPageButton: TextView
    private lateinit var nextPageButton: TextView
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var historyPanel: LinearLayout
    private lateinit var rootView: LinearLayout
    private var appliedDarkMode = false
    private lateinit var sourceTabs: LinearLayout
    private lateinit var sourceStripView: View
    private lateinit var disclaimerView: View
    private lateinit var paginationView: View
    private var currentPage = 1
    private var selectedSourcePosition = 0
    private var contentType = "小说"
    private var enabledSources: List<BookSource> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appliedDarkMode = darkMode
        BadcatTheme.applyWindow(this)
        engine = BadcatReadingEngineFactory(applicationContext).create()
        contentType = intent.getStringExtra(EXTRA_CONTENT_TYPE)
            ?.takeIf { it in listOf("小说", "漫画", "综合") } ?: "小说"
        status = TextView(this).apply {
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 0, 0, dp(10))
            visibility = View.GONE
        }
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
        }
        rootView.addView(buildSearchControls())

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(LinearLayout(this@SourceSearchActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(24))
                historyPanel = LinearLayout(this@SourceSearchActivity).apply {
                    orientation = LinearLayout.VERTICAL
                }
                // History is the first content below the search field. Source
                // filters and notices stay available, but never push history down.
                addView(historyPanel)
                sourceStripView = buildSourceStrip().apply { visibility = View.GONE }
                disclaimerView = buildDisclaimer().apply { visibility = View.GONE }
                addView(sourceStripView)
                addView(disclaimerView)
                addView(status)
                addView(results)
            })
        }
        rootView.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        paginationView = buildPagination().apply { visibility = View.GONE }
        rootView.addView(paginationView)
        setContentView(rootView)
        renderSearchHistory()
        BadcatTheme.applyTree(this, rootView)
        loadSources()
        if (intent.getBooleanExtra(EXTRA_FOCUS_SEARCH, false)) {
            keywordInput.requestFocus()
            keywordInput.post {
                val inputManager = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                inputManager.showSoftInput(keywordInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    /** Search is the first element below the status bar; no empty title toolbar above it. */
    private fun buildSearchControls(): View {
        val appearance = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getString("home_layout_mode", "integrated") ?: "integrated"
        val sharedBar = BadcatSearchBarView(
            context = this,
            initialQuery = intent.getStringExtra(EXTRA_INITIAL_QUERY).orEmpty(),
            initialType = contentType,
            appearance = appearance,
            dark = darkMode,
            onSearch = { query, type ->
                contentType = type
                keywordInput.setText(query)
                search()
            },
            onTypeChanged = { selected ->
                contentType = selected
                if (::keywordInput.isInitialized && keywordInput.text.toString().isNotBlank()) search()
            },
            searchPageStyle = true,
        )
        keywordInput = sharedBar.input
        searchButton = sharedBar.searchAction
        val configuredBarHeight = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getInt("home_search_bar_height_dp", 40).coerceIn(34, 46)
        return FrameLayout(this).apply {
            setBackgroundColor(page)
            val glow = View(this@SourceSearchActivity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (darkMode) Color.rgb(43, 91, 143) else Color.rgb(185, 218, 255))
                }
                alpha = if (darkMode) 0.22f else 0.42f
                if (Build.VERSION.SDK_INT >= 31) {
                    setRenderEffect(RenderEffect.createBlurEffect(
                        dp(22).toFloat(), dp(22).toFloat(), Shader.TileMode.CLAMP
                    ))
                }
            }
            addView(glow, FrameLayout.LayoutParams(dp(190), dp(48), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
            val row = LinearLayout(this@SourceSearchActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(6), dp(14), dp(6))
                addView(TextView(this@SourceSearchActivity).apply {
                    text = "<"
                    textSize = 23f
                    gravity = Gravity.CENTER
                    setTextColor(ink)
                    isClickable = true
                    isFocusable = true
                    contentDescription = "返回"
                    setOnClickListener { finish() }
                }, LinearLayout.LayoutParams(dp(32), dp(configuredBarHeight)))
                addView(sharedBar, LinearLayout.LayoutParams(0, dp(configuredBarHeight), 1f))
            }
            val topRow = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(configuredBarHeight + 20), Gravity.TOP
            )
            row.setPadding(dp(14), dp(10), dp(14), dp(10))
            addView(row, topRow)
        }
    }

    private fun buildSourceStrip(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(card)
        val scroll = HorizontalScrollView(this@SourceSearchActivity).apply {
            isHorizontalScrollBarEnabled = false
            sourceTabs = LinearLayout(this@SourceSearchActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(4), dp(12), dp(10))
            }
            addView(sourceTabs)
        }
        addView(scroll)
    }

    private fun buildDisclaimer(): View = TextView(this).apply {
        text = "免责声明：搜索结果来自已导入并启用的书源，请确认内容来源及版权。"
        textSize = 11f
        setTextColor(muted)
        setPadding(dp(16), dp(10), dp(16), dp(10))
        setBackgroundColor(if (darkMode) Color.rgb(32, 36, 43) else Color.rgb(250, 250, 250))
    }

    private fun buildPagination(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(8), dp(16), dp(8))
        setBackgroundColor(card)
        previousPageButton = pageButton("上一页") {
            if (currentPage > 1) {
                currentPage--
                search(resetPage = false)
            }
        }
        nextPageButton = pageButton("下一页") {
            currentPage++
            search(resetPage = false)
        }
        addView(previousPageButton, LinearLayout.LayoutParams(0, dp(40), 1f).apply {
            marginEnd = dp(8)
        })
        addView(nextPageButton, LinearLayout.LayoutParams(0, dp(40), 1f))
        previousPageButton.isEnabled = false
        nextPageButton.isEnabled = false
    }

    private fun pageButton(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 13f
        gravity = Gravity.CENTER
        setTextColor(blue)
        background = rounded(if (darkMode) Color.rgb(44, 57, 72) else Color.rgb(241, 247, 253), 18)
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }



    private fun loadSources() {
        searchButton.isEnabled = false
        status.text = "正在读取已启用书源…"
        worker.execute {
            val result = runBlocking {
                try {
                    engine.sources()
                } catch (error: Exception) {
                    EngineResult.Failure(
                        EngineErrorCode.UNKNOWN,
                        error.message ?: "读取书源时发生异常",
                        error,
                    )
                }
            }
            mainHandler.post {
                when (result) {
                    is EngineResult.Success -> {
                        enabledSources = result.value.filter {
                            it.enabled && !it.searchUrl.isNullOrBlank()
                        }
                        renderSourceTabs()
                        BadcatTheme.applyTree(this@SourceSearchActivity, rootView)
                        // Do not inject source-management notices into the search-history view.
                        status.text = ""
                        status.visibility = View.GONE
                        searchButton.isEnabled = true
                        if (keywordInput.text.toString().isNotBlank() && enabledSources.isNotEmpty()) {
                            search()
                        }
                    }
                    is EngineResult.Failure -> showServerError("服务器异常：${result.message}") { loadSources() }
                }
            }
        }
    }

    private fun renderSourceTabs() {
        sourceTabs.removeAllViews()
        val labels = enabledSources.map { it.name }
        labels.forEachIndexed { index, label ->
            val selected = selectedSourcePosition == index + 1
            sourceTabs.addView(TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(if (selected) Color.WHITE else muted)
                background = rounded(
                    if (selected) blue else if (darkMode) Color.rgb(49, 55, 65) else Color.rgb(246, 247, 249), 18
                )
                setPadding(dp(15), dp(8), dp(15), dp(8))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedSourcePosition = index + 1
                    currentPage = 1
                    renderSourceTabs()
                    if (::keywordInput.isInitialized && keywordInput.text.toString().isNotBlank()) {
                        search()
                    } else {
                        status.text = "已选择书源：$label"
                    }
                }
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)
            ).apply { marginEnd = dp(8) })
        }
    }

    private fun search(resetPage: Boolean = true) {
        if (resetPage) currentPage = 1
        previousPageButton.isEnabled = false
        nextPageButton.isEnabled = false
        if (contentType != "小说") {
            historyPanel.visibility = View.GONE
            sourceStripView.visibility = View.GONE
            disclaimerView.visibility = View.GONE
            paginationView.visibility = View.GONE
            status.visibility = View.VISIBLE
            results.removeAllViews()
            status.text = if (contentType == "漫画") {
                "漫画类型的专用书源解析尚未接入。当前不会把小说书源结果误显示为漫画。"
            } else {
                "综合搜索聚合尚未接入。当前只支持已导入的 Legado 小说书源搜索。"
            }
            return
        }
        val requestedPage = currentPage
        val query = keywordInput.text.toString().trim()
        // Empty input means the user is browsing search history, not attempting
        // a source lookup. Keep source availability warnings out of this state.
        if (query.isEmpty()) {
            status.text = ""
            status.visibility = View.GONE
            historyPanel.visibility = View.VISIBLE
            sourceStripView.visibility = View.GONE
            disclaimerView.visibility = View.GONE
            paginationView.visibility = View.GONE
            results.removeAllViews()
            renderSearchHistory()
            return
        }
        val selectedSources = if (selectedSourcePosition == 0) enabledSources
            else listOfNotNull(enabledSources.getOrNull(selectedSourcePosition - 1))
        if (selectedSources.isEmpty()) {
            status.visibility = View.VISIBLE
            status.text = "请先添加并启用书源后再搜索"
            return
        }
        rememberSearch(query)
        historyPanel.visibility = View.GONE
        sourceStripView.visibility = View.VISIBLE
        disclaimerView.visibility = View.VISIBLE
        paginationView.visibility = View.VISIBLE
        status.visibility = View.VISIBLE
        searchButton.isEnabled = false
        status.text = "正在搜索（第 ${requestedPage} 页）…"
        results.removeAllViews()
        worker.execute {
            val collected = mutableListOf<BookCandidate>()
            val failures = mutableListOf<String>()
            selectedSources.forEach { source ->
                val result = runBlocking {
                    try {
                        engine.searchPage(source.id, query, requestedPage)
                    } catch (error: Exception) {
                        EngineResult.Failure(
                            EngineErrorCode.UNKNOWN,
                            error.message ?: "搜索时发生异常",
                            error,
                        )
                    }
                }
                when (result) {
                    is EngineResult.Success -> collected.addAll(result.value)
                    is EngineResult.Failure -> failures.add("${source.name}：${result.message}")
                }
            }
            val unique = collected.distinctBy { "${it.sourceId}|${it.bookUrl}" }
            mainHandler.post {
                searchButton.isEnabled = enabledSources.isNotEmpty()
                previousPageButton.isEnabled = requestedPage > 1
                nextPageButton.isEnabled = unique.isNotEmpty()
                showResults(requestedPage, unique, failures)
                BadcatTheme.applyTree(this@SourceSearchActivity, rootView)
            }
        }
    }

    private fun readSearchHistory(): List<String> = runCatching {
        val raw = getSharedPreferences(SEARCH_HISTORY_PREFS, MODE_PRIVATE)
            .getString(SEARCH_HISTORY_KEY, "[]").orEmpty()
        val array = org.json.JSONArray(raw)
        (0 until array.length()).mapNotNull { array.optString(it).trim().takeIf(String::isNotBlank) }
            .distinct().take(MAX_SEARCH_HISTORY)
    }.getOrDefault(emptyList())

    private fun rememberSearch(query: String) {
        val next = (listOf(query.trim()) + readSearchHistory())
            .filter(String::isNotBlank).distinct().take(MAX_SEARCH_HISTORY)
        getSharedPreferences(SEARCH_HISTORY_PREFS, MODE_PRIVATE).edit()
            .putString(SEARCH_HISTORY_KEY, org.json.JSONArray(next).toString()).apply()
        renderSearchHistory()
    }

    private fun renderSearchHistory() {
        if (!::historyPanel.isInitialized) return
        historyPanel.removeAllViews()
        historyPanel.visibility = View.VISIBLE
        val entries = readSearchHistory().take(12)
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@SourceSearchActivity).apply {
                text = "搜索历史"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ink)
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
            if (entries.isNotEmpty()) {
                addView(android.widget.ImageView(this@SourceSearchActivity).apply {
                    setImageResource(android.R.drawable.ic_menu_delete)
                    setColorFilter(muted)
                    scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                    contentDescription = "清空搜索历史"
                    setPadding(dp(10), dp(10), dp(6), dp(10))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        BadcatTheme.alertBuilder(this@SourceSearchActivity)
                            .setMessage("确定清空全部搜索历史吗？")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("清空") { _, _ ->
                                getSharedPreferences(SEARCH_HISTORY_PREFS, MODE_PRIVATE).edit()
                                    .remove(SEARCH_HISTORY_KEY).apply()
                                renderSearchHistory()
                            }.show()
                    }
                }, LinearLayout.LayoutParams(dp(40), dp(40)))
            }
        }
        historyPanel.addView(heading)
        if (entries.isEmpty()) {
            historyPanel.addView(TextView(this).apply {
                text = "搜索过的书名会显示在这里"
                textSize = 13f
                setTextColor(muted)
                setPadding(dp(2), dp(4), dp(2), dp(16))
            })
            return
        }
        entries.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            pair.forEach { query ->
                row.addView(TextView(this@SourceSearchActivity).apply {
                    text = query
                    textSize = 14f
                    setTextColor(ink)
                    gravity = Gravity.CENTER_VERTICAL
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding(dp(2), dp(9), dp(10), dp(9))
                    isClickable = true
                    isFocusable = true
                    contentDescription = "再次搜索$query"
                    setOnClickListener {
                        keywordInput.setText(query)
                        keywordInput.setSelection(keywordInput.text.length)
                        search()
                    }
                    setOnLongClickListener {
                        val next = readSearchHistory().filterNot { it == query }
                        getSharedPreferences(SEARCH_HISTORY_PREFS, MODE_PRIVATE).edit()
                            .putString(SEARCH_HISTORY_KEY, org.json.JSONArray(next).toString()).apply()
                        renderSearchHistory()
                        true
                    }
                }, LinearLayout.LayoutParams(0, dp(42), 1f))
            }
            if (pair.size == 1) {
                row.addView(View(this@SourceSearchActivity), LinearLayout.LayoutParams(0, dp(42), 1f))
            }
            historyPanel.addView(row)
        }
    }

    private fun showResults(page: Int, items: List<BookCandidate>, failures: List<String>) {
        status.text = "第 ${page} 页 · 找到 ${items.size} 条结果" +
            if (failures.isNotEmpty()) " · ${failures.size} 个书源失败" else ""
        if (failures.isNotEmpty()) {
            results.addView(TextView(this).apply {
                text = failures.joinToString("\n") { "• $it" }
                textSize = 12f
                setTextColor(muted)
                setPadding(0, dp(4), 0, dp(10))
            })
        }
        if (items.isEmpty()) {
            if (failures.isNotEmpty()) showServerError("当前服务出了点问题，请稍候再试") { search() }
            else results.addView(TextView(this).apply {
                text = "暂未找到相关书籍"; textSize = 14f; gravity = Gravity.CENTER
                setTextColor(muted); setPadding(0, dp(42), 0, dp(42))
            })
            return
        }
        items.forEach { item ->
            results.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(card, 12)
                setPadding(dp(14), dp(13), dp(14), dp(13))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(Intent(this@SourceSearchActivity, SourceBookActivity::class.java).apply {
                        putExtra(SourceBookActivity.EXTRA_SOURCE_ID, item.sourceId)
                        putExtra(SourceBookActivity.EXTRA_BOOK_URL, item.bookUrl)
                        putExtra(SourceBookActivity.EXTRA_BOOK_NAME, item.name)
                        putExtra(SourceBookActivity.EXTRA_BOOK_COVER_URL, item.coverUrl)
                        putExtra(SourceBookActivity.EXTRA_BOOK_STATUS, item.status)
                    })
                }
                val info = LinearLayout(this@SourceSearchActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(this@SourceSearchActivity).apply {
                        text = item.name
                        textSize = 16f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(ink)
                    })
                    item.author?.takeIf { it.isNotBlank() }?.let { author ->
                        addView(resultMeta("作者：$author"))
                    }
                    item.latestChapter?.takeIf { it.isNotBlank() }?.let { chapter ->
                        addView(resultMeta("最新：$chapter"))
                    }
                    item.intro?.takeIf { it.isNotBlank() }?.let { intro ->
                        addView(TextView(this@SourceSearchActivity).apply {
                            text = intro
                            textSize = 12f
                            maxLines = 2
                            setTextColor(muted)
                            setPadding(0, dp(6), 0, 0)
                        })
                    }
                    addView(resultMeta("书源：${item.sourceId}"))
                }
                addView(LinearLayout(this@SourceSearchActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                    addView(BookStatusBadge.cover(this@SourceSearchActivity, item.coverUrl, item.status, 62, 84),
                        LinearLayout.LayoutParams(dp(62), dp(84)))
                    addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = dp(12)
                    })
                })
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) })
        }
    }

    private fun showServerError(message: String, retry: () -> Unit) {
        status.text = ""
        results.removeAllViews()
        results.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(150), dp(20), dp(80))
            addView(TextView(this@SourceSearchActivity).apply {
                text = "服务器异常"; textSize = 20f; setTextColor(ink); gravity = Gravity.CENTER
            })
            addView(TextView(this@SourceSearchActivity).apply {
                text = message; textSize = 14f; setTextColor(muted); gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(22))
            })
            addView(TextView(this@SourceSearchActivity).apply {
                text = "刷新重试"; textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                background = rounded(blue, 28); setPadding(dp(52), dp(13), dp(52), dp(13))
                setOnClickListener { retry() }
            })
        })
    }

    private fun resultMeta(value: String) = TextView(this).apply {
        text = value
        textSize = 12f
        setTextColor(muted)
        setPadding(0, dp(5), 0, 0)
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        val currentDark = BadcatTheme.isDark(this)
        if (currentDark != appliedDarkMode) {
            recreate()
            return
        }
        if (::rootView.isInitialized) BadcatTheme.applyTree(this, rootView)
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

}
