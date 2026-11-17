package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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
    }

    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val blue = Color.rgb(55, 137, 222)
    private val ink = Color.rgb(48, 55, 66)
    private val muted = Color.rgb(135, 145, 158)
    private val page = Color.rgb(247, 249, 252)
    private lateinit var engine: ReadingEngine
    private lateinit var keywordInput: EditText
    private lateinit var searchButton: TextView
    private lateinit var previousPageButton: TextView
    private lateinit var nextPageButton: TextView
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var sourceTabs: LinearLayout
    private var currentPage = 1
    private var selectedSourcePosition = 0
    private var contentType = "小说"
    private var enabledSources: List<BookSource> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = BadcatReadingEngineFactory(applicationContext).create()
        contentType = intent.getStringExtra(EXTRA_CONTENT_TYPE)
            ?.takeIf { it in listOf("小说", "漫画", "综合") } ?: "小说"
        status = TextView(this).apply {
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 0, 0, dp(10))
        }
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
        }
        root.addView(buildToolbar())
        root.addView(buildSearchControls())
        root.addView(buildSourceStrip())
        root.addView(buildDisclaimer())

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(LinearLayout(this@SourceSearchActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(24))
                addView(status)
                addView(results)
            })
        }
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(buildPagination())
        setContentView(root)
        loadSources()
    }

    private fun buildToolbar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(6), dp(16), dp(6))
        setBackgroundColor(Color.WHITE)
        addView(TextView(this@SourceSearchActivity).apply {
            text = "‹"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(ink)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(42), dp(44)))
        addView(TextView(this@SourceSearchActivity).apply {
            text = "搜索"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setPadding(dp(8), 0, 0, 0)
        })
    }

    private fun buildSearchControls(): View {
        val appearance = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getString("home_layout_mode", "integrated") ?: "integrated"
        val sharedBar = BadcatSearchBarView(
            context = this,
            initialQuery = intent.getStringExtra(EXTRA_INITIAL_QUERY).orEmpty(),
            initialType = contentType,
            appearance = appearance,
            onSearch = { query, type ->
                contentType = type
                keywordInput.setText(query)
                search()
            },
            onTypeChanged = { selected ->
                contentType = selected
                if (::keywordInput.isInitialized && keywordInput.text.toString().isNotBlank()) search()
            },
        )
        keywordInput = sharedBar.input
        searchButton = sharedBar.searchAction
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(7), dp(14), dp(7))
            setBackgroundColor(Color.WHITE)
            addView(sharedBar, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(if (appearance == "small") 44 else 48)
            ))
        }
    }

    private fun buildSourceStrip(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.WHITE)
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
        setBackgroundColor(Color.rgb(250, 250, 250))
    }

    private fun buildPagination(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(8), dp(16), dp(8))
        setBackgroundColor(Color.WHITE)
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
        background = rounded(Color.rgb(241, 247, 253), 18)
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
                        status.text = if (enabledSources.isEmpty()) {
                            "暂无可搜索书源，请先到「我的 - 书源管理」导入 Legado 书源"
                        } else {
                            "已载入 ${enabledSources.size} 个可搜索书源"
                        }
                        searchButton.isEnabled = enabledSources.isNotEmpty()
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
        val labels = listOf("全部") + enabledSources.map { it.name }
        labels.forEachIndexed { index, label ->
            val selected = index == selectedSourcePosition
            sourceTabs.addView(TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(if (selected) Color.WHITE else muted)
                background = rounded(
                    if (selected) blue else Color.rgb(246, 247, 249), 18
                )
                setPadding(dp(15), dp(8), dp(15), dp(8))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedSourcePosition = index
                    currentPage = 1
                    renderSourceTabs()
                    if (::keywordInput.isInitialized && keywordInput.text.toString().isNotBlank()) {
                        search()
                    } else {
                        status.text = if (index == 0) {
                            "已选择全部书源"
                        } else {
                            "已选择书源：$label"
                        }
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
            results.removeAllViews()
            status.text = if (contentType == "漫画") {
                "漫画类型的专用书源解析尚未接入。当前不会把小说书源结果误显示为漫画。"
            } else {
                "综合搜索聚合尚未接入。当前只支持已导入的 Legado 小说书源搜索。"
            }
            return
        }
        val requestedPage = currentPage
        val selectedSources = if (selectedSourcePosition == 0) enabledSources
            else listOfNotNull(enabledSources.getOrNull(selectedSourcePosition - 1))
        if (selectedSources.isEmpty()) {
            status.text = "没有可搜索的书源"
            return
        }
        val query = keywordInput.text.toString().trim()
        if (query.isEmpty()) {
            status.text = "请输入搜索关键词"
            return
        }
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
            }
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
                background = rounded(Color.WHITE, 12)
                setPadding(dp(14), dp(13), dp(14), dp(13))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(Intent(this@SourceSearchActivity, SourceBookActivity::class.java).apply {
                        putExtra(SourceBookActivity.EXTRA_SOURCE_ID, item.sourceId)
                        putExtra(SourceBookActivity.EXTRA_BOOK_URL, item.bookUrl)
                        putExtra(SourceBookActivity.EXTRA_BOOK_NAME, item.name)
                        putExtra(SourceBookActivity.EXTRA_BOOK_COVER_URL, item.coverUrl)
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
                    addView(android.widget.ImageView(this@SourceSearchActivity).apply {
                        contentDescription = "小说封面"
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        background = rounded(Color.rgb(235, 239, 245), 5)
                        clipToOutline = true
                        BookCoverLoader.load(this, item.coverUrl)
                    }, LinearLayout.LayoutParams(dp(62), dp(84)))
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

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
