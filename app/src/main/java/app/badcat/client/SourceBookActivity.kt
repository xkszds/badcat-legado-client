package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.badcat.client.runtime.BadcatReadingEngineFactory
import app.badcat.engine.BookChapter
import app.badcat.engine.EngineResult
import app.badcat.engine.ReadingEngine
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

/**
 * Development-only book detail/TOC flow. It deliberately does not imitate
 * or replace the original Badcat detail page.
 */
class SourceBookActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var engine: ReadingEngine
    private lateinit var sourceId: String
    private lateinit var bookUrl: String
    private lateinit var bookName: String
    private lateinit var status: TextView
    private lateinit var chapterList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty()
        bookUrl = intent.getStringExtra(EXTRA_BOOK_URL).orEmpty()
        bookName = intent.getStringExtra(EXTRA_BOOK_NAME).orEmpty()
        engine = BadcatReadingEngineFactory(applicationContext).create()

        status = TextView(this).apply { text = "正在加载书籍详情…" }
        chapterList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            addView(TextView(this@SourceBookActivity).apply {
                text = "书籍详情与目录（开发验证界面）"
                textSize = 20f
            })
            addView(status)
            addView(chapterList)
        }
        setContentView(ScrollView(this).apply { addView(root) })
        if (sourceId.isBlank() || bookUrl.isBlank()) {
            status.text = "缺少书源或书籍地址"
        } else {
            loadBook()
        }
    }

    private fun loadBook() {
        worker.execute {
            val detail = runBlocking { engine.bookDetail(sourceId, bookUrl) }
            val toc = if (detail is EngineResult.Success) {
                runBlocking { engine.chapters(sourceId, bookUrl) }
            } else null
            mainHandler.post {
                when (detail) {
                    is EngineResult.Failure -> status.text = "详情加载失败（${detail.code}）：${detail.message}"
                    is EngineResult.Success -> {
                        val book = detail.value
                        status.text = buildString {
                            append(book.name.ifBlank { bookName })
                            book.author?.let { append("\n作者：").append(it) }
                            book.latestChapter?.let { append("\n最新章节：").append(it) }
                            book.intro?.takeIf { it.isNotBlank() }?.let { append("\n\n").append(it) }
                        }
                        when (toc) {
                            is EngineResult.Success -> showChapters(toc.value)
                            is EngineResult.Failure -> status.append("\n\n目录加载失败（${toc.code}）：${toc.message}")
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun showChapters(chapters: List<BookChapter>) {
        chapterList.removeAllViews()
        status.append("\n\n目录：${chapters.size} 章（点击章节开始阅读）")
        if (chapters.isEmpty()) {
            chapterList.addView(TextView(this).apply { text = "目录为空" })
            return
        }
        chapters.forEach { chapter ->
            chapterList.addView(TextView(this).apply {
                text = "${chapter.index + 1}. ${chapter.title}"
                textSize = 16f
                setPadding(0, 12, 0, 12)
                isClickable = true
                setOnClickListener {
                    startActivity(Intent(this@SourceBookActivity, SourceChapterActivity::class.java).apply {
                        putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, sourceId)
                        putExtra(SourceChapterActivity.EXTRA_BOOK_URL, bookUrl)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, chapter.url)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, chapter.title)
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

    companion object {
        const val EXTRA_SOURCE_ID = "source_id"
        const val EXTRA_BOOK_URL = "book_url"
        const val EXTRA_BOOK_NAME = "book_name"
    }
}

/** Minimal reader surface for validating chapter-content extraction. */
class SourceChapterActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var engine: ReadingEngine
    private lateinit var status: TextView
    private lateinit var body: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = BadcatReadingEngineFactory(applicationContext).create()
        val sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty()
        val bookUrl = intent.getStringExtra(EXTRA_BOOK_URL).orEmpty()
        val chapterUrl = intent.getStringExtra(EXTRA_CHAPTER_URL).orEmpty()
        val chapterTitle = intent.getStringExtra(EXTRA_CHAPTER_TITLE).orEmpty()
        status = TextView(this).apply { text = "正在加载正文…" }
        body = TextView(this).apply {
            textSize = 18f
            setTextIsSelectable(true)
            setPadding(20, 16, 20, 24)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 16)
            addView(status)
            addView(body)
        }
        setContentView(ScrollView(this).apply { addView(root) })
        if (sourceId.isBlank() || bookUrl.isBlank() || chapterUrl.isBlank()) {
            status.text = "缺少章节参数"
            return
        }
        worker.execute {
            val result = runBlocking { engine.chapterContent(sourceId, bookUrl, chapterUrl) }
            mainHandler.post {
                when (result) {
                    is EngineResult.Success -> {
                        status.text = result.value.title.ifBlank { chapterTitle }
                        body.text = result.value.text
                    }
                    is EngineResult.Failure ->
                        status.text = "正文加载失败（${result.code}）：${result.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SOURCE_ID = "reader_source_id"
        const val EXTRA_BOOK_URL = "reader_book_url"
        const val EXTRA_CHAPTER_URL = "reader_chapter_url"
        const val EXTRA_CHAPTER_TITLE = "reader_chapter_title"
    }
}
