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
        // Keep the home surface clean: no unrequested recent-reading panel.
        addView(View(this@MainActivity), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        addView(MainBottomNavigation.create(this@MainActivity, "search"))
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
        homeSearchBar = bar
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
        val saved = prefs.getInt("home_search_top_spacing_dp", 24)
        // Keep the three layout choices visually distinct and bounded.
        val migrated = if (saved !in listOf(8, 24, 40)) 24 else saved
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
