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

/** Compact search landing page; the same search bar is reused on results. */
class MainActivity : Activity() {
    private var contentType = "小说"
    private var appliedSearchBarMode = "integrated"
    private var appliedSearchBarTopSpacing = 24
    private var darkMode = false
    private lateinit var homeSearchBar: BadcatSearchBarView
    private val blue = Color.rgb(55, 137, 222)
    private val ink get() = if (darkMode) Color.rgb(235, 237, 240) else Color.rgb(48, 55, 66)
    private val muted get() = if (darkMode) Color.rgb(165, 172, 182) else Color.rgb(135, 145, 158)
    private val page get() = if (darkMode) Color.rgb(25, 28, 33) else Color.WHITE

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
        addView(buildHomeEmptyState(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        addView(MainBottomNavigation.create(this@MainActivity, "search"))
    }

    private fun buildHomeEmptyState(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(page)
        addView(buildHomeShortcuts(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(94)
        ))
        val errorPanel = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(20), dp(24), dp(24))
            addView(TextView(this@MainActivity).apply {
                text = "🐈"
                textSize = 72f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(185, 207, 242))
            }, LinearLayout.LayoutParams(-1, dp(132)))
            addView(TextView(this@MainActivity).apply {
                text = "服务器异常"
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(ink)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            addView(TextView(this@MainActivity).apply {
                text = "当前服务出了点问题，请稍候再试"
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(muted)
                setPadding(0, dp(8), 0, dp(22))
            })
            addView(TextView(this@MainActivity).apply {
                text = "刷新重试"
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = rounded(blue, 28)
                setPadding(dp(54), dp(13), dp(54), dp(13))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, SourceSearchActivity::class.java).apply {
                        putExtra(SourceSearchActivity.EXTRA_CONTENT_TYPE, contentType)
                    })
                }
            })
        }
        addView(errorPanel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
    }

    private fun buildHomeShortcuts(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setBackgroundColor(if (darkMode) Color.rgb(31, 35, 42) else Color.WHITE)
        listOf("赚取现金").forEach { label ->
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(TextView(this@MainActivity).apply {
                    text = "🐾"
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setTextColor(if (darkMode) Color.rgb(180, 188, 201) else Color.rgb(210, 213, 219))
                    background = rounded(if (darkMode) Color.rgb(43, 48, 57) else Color.rgb(247, 248, 250), 40)
                    setPadding(dp(12), dp(7), dp(12), dp(7))
                }, LinearLayout.LayoutParams(dp(48), dp(48)))
                addView(TextView(this@MainActivity).apply {
                    text = label
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setTextColor(muted)
                    setPadding(0, dp(5), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, -1, 1f))
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

        // Original home hierarchy: 小说 / 漫画 / 综合 directly under the Badcat wordmark.
        val typeChoices = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(2))
        }
        val typeLabels = mutableListOf<TextView>()
        fun renderTypeChoices() {
            typeLabels.forEach { label ->
                val chosen = label.tag == contentType
                label.setTextColor(if (chosen) Color.WHITE else Color.rgb(215, 230, 247))
                label.typeface = if (chosen) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                label.alpha = if (chosen) 1f else 0.78f
            }
        }
        listOf("小说", "漫画", "综合").forEach { type ->
            val label = TextView(this@MainActivity).apply {
                tag = type
                text = type
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(5), dp(18), dp(5))
                isClickable = true
                isFocusable = true
                setOnClickListener { contentType = type; renderTypeChoices() }
            }
            typeLabels += label
            typeChoices.addView(label, LinearLayout.LayoutParams(-2, dp(32)))
        }
        addView(typeChoices, LinearLayout.LayoutParams(-1, dp(34)))
        renderTypeChoices()

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
        )
        bar.setHomeTapToNavigate {
            startActivity(Intent(this@MainActivity, SourceSearchActivity::class.java).apply {
                putExtra(SourceSearchActivity.EXTRA_CONTENT_TYPE, contentType)
                putExtra(SourceSearchActivity.EXTRA_FOCUS_SEARCH, true)
            })
        }
        homeSearchBar = bar
        val configuredBarHeight = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getInt("home_search_bar_height_dp", 40).coerceIn(34, 46)
        addView(bar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(configuredBarHeight)
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
        val saved = prefs.getInt("home_search_top_spacing_dp", 12)
        val migrated = if (saved !in listOf(4, 12, 24)) 12 else saved
        if (migrated != saved) prefs.edit().putInt("home_search_top_spacing_dp", migrated).apply()
        return migrated
    }

    private fun searchBarMode(): String =
        getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getString("home_layout_mode", "integrated")
            ?.takeIf { it == "small" || it == "integrated" } ?: "integrated"

    override fun onResume() {
        super.onResume()
        val selected = searchBarMode()
        val selectedSpacing = searchBarTopSpacing()
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
