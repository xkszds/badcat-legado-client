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
                text = "书源搜索（开发验证界面）"
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
            val result = runBlocking { engine.sources() }
            mainHandler.post {
                when (result) {
                    is EngineResult.Success -> {
                        enabledSources = result.value.filter { it.enabled && !it.searchUrl.isNullOrBlank() }
                        sourceSpinner.adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            enabledSources.map { it.name },
                        )
                        status.text = if (enabledSources.isEmpty()) {
                            "没有可搜索的已启用书源（请先导入包含 searchUrl 的 Legado 书源）"
                        } else {
                            "已载入 ${enabledSources.size} 个可搜索书源"
                        }
                        searchButton.isEnabled = enabledSources.isNotEmpty()
                    }
                    is EngineResult.Failure -> status.text = "读取书源失败：${result.message}"
                }
            }
        }
    }

    private fun search() {
        val source = enabledSources.getOrNull(sourceSpinner.selectedItemPosition) ?: return
        val keyword = keywordInput.text.toString().trim()
        if (keyword.isEmpty()) {
            status.text = "请输入搜索关键词"
            return
        }
        searchButton.isEnabled = false
        status.text = "正在搜索：${source.name}"
        results.removeAllViews()
        worker.execute {
            val result = runBlocking { engine.search(source.id, keyword) }
            mainHandler.post {
                searchButton.isEnabled = true
                when (result) {
                    is EngineResult.Success -> showResults(result.value)
                    is EngineResult.Failure -> status.text = "搜索失败（${result.code}）：${result.message}"
                }
            }
        }
    }

    private fun showResults(items: List<BookCandidate>) {
        status.text = "搜索完成：找到 ${items.size} 条结果"
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
