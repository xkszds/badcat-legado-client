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
import android.widget.Button
import android.view.View
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

/**
 * Book detail and table-of-contents screen backed by the selected reading source.
 */
class SourceBookActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val coverWorker = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var engine: ReadingEngine
    private lateinit var webViewPurificationAdapter: app.badcat.client.runtime.BadcatWebViewPurificationAdapter
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
    private lateinit var startReadingButton: Button
    private var pendingStartReading = false
    private var bookLoading = false
    private var introExpanded = false
    private var currentCoverUrl: String? = null
    private var currentBookStatus: String? = null
    private var candidateAuthor: String? = null
    private var candidateLatestChapter: String? = null
    private var candidateIntro: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty()
        bookUrl = intent.getStringExtra(EXTRA_BOOK_URL).orEmpty()
        bookName = intent.getStringExtra(EXTRA_BOOK_NAME).orEmpty()
        currentCoverUrl = intent.getStringExtra(EXTRA_BOOK_COVER_URL)
        currentBookStatus = intent.getStringExtra(EXTRA_BOOK_STATUS)
        candidateAuthor = intent.getStringExtra(EXTRA_BOOK_AUTHOR)
        candidateLatestChapter = intent.getStringExtra(EXTRA_BOOK_LATEST_CHAPTER)
        candidateIntro = intent.getStringExtra(EXTRA_BOOK_INTRO)
        val engineFactory = BadcatReadingEngineFactory(applicationContext)
        engine = engineFactory.create()
        webViewPurificationAdapter = engineFactory.webViewPurificationAdapter()

        status = TextView(this).apply {
            text = candidateIntro?.takeIf { it.isNotBlank() } ?: "正在加载书籍详情…"
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
                val added = FavoriteBooks.add(this@SourceBookActivity, sourceId, bookUrl, bookName, currentCoverUrl, currentBookStatus)
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
            addView(BadcatLineIconView(this@SourceBookActivity, "back", Color.DKGRAY).apply {
                contentDescription = "返回"
                isClickable = true
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
            text = buildString {
                append(candidateAuthor?.takeIf { it.isNotBlank() }?.let { "作者：$it" } ?: "作者：未知")
                candidateLatestChapter?.takeIf { it.isNotBlank() }?.let { append("\n最新：").append(it) }
                append("\n正在加载目录…")
            }
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
            startReadingButton = Button(this@SourceBookActivity).apply {
                text = "开始阅读"
                // Keep taps available while the TOC loads; the request is queued.
                setOnClickListener { beginReadingOrQueue() }
            }
            addView(startReadingButton, LinearLayout.LayoutParams(0, dpBook(48), 1f).apply {
                leftMargin = dpBook(8)
            })
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
        BadcatTheme.applyTree(this, window.decorView)
        currentCoverUrl?.let { loadCover(it) }
        if (sourceId.isBlank() || bookUrl.isBlank()) {
            status.text = "缺少书源或书籍地址"
        } else {
            loadBook()
        }
    }

    private fun beginReadingOrQueue() {
        if (allChapters.isEmpty()) {
            pendingStartReading = true
            status.text = "目录还在加载，加载完成后会自动打开第一章"
            android.widget.Toast.makeText(
                this,
                "正在等待目录加载，完成后自动开始阅读",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        pendingStartReading = false
        val readingState = BadcatReadingStateRepository(this)
        val saved = readingState.lastChapterUrl(sourceId, bookUrl)
        val resumeChapter = ChapterCatalogQuery.findByUrl(allChapters, saved)
        if (resumeChapter != null) {
            openChapter(resumeChapter)
        } else {
            // A source may remove or rewrite a chapter URL. Do not send the reader
            // to a stale URL; clear the invalid bookmark and start from the first chapter.
            if (!saved.isNullOrBlank()) {
                readingState.clearLastChapterUrl(sourceId, bookUrl)
            }
            openChapter(allChapters.first())
        }
    }

    private fun dpBook(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun loadBook() {
        if (bookLoading || isFinishing || isDestroyed) return
        bookLoading = true
        startReadingButton.isEnabled = false
        startReadingButton.text = "正在加载目录…"
        worker.execute {
            val (detail, toc) = runBlocking {
                coroutineScope {
                    val detailTask = async(Dispatchers.IO) {
                        safeEngineCall { engine.bookDetailWithFallback(sourceId, bookUrl, bookName) }
                    }
                    val tocTask = async(Dispatchers.IO) {
                        safeEngineCall {
                            engine.chaptersIncrementalWithFallback(sourceId, bookUrl, bookName) { partial ->
                                mainHandler.post {
                                    if (!isFinishing && !isDestroyed) showChapters(partial)
                                }
                            }
                        }
                    }
                    detailTask.await() to tocTask.await()
                }
            }
            mainHandler.post {
                if (isFinishing || isDestroyed) return@post
                bookLoading = false
                when (detail) {
                    is EngineResult.Failure -> {
                        val fallbackIntro = candidateIntro?.takeIf { it.isNotBlank() }
                        status.text = (fallbackIntro ?: "暂无简介") +
                            "\n\n详情加载失败（${detail.code}）：${detail.message}"
                    }
                    is EngineResult.Success -> {
                        val book = detail.value
                        book.status?.takeIf { it.isNotBlank() }?.let { sourceStatus ->
                            currentBookStatus = sourceStatus
                            FavoriteBooks.updateStatus(this@SourceBookActivity, sourceId, bookUrl, sourceStatus)
                        }
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
                        status.text = book.intro?.takeIf { it.isNotBlank() }
                            ?: candidateIntro?.takeIf { it.isNotBlank() }
                            ?: "暂无简介"
                        status.contentDescription = if (status.text == "暂无简介") "暂无简介" else "点击展开或收起简介"
                        status.maxLines = 4
                        introExpanded = false
                    }
                }
                when (toc) {
                    is EngineResult.Success -> showChapters(toc.value)
                    is EngineResult.Failure -> {
                        // Do not leave a failed catalog in the "waiting to read" state.
                        // If no chapters arrived, turn the primary action into an explicit retry.
                        pendingStartReading = false
                        if (status.text.isNotBlank()) status.append("\n\n")
                        status.append("目录加载失败（${toc.code}）：${toc.message}")
                        detailMetadata.append("\n目录加载失败")
                        if (allChapters.isEmpty()) {
                            startReadingButton.text = "重试目录"
                            startReadingButton.isEnabled = true
                            startReadingButton.setOnClickListener { loadBook() }
                        } else {
                            startReadingButton.text = "开始阅读"
                            startReadingButton.isEnabled = true
                            startReadingButton.setOnClickListener { beginReadingOrQueue() }
                        }
                    }
                }
                BadcatTheme.applyTree(this@SourceBookActivity, window.decorView)
            }
        }
    }

    private fun loadCover(rawUrl: String) {
        coverWorker.execute {
            var currentUrl = runCatching { java.net.URL(rawUrl) }.getOrNull() ?: return@execute
            if (!CoverUrlPolicy.isSafe(currentUrl)) return@execute
            var redirects = 0
            while (true) {
                val connection = try {
                    (currentUrl.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                        instanceFollowRedirects = false
                        setRequestProperty("Accept", "image/*")
                    }
                } catch (_: Exception) {
                    return@execute
                }
                try {
                    val responseCode = connection.responseCode
                    if (responseCode in COVER_REDIRECT_CODES) {
                        val location = connection.getHeaderField("Location") ?: return@execute
                        val next = CoverUrlPolicy.resolveRedirect(currentUrl, location) ?: return@execute
                        if (redirects >= MAX_COVER_REDIRECTS) return@execute
                        redirects++
                        currentUrl = next
                        continue
                    }
                    if (responseCode !in 200..299) return@execute
                    if (connection.contentLength > MAX_COVER_BYTES) return@execute
                    val bytes = connection.inputStream.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8 * 1024)
                        var total = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_COVER_BYTES) return@execute
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                        bounds.outWidth > MAX_COVER_DIMENSION || bounds.outHeight > MAX_COVER_DIMENSION
                    ) return@execute
                    val sample = maxOf(
                        1,
                        Integer.highestOneBit(maxOf(bounds.outWidth / 720, bounds.outHeight / 960)),
                    )
                    val bitmap = BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = sample },
                    ) ?: return@execute
                    mainHandler.post {
                        if (!isFinishing && !isDestroyed) coverView.setImageBitmap(bitmap)
                        else bitmap.recycle()
                    }
                    return@execute
                } catch (_: Exception) {
                    return@execute
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    private fun showChapters(chapters: List<BookChapter>) {
        allChapters = chapters
        startReadingButton.text = "开始阅读"
        startReadingButton.isEnabled = chapters.isNotEmpty()
        startReadingButton.setOnClickListener { beginReadingOrQueue() }
        if (pendingStartReading && chapters.isNotEmpty()) {
            pendingStartReading = false
            openChapter(chapters.first())
        }
        chapterList.removeAllViews()
        status.append("\n\n目录：${chapters.size} 章（点击章节开始阅读）")
        val readingState = BadcatReadingStateRepository(this)
        val savedChapterUrl = readingState.lastChapterUrl(sourceId, bookUrl)
        if (!savedChapterUrl.isNullOrBlank()) {
            chapterList.addView(Button(this).apply {
                text = "继续上次阅读"
                setOnClickListener {
                    val resumeChapter = ChapterCatalogQuery.findByUrl(chapters, savedChapterUrl)
                    if (resumeChapter == null) {
                        android.widget.Toast.makeText(
                            this@SourceBookActivity,
                            "上次章节无法唯一定位，请从目录选择章节",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                        return@setOnClickListener
                    }
                    startActivity(Intent(this@SourceBookActivity, SourceChapterActivity::class.java).apply {
                        putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, sourceId)
                        putExtra(SourceChapterActivity.EXTRA_BOOK_URL, bookUrl)
                        putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, bookName)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, resumeChapter.url)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_INDEX, resumeChapter.index)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, resumeChapter.title)
                    })
                }
            })
        }
        if (chapters.isEmpty()) {
            chapterList.addView(TextView(this).apply { text = "目录为空" })
            return
        }
        val readingState = BadcatReadingStateRepository(this)
        val bookmarked = chapters.filter { readingState.isBookmarked(sourceId, bookUrl, it.url) }
        if (bookmarked.isNotEmpty()) {
            chapterList.addView(Button(this).apply {
                text = "本书书签（" + bookmarked.size + "）"
                setOnClickListener {
                    val labels = bookmarked.map { chapter -> (chapter.index + 1).toString() + ". " + chapter.title }.toTypedArray()
                    BadcatTheme.alertBuilder(this@SourceBookActivity)
                        .setTitle("选择书签章节")
                        .setItems(labels) { _, which ->
                            val chapter = bookmarked[which]
                            BadcatTheme.alertBuilder(this@SourceBookActivity)
                                .setTitle(chapter.title)
                                .setItems(arrayOf("打开书签", "删除书签")) { _, action ->
                                    if (action == 0) {
                                        startActivity(Intent(this@SourceBookActivity, SourceChapterActivity::class.java).apply {
                                            putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, sourceId)
                                            putExtra(SourceChapterActivity.EXTRA_BOOK_URL, bookUrl)
                                            putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, bookName)
                                            putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, chapter.url)
                                            putExtra(SourceChapterActivity.EXTRA_CHAPTER_INDEX, chapter.index)
                                            putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, chapter.title)
                                            putExtra(SourceChapterActivity.EXTRA_JUMP_TO_BOOKMARK, true)
                                        })
                                    } else {
                                        readingState.removeBookmark(sourceId, bookUrl, chapter.url)
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
        BadcatTheme.applyTree(this, window.decorView)
    }

    private fun openChapter(chapter: BookChapter) {
        startActivity(Intent(this, SourceChapterActivity::class.java).apply {
            putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, sourceId)
            putExtra(SourceChapterActivity.EXTRA_BOOK_URL, bookUrl)
            putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, bookName)
            putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, chapter.url)
            putExtra(SourceChapterActivity.EXTRA_CHAPTER_INDEX, chapter.index)
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
        coverWorker.shutdownNow()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        BadcatTheme.applyTree(this, window.decorView)
    }

    companion object {
        private val COVER_REDIRECT_CODES = setOf(300, 301, 302, 303, 307, 308)
        private const val MAX_COVER_REDIRECTS = 5
        private const val MAX_COVER_BYTES = 2 * 1024 * 1024
        private const val MAX_COVER_DIMENSION = 8_000

        const val EXTRA_SOURCE_ID = "source_id"
        const val EXTRA_BOOK_URL = "book_url"
        const val EXTRA_BOOK_NAME = "book_name"
        const val EXTRA_BOOK_COVER_URL = "book_cover_url"
        const val EXTRA_BOOK_STATUS = "book_status"
        const val EXTRA_BOOK_AUTHOR = "book_author"
        const val EXTRA_BOOK_LATEST_CHAPTER = "book_latest_chapter"
        const val EXTRA_BOOK_INTRO = "book_intro"
    }
}

/** Development reader with TOC-based chapter navigation and saved progress. */
class SourceChapterActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var engine: ReadingEngine
    private lateinit var webViewPurificationAdapter: app.badcat.client.runtime.BadcatWebViewPurificationAdapter
    private lateinit var status: TextView
    private lateinit var body: android.webkit.WebView
    private lateinit var previousButton: android.widget.Button
    private lateinit var nextButton: android.widget.Button
    private lateinit var readerScroll: android.webkit.WebView
    private lateinit var readerRoot: LinearLayout
    private lateinit var chapterProgress: android.widget.SeekBar
    private lateinit var chapterTitleView: TextView
    private lateinit var readerInfo: TextView
    private val readerUiPrefs by lazy { getSharedPreferences(ReaderSettingsActivity.PREFS, MODE_PRIVATE) }
    private var lockScreenRunnable: Runnable? = null
    private val infoRefreshRunnable = object : Runnable {
        override fun run() {
            if (::readerInfo.isInitialized && !isFinishing && !isDestroyed) {
                updateReaderProgress()
                mainHandler.postDelayed(this, 60_000L)
            }
        }
    }
    private lateinit var sourceId: String
    private lateinit var bookUrl: String
    private lateinit var bookName: String
    private var chapters: List<BookChapter> = emptyList()
    private var currentIndex: Int = -1
    private var currentChapterUrl: String = ""
    private var requestedChapterIndex: Int = -1
    private var textSizeSp: Float = 18f
    private var lineSpacingMultiplier: Float = 1.15f
    private var readerTheme: Int = 0
    private var currentChapterText: String = ""
    private var currentChapterTitle: String = ""
    private var pendingScrollY: Int? = null
    private var pendingBookmarkY: Int? = null
    private var lastScrollPositionPersistedAt: Long = 0L
    private var readerDocumentVersion: Long = 0L
    private var latestReaderBaseUrl: String = "https://reader.invalid/"
    private var readingSessionStartedAt: Long = 0L
    private var lastBatterySampleAt: Long = 0L
    private var cachedBatteryPercent: Int? = null
    private val preferences by lazy {
        getSharedPreferences("badcat_reading_progress", MODE_PRIVATE)
    }
    private val readingState by lazy { BadcatReadingStateRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val engineFactory = BadcatReadingEngineFactory(applicationContext)
        engine = engineFactory.create()
        webViewPurificationAdapter = engineFactory.webViewPurificationAdapter()
        sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty()
        bookUrl = intent.getStringExtra(EXTRA_BOOK_URL).orEmpty()
        bookName = intent.getStringExtra(EXTRA_BOOK_NAME).orEmpty()
        currentChapterUrl = intent.getStringExtra(EXTRA_CHAPTER_URL).orEmpty()
        requestedChapterIndex = intent.getIntExtra(EXTRA_CHAPTER_INDEX, -1)
        val chapterTitle = intent.getStringExtra(EXTRA_CHAPTER_TITLE).orEmpty()
        if (currentChapterUrl.isBlank()) {
            currentChapterUrl = readingState.lastChapterUrl(sourceId, bookUrl).orEmpty()
        }
        textSizeSp = preferences.getFloat("reader_text_size", 18f).coerceIn(14f, 32f)
        lineSpacingMultiplier = preferences.getFloat("reader_line_spacing", 1.15f).coerceIn(1.0f, 2.0f)
        readerTheme = preferences.getInt("reader_theme", 1).coerceIn(0, 2)

        status = TextView(this).apply { text = "正在加载目录…" }
        body = object : android.webkit.WebView(this) {
            override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
                super.onScrollChanged(l, t, oldl, oldt)
                updateReaderProgress()
                persistScrollPositionIfNeeded()
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
                    // Ignore callbacks from an older HTML load; only the newest document
                    // may consume the pending bookmark/scroll restoration position.
                    if (url != latestReaderBaseUrl) return
                    webViewPurificationAdapter.onPageFinished(view, url.orEmpty())
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
        val back = BadcatLineIconView(this, "back", Color.rgb(55, 55, 55)).apply {
            contentDescription = "返回"
            isClickable = true
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
        readerInfo = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.GRAY)
            gravity = android.view.Gravity.CENTER
            setPadding(dpReader(10), dpReader(5), dpReader(10), dpReader(5))
            setBackgroundColor(Color.rgb(250, 250, 250))
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
        val readerActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(dpReader(8), dpReader(3), dpReader(8), dpReader(3))
            setBackgroundColor(Color.rgb(250, 250, 250))
            fun action(label: String, onClick: () -> Unit) {
                addView(TextView(this@SourceChapterActivity).apply {
                    text = label
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                    setTextColor(Color.rgb(75, 125, 185))
                    isClickable = true
                    isFocusable = true
                    setPadding(dpReader(10), dpReader(9), dpReader(10), dpReader(9))
                    setOnClickListener { onClick() }
                }, LinearLayout.LayoutParams(0, dpReader(40), 1f))
            }
            action("目录") { showChapterCatalog() }
            action("换源") { showSourceSwitchDialog() }
            action("亮度") { cycleReaderTheme() }
            action("设置") { showReaderTools() }
        }
        readerRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(topBar)
            addView(status)
            addView(body, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f,
            ))
            addView(readerActions)
            addView(readerInfo, LinearLayout.LayoutParams(-1, -2))
            addView(bottomBar)
        }
        readerScroll = body
        applyReaderTheme()
        setContentView(readerRoot)
        applyReaderUiSettings()
        if (sourceId.isBlank() || bookUrl.isBlank() || currentChapterUrl.isBlank()) {
            status.text = "缺少书源、书籍地址或章节地址"
            return
        }
        loadInitial(chapterTitle)
    }

    private fun loadInitial(fallbackTitle: String) {
        status.text = "正在加载正文和目录…"
        worker.execute {
            runBlocking {
                coroutineScope {
                    val tocTask = async(Dispatchers.IO) {
                        safeEngineCall { engine.chapters(sourceId, bookUrl) }
                    }
                    // Indexed launches keep the fast parallel path. Legacy URL-only
                    // launches must resolve against the fresh TOC before requesting content.
                    val resolvedLegacyChapter: BookChapter? = if (requestedChapterIndex < 0) {
                        val currentToc = tocTask.await()
                        val currentCatalog = (currentToc as? EngineResult.Success)?.value.orEmpty()
                        ChapterCatalogQuery.findByUrl(currentCatalog, currentChapterUrl)
                    } else {
                        null
                    }
                    val content: EngineResult<app.badcat.engine.ChapterContent> =
                        if (requestedChapterIndex >= 0) {
                            async(Dispatchers.IO) {
                                safeEngineCall {
                                    engine.chapterContent(
                                        sourceId, bookUrl,
                                        BookChapter(requestedChapterIndex, chapterTitle, currentChapterUrl),
                                        bookName,
                                    )
                                }
                            }.await()
                        } else if (resolvedLegacyChapter != null) {
                            safeEngineCall {
                                engine.chapterContent(sourceId, bookUrl, resolvedLegacyChapter, bookName)
                            }
                        } else {
                            EngineResult.Failure(
                                EngineErrorCode.NOT_FOUND,
                                "保存的章节地址在当前目录中不存在或不唯一，未请求正文",
                            )
                        }
                    // Indexed launches render as soon as content is ready without waiting for TOC.
                    val fallbackChapter = resolvedLegacyChapter ?: BookChapter(
                        index = requestedChapterIndex.coerceAtLeast(0),
                        title = fallbackTitle.ifBlank { bookName },
                        url = currentChapterUrl,
                    )
                    mainHandler.post {
                        if (isFinishing || isDestroyed) return@post
                        currentIndex = 0
                        renderChapter(
                            fallbackChapter,
                            content,
                            EngineResult.Success(emptyList()),
                        )
                    }

                    // Navigation metadata can arrive later without blocking the text.
                    val toc = tocTask.await()
                    val chapterList = (toc as? EngineResult.Success)?.value.orEmpty()
                    val selected = ChapterCatalogQuery.findByIdentity(chapterList, requestedChapterIndex.takeIf { it >= 0 }, currentChapterUrl)
                        ?.let { chapterList.indexOf(it) } ?: -1
                    mainHandler.post {
                        if (isFinishing || isDestroyed) return@post
                        chapters = chapterList
                        if (selected < 0 && chapterList.isNotEmpty()) {
                            // A stale or ambiguous legacy URL must never silently select another chapter.
                            currentIndex = -1
                            status.text = "保存的章节地址已失效或无法唯一定位，请返回目录重新选择"
                            return@post
                        }
                        currentIndex = selected
                        previousButton.isEnabled = selected > 0
                        nextButton.isEnabled = selected >= 0 && selected < chapterList.lastIndex
                        if (toc is EngineResult.Failure) {
                            status.append("\n目录导航不可用：${toc.message}")
                        }
                        updateReaderProgress()
                    }
                }
            }
        }
    }

    private fun saveCurrentScrollPosition() {
        if (!::readerScroll.isInitialized || currentChapterUrl.isBlank()) return
        readingState.saveChapterScrollY(sourceId, bookUrl, currentChapterUrl, readerScroll.scrollY)
        lastScrollPositionPersistedAt = android.os.SystemClock.elapsedRealtime()
    }

    /**
     * Persist during long reading sessions as well as on pause/chapter changes.
     * This limits writes to roughly one per second while reducing progress loss
     * if Android kills the process before onPause can run.
     */
    private fun persistScrollPositionIfNeeded() {
        if (currentChapterUrl.isBlank()) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastScrollPositionPersistedAt < SCROLL_POSITION_SAVE_INTERVAL_MS) return
        readingState.saveChapterScrollY(sourceId, bookUrl, currentChapterUrl, body.scrollY)
        lastScrollPositionPersistedAt = now
    }

    private fun restoreCurrentScrollPosition() {
        val savedY = readingState.chapterScrollY(sourceId, bookUrl, currentChapterUrl)
        pendingScrollY = savedY
    }

    private fun updateReaderProgress() {
        if (!::chapterProgress.isInitialized || !::body.isInitialized) return
        val progress = ReaderProgressMetrics.calculate(
            scrollY = body.scrollY,
            contentHeight = body.contentHeight,
            scale = body.scale,
            viewportHeight = body.height,
        )
        val percent = progress.percent
        chapterProgress.progress = percent
        if (!::readerInfo.isInitialized) return

        val pieces = mutableListOf<String>()
        if (readerUiPrefs.getBoolean(ReaderSettingsActivity.KEY_SHOW_PROGRESS, true)) {
            if (readerUiPrefs.getString(ReaderSettingsActivity.KEY_PROGRESS_MODE, "percent") == "pages") {
                val chapterCount = chapters.size.coerceAtLeast(1)
                pieces.add("第${progress.currentPage}/${progress.totalPages}页 · 第${(currentIndex + 1).coerceAtLeast(1)}/${chapterCount}章")
            } else {
                pieces.add("${percent}%")
            }
        }
        if (readerUiPrefs.getBoolean(ReaderSettingsActivity.KEY_SHOW_TIME, true)) {
            pieces.add(java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
        }
        if (readerUiPrefs.getBoolean(ReaderSettingsActivity.KEY_SHOW_BATTERY, true)) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (lastBatterySampleAt == 0L || now - lastBatterySampleAt >= 60_000L) {
                val battery = registerReceiver(
                    null,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED),
                )
                val level = battery?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = battery?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100) ?: 100
                cachedBatteryPercent = if (level >= 0 && scale > 0) {
                    (level * 100 / scale).coerceIn(0, 100)
                } else null
                lastBatterySampleAt = now
            }
            cachedBatteryPercent?.let { pieces.add("电量 ${it}%") }
        }
        readerInfo.text = pieces.joinToString("   ")
        readerInfo.visibility = if (pieces.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun applyReaderUiSettings() {
        val showProgress = readerUiPrefs.getBoolean(ReaderSettingsActivity.KEY_SHOW_PROGRESS, true)
        chapterProgress.visibility = if (showProgress) View.VISIBLE else View.GONE
        val keepStatusBar = readerUiPrefs.getBoolean(ReaderSettingsActivity.KEY_STATUS_BAR, true)
        if (keepStatusBar) window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        else window.setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        updateReaderProgress()
        scheduleReadingLock()
    }

    private fun scheduleReadingLock() {
        lockScreenRunnable?.let { mainHandler.removeCallbacks(it) }
        lockScreenRunnable = null
        val minutes = readerUiPrefs.getInt(ReaderSettingsActivity.KEY_LOCK_MINUTES, 0).coerceIn(0, 60)
        if (minutes <= 0) {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            return
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val task = Runnable {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            lockScreenRunnable = null
        }
        lockScreenRunnable = task
        mainHandler.postDelayed(task, minutes * 60_000L)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::body.isInitialized) scheduleReadingLock()
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (readerUiPrefs.getBoolean(ReaderSettingsActivity.KEY_VOLUME_PAGE, true) &&
            (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP)
        ) {
            val direction = if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) 1 else -1
            if (::body.isInitialized) {
                body.post {
                    val distance = (body.height * 0.88f).toInt().coerceAtLeast(1)
                    body.scrollBy(0, distance * direction)
                }
            }
            scheduleReadingLock()
            return true
        }
        return super.onKeyDown(keyCode, event)
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
        BadcatTheme.alertBuilder(this)
            .setTitle("章节目录（${chapters.size} 章）")
            .setItems(labels) { _, index -> loadChapterAt(index) }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showSourceSwitchDialog() {
        status.text = "正在读取可用书源…"
        worker.execute {
            val result = runBlocking { safeEngineCall { engine.sources() } }
            mainHandler.post {
                if (isFinishing || isDestroyed) return@post
                when (result) {
                    is EngineResult.Success -> {
                        val sources = result.value.filter { it.enabled && !it.searchUrl.isNullOrBlank() && it.id != sourceId }
                        if (sources.isEmpty()) {
                            status.text = "没有其他已启用的可搜索书源"
                            return@post
                        }
                        BadcatTheme.alertBuilder(this@SourceChapterActivity)
                            .setTitle("选择换源")
                            .setItems(sources.map { it.name }.toTypedArray()) { _, index ->
                                searchBookInSource(sources[index])
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                    is EngineResult.Failure -> status.text = "读取书源失败：${result.message}"
                }
            }
        }
    }

    private fun searchBookInSource(source: app.badcat.engine.BookSource) {
        status.text = "正在「${source.name}」中查找《${bookName}》…"
        worker.execute {
            val result = runBlocking { safeEngineCall { engine.searchPage(source.id, bookName, 1) } }
            mainHandler.post {
                if (isFinishing || isDestroyed) return@post
                when (result) {
                    is EngineResult.Success -> {
                        val candidates = result.value.distinctBy { it.bookUrl }
                        if (candidates.isEmpty()) {
                            status.text = "该书源没有找到匹配的书籍"
                            return@post
                        }
                        BadcatTheme.alertBuilder(this@SourceChapterActivity)
                            .setTitle("选择《${bookName}》")
                            .setItems(candidates.map { it.name + (it.author?.takeIf(String::isNotBlank)?.let { author -> " · " + author } ?: "") }.toTypedArray()) { _, index ->
                                val candidate = candidates[index]
                                startActivity(Intent(this@SourceChapterActivity, SourceBookActivity::class.java).apply {
                                    putExtra(SourceBookActivity.EXTRA_SOURCE_ID, candidate.sourceId)
                                    putExtra(SourceBookActivity.EXTRA_BOOK_URL, candidate.bookUrl)
                                    putExtra(SourceBookActivity.EXTRA_BOOK_NAME, candidate.name)
                                    putExtra(SourceBookActivity.EXTRA_BOOK_COVER_URL, candidate.coverUrl)
                                })
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                    is EngineResult.Failure -> status.text = "换源搜索失败：${result.message}"
                }
            }
        }
    }

    private fun loadChapterAt(target: Int) {
        if (target !in chapters.indices || target == currentIndex) return
        saveCurrentScrollPosition()
        val chapter = chapters[target]
        previousButton.isEnabled = false
        nextButton.isEnabled = false
        status.text = "正在加载：${chapter.title}"
        worker.execute {
            val result = runBlocking { safeEngineCall { engine.chapterContent(sourceId, bookUrl, chapter, bookName) } }
            mainHandler.post {
                if (isFinishing || isDestroyed) return@post
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
                if (currentChapterText.isBlank()) {
                    status.text = "正文解析结果为空，请检查该书源的正文规则"
                }
                if (readingSessionStartedAt == 0L) readingSessionStartedAt = android.os.SystemClock.elapsedRealtime()
                renderReaderDocument()
                currentChapterUrl = chapter.url
                readingState.saveLastChapterUrl(sourceId, bookUrl, chapter.url)
                readingState.saveHistory(sourceId, bookUrl, bookName.ifBlank { "未命名书籍" }, chapter.url, result.value.title.ifBlank { chapter.title })
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
                val bookmarkY = readingState.bookmarkScrollY(sourceId, bookUrl, currentChapterUrl) ?: -1
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
        readingState.saveBookmark(
            sourceId, bookUrl, currentChapterUrl, bookName, currentChapterTitle, position,
        )
        status.text = "书签已保存（当前位置），可在「我的 - 书签管理」查看"
    }

    private fun jumpToChapterBookmark() {
        if (currentChapterUrl.isBlank()) return
        val savedPosition = readingState.bookmarkScrollY(sourceId, bookUrl, currentChapterUrl)
        if (savedPosition == null) {
            status.text = "本章还没有保存书签"
            return
        }
        val position = savedPosition
        readerScroll.post {
            readerScroll.post { readerScroll.scrollTo(0, position) }
        }
        status.text = "已跳转到本章书签"
    }

    private fun showReaderTools() {
        val actions = arrayOf("字号减小", "字号增大", "行距减小", "行距增大", "切换背景", "阅读设置", "CSS 自定义", "章节目录", "保存书签", "跳转本章书签", "恢复默认阅读设置")
        BadcatTheme.alertBuilder(this)
            .setTitle("阅读设置 · " + textSizeSp.toInt() + "sp · 行距 " + lineSpacingMultiplier)
            .setItems(actions) { _, index ->
                when (index) {
                    0 -> changeTextSize(-1f)
                    1 -> changeTextSize(1f)
                    2 -> changeLineSpacing(-0.1f)
                    3 -> changeLineSpacing(0.1f)
                    4 -> cycleReaderTheme()
                    5 -> startActivity(Intent(this, ReaderSettingsActivity::class.java))
                    6 -> startActivity(Intent(this, ReaderCssActivity::class.java))
                    7 -> showChapterCatalog()
                    8 -> saveChapterBookmark()
                    9 -> jumpToChapterBookmark()
                    10 -> resetReaderSettings()
                }
            }.show()
    }

    private fun resetReaderSettings() {
        textSizeSp = 18f
        lineSpacingMultiplier = 1.15f
        readerTheme = 1
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
        // Keep the current reading position when changing font, spacing, theme or CSS.
        // A saved chapter position/bookmark already queued for restoration takes priority.
        if (currentChapterUrl.isNotBlank() && pendingBookmarkY == null && pendingScrollY == null &&
            body.contentHeight > 0
        ) {
            pendingScrollY = body.scrollY.coerceAtLeast(0)
        }
        val (background, foreground) = when (readerTheme) {
            1 -> Color.rgb(244, 234, 211) to Color.rgb(75, 61, 43)
            2 -> Color.rgb(28, 30, 34) to Color.rgb(220, 222, 226)
            else -> Color.WHITE to Color.rgb(35, 35, 35)
        }
        val html = ReaderCssDocument.build(
            context = this,
            text = currentChapterText,
            textSizeSp = textSizeSp,
            lineSpacing = lineSpacingMultiplier,
            background = background,
            foreground = foreground,
            bookTitle = bookName,
            chapterTitle = currentChapterTitle,
        )
        latestReaderBaseUrl = "https://reader.invalid/${++readerDocumentVersion}/"
        body.loadDataWithBaseURL(latestReaderBaseUrl, html, "text/html", "UTF-8", null)
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
        window.statusBarColor = background
        window.navigationBarColor = background
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (readerTheme == 2) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        if (currentChapterText.isNotEmpty()) renderReaderDocument()
        status.setTextColor(foreground)
        if (::readerInfo.isInitialized) {
            readerInfo.setTextColor(foreground)
            readerInfo.setBackgroundColor(background)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) {
            applyReaderUiSettings()
            mainHandler.removeCallbacks(infoRefreshRunnable)
            mainHandler.postDelayed(infoRefreshRunnable, 60_000L)
        }
        if (::body.isInitialized && currentChapterText.isNotEmpty()) {
            pendingScrollY = body.scrollY
            renderReaderDocument()
            if (readingSessionStartedAt == 0L) readingSessionStartedAt = android.os.SystemClock.elapsedRealtime()
        }
    }

    override fun onPause() {
        mainHandler.removeCallbacks(infoRefreshRunnable)
        saveCurrentScrollPosition()
        if (readingSessionStartedAt > 0L) {
            val elapsed = (android.os.SystemClock.elapsedRealtime() - readingSessionStartedAt).coerceAtLeast(0L)
            val stats = getSharedPreferences("badcat_reading_stats", MODE_PRIVATE)
            stats.edit().putLong("total_reading_ms", stats.getLong("total_reading_ms", 0L) + elapsed).apply()
            readingSessionStartedAt = 0L
        }
        super.onPause()
    }

    override fun onDestroy() {
        lockScreenRunnable?.let { mainHandler.removeCallbacks(it) }
        mainHandler.removeCallbacks(infoRefreshRunnable)
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        worker.shutdownNow()
        super.onDestroy()
    }

    companion object {
        private const val SCROLL_POSITION_SAVE_INTERVAL_MS = 1_000L

        const val EXTRA_SOURCE_ID = "reader_source_id"
        const val EXTRA_BOOK_URL = "reader_book_url"
        const val EXTRA_BOOK_NAME = "reader_book_name"
        const val EXTRA_CHAPTER_URL = "reader_chapter_url"
        const val EXTRA_CHAPTER_TITLE = "reader_chapter_title"
        const val EXTRA_CHAPTER_INDEX = "reader_chapter_index"
        const val EXTRA_JUMP_TO_BOOKMARK = "reader_jump_to_bookmark"
    }
}
