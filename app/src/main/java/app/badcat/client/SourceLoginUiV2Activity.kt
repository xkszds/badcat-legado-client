package app.badcat.client

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import app.badcat.client.runtime.LegadoSourceLoginUiV2Service
import app.badcat.engine.BookSource
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoLoginUiV2
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Native host for the declarative Legado Login UI v2 contract.
 * Form values and source-returned state are kept separately. Login JSON is
 * stored app-privately; it is not silently copied into request headers.
 */
class SourceLoginUiV2Activity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val service by lazy { LegadoSourceLoginUiV2Service() }
    private val preferences by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private lateinit var sourceId: String
    private lateinit var content: LinearLayout
    private lateinit var message: TextView
    private lateinit var source: BookSource
    private val controls = linkedMapOf<String, View>()
    private var stateJson: String = "{}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(24))
        }
        root.addView(TextView(this).apply { text = "书源登录"; textSize = 22f })
        message = TextView(this).apply {
            textSize = 13f
            setPadding(0, dp(8), 0, dp(12))
        }
        root.addView(message)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)
        setContentView(ScrollView(this).apply { addView(root) })
        if (sourceId.isBlank()) {
            message.text = "缺少书源标识"
            return
        }
        loadSource()
    }

    private fun loadSource() {
        scope.launch {
            val found = try {
                withContext(Dispatchers.IO) {
                    when (val result = FileBookSourceCatalog(applicationContext).all()) {
                        is EngineResult.Success -> result.value.firstOrNull { it.id == sourceId }
                        is EngineResult.Failure -> throw IllegalStateException(result.message)
                    }
                }
            } catch (error: Exception) {
                message.text = "读取书源失败：" + (error.message ?: "未知错误")
                return@launch
            }
            if (found == null) {
                message.text = "找不到该书源，请返回书源管理刷新列表"
                return@launch
            }
            source = found
            if (!LegadoLoginUiV2.isV2(source.loginUi)) {
                message.text = "此书源未声明 Login UI v2，暂不支持此登录界面"
                return@launch
            }
            stateJson = preferences.getString(stateKey(sourceId), "{}") ?: "{}"
            render()
        }
    }

    private fun render() {
        scope.launch {
            content.removeAllViews()
            controls.clear()
            message.text = "正在读取 ${source.name} 的登录表单…"
            when (val result = withContext(Dispatchers.IO) { service.render(source, stateJson) }) {
                is EngineResult.Failure -> message.text = "登录表单读取失败（${result.code}）：${result.message}"
                is EngineResult.Success -> {
                    message.text = "书源：${source.name}。填写表单后点击对应操作按钮。"
                    result.value.forEach { row ->
                        content.addView(TextView(this@SourceLoginUiV2Activity).apply {
                            text = row.name
                            textSize = 14f
                            setPadding(0, dp(10), 0, dp(4))
                        })
                        when (row.type) {
                            LegadoLoginUiV2.RowType.LABEL -> content.addView(TextView(this@SourceLoginUiV2Activity).apply {
                                text = row.value.orEmpty()
                                textSize = 13f
                            })
                            LegadoLoginUiV2.RowType.TEXT,
                            LegadoLoginUiV2.RowType.PASSWORD -> {
                                val key = row.key ?: return@forEach
                                val input = EditText(this@SourceLoginUiV2Activity).apply {
                                    hint = row.hint.orEmpty()
                                    isSingleLine = true
                                    inputType = if (row.type == LegadoLoginUiV2.RowType.PASSWORD)
                                        android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                                    else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                                    setText(readFormValue(key, row.value))
                                }
                                controls[key] = input
                                content.addView(input, matchWidth())
                            }
                            LegadoLoginUiV2.RowType.SELECT -> {
                                val key = row.key ?: return@forEach
                                val spinner = Spinner(this@SourceLoginUiV2Activity)
                                spinner.adapter = ArrayAdapter(
                                    this@SourceLoginUiV2Activity,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    row.options,
                                )
                                val selected = readFormValue(key, row.value)
                                spinner.setSelection(row.options.indexOf(selected).takeIf { it >= 0 } ?: 0)
                                controls[key] = spinner
                                content.addView(spinner, matchWidth())
                            }
                            LegadoLoginUiV2.RowType.TOGGLE -> {
                                val key = row.key ?: return@forEach
                                val toggle = Switch(this@SourceLoginUiV2Activity).apply {
                                    text = row.hint.orEmpty()
                                    isChecked = readFormValue(key, row.value ?: "false").equals("true", true)
                                }
                                controls[key] = toggle
                                content.addView(toggle)
                                row.action?.let { action ->
                                    toggle.setOnCheckedChangeListener { _, _ -> dispatch(action) }
                                }
                            }
                            LegadoLoginUiV2.RowType.BUTTON -> {
                                val action = row.action ?: return@forEach
                                content.addView(Button(this@SourceLoginUiV2Activity).apply {
                                    text = row.value?.takeIf { it.isNotBlank() } ?: row.name
                                    setOnClickListener { dispatch(action) }
                                }, matchWidth())
                            }
                        }
                    }
                    if (!source.loginCheckJs.isNullOrBlank()) {
                        content.addView(Button(this@SourceLoginUiV2Activity).apply {
                            text = "检查登录状态"
                            setOnClickListener { checkLogin() }
                        }, matchWidth())
                    }
                }
            }
        }
    }

    private fun dispatch(action: String) {
        scope.launch {
            val form = collectForm()
            persistForm(form)
            message.text = "正在执行：$action"
            when (val result = withContext(Dispatchers.IO) {
                service.dispatchAction(source, action, stateJson, form.toString())
            }) {
                is EngineResult.Failure -> message.text = "登录操作失败（${result.code}）：${result.message}"
                is EngineResult.Success -> {
                    val value = result.value
                    value.stateJson?.let {
                        stateJson = it
                        preferences.edit().putString(stateKey(sourceId), it).apply()
                    }
                    value.loginJson?.let {
                        preferences.edit().putString(loginKey(sourceId), it).apply()
                    }
                    message.text = when {
                        value.errors.isNotEmpty() -> value.errors.entries.joinToString("；") { "${it.key}：${it.value}" }
                        value.loginJson != null -> "书源已返回登录结果并保存在本机。登录脚本可调用 source.putLoginHeader(JSON 字符串) 保存请求头，后续书源请求会自动携带；Cookie 由 HTTP 响应自动保存。"
                        value.unknownKeys.isNotEmpty() -> "操作已完成；忽略未识别字段：${value.unknownKeys.joinToString()}"
                        else -> "操作已完成"
                    }
                    if (value.close) finish() else render()
                }
            }
        }
    }

    private fun checkLogin() {
        scope.launch {
            message.text = "正在检查登录状态…"
            when (val result = withContext(Dispatchers.IO) { service.checkLogin(source) }) {
                is EngineResult.Success -> message.text = if (result.value) "书源报告：已登录" else "书源报告：未登录"
                is EngineResult.Failure -> message.text = "登录状态检查失败（${result.code}）：${result.message}"
            }
        }
    }

    private fun collectForm(): JSONObject = JSONObject().apply {
        controls.forEach { (key, view) ->
            val value = when (view) {
                is EditText -> view.text.toString()
                is Spinner -> view.selectedItem?.toString().orEmpty()
                is Switch -> view.isChecked.toString()
                else -> ""
            }
            put(key, value)
        }
    }

    private fun readFormValue(key: String, fallback: String?): String =
        preferences.getString(formKey(sourceId, key), null) ?: fallback.orEmpty()

    private fun persistForm(form: JSONObject) {
        val editor = preferences.edit()
        form.keys().forEach { key ->
            editor.putString(formKey(sourceId, key), form.optString(key))
        }
        editor.apply()
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onPause() {
        if (::source.isInitialized) persistForm(collectForm())
        super.onPause()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun stateKey(id: String) = "state_$id"
    private fun loginKey(id: String) = "login_$id"
    private fun formKey(id: String, key: String) = "form_${id}_$key"

    companion object {
        const val EXTRA_SOURCE_ID = "source_login_source_id"
        private const val PREFS = "badcat_legado_login_ui_v2"
    }
}
