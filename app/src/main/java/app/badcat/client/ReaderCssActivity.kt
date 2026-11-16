package app.badcat.client

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Local editor for named reader CSS profiles and their page-child selector rules. */
class ReaderCssActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("reader_css_profiles", MODE_PRIVATE) }
    private lateinit var list: LinearLayout
    private lateinit var message: TextView

    private data class SelectorRule(val name: String, val note: String, val selector: String, val css: String)
    private data class Profile(
        val id: String,
        val name: String,
        val note: String,
        val enabled: Boolean,
        val rules: List<SelectorRule>,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        root.addView(TextView(this).apply {
            text = "阅读页 CSS 自定义"
            textSize = 22f
        })
        root.addView(TextView(this).apply {
            text = "可创建多套样式方案；每套方案可添加多个页面子选择器，并分别填写名称、备注、CSS 选择器和样式声明。"
            textSize = 13f
            setPadding(0, 8, 0, 12)
        })
        root.addView(Button(this).apply {
            text = "＋ 新建 CSS 方案"
            setOnClickListener { editProfile(null) }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        message = TextView(this).apply { setPadding(0, 12, 0, 0) }
        root.addView(message)
        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
    }

    private fun readProfiles(): List<Profile> = runCatching {
        val array = JSONArray(prefs.getString(KEY_PROFILES, "[]"))
        (0 until array.length()).mapNotNull { i ->
            val item = array.optJSONObject(i) ?: return@mapNotNull null
            val rulesJson = item.optJSONArray("rules") ?: JSONArray()
            val rules = (0 until rulesJson.length()).mapNotNull { j ->
                val rule = rulesJson.optJSONObject(j) ?: return@mapNotNull null
                SelectorRule(
                    rule.optString("name"), rule.optString("note"),
                    rule.optString("selector"), rule.optString("css"),
                )
            }
            Profile(item.optString("id"), item.optString("name"), item.optString("note"),
                item.optBoolean("enabled"), rules)
        }
    }.getOrDefault(emptyList())

    private fun saveProfiles(profiles: List<Profile>) {
        val array = JSONArray()
        profiles.forEach { profile ->
            val rules = JSONArray()
            profile.rules.forEach { rule ->
                rules.put(JSONObject().put("name", rule.name).put("note", rule.note)
                    .put("selector", rule.selector).put("css", rule.css))
            }
            array.put(JSONObject().put("id", profile.id).put("name", profile.name)
                .put("note", profile.note).put("enabled", profile.enabled).put("rules", rules))
        }
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    private fun refresh() {
        list.removeAllViews()
        val profiles = readProfiles()
        if (profiles.isEmpty()) {
            list.addView(TextView(this).apply { text = "还没有 CSS 方案，点击上方按钮创建。" })
        }
        profiles.forEach { profile ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 14, 0, 14)
            }
            card.addView(TextView(this).apply {
                text = profile.name + if (profile.note.isBlank()) "" else "\n备注：" + profile.note
                textSize = 16f
            })
            card.addView(TextView(this).apply {
                text = "子选择器：${profile.rules.size}"
                textSize = 12f
            })
            card.addView(Switch(this).apply {
                text = "启用此方案"
                isChecked = profile.enabled
                setOnCheckedChangeListener { _, checked ->
                    saveProfiles(readProfiles().map {
                        if (it.id == profile.id) it.copy(enabled = checked) else it
                    })
                }
            })
            card.addView(Button(this).apply {
                text = "管理方案 / 子选择器"
                setOnClickListener { manageProfile(profile.id) }
            })
            card.addView(Button(this).apply {
                text = "删除方案"
                setOnClickListener {
                    android.app.AlertDialog.Builder(this@ReaderCssActivity)
                        .setMessage("删除方案“${profile.name}”及其全部子选择器？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除") { _, _ ->
                            saveProfiles(readProfiles().filterNot { it.id == profile.id })
                            refresh()
                        }.show()
                }
            })
            list.addView(card)
            list.addView(android.view.View(this).apply {
                setBackgroundColor(0x22000000)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
            })
        }
    }

    private fun editProfile(existing: Profile?) {
        val name = EditText(this).apply { hint = "方案名称（必填）"; setText(existing?.name.orEmpty()) }
        val note = EditText(this).apply { hint = "方案备注"; setText(existing?.note.orEmpty()) }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 4, 20, 4)
            addView(name); addView(note)
        }
        android.app.AlertDialog.Builder(this).setTitle(if (existing == null) "新建 CSS 方案" else "编辑方案信息")
            .setView(box).setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val label = name.text.toString().trim()
                if (label.isEmpty()) {
                    message.text = "方案名称不能为空"
                    return@setPositiveButton
                }
                val old = readProfiles()
                val updated = existing?.copy(name = label, note = note.text.toString().trim())
                    ?: Profile(UUID.randomUUID().toString(), label, note.text.toString().trim(), true, emptyList())
                saveProfiles(if (existing == null) old + updated else old.map { if (it.id == updated.id) updated else it })
                refresh()
            }.show()
    }

    private fun manageProfile(id: String) {
        val profile = readProfiles().firstOrNull { it.id == id } ?: return
        val labels = profile.rules.map { "${it.name.ifBlank { it.selector }}\n${it.selector}" }.toTypedArray()
        val actions = arrayOf("添加子选择器", "编辑方案名称和备注", "编辑已有子选择器", "删除子选择器", "上移子选择器（调整优先级）", "下移子选择器（调整优先级）")
        android.app.AlertDialog.Builder(this).setTitle(profile.name)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> editRule(id, null)
                    1 -> editProfile(profile)
                    2 -> if (profile.rules.isEmpty()) message.text = "请先添加子选择器" else
                        android.app.AlertDialog.Builder(this).setTitle("选择要编辑的子选择器")
                            .setItems(labels) { _, index -> editRule(id, index) }.show()
                    3 -> if (profile.rules.isEmpty()) message.text = "没有可删除的子选择器" else
                        android.app.AlertDialog.Builder(this).setTitle("选择要删除的子选择器")
                            .setItems(labels) { _, index ->
                                saveProfiles(readProfiles().map { p ->
                                    if (p.id == id) p.copy(rules = p.rules.filterIndexed { i, _ -> i != index }) else p
                                })
                                refresh()
                            }.show()
                    4, 5 -> if (profile.rules.size < 2) message.text = "至少需要两个子选择器才能调整顺序" else
                        android.app.AlertDialog.Builder(this).setTitle("选择要${if (which == 4) "上移" else "下移"}的子选择器")
                            .setItems(labels) { _, index ->
                                val target = if (which == 4) index - 1 else index + 1
                                if (target !in profile.rules.indices) {
                                    message.text = "已经到达${if (which == 4) "最前" else "最后"}位置"
                                } else {
                                    saveProfiles(readProfiles().map { p ->
                                        if (p.id != id) p else {
                                            val reordered = p.rules.toMutableList()
                                            val selected = reordered.removeAt(index)
                                            reordered.add(target, selected)
                                            p.copy(rules = reordered)
                                        }
                                    })
                                    refresh()
                                    message.text = "子选择器顺序已调整；后面的规则在 CSS 层叠中优先"
                                }
                            }.show()
                }
            }.setNegativeButton("关闭", null).show()
    }

    private fun editRule(profileId: String, index: Int?) {
        val profile = readProfiles().firstOrNull { it.id == profileId } ?: return
        val old = index?.let { profile.rules.getOrNull(it) }
        val name = EditText(this).apply { hint = "子选择器名称"; setText(old?.name.orEmpty()) }
        val note = EditText(this).apply { hint = "备注 / 用途"; setText(old?.note.orEmpty()) }
        val selector = EditText(this).apply {
            hint = "CSS 选择器，例如 .reader-content、.reader-paragraph:nth-child(2)"
            setText(old?.selector.orEmpty())
        }
        val css = EditText(this).apply {
            hint = "CSS 声明，例如 font-size: 18px; line-height: 1.8;"
            setText(old?.css.orEmpty())
            minLines = 3
            gravity = android.view.Gravity.TOP
        }
        val presetButton = Button(this).apply {
            text = "插入常用页面选择器"
            setOnClickListener {
                val presets = arrayOf(
                    ".reader-root — 阅读页整体",
                    ".reader-content — 正文容器",
                    ".reader-paragraph — 所有段落",
                    ".reader-paragraph:first-child — 第一段",
                    ".reader-paragraph:nth-child(2) — 第二段",
                    ".reader-paragraph:last-child — 最后一段",
                )
                val values = arrayOf(
                    ".reader-root", ".reader-content", ".reader-paragraph",
                    ".reader-paragraph:first-child", ".reader-paragraph:nth-child(2)",
                    ".reader-paragraph:last-child",
                )
                android.app.AlertDialog.Builder(this@ReaderCssActivity)
                    .setTitle("选择要插入的选择器")
                    .setItems(presets) { _, selected -> selector.setText(values[selected]) }
                    .show()
            }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 4, 20, 4)
            addView(name); addView(note); addView(presetButton); addView(selector); addView(css)
            addView(TextView(this@ReaderCssActivity).apply {
                text = "仅支持增强阅读器生成的正文节点。示例：font-size: 18px; line-height: 1.8; color: #555555;"
                textSize = 12f
            })
        }
        android.app.AlertDialog.Builder(this).setTitle(if (old == null) "添加页面子选择器" else "编辑页面子选择器")
            .setView(ScrollView(this).apply { addView(box) })
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val selectorText = selector.text.toString().trim()
                if (selectorText.isEmpty()) {
                    message.text = "CSS 选择器不能为空"
                    return@setPositiveButton
                }
                val rule = SelectorRule(name.text.toString().trim(), note.text.toString().trim(),
                    selectorText, css.text.toString().trim())
                val current = readProfiles()
                saveProfiles(current.map { p ->
                    if (p.id != profileId) p else {
                        val rules = p.rules.toMutableList()
                        if (index == null) rules.add(rule) else if (index in rules.indices) rules[index] = rule
                        p.copy(rules = rules)
                    }
                })
                refresh()
                message.text = "子选择器已保存，返回阅读页即可应用"
            }.show()
    }

    companion object { private const val KEY_PROFILES = "profiles_v1" }
}
