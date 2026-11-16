package app.badcat.client

import android.app.Activity
import android.graphics.Color
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
            addView(Button(this@SourceBookActivity).apply {
                text = if (FavoriteBooks.contains(this@SourceBookActivity, sourceId, bookUrl)) "已加入收藏" else "加入收藏"
                setOnClickListener {
                    val added = FavoriteBooks.add(this@SourceBookActivity, sourceId, bookUrl, bookName)
                    text = if (added) "已加入收藏" else "已在收藏夹"
                    android.widget.Toast.makeText(
                        this@SourceBookActivity,
                        if (added) "已加入收藏夹" else "这本书已经收藏",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            })
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
            val detail = runBlocking { safeEngineCall { engine.bookDetail(sourceId, bookUrl) } }
            val toc = if (detail is EngineResult.Success) {
                runBlocking { safeEngineCall { engine.chapters(sourceId, bookUrl) } }
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
                        putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, bookName)
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
        val prefs = getSharedPreferences("badcat_reading_progress", MODE_PRIVATE)
        val bookmarked = chapters.filter { prefs.contains(chapterBookmarkKey(sourceId, bookUrl, it.url)) }
        if (bookmarked.isNotEmpty()) {
            chapterList.addView(Button(this).apply {
                text = "本书书签（" + bookmarked.size + "）"
                setOnClickListener {
                    val labels = bookmarked.map { chapter -> (chapter.index + 1).toString() + ". " + chapter.title }.toTypedArray()
                    android.app.AlertDialog.Builder(this@SourceBookActivity)
                        .setTitle("选择书签章节")
                        .setItems(labels) { _, which ->
                            val chapter = bookmarked[which]
                            android.app.AlertDialog.Builder(this@SourceBookActivity)
                                .setTitle(chapter.title)
                                .setItems(arrayOf("打开书签", "删除书签")) { _, action ->
                                    val key = chapterBookmarkKey(sourceId, bookUrl, chapter.url)
                                    if (action == 0) {
                                        startActivity(Intent(this@SourceBookActivity, SourceChapterActivity::class.java).apply {
                                            putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, sourceId)
                                            putExtra(SourceChapterActivity.EXTRA_BOOK_URL, bookUrl)
                                        putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, bookName)
                                            putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, chapter.url)
                                            putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, chapter.title)
                                            putExtra(SourceChapterActivity.EXTRA_JUMP_TO_BOOKMARK, true)
                                        })
                                    } else {
                                        prefs.edit().remove(key).apply()
                                        showChapters(chapters)
                                    }
                                }
                                .setNegativeButton("取消", null)
                                .show()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            })
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
                        putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, bookName)
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
    private lateinit var body: android.webkit.WebView
    private lateinit var previousButton: android.widget.Button
    private lateinit var nextButton: android.widget.Button
    private lateinit var readerScroll: android.webkit.WebView
    private lateinit var readerRoot: LinearLayout
    private lateinit var chapterProgress: android.widget.SeekBar
    private lateinit var chapterTitleView: TextView
    private lateinit var sourceId: String
    private lateinit var bookUrl: String
    private lateinit var bookName: String
    private var chapters: List<BookChapter> = emptyList()
    private var currentIndex: Int = -1
    private var currentChapterUrl: String = ""
    private var textSizeSp: Float = 18f
    private var lineSpacingMultiplier: Float = 1.15f
    private var readerTheme: Int = 0
    private var currentChapterText: String = ""
    private var currentChapterTitle: String = ""
    private var pendingScrollY: Int? = null
    private var pendingBookmarkY: Int? = null
    private val preferences by lazy {
        getSharedPreferences("badcat_reading_progress", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = BadcatReadingEngineFactory(applicationContext).create()
        sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty()
        bookUrl = intent.getStringExtra(EXTRA_BOOK_URL).orEmpty()
        bookName = intent.getStringExtra(EXTRA_BOOK_NAME).orEmpty()
        currentChapterUrl = intent.getStringExtra(EXTRA_CHAPTER_URL).orEmpty()
        val chapterTitle = intent.getStringExtra(EXTRA_CHAPTER_TITLE).orEmpty()
        if (currentChapterUrl.isBlank()) {
            currentChapterUrl = preferences.getString(readingProgressKey(sourceId, bookUrl), "").orEmpty()
        }
        textSizeSp = preferences.getFloat("reader_text_size", 18f).coerceIn(14f, 32f)
        lineSpacingMultiplier = preferences.getFloat("reader_line_spacing", 1.15f).coerceIn(1.0f, 2.0f)
        readerTheme = preferences.getInt("reader_theme", 0).coerceIn(0, 2)

        status = TextView(this).apply { text = "正在加载目录…" }
        body = android.webkit.WebView(this).apply {
            settings.javaScriptEnabled = false
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportMultipleWindows(false)
            isVerticalScrollBarEnabled = false
            setBackgroundColor(Color.WHITE)
            webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                    super.onPageFinished(view, url)
                    val target = pendingBookmarkY ?: pendingScrollY
                    pendingBookmarkY = null
                    pendingScrollY = null
                    if (target != null) view.post { view.scrollTo(0, target) }
                }
            }
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
        chapterTitleView = TextView(this).apply {
            text = chapterTitle.ifBlank { bookName }
            textSize = 16f
            setTextColor(Color.rgb(55, 55, 55))
            gravity = android.view.Gravity.CENTER_VERTICAL
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 32f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.rgb(55, 55, 55))
            setOnClickListener { finish() }
        }
        val more = TextView(this).apply {
            text = "⋯"
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.rgb(55, 55, 55))
            setOnClickListener { showReaderTools() }
        }
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpReader(10), dpReader(4), dpReader(10), dpReader(4))
            setBackgroundColor(Color.rgb(250, 250, 250))
            addView(back, LinearLayout.LayoutParams(dpReader(42), dpReader(48)))
            addView(chapterTitleView, LinearLayout.LayoutParams(0, dpReader(48), 1f))
            addView(more, LinearLayout.LayoutParams(dpReader(42), dpReader(48)))
        }
        status = TextView(this).apply {
            text = "正在加载目录…"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(dpReader(18), dpReader(4), dpReader(18), dpReader(4))
            setBackgroundColor(Color.rgb(250, 250, 250))
        }
        chapterProgress = android.widget.SeekBar(this).apply {
            max = 100
            progress = 0
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) = Unit
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                    val target = seekBar?.progress ?: return
                    if (chapters.isNotEmpty()) loadChapterAt(((chapters.lastIndex * target) / 100.0).toInt())
                }
            })
        }
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpReader(8), dpReader(3), dpReader(8), dpReader(3))
            setBackgroundColor(Color.rgb(250, 250, 250))
            addView(previousButton, LinearLayout.LayoutParams(0, dpReader(46), 1f))
            addView(chapterProgress, LinearLayout.LayoutParams(0, dpReader(40), 2f))
            addView(nextButton, LinearLayout.LayoutParams(0, dpReader(46), 1f))
        }
        readerRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(topBar)
            addView(status)
            addView(body, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f,
            ))
            addView(bottomBar)
        }
        readerScroll = body
        applyReaderTheme()
        setContentView(readerRoot)
        if (sourceId.isBlank() || bookUrl.isBlank() || currentChapterUrl.isBlank()) {
            status.text = "缺少书源、书籍地址或章节地址"
            return
        }
        loadInitial(chapterTitle)
    }

    private fun loadInitial(fallbackTitle: String) {
        worker.execute {
            val toc = runBlocking { safeEngineCall { engine.chapters(sourceId, bookUrl) } }
            val chapterList = (toc as? EngineResult.Success)?.value.orEmpty()
            val selected = chapterList.indexOfFirst { it.url == currentChapterUrl }
            val initialIndex = selected
            val chapter = if (selected >= 0) chapterList[selected] else {
                BookChapter(index = 0, title = fallbackTitle, url = currentChapterUrl)
            }
            val content = runBlocking { safeEngineCall { engine.chapterContent(sourceId, bookUrl, chapter.url, bookName) } }
            mainHandler.post {
                chapters = chapterList
                currentIndex = initialIndex
                renderChapter(chapter, content, toc)
            }
        }
    }

    private fun saveCurrentScrollPosition() {
        if (!::readerScroll.isInitialized || currentChapterUrl.isBlank()) return
        preferences.edit()
            .putInt(chapterScrollPositionKey(sourceId, bookUrl, currentChapterUrl), readerScroll.scrollY)
            .apply()
    }

    private fun restoreCurrentScrollPosition() {
        val savedY = preferences.getInt(
            chapterScrollPositionKey(sourceId, bookUrl, currentChapterUrl),
            0,
        ).coerceAtLeast(0)
        pendingScrollY = savedY
    }

    private fun moveChapter(delta: Int) {
        loadChapterAt(currentIndex + delta)
    }

    private fun showChapterCatalog() {
        if (chapters.isEmpty()) {
            status.text = "目录尚未加载"
            return
        }
        val labels = chapters.mapIndexed { index, chapter ->
            (index + 1).toString() + ". " + chapter.title
        }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("章节目录（${chapters.size} 章）")
            .setItems(labels) { _, index -> loadChapterAt(index) }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun loadChapterAt(target: Int) {
        if (target !in chapters.indices || target == currentIndex) return
        saveCurrentScrollPosition()
        val chapter = chapters[target]
        previousButton.isEnabled = false
        nextButton.isEnabled = false
        status.text = "正在加载：${chapter.title}"
        worker.execute {
            val result = runBlocking { safeEngineCall { engine.chapterContent(sourceId, bookUrl, chapter.url, bookName) } }
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
                currentChapterTitle = result.value.title.ifBlank { chapter.title }
                status.text = "阅读中 · ${currentIndex + 1}/${chapters.size.coerceAtLeast(1)} 章"
                chapterTitleView.text = currentChapterTitle
                currentChapterText = result.value.text
                renderReaderDocument()
                currentChapterUrl = chapter.url
                preferences.edit().putString(readingProgressKey(sourceId, bookUrl), chapter.url).apply()
                ReadingHistoryActivity.save(this, sourceId, bookUrl, bookName.ifBlank { "未命名书籍" }, chapter.url, result.value.title.ifBlank { chapter.title })
            }
            is EngineResult.Failure -> status.text = "正文加载失败（${result.code}）：${result.message}"
        }
        if (tocResult is EngineResult.Failure) {
            status.append("\n目录导航不可用：${tocResult.message}")
        }
        previousButton.isEnabled = currentIndex > 0
        nextButton.isEnabled = currentIndex >= 0 && currentIndex < chapters.lastIndex
        if (chapters.size > 1 && currentIndex >= 0) chapterProgress.progress = (currentIndex * 100 / chapters.lastIndex).coerceIn(0, 100)
        if (result is EngineResult.Success) {
            restoreCurrentScrollPosition()
            if (intent.getBooleanExtra(EXTRA_JUMP_TO_BOOKMARK, false)) {
                intent.removeExtra(EXTRA_JUMP_TO_BOOKMARK)
                val bookmarkY = preferences.getInt(
                    chapterBookmarkKey(sourceId, bookUrl, currentChapterUrl),
                    -1,
                )
                if (bookmarkY >= 0) {
                    pendingBookmarkY = bookmarkY
                }
            }
        }
    }

    private fun saveChapterBookmark() {
        if (currentChapterUrl.isBlank()) {
            status.text = "当前章节尚未加载，无法保存书签"
            return
        }
        val position = readerScroll.scrollY.coerceAtLeast(0)
        val bookmarkKey = chapterBookmarkKey(sourceId, bookUrl, currentChapterUrl)
        val encoded = bookmarkKey.removePrefix("chapter_bookmark_")
        preferences.edit()
            .putInt(bookmarkKey, position)
            .putString("chapter_bookmark_title_" + encoded, currentChapterTitle.ifBlank { "章节书签" })
            .putString("chapter_bookmark_book_" + encoded, bookName.ifBlank { "未命名小说" })
            .putLong("chapter_bookmark_time_" + encoded, System.currentTimeMillis())
            .apply()
        status.text = "书签已保存（当前位置），可在「我的 - 书签管理」查看"
    }

    private fun jumpToChapterBookmark() {
        if (currentChapterUrl.isBlank()) return
        val key = chapterBookmarkKey(sourceId, bookUrl, currentChapterUrl)
        if (!preferences.contains(key)) {
            status.text = "本章还没有保存书签"
            return
        }
        val position = preferences.getInt(key, 0).coerceAtLeast(0)
        readerScroll.post {
            readerScroll.post { readerScroll.scrollTo(0, position) }
        }
        status.text = "已跳转到本章书签"
    }

    private fun showReaderTools() {
        val actions = arrayOf("字号减小", "字号增大", "行距减小", "行距增大", "切换背景", "CSS 自定义", "章节目录", "保存书签", "跳转本章书签")
        android.app.AlertDialog.Builder(this)
            .setTitle("阅读设置")
            .setItems(actions) { _, index ->
                when (index) {
                    0 -> changeTextSize(-1f)
                    1 -> changeTextSize(1f)
                    2 -> changeLineSpacing(-0.1f)
                    3 -> changeLineSpacing(0.1f)
                    4 -> cycleReaderTheme()
                    5 -> startActivity(Intent(this, ReaderCssActivity::class.java))
                    6 -> showChapterCatalog()
                    7 -> saveChapterBookmark()
                    8 -> jumpToChapterBookmark()
                }
            }.show()
    }

    private fun dpReader(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun changeTextSize(delta: Float) {
        textSizeSp = (textSizeSp + delta).coerceIn(14f, 32f)
        renderReaderDocument()
        preferences.edit().putFloat("reader_text_size", textSizeSp).apply()
    }

    private fun changeLineSpacing(delta: Float) {
        lineSpacingMultiplier = (lineSpacingMultiplier + delta).coerceIn(1.0f, 2.0f)
        renderReaderDocument()
        preferences.edit().putFloat("reader_line_spacing", lineSpacingMultiplier).apply()
    }

    private fun cycleReaderTheme() {
        readerTheme = (readerTheme + 1) % 3
        preferences.edit().putInt("reader_theme", readerTheme).apply()
        applyReaderTheme()
    }

    private fun renderReaderDocument() {
        if (!::body.isInitialized || currentChapterText.isEmpty()) return
        val (background, foreground) = when (readerTheme) {
            1 -> Color.rgb(244, 234, 211) to Color.rgb(75, 61, 43)
            2 -> Color.rgb(28, 30, 34) to Color.rgb(220, 222, 226)
            else -> Color.WHITE to Color.rgb(35, 35, 35)
        }
        val html = ReaderCssDocument.build(
            this, currentChapterText, textSizeSp, lineSpacingMultiplier, background, foreground,
        )
        body.loadDataWithBaseURL("https://reader.invalid/", html, "text/html", "UTF-8", null)
    }

    private fun applyReaderTheme() {
        val (background, foreground) = when (readerTheme) {
            1 -> Color.rgb(244, 234, 211) to Color.rgb(75, 61, 43)
            2 -> Color.rgb(28, 30, 34) to Color.rgb(220, 222, 226)
            else -> Color.WHITE to Color.rgb(35, 35, 35)
        }
        readerRoot.setBackgroundColor(background)
        readerScroll.setBackgroundColor(background)
        body.setBackgroundColor(background)
        if (currentChapterText.isNotEmpty()) renderReaderDocument()
        status.setTextColor(foreground)
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized && currentChapterText.isNotEmpty()) {
            pendingScrollY = body.scrollY
            renderReaderDocument()
        }
    }

    override fun onPause() {
        saveCurrentScrollPosition()
        super.onPause()
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SOURCE_ID = "reader_source_id"
        const val EXTRA_BOOK_URL = "reader_book_url"
        const val EXTRA_BOOK_NAME = "reader_book_name"
        const val EXTRA_CHAPTER_URL = "reader_chapter_url"
        const val EXTRA_CHAPTER_TITLE = "reader_chapter_title"
        const val EXTRA_JUMP_TO_BOOKMARK = "reader_jump_to_bookmark"
    }
}

private fun readingProgressKey(sourceId: String, bookUrl: String): String {
    val raw = "$sourceId\n$bookUrl".toByteArray(Charsets.UTF_8)
    return "last_chapter_" + Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_WRAP)
}

private fun chapterScrollPositionKey(
    sourceId: String,
    bookUrl: String,
    chapterUrl: String,
): String {
    val raw = "$sourceId\n$bookUrl\n$chapterUrl".toByteArray(Charsets.UTF_8)
    return "chapter_scroll_" + Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_WRAP)
}

private fun chapterBookmarkKey(
    sourceId: String,
    bookUrl: String,
    chapterUrl: String,
): String {
    val raw = "$sourceId\n$bookUrl\n$chapterUrl".toByteArray(Charsets.UTF_8)
    return "chapter_bookmark_" + Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_WRAP)
}
