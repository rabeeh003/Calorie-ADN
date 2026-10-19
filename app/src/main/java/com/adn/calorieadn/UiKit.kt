package com.adn.calorieadn

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Editable
import android.text.TextWatcher
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

internal object Palette {
    val ink = Color.rgb(28, 48, 41)
    val green = Color.rgb(28, 91, 69)
    val muted = Color.rgb(102, 117, 107)
    val cream = Color.rgb(247, 248, 242)
    val line = Color.rgb(222, 229, 218)
    val mint = Color.rgb(228, 239, 219)
    val orange = Color.rgb(235, 164, 91)
    val red = Color.rgb(166, 58, 49)
}
internal fun Context.dp(value: Int) = (value * resources.displayMetrics.density).toInt()
internal fun Context.shape(color: Int, radius: Int = 20, border: Int? = null) = GradientDrawable().apply {
    setColor(color); cornerRadius = dp(radius).toFloat()
    if (border != null) setStroke(dp(1), border)
}
internal fun MainActivity.label(value: String, size: Float = 15f, color: Int = Palette.ink, bold: Boolean = false) = TextView(this).apply {
    text = value; textSize = size; setTextColor(color)
    if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    setLineSpacing(dp(3).toFloat(), 1f)
}
internal fun MainActivity.column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
internal fun MainActivity.row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
internal fun LinearLayout.put(view: View, margin: Int = 0) {
    addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = context.dp(margin) })
}
internal fun MainActivity.card(color: Int = Color.WHITE, build: LinearLayout.() -> Unit): LinearLayout = column().apply {
    background = shape(color, 24, if (color == Color.WHITE) Palette.line else null)
    setPadding(dp(20), dp(20), dp(20), dp(20)); build()
}
internal fun MainActivity.button(value: String, primary: Boolean = true, action: () -> Unit) = Button(this).apply {
    text = value; textSize = 15f; isAllCaps = false; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    setTextColor(if (primary) Color.WHITE else Palette.green)
    background = RippleDrawable(ColorStateList.valueOf(0x22000000), shape(if (primary) Palette.green else Palette.mint, 16), null)
    minHeight = dp(52); minimumHeight = dp(52); setPadding(dp(16), dp(10), dp(16), dp(10))
    stateListAnimator = null; setOnClickListener { action() }
}
internal fun MainActivity.section(title: String, detail: String? = null) {
    add(column().apply { put(label(title, 20f, bold = true), if (detail == null) 0 else 4); if (detail != null) put(label(detail, 13f, Palette.muted)) })
}
internal fun MainActivity.notice(title: String, message: String, color: Int = Palette.mint) = card(color) {
    put(label(title, 15f, Palette.green, true), 5); put(label(message, 13f, Palette.muted))
}
internal fun MainActivity.input(parent: LinearLayout, title: String, value: String = "", numeric: Boolean = false, multiline: Boolean = false, changed: (String) -> Unit = {}): EditText {
    parent.put(label(title, 13f, Palette.muted, true), 6)
    val edit = EditText(this).apply {
        contentDescription = title
        setText(value); textSize = 16f; setTextColor(Palette.ink); setHintTextColor(Palette.muted)
        background = shape(Palette.cream, 12, Palette.line); minHeight = dp(52)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        if (multiline) { inputType = inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE; minLines = 3; gravity = Gravity.TOP }
        else setSingleLine(true)
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    parent.put(edit, 16); return edit
}
internal fun MainActivity.choice(parent: LinearLayout, title: String, options: Array<String>, selected: String, changed: (String) -> Unit): Spinner {
    parent.put(label(title, 13f, Palette.muted, true), 6)
    val spinner = Spinner(this).apply {
        adapter = ArrayAdapter(this@choice, android.R.layout.simple_spinner_dropdown_item, options)
        setSelection(options.indexOf(selected).coerceAtLeast(0)); background = shape(Palette.cream, 12, Palette.line)
        setPadding(dp(8), dp(6), dp(8), dp(6)); minimumHeight = dp(52)
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { changed(options[position]) }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    parent.put(spinner, 16); return spinner
}
internal fun MainActivity.linkCard(symbol: String, title: String, subtitle: String, action: () -> Unit) = card {
    val line = row()
    line.addView(label(symbol, 24f, Palette.green).apply { gravity = Gravity.CENTER; background = shape(Palette.mint, 14) }, LinearLayout.LayoutParams(dp(48), dp(48)))
    line.addView(column().apply { setPadding(dp(14), 0, dp(8), 0); put(label(title, 16f, bold = true), 3); put(label(subtitle, 12f, Palette.muted)) }, LinearLayout.LayoutParams(0, -2, 1f))
    line.addView(label("›", 26f, Palette.green)); put(line)
    isClickable = true; isFocusable = true; contentDescription = "$title. $subtitle"; setOnClickListener { action() }
}

internal fun MainActivity.shell() {
    val outer = column().apply { setBackgroundColor(Palette.cream) }
    outer.setOnApplyWindowInsetsListener { v, insets ->
        v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        insets.consumeSystemWindowInsets()
    }
    val header = row().apply { setPadding(dp(20), dp(12), dp(20), dp(12)) }
    if (page !in listOf(Page.TODAY, Page.DIARY, Page.PLAN, Page.SETTINGS)) {
        header.addView(label("‹", 32f, Palette.green).apply {
            gravity = Gravity.CENTER; contentDescription = "Navigate back"; setOnClickListener { back() }
            isFocusable = true
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
    }
    header.addView(column().apply {
        put(label("CALORIE / ADN", 11f, Palette.green, true), 2)
        put(label(if (page == Page.FOOD && editIndex >= 0) "Edit food" else page.title, 24f, bold = true))
    }, LinearLayout.LayoutParams(0, -2, 1f))
    header.addView(label(if (page == Page.TODAY) "✦" else "•", 28f, Palette.green))
    outer.addView(header)
    body = column().apply { setPadding(dp(20), dp(8), dp(20), dp(20)) }
    val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; addView(body) }
    outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    if (page in listOf(Page.TODAY, Page.DIARY, Page.PLAN, Page.SETTINGS)) {
        val nav = row().apply { background = shape(Color.WHITE, 0); setPadding(dp(8), dp(8), dp(8), dp(8)); elevation = dp(6).toFloat() }
        listOf(Triple(Page.TODAY, "◉", "Today"), Triple(Page.DIARY, "≡", "Diary"), Triple(Page.PLAN, "▦", "Plan"), Triple(Page.SETTINGS, "⚙", "Settings")).forEach { (target, icon, title) ->
            val item = column().apply {
                gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(6)); minimumHeight = dp(56)
                if (page == target) background = shape(Palette.mint, 16)
                put(label(icon, 21f, if (page == target) Palette.green else Palette.muted).apply { gravity = Gravity.CENTER }, 1)
                put(label(title, 11f, if (page == target) Palette.green else Palette.muted, page == target).apply { gravity = Gravity.CENTER })
                contentDescription = "$title tab"; isFocusable = true; setOnClickListener { navigate(target, true) }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })
        }
        outer.addView(nav)
    }
    setContentView(outer); outer.requestApplyInsets()
}

internal class CalorieRing(context: Context, private val eaten: Int, private val goal: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    init { contentDescription = "$eaten of $goal calories eaten" }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = context.dp(12).toFloat()
        val size = minOf(width, height).toFloat() - stroke * 2
        val left = (width - size) / 2; val top = (height - size) / 2
        rect.set(left, top, left + size, top + size)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = stroke; paint.strokeCap = Paint.Cap.ROUND; paint.color = Color.rgb(75, 119, 91)
        canvas.drawArc(rect, -90f, 360f, false, paint)
        paint.color = Color.rgb(211, 235, 154)
        canvas.drawArc(rect, -90f, (eaten.toFloat() / goal.coerceAtLeast(1) * 360).coerceIn(0f, 360f), false, paint)
        paint.style = Paint.Style.FILL; paint.textAlign = Paint.Align.CENTER; paint.color = Color.WHITE
        paint.typeface = medium; paint.textSize = context.dp(34).toFloat()
        canvas.drawText(eaten.toString(), width / 2f, height / 2f + context.dp(4), paint)
        paint.typeface = Typeface.DEFAULT; paint.textSize = context.dp(12).toFloat(); paint.color = Color.rgb(219, 231, 219)
        canvas.drawText("of $goal kcal", width / 2f, height / 2f + context.dp(27), paint)
    }
}
