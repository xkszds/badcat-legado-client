package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Button
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
        val savedChapterUrl = getSharedPreferences("badcat_reading_progress", MODE_PRIVATE)
            .getString(readingProgressKey(sourceId, bookUrl), null)
        if (!savedChapterUrl.isNullOrBlank()) {
            chapterList.addView(Button(this).apply {
                text = "继续上次阅读"
                setOnClickListener {
                    startActivity(Intent(this@SourceBookActivity, SourceChapterActivity::class.java).apply {
                        putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, sourceId)
                        putExtra(SourceChapterActivity.EXTRA_BOOK_URL, bookUrl)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, savedChapterUrl)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, "继续阅读")
                    })
                }
            })
        }
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

/** Development reader with TOC-based chapter navigation and saved progress. */
class SourceChapterActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var engine: ReadingEngine
    private lateinit var status: TextView
    private lateinit var body: TextView
    private lateinit var previousButton: android.widget.Button
    private lateinit var nextButton: android.widget.Button
    private lateinit var readerScroll: ScrollView
    private lateinit var sourceId: String
    private lateinit var bookUrl: String
    private var chapters: List<BookChapter> = emptyList()
    private var currentIndex: Int = -1
    private var currentChapterUrl: String = ""
    private var textSizeSp: Float = 18f
    private val preferences by lazy {
        getSharedPreferences("badcat_reading_progress", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = BadcatReadingEngineFactory(applicationContext).create()
        sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty()
        bookUrl = intent.getStringExtra(EXTRA_BOOK_URL).orEmpty()
        currentChapterUrl = intent.getStringExtra(EXTRA_CHAPTER_URL).orEmpty()
        val chapterTitle = intent.getStringExtra(EXTRA_CHAPTER_TITLE).orEmpty()
        if (currentChapterUrl.isBlank()) {
            currentChapterUrl = preferences.getString(readingProgressKey(sourceId, bookUrl), "").orEmpty()
        }
        textSizeSp = preferences.getFloat("reader_text_size", 18f).coerceIn(14f, 32f)

        status = TextView(this).apply { text = "正在加载目录…" }
        body = TextView(this).apply {
            textSize = textSizeSp
            setTextIsSelectable(true)
            setPadding(20, 16, 20, 24)
            setLineSpacing(6f, 1.15f)
        }
        previousButton = android.widget.Button(this).apply {
            text = "上一章"
            isEnabled = false
            setOnClickListener { moveChapter(-1) }
        }
        nextButton = android.widget.Button(this).apply {
            text = "下一章"
            isEnabled = false
            setOnClickListener { moveChapter(1) }
        }
        val smaller = android.widget.Button(this).apply {
            text = "A−"
            setOnClickListener { changeTextSize(-1f) }
        }
        val larger = android.widget.Button(this).apply {
            text = "A+"
            setOnClickListener { changeTextSize(1f) }
        }
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(previousButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(smaller, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(larger, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(nextButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            addView(status)
            addView(controls)
            addView(body)
        }
        readerScroll = ScrollView(this).apply { addView(root) }
        setContentView(readerScroll)
        if (sourceId.isBlank() || bookUrl.isBlank() || currentChapterUrl.isBlank()) {
            status.text = "缺少书源、书籍地址或章节地址"
            return
        }
        loadInitial(chapterTitle)
    }

    private fun loadInitial(fallbackTitle: String) {
        worker.execute {
            val toc = runBlocking { engine.chapters(sourceId, bookUrl) }
            val chapterList = (toc as? EngineResult.Success)?.value.orEmpty()
            val selected = chapterList.indexOfFirst { it.url == currentChapterUrl }
            val initialIndex = selected
            val chapter = if (selected >= 0) chapterList[selected] else {
                BookChapter(index = 0, title = fallbackTitle, url = currentChapterUrl)
            }
            val content = runBlocking { engine.chapterContent(sourceId, bookUrl, chapter.url) }
            mainHandler.post {
                chapters = chapterList
                currentIndex = initialIndex
                renderChapter(chapter, content, toc)
            }
        }
    }

    private fun moveChapter(delta: Int) {
        val target = currentIndex + delta
        if (target !in chapters.indices) return
        val chapter = chapters[target]
        previousButton.isEnabled = false
        nextButton.isEnabled = false
        status.text = "正在加载：${chapter.title}"
        worker.execute {
            val result = runBlocking { engine.chapterContent(sourceId, bookUrl, chapter.url) }
            mainHandler.post {
                if (result is EngineResult.Success) currentIndex = target
                renderChapter(chapter, result, EngineResult.Success(chapters))
            }
        }
    }

    private fun renderChapter(
        chapter: BookChapter,
        result: EngineResult<app.badcat.engine.ChapterContent>,
        tocResult: EngineResult<List<BookChapter>>,
    ) {
        when (result) {
            is EngineResult.Success -> {
                status.text = result.value.title.ifBlank { chapter.title }
                body.text = result.value.text
                currentChapterUrl = chapter.url
                preferences.edit().putString(readingProgressKey(sourceId, bookUrl), chapter.url).apply()
            }
            is EngineResult.Failure -> status.text = "正文加载失败（${result.code}）：${result.message}"
        }
        if (tocResult is EngineResult.Failure) {
            status.append("\n目录导航不可用：${tocResult.message}")
        }
        previousButton.isEnabled = currentIndex > 0
        nextButton.isEnabled = currentIndex >= 0 && currentIndex < chapters.lastIndex
        readerScroll.post { readerScroll.scrollTo(0, 0) }
    }

    private fun changeTextSize(delta: Float) {
        textSizeSp = (textSizeSp + delta).coerceIn(14f, 32f)
        body.textSize = textSizeSp
        preferences.edit().putFloat("reader_text_size", textSizeSp).apply()
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

private fun readingProgressKey(sourceId: String, bookUrl: String): String {
    val raw = "$sourceId\n$bookUrl".toByteArray(Charsets.UTF_8)
    return "last_chapter_" + Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_WRAP)
}
