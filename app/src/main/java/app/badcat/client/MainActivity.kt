package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

/** Compact search landing page; the same search bar is reused on results. */
class MainActivity : Activity() {
    private var contentType = "小说"
    private var appliedSearchBarMode = "integrated"
    private var appliedSearchBarTopSpacing = 22
    private var darkMode = false
    private lateinit var homeContent: LinearLayout
    private val blue = Color.rgb(55, 137, 222)
    private val ink = Color.rgb(48, 55, 66)
    private val muted = Color.rgb(135, 145, 158)
    private val page get() = if (darkMode) Color.rgb(25, 28, 33) else Color.rgb(247, 249, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        darkMode = getSharedPreferences("badcat_appearance", MODE_PRIVATE)
            .getString("appearance_mode", "light") == "dark"
        appliedSearchBarMode = searchBarMode()
        appliedSearchBarTopSpacing = searchBarTopSpacing()
        window.statusBarColor = blue
        window.navigationBarColor = if (darkMode) Color.rgb(25, 28, 33) else Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (darkMode) 0 else View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContentView(buildHome())
    }

    private fun buildHome(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(page)
        addView(buildHeader())
        val scroll = ScrollView(this@MainActivity).apply {
            isFillViewport = true
            homeContent = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(18), dp(16), dp(22))
            }
            addView(homeContent)
        }
        addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        addView(MainBottomNavigation.create(this@MainActivity, "search"))
        refreshRecentReading()
    }

    /**
     * The former home body was an empty weighted spacer. Show real local reading
     * history instead of reintroducing removed ranking/lottery/cash shortcuts.
     */
    private fun refreshRecentReading() {
        if (!::homeContent.isInitialized) return
        homeContent.removeAllViews()
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = "最近阅读"
                textSize = 17f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(if (darkMode) Color.rgb(235, 237, 240) else ink)
            }, LinearLayout.LayoutParams(0, dp(34), 1f))
            addView(TextView(this@MainActivity).apply {
                text = "全部"
                textSize = 13f
                setTextColor(blue)
                gravity = Gravity.CENTER
                setPadding(dp(12), 0, dp(4), 0)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, ReadingHistoryActivity::class.java))
                }
            }, LinearLayout.LayoutParams(-2, dp(34)))
        }
        homeContent.addView(heading)

        val records = runCatching {
            val raw = getSharedPreferences(ReadingHistoryActivity.PREFS, MODE_PRIVATE)
                .getString(ReadingHistoryActivity.KEY, "[]").orEmpty()
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                json.optJSONObject(index)?.takeIf {
                    it.optString("sourceId").isNotBlank() &&
                        it.optString("bookUrl").isNotBlank() &&
                        it.optString("chapterUrl").isNotBlank()
                }
            }.sortedByDescending { it.optLong("updatedAt", 0L) }.take(3)
        }.getOrDefault(emptyList())

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(
                if (darkMode) Color.rgb(38, 42, 49) else Color.WHITE,
                14
            )
            setPadding(dp(14), dp(4), dp(14), dp(4))
        }
        if (records.isEmpty()) {
            card.addView(TextView(this).apply {
                text = "还没有阅读记录\n搜索并打开一本小说，阅读进度会自动保存在这里。"
                textSize = 13f
                setTextColor(if (darkMode) Color.rgb(165, 172, 182) else muted)
                setPadding(dp(4), dp(18), dp(4), dp(18))
            })
        } else {
            records.forEachIndexed { index, item ->
                card.addView(recentReadingRow(item))
                if (index != records.lastIndex) {
                    card.addView(View(this).apply {
                        setBackgroundColor(if (darkMode) Color.rgb(57, 62, 70) else Color.rgb(238, 240, 243))
                    }, LinearLayout.LayoutParams(-1, dp(1)))
                }
            }
        }
        homeContent.addView(card)
    }

    private fun recentReadingRow(item: JSONObject): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), dp(13), dp(4), dp(13))
        isClickable = true
        isFocusable = true
        val bookName = item.optString("bookName").ifBlank { "未命名小说" }
        val chapterTitle = item.optString("chapterTitle").ifBlank { "继续阅读" }
        addView(TextView(this@MainActivity).apply {
            text = bookName
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(if (darkMode) Color.rgb(235, 237, 240) else ink)
            maxLines = 1
        })
        addView(TextView(this@MainActivity).apply {
            text = chapterTitle
            textSize = 12f
            setTextColor(if (darkMode) Color.rgb(165, 172, 182) else muted)
            maxLines = 1
            setPadding(0, dp(5), 0, 0)
        })
        setOnClickListener {
            startActivity(Intent(this@MainActivity, SourceChapterActivity::class.java).apply {
                putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, item.optString("sourceId"))
                putExtra(SourceChapterActivity.EXTRA_BOOK_URL, item.optString("bookUrl"))
                putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, bookName)
                putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, item.optString("chapterUrl"))
                putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, chapterTitle)
            })
        }
    }

    private fun buildHeader(): View = LinearLayout(this).apply {
        val compact = searchBarMode() == "small"
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(18), dp(if (compact) 8 else 10), dp(18), dp(if (compact) 10 else 12))
        background = rounded(blue, 0, 0, 0, 18, 18)

        addView(BadcatHomeLogo.create(this@MainActivity), LinearLayout.LayoutParams(
            dp(if (compact) 154 else 178), dp(if (compact) 48 else 56)
        ).apply { gravity = Gravity.CENTER_HORIZONTAL })

        val bar = BadcatSearchBarView(
            context = this@MainActivity,
            appearance = searchBarMode(),
            dark = darkMode,
            onSearch = { query, type ->
                contentType = type
                openSearch(query)
            },
            onTypeChanged = { contentType = it },
        )
        bar.setHomeTapToNavigate {
            startActivity(Intent(this@MainActivity, SourceSearchActivity::class.java).apply {
                putExtra(SourceSearchActivity.EXTRA_CONTENT_TYPE, contentType)
                putExtra(SourceSearchActivity.EXTRA_FOCUS_SEARCH, true)
            })
        }
        addView(bar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(if (compact) 44 else 48)
        ).apply {
            topMargin = dp(searchBarTopSpacing())
        })
    }

    private fun openSearch(query: String) {
        if (query.isBlank()) {
            android.widget.Toast.makeText(this, "请输入书名或作者", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, SourceSearchActivity::class.java).apply {
            putExtra(SourceSearchActivity.EXTRA_INITIAL_QUERY, query)
            putExtra(SourceSearchActivity.EXTRA_CONTENT_TYPE, contentType)
        })
    }

    private fun rounded(
        color: Int, radius: Int, tl: Int = radius, tr: Int = radius, br: Int = radius, bl: Int = radius,
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadii = floatArrayOf(
            dp(tl).toFloat(), dp(tl).toFloat(), dp(tr).toFloat(), dp(tr).toFloat(),
            dp(br).toFloat(), dp(br).toFloat(), dp(bl).toFloat(), dp(bl).toFloat()
        )
    }

    private fun searchBarTopSpacing(): Int {
        val prefs = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
        val saved = prefs.getInt("home_search_top_spacing_dp", 46)
        // Earlier builds defaulted to 22dp, which left the search field too close
        // to the logo. Migrate that old default to a visibly lower position.
        val migrated = if (saved !in listOf(34, 46, 58)) 46 else saved
        if (migrated != saved) prefs.edit().putInt("home_search_top_spacing_dp", migrated).apply()
        return migrated.coerceIn(24, 72)
    }

    private fun searchBarMode(): String =
        getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getString("home_layout_mode", "integrated")
            ?.takeIf { it == "small" || it == "integrated" } ?: "integrated"

    override fun onResume() {
        super.onResume()
        refreshRecentReading()
        val selected = searchBarMode()
        val selectedSpacing = searchBarTopSpacing()
        if (selected != appliedSearchBarMode || selectedSpacing != appliedSearchBarTopSpacing) {
            appliedSearchBarMode = selected
            appliedSearchBarTopSpacing = selectedSpacing
            recreate()
        }
        val currentDark = getSharedPreferences("badcat_appearance", MODE_PRIVATE)
            .getString("appearance_mode", "light") == "dark"
        if (currentDark != darkMode) recreate()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
