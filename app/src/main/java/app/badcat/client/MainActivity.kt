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
import android.widget.TextView
import org.json.JSONArray

/** Compact search landing page; the same search bar is reused on results. */
class MainActivity : Activity() {
    private var contentType = "小说"
    private var appliedSearchBarMode = "integrated"
    private var appliedCategoryMode = "top"
    private var appliedSearchBarTopSpacing = 24
    private var darkMode = false
    private lateinit var homeSearchBar: BadcatSearchBarView
    private lateinit var recentReadingPanel: LinearLayout
    private val blue = Color.rgb(55, 137, 222)
    private val ink get() = if (darkMode) Color.rgb(235, 237, 240) else Color.rgb(48, 55, 66)
    private val muted get() = if (darkMode) Color.rgb(165, 172, 182) else Color.rgb(135, 145, 158)
    private val page get() = if (darkMode) Color.rgb(25, 28, 33) else Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        darkMode = getSharedPreferences("badcat_appearance", MODE_PRIVATE)
            .getString("appearance_mode", "light") == "dark"
        appliedSearchBarMode = searchBarMode()
        appliedCategoryMode = categoryDisplayMode()
        appliedSearchBarTopSpacing = searchBarTopSpacing()
        window.statusBarColor = blue
        window.navigationBarColor = if (darkMode) Color.rgb(25, 28, 33) else Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (darkMode) 0 else View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContentView(buildHome())
        refreshRecentReadingPanel()
    }

    private fun buildHome(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(page)
        addView(buildHeader())
        recentReadingPanel = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(8))
            visibility = View.GONE
        }
        addView(recentReadingPanel)
        // Keep the remaining home canvas calm; only real saved reading records are shown.
        addView(View(this@MainActivity), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        addView(MainBottomNavigation.create(this@MainActivity, "search"))
    }

    private fun buildHeader(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(18), dp(10), dp(18), dp(12))
        background = rounded(blue, 0, 0, 0, 18, 18)

        addView(BadcatHomeLogo.create(this@MainActivity), LinearLayout.LayoutParams(
            dp(132), dp(40)
        ).apply { gravity = Gravity.CENTER_HORIZONTAL })

        // Original home hierarchy: 小说 / 漫画 / 综合 directly under the Badcat wordmark.
        val categoryMode = categoryDisplayMode()
        val typeChoices = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, dp(7))
        }
        val typeLabels = mutableListOf<TextView>()
        var syncSearchType: ((String) -> Unit)? = null
        fun renderTypeChoices() {
            typeLabels.forEach { label ->
                val chosen = label.tag == contentType
                label.setTextColor(if (chosen) Color.WHITE else Color.rgb(215, 230, 247))
                label.typeface = if (chosen) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                label.alpha = if (chosen) 1f else 0.78f
            }
        }
        val configuredCategories = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getStringSet("home_visible_categories", setOf("小说", "漫画", "综合"))
            ?.toSet() ?: setOf("小说", "漫画", "综合")
        val availableCategories = HomeCategorySelection.visibleCategories(configuredCategories)
        if (contentType !in availableCategories) {
            contentType = HomeCategorySelection.initialSelection(contentType, availableCategories)
        }
        availableCategories.forEach { type ->
            val label = TextView(this@MainActivity).apply {
                tag = type
                text = type
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(6), dp(20), dp(6))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    contentType = type
                    syncSearchType?.invoke(type)
                    renderTypeChoices()
                }
            }
            typeLabels += label
            typeChoices.addView(label, LinearLayout.LayoutParams(-2, dp(38)).apply { marginStart = dp(7); marginEnd = dp(7) })
        }
        if (categoryMode == "top") {
            addView(typeChoices, LinearLayout.LayoutParams(-1, dp(48)))
            renderTypeChoices()
        }

        val bar = BadcatSearchBarView(
            context = this@MainActivity,
            appearance = searchBarMode(),
            dark = darkMode,
            onSearch = { query, type ->
                contentType = type
                openSearch(query)
            },
            onTypeChanged = { contentType = it },
            homePageStyle = true,
            showHomeTypeSelector = categoryMode == "selector",
        )
        bar.setHomeTapToNavigate {
            startActivity(Intent(this@MainActivity, SourceSearchActivity::class.java).apply {
                putExtra(SourceSearchActivity.EXTRA_CONTENT_TYPE, contentType)
                putExtra(SourceSearchActivity.EXTRA_FOCUS_SEARCH, true)
            })
        }
        homeSearchBar = bar
        syncSearchType = { selected -> bar.setContentType(selected) }
        val configuredBarHeight = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getInt("home_search_bar_height_dp", 40).coerceIn(34, 46)
        addView(bar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(configuredBarHeight)
        ).apply {
            topMargin = dp(searchBarTopSpacing())
        })
    }

    private fun refreshRecentReadingPanel() {
        if (!::recentReadingPanel.isInitialized) return
        recentReadingPanel.removeAllViews()
        val raw = getSharedPreferences(ReadingHistoryActivity.PREFS, MODE_PRIVATE)
            .getString(ReadingHistoryActivity.KEY, "[]").orEmpty()
        val records = runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                json.optJSONObject(index)?.takeIf { item ->
                    item.optString("sourceId").isNotBlank() &&
                        item.optString("bookUrl").isNotBlank() &&
                        item.optString("chapterUrl").isNotBlank()
                }
            }.sortedByDescending { it.optLong("updatedAt") }.take(3)
        }.getOrDefault(emptyList())

        if (records.isEmpty()) {
            recentReadingPanel.visibility = View.GONE
            return
        }
        recentReadingPanel.visibility = View.VISIBLE
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = "最近阅读"
                textSize = 17f
                setTextColor(ink)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(TextView(this@MainActivity).apply {
                text = "全部"
                textSize = 13f
                setTextColor(blue)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, ReadingHistoryActivity::class.java))
                }
            })
        }
        recentReadingPanel.addView(heading, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })

        records.forEach { item ->
            val bookName = item.optString("bookName").ifBlank { "未命名书籍" }
            val chapterTitle = item.optString("chapterTitle").ifBlank { "继续阅读" }
            val sourceId = item.optString("sourceId")
            val bookUrl = item.optString("bookUrl")
            val chapterUrl = item.optString("chapterUrl")
            recentReadingPanel.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = rounded(
                    if (darkMode) Color.rgb(38, 43, 51) else Color.rgb(247, 249, 252),
                    12,
                )
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, SourceChapterActivity::class.java).apply {
                        putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, sourceId)
                        putExtra(SourceChapterActivity.EXTRA_BOOK_URL, bookUrl)
                        putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, bookName)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, chapterUrl)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, chapterTitle)
                    })
                }
                addView(TextView(this@MainActivity).apply {
                    text = bookName
                    textSize = 15f
                    setTextColor(ink)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                addView(TextView(this@MainActivity).apply {
                    text = chapterTitle
                    textSize = 12f
                    setTextColor(muted)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding(0, dp(4), 0, 0)
                })
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) })
        }
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
        val saved = prefs.getInt("home_search_top_spacing_dp", 12)
        val migrated = if (saved !in listOf(4, 12, 24)) 12 else saved
        if (migrated != saved) prefs.edit().putInt("home_search_top_spacing_dp", migrated).apply()
        return migrated
    }

    private fun searchBarMode(): String = "integrated"

    private fun categoryDisplayMode(): String =
        getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getString("home_category_display_mode", "top")
            ?.takeIf { it == "selector" || it == "top" } ?: "top"

    override fun onResume() {
        super.onResume()
        val selected = searchBarMode()
        val selectedSpacing = searchBarTopSpacing()
        val selectedCategoryMode = categoryDisplayMode()
        if (selectedCategoryMode != appliedCategoryMode) {
            appliedCategoryMode = selectedCategoryMode
            recreate()
            return
        }
        refreshRecentReadingPanel()
        if (selected != appliedSearchBarMode) {
            appliedSearchBarMode = selected
            appliedSearchBarTopSpacing = selectedSpacing
            recreate()
            return
        }
        if (selectedSpacing != appliedSearchBarTopSpacing) {
            appliedSearchBarTopSpacing = selectedSpacing
            applyHomeSearchBarPosition(selectedSpacing)
        }
        val currentDark = getSharedPreferences("badcat_appearance", MODE_PRIVATE)
            .getString("appearance_mode", "light") == "dark"
        if (currentDark != darkMode) recreate()
    }

    private fun applyHomeSearchBarPosition(spacingDp: Int) {
        if (!::homeSearchBar.isInitialized) return
        val params = homeSearchBar.layoutParams as? LinearLayout.LayoutParams ?: return
        params.topMargin = dp(spacingDp)
        homeSearchBar.layoutParams = params
        homeSearchBar.requestLayout()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val mode = searchBarMode()
        val spacing = searchBarTopSpacing()
        val categoryMode = categoryDisplayMode()
        if (categoryMode != appliedCategoryMode) {
            appliedCategoryMode = categoryMode
            recreate()
            return
        }
        if (mode != appliedSearchBarMode) {
            appliedSearchBarMode = mode
            appliedSearchBarTopSpacing = spacing
            recreate()
        } else if (spacing != appliedSearchBarTopSpacing) {
            appliedSearchBarTopSpacing = spacing
            applyHomeSearchBarPosition(spacing)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
