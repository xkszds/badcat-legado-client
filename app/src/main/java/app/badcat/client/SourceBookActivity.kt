package app.badcat.client

import android.app.Activity
import android.graphics.Color
import android.graphics.BitmapFactory
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
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
import app.badcat.engine.ChapterCatalogQuery
import app.badcat.engine.EngineResult
import app.badcat.engine.ReadingEngine
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

/**
 * Book detail and table-of-contents screen backed by the selected reading source.
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
    private lateinit var chapterRows: LinearLayout
    private lateinit var catalogSearch: android.widget.EditText
    private var allChapters: List<BookChapter> = emptyList()
    private var reverseCatalog = false
    private lateinit var detailTitle: TextView
    private lateinit var detailMetadata: TextView
    private lateinit var coverView: ImageView
    private var introExpanded = false
    private var currentCoverUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty()
        bookUrl = intent.getStringExtra(EXTRA_BOOK_URL).orEmpty()
        bookName = intent.getStringExtra(EXTRA_BOOK_NAME).orEmpty()
        currentCoverUrl = intent.getStringExtra(EXTRA_BOOK_COVER_URL)
        engine = BadcatReadingEngineFactory(applicationContext).create()

        status = TextView(this).apply {
            text = "正在加载书籍详情…"
            textSize = 14f
            setTextColor(Color.rgb(90, 94, 102))
            setPadding(0, dpBook(12), 0, dpBook(12))
            maxLines = 4
            ellipsize = android.text.TextUtils.TruncateAt.END
            setOnClickListener {
                introExpanded = !introExpanded
                maxLines = if (introExpanded) Int.MAX_VALUE else 4
                ellipsize = if (introExpanded) null else android.text.TextUtils.TruncateAt.END
            }
        }
        chapterList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val favoriteButton = Button(this).apply {
            text = if (FavoriteBooks.contains(this@SourceBookActivity, sourceId, bookUrl)) "已收藏" else "加入收藏"
            setOnClickListener {
                val added = FavoriteBooks.add(this@SourceBookActivity, sourceId, bookUrl, bookName, currentCoverUrl)
                text = if (added) "已收藏" else "已收藏"
                android.widget.Toast.makeText(
                    this@SourceBookActivity,
                    if (added) "已加入小说收藏" else "这本小说已经收藏",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpBook(16))
            addView(TextView(this@SourceBookActivity).apply {
                text = "‹"; textSize = 32f; setTextColor(Color.DKGRAY)
                gravity = android.view.Gravity.CENTER
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(dpBook(42), dpBook(48)))
            addView(TextView(this@SourceBookActivity).apply {
                text = "小说详情"; textSize = 18f; setTextColor(Color.DKGRAY)
            })
        }
        coverView = ImageView(this).apply {
            contentDescription = "小说封面"
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(111, 151, 194))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(111, 151, 194))
                cornerRadius = dpBook(8).toFloat()
            }
            clipToOutline = true
            setImageResource(android.R.drawable.ic_menu_gallery)
        }
        detailTitle = TextView(this).apply {
            text = bookName
            textSize = 21f
            setTextColor(Color.rgb(40, 44, 52))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        detailMetadata = TextView(this).apply {
            text = "书源详情 · 目录加载中"
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(0, dpBook(8), 0, 0)
        }
        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.TOP
            addView(coverView, LinearLayout.LayoutParams(dpBook(92), dpBook(124)))
            addView(LinearLayout(this@SourceBookActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpBook(14), 0, 0, 0)
                addView(detailTitle)
                addView(detailMetadata)
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, dpBook(14), 0, dpBook(14))
            addView(favoriteButton, LinearLayout.LayoutParams(0, dpBook(48), 1f))
            addView(Button(this@SourceBookActivity).apply {
                text = "开始阅读"
                setOnClickListener {
                    val saved = getSharedPreferences("badcat_reading_progress", MODE_PRIVATE)
                        .getString(readingProgressKey(sourceId, bookUrl), null)
                    if (!saved.isNullOrBlank()) {
                        startActivity(Intent(this@SourceBookActivity, SourceChapterActivity::class.java).apply {
                            putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, sourceId)
                            putExtra(SourceChapterActivity.EXTRA_BOOK_URL, bookUrl)
                            putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, bookName)
                            putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, saved)
                        })
                    } else {
                        allChapters.firstOrNull()?.let { openChapter(it) }
                    }
                }
            }, LinearLayout.LayoutParams(0, dpBook(48), 1f).apply { leftMargin = dpBook(8) })
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpBook(18), dpBook(12), dpBook(18), dpBook(24))
            setBackgroundColor(Color.WHITE)
            addView(header)
            addView(summary)
            addView(actions)
            addView(TextView(this@SourceBookActivity).apply {
                text = "内容简介"; textSize = 17f; setTextColor(Color.DKGRAY)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(status)
            addView(TextView(this@SourceBookActivity).apply {
                text = "章节目录"; textSize = 17f; setTextColor(Color.DKGRAY)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dpBook(14), 0, dpBook(8))
            })
            val catalogTools = LinearLayout(this@SourceBookActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                catalogSearch = android.widget.EditText(this@SourceBookActivity).apply {
                    hint = "搜索章节名称"
                    textSize = 13f
                    isSingleLine = true
                    setPadding(dpBook(10), 0, dpBook(8), 0)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.rgb(246, 247, 249))
                        cornerRadius = dpBook(8).toFloat()
                    }
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderChapterRows() }
                        override fun afterTextChanged(s: android.text.Editable?) = Unit
                    })
                }
                addView(catalogSearch, LinearLayout.LayoutParams(0, dpBook(42), 1f))
                addView(Button(this@SourceBookActivity).apply {
                    text = "倒序"
                    setOnClickListener {
                        reverseCatalog = !reverseCatalog
                        text = if (reverseCatalog) "正序" else "倒序"
                        renderChapterRows()
                    }
                })
            }
            addView(catalogTools)
            addView(chapterList)
        }
        setContentView(ScrollView(this).apply { addView(root) })
        currentCoverUrl?.let { loadCover(it) }
        if (sourceId.isBlank() || bookUrl.isBlank()) {
            status.text = "缺少书源或书籍地址"
        } else {
            loadBook()
        }
    }

    private fun dpBook(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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
                        book.coverUrl?.takeIf { it.isNotBlank() }?.let { coverUrl ->
                            currentCoverUrl = coverUrl
                            FavoriteBooks.updateCover(this@SourceBookActivity, sourceId, bookUrl, coverUrl)
                            loadCover(coverUrl)
                        }
                        detailTitle.text = book.name.ifBlank { bookName }
                        detailMetadata.text = buildString {
                            append(book.author?.takeIf { it.isNotBlank() }?.let { "作者：$it" } ?: "作者：未知")
                            book.latestChapter?.takeIf { it.isNotBlank() }?.let { append("\n最新：").append(it) }
                        }
                        status.text = book.intro?.takeIf { it.isNotBlank() } ?: "暂无简介"
                        status.contentDescription = if (book.intro.isNullOrBlank()) "暂无简介" else "点击展开或收起简介"
                        status.maxLines = 4
                        introExpanded = false
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

    private fun loadCover(rawUrl: String) {
        worker.execute {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(rawUrl)
                require(url.protocol == "http" || url.protocol == "https")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "image/*")
                }
                connection.connect()
                if (connection.responseCode !in 200..299) return@execute
                if (connection.contentLength > 2 * 1024 * 1024) return@execute
                val bytes = connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 2 * 1024 * 1024) return@execute
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth > 8_000 || bounds.outHeight > 8_000) return@execute
                val sample = maxOf(1, Integer.highestOneBit(maxOf(bounds.outWidth / 720, bounds.outHeight / 960)))
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@execute
                mainHandler.post { if (!isFinishing && !isDestroyed) coverView.setImageBitmap(bitmap) }
            } catch (_: Exception) {
                // Keep the local placeholder when a source cover is unavailable.
            } finally {
                connection?.disconnect()
            }
        }
    }
    private fun showChapters(chapters: List<BookChapter>) {
        allChapters = chapters
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
        chapterRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        chapterList.addView(chapterRows)
        renderChapterRows()
    }

    private fun openChapter(chapter: BookChapter) {
        startActivity(Intent(this, SourceChapterActivity::class.java).apply {
            putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, sourceId)
            putExtra(SourceChapterActivity.EXTRA_BOOK_URL, bookUrl)
            putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, bookName)
            putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, chapter.url)
            putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, chapter.title)
        })
    }

    private fun renderChapterRows() {
        if (!::chapterRows.isInitialized) return
        chapterRows.removeAllViews()
        val query = if (::catalogSearch.isInitialized) catalogSearch.text.toString().trim() else ""
        val ordered = ChapterCatalogQuery.apply(allChapters, query, reverseCatalog)
        if (ordered.isEmpty()) {
            chapterRows.addView(TextView(this).apply {
                text = if (query.isBlank()) "目录为空" else "没有匹配的章节"
                textSize = 13f
                setTextColor(Color.GRAY)
                setPadding(0, dpBook(14), 0, dpBook(14))
            })
            return
        }
        ordered.forEach { chapter ->
            chapterRows.addView(TextView(this).apply {
                text = "${chapter.index + 1}. ${chapter.title}"
                textSize = 16f
                setPadding(0, 12, 0, 12)
                isClickable = true
                setOnClickListener { openChapter(chapter) }
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
        const val EXTRA_BOOK_COVER_URL = "book_cover_url"
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
        body = object : android.webkit.WebView(this) {
            override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
                super.onScrollChanged(l, t, oldl, oldt)
                updateReaderProgress()
            }
        }.apply {
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
                    view.post { updateReaderProgress() }
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
                    val maxScroll = (body.contentHeight * body.scale - body.height).toInt().coerceAtLeast(0)
                    body.post { body.scrollTo(0, (maxScroll * target / 100).coerceAtLeast(0)) }
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

    private fun updateReaderProgress() {
        if (!::chapterProgress.isInitialized || !::body.isInitialized) return
        val maxScroll = (body.contentHeight * body.scale - body.height).toInt().coerceAtLeast(0)
        chapterProgress.progress = if (maxScroll == 0) 0 else
            (body.scrollY * 100 / maxScroll).coerceIn(0, 100)
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
        val actions = arrayOf("字号减小", "字号增大", "行距减小", "行距增大", "切换背景", "CSS 自定义", "章节目录", "保存书签", "跳转本章书签", "恢复默认阅读设置")
        android.app.AlertDialog.Builder(this)
            .setTitle("阅读设置 · " + textSizeSp.toInt() + "sp · 行距 " + lineSpacingMultiplier)
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
                    9 -> resetReaderSettings()
                }
            }.show()
    }

    private fun resetReaderSettings() {
        textSizeSp = 18f
        lineSpacingMultiplier = 1.15f
        readerTheme = 0
        preferences.edit()
            .putFloat("reader_text_size", textSizeSp)
            .putFloat("reader_line_spacing", lineSpacingMultiplier)
            .putInt("reader_theme", readerTheme)
            .apply()
        applyReaderTheme()
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
