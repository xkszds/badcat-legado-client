package app.badcat.client

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.content.Intent
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
 * Minimal source-aware search screen for exercising the composed ReadingEngine.
 * This is a development workflow, not a recreation of Badcat's original UI.
 */
class SourceSearchActivity : Activity() {
    companion object {
        const val EXTRA_INITIAL_QUERY = "initial_query"
        const val EXTRA_CONTENT_TYPE = "content_type"
    }
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var engine: ReadingEngine
    private lateinit var sourceSpinner: Spinner
    private lateinit var keywordInput: EditText
    private lateinit var searchButton: Button
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private var enabledSources: List<BookSource> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = BadcatReadingEngineFactory(applicationContext).create()

        sourceSpinner = Spinner(this)
        keywordInput = EditText(this).apply {
            hint = "输入书名或关键词"
            setSingleLine(true)
            setText(intent.getStringExtra(EXTRA_INITIAL_QUERY).orEmpty())
        }
        searchButton = Button(this).apply {
            text = "搜索"
            setOnClickListener { search() }
        }
        status = TextView(this).apply { text = "正在读取已启用书源…" }
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            addView(TextView(this@SourceSearchActivity).apply {
                text = "书源搜索 · ${intent.getStringExtra(EXTRA_CONTENT_TYPE).orEmpty().ifBlank { "全部" }}"
                textSize = 20f
            })
            addView(sourceSpinner)
            addView(keywordInput)
            addView(searchButton)
            addView(status)
            addView(results)
        }
        setContentView(ScrollView(this).apply { addView(root) })
        loadSources()
    }

    private fun loadSources() {
        searchButton.isEnabled = false
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
                        enabledSources = result.value.filter { it.enabled && !it.searchUrl.isNullOrBlank() }
                        sourceSpinner.adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            listOf("全部书源") + enabledSources.map { it.name },
                        )
                        status.text = if (enabledSources.isEmpty()) {
                            "没有可搜索的已启用书源（请先导入包含 searchUrl 的 Legado 书源）"
                        } else {
                            "已载入 ${enabledSources.size} 个可搜索书源；可选择单个书源或全部搜索"
                        }
                        searchButton.isEnabled = enabledSources.isNotEmpty()
                    }
                    is EngineResult.Failure -> status.text = "读取书源失败：${result.message}"
                }
            }
        }
    }

    private fun search() {
        val selectedPosition = sourceSpinner.selectedItemPosition
        val selectedSources = if (selectedPosition <= 0) enabledSources
            else listOfNotNull(enabledSources.getOrNull(selectedPosition - 1))
        if (selectedSources.isEmpty()) return
        val keyword = keywordInput.text.toString().trim()
        if (keyword.isEmpty()) {
            status.text = "请输入搜索关键词"
            return
        }
        searchButton.isEnabled = false
        status.text = if (selectedSources.size == 1) "正在搜索：${selectedSources.first().name}" else "正在搜索全部 ${selectedSources.size} 个书源…"
        results.removeAllViews()
        worker.execute {
            val collected = mutableListOf<BookCandidate>()
            val failures = mutableListOf<String>()
            selectedSources.forEach { source ->
                val result = runBlocking {
                    try {
                        engine.search(source.id, keyword)
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
                searchButton.isEnabled = true
                showResults(unique, failures)
            }
        }
    }

    private fun showResults(items: List<BookCandidate>, failures: List<String>) {
        status.text = buildString {
            append("搜索完成：找到 ${items.size} 条结果")
            if (failures.isNotEmpty()) append("；${failures.size} 个书源失败")
        }
        if (failures.isNotEmpty()) {
            results.addView(TextView(this).apply {
                text = failures.joinToString("\n") { "• $it" }
                textSize = 12f
                setPadding(0, 4, 0, 10)
            })
        }
        if (items.isEmpty()) {
            results.addView(TextView(this).apply { text = "没有匹配的书籍" })
            return
        }
        items.forEach { item ->
            results.addView(TextView(this).apply {
                text = buildString {
                    append(item.name)
                    item.author?.let { append("\n作者：").append(it) }
                    item.latestChapter?.let { append("\n最新章节：").append(it) }
                    item.intro?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
                    append("\n书源：").append(item.sourceId)
                }
                textSize = 15f
                setPadding(0, 14, 0, 14)
                isClickable = true
                setOnClickListener {
                    startActivity(Intent(this@SourceSearchActivity, SourceBookActivity::class.java).apply {
                        putExtra(SourceBookActivity.EXTRA_SOURCE_ID, item.sourceId)
                        putExtra(SourceBookActivity.EXTRA_BOOK_URL, item.bookUrl)
                        putExtra(SourceBookActivity.EXTRA_BOOK_NAME, item.name)
                    })
                }
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
