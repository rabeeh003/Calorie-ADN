package com.adn.calorieadn

import android.app.DatePickerDialog
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import org.json.JSONObject

internal fun MainActivity.todayScreen() {
    val foods = entries(date()).map { it.second }
    val eaten = foods.sumOf { it.optInt("calories") }
    val goal = prefs.getInt("goal", 2000)
    section("A little better, every day.", prettyDate(date()))
    add(card(Palette.green) {
        put(label("YOUR DAILY BALANCE", 11f, Color.rgb(211, 235, 154), true), 8)
        val balance = row()
        balance.addView(CalorieRing(this@todayScreen, eaten, goal), LinearLayout.LayoutParams(dp(170), dp(170)))
        balance.addView(column().apply {
            put(label(kotlin.math.abs(goal - eaten).toString(), 30f, Color.WHITE, true), 2)
            put(label(if (eaten <= goal) "kcal remaining" else "kcal above target", 13f, Color.rgb(219, 231, 219)), 12)
            put(label("${foods.size} foods logged", 12f, Color.WHITE))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        put(balance, 12)
        put(button("+  Log a meal", false) { newFood() })
    })
    val macros = row()
    listOf(Triple("Protein", "protein", Palette.green), Triple("Carbs", "carbs", Color.rgb(155, 101, 45)), Triple("Fat", "fat", Color.rgb(119, 101, 156))).forEach { (title, key, color) ->
        macros.addView(card {
            setPadding(dp(12), dp(16), dp(12), dp(16))
            put(label("${number(foods.sumOf { it.optDouble(key, 0.0) })} g", 21f, color, true), 3)
            put(label(title, 12f, Palette.muted))
        }, LinearLayout.LayoutParams(0, -1, 1f).apply { marginEnd = dp(if (title == "Fat") 0 else 8) })
    }
    add(macros)
    add(linkCard("✧", "Let your photo do the typing", "Scan a meal · review the nutrition") { navigate(Page.SCAN) })
    section("Small habits, big difference")
    val glasses = prefs.getInt("water_${date()}", 0)
    add(card {
        val line = row()
        line.addView(column().apply {
            put(label("Water", 17f, bold = true), 4)
            put(label("${number(glasses * 0.25)} L logged", 23f, Palette.green, true), 3)
            put(label("1 glass = 250 ml", 12f, Palette.muted))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(button("−", false) { prefs.edit().putInt("water_${date()}", (glasses - 1).coerceAtLeast(0)).apply(); render() }, LinearLayout.LayoutParams(dp(52), dp(52)))
        line.addView(button("+", false) { prefs.edit().putInt("water_${date()}", glasses + 1).apply(); render() }, LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginStart = dp(8) })
        put(line)
    })
    section("On the menu today", "Make room for meals you enjoy.")
    val day = SimpleDateFormat("EEEE", Locale.US).format(Calendar.getInstance().time)
    val planned = plan().optString(day)
    add(card {
        put(label(day, 15f, Palette.green, true), 6)
        put(label(planned.ifBlank { "Your next meal starts with a little planning." }, 14f, Palette.muted), 12)
        put(button(if (planned.isBlank()) "Build my meal plan" else "Open meal plan", false) { navigate(Page.PLAN, true) })
    })
}

internal fun MainActivity.diaryScreen() {
    val dateRow = row()
    dateRow.addView(button("‹", false) { changeDiaryDay(-1) }, LinearLayout.LayoutParams(dp(52), dp(52)))
    dateRow.addView(label(if (diaryDate == date()) "Today · ${prettyDate(diaryDate)}" else prettyDate(diaryDate), 14f, Palette.green, true).apply {
        gravity = Gravity.CENTER; minimumHeight = dp(52); contentDescription = "Choose diary date"; isFocusable = true
        setOnClickListener {
            val parts = diaryDate.split("-").map { it.toInt() }
            DatePickerDialog(this@diaryScreen, { _, year, month, day ->
                diaryDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day); render()
            }, parts[0], parts[1] - 1, parts[2]).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
        }
    }, LinearLayout.LayoutParams(0, -2, 1f))
    dateRow.addView(button("›", false) { changeDiaryDay(1) }.apply { isEnabled = diaryDate < date(); alpha = if (isEnabled) 1f else 0.35f }, LinearLayout.LayoutParams(dp(52), dp(52)))
    add(dateRow)
    val foods = entries(diaryDate)
    add(card(Palette.mint) {
        put(label("${foods.sumOf { it.second.optInt("calories") }} kcal", 28f, Palette.green, true), 4)
        put(label("P ${number(foods.sumOf { it.second.optDouble("protein", 0.0) })} g   ·   C ${number(foods.sumOf { it.second.optDouble("carbs", 0.0) })} g   ·   F ${number(foods.sumOf { it.second.optDouble("fat", 0.0) })} g", 13f, Palette.green))
    })
    mealNames.forEach { meal ->
        val group = foods.filter { it.second.optString("meal") == meal || (meal == "Snack" && it.second.optString("meal") !in mealNames) }
        add(card {
            val heading = row()
            heading.addView(label(meal, 18f, bold = true), LinearLayout.LayoutParams(0, -2, 1f))
            heading.addView(label("${group.sumOf { it.second.optInt("calories") }} kcal", 13f, Palette.muted))
            put(heading, 12)
            group.forEach { (index, food) ->
                put(column().apply {
                    put(label(food.optString("name"), 16f, bold = true), 3)
                    put(label("${food.optString("portion", "1 serving")} · ${food.optInt("calories")} kcal", 13f, Palette.muted), 3)
                    put(label("P ${number(food.optDouble("protein"))} g · C ${number(food.optDouble("carbs"))} g · F ${number(food.optDouble("fat"))} g${if (food.optString("source") == "Photo AI") " · AI estimate" else ""}", 12f, Palette.muted), 6)
                    put(button("Edit entry", false) { editFood(index, food) })
                }, 12)
            }
            if (group.isEmpty()) put(label("Nothing logged yet. Add your $meal here.".replace(meal, meal.lowercase()), 13f, Palette.muted), 12)
            put(button("+  Add ${meal.lowercase()}", false) { newFood(meal) })
        })
    }
}

private fun MainActivity.changeDiaryDay(amount: Int) {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(diaryDate) ?: return
    val cal = Calendar.getInstance().apply { time = parsed; add(Calendar.DAY_OF_YEAR, amount) }
    diaryDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time); render()
}

internal fun MainActivity.foodScreen() {
    val isAi = page == Page.REVIEW
    if (isAi) add(notice("A starting point, reviewed by you", foodDraft.optString("notes", "Check food, portion and nutrition before saving. Hidden oils and ingredients can change the estimate.")))
    else section(if (editIndex >= 0) "Keep your diary accurate." else "What's on your plate?", "Logging for ${prettyDate(foodDraft.optString("date", date()))}")
    val form = card {
        input(this, "Food name", foodDraft.optString("name")) { foodDraft.put("name", it) }
        input(this, "Portion · include weight if known", foodDraft.optString("portion", "1 serving")) { foodDraft.put("portion", it) }
        choice(this, "Meal", mealNames, foodDraft.optString("meal", "Lunch")) { foodDraft.put("meal", it) }
        input(this, "Calories · kcal", foodDraft.optString("calories"), true) { foodDraft.put("calories", it) }
        put(label("MACRONUTRIENTS", 11f, Palette.green, true), 12)
        listOf("protein" to "Protein · g", "carbs" to "Carbohydrates · g", "fat" to "Fat · g").forEach { (key, title) ->
            input(this, title, foodDraft.optString(key), true) { foodDraft.put(key, it) }
        }
        if (isAi) put(label("Nutrition is for the portion shown above. If you change the portion, adjust the nutrition too.", 12f, Palette.muted), 10)
        put(button(if (editIndex >= 0) "Save changes" else "Save to food diary") { saveFoodForm() })
    }
    add(form)
    if (editIndex >= 0) add(button("Delete entry", false) {
        confirm("Delete this food?", "This entry will be removed from your diary.") {
            val data = diary(); data.remove(editIndex); saveDiary(data); toast("Entry deleted"); completeFood()
        }
    }.apply { setTextColor(Palette.red) })
}

private fun MainActivity.saveFoodForm() {
    val name = foodDraft.optString("name").trim()
    if (name.isBlank()) { toast("Enter a food name"); return }
    val values = mutableMapOf<String, Double>()
    for (key in listOf("calories", "protein", "carbs", "fat")) {
        val raw = foodDraft.optString(key).trim()
        val value = if (raw.isBlank() && key != "calories") 0.0 else raw.toDoubleOrNull()
        if (value == null || !value.isFinite() || value < 0 || value > (if (key == "calories") 20000 else 3000)) {
            toast("Enter a valid, non-negative $key value"); return
        }
        values[key] = value
    }
    foodDraft.put("name", name).put("portion", foodDraft.optString("portion").trim().ifBlank { "1 serving" })
    values.forEach { (key, value) -> foodDraft.put(key, if (key == "calories") kotlin.math.round(value).toInt() else value) }
    if (!foodDraft.has("date")) foodDraft.put("date", date())
    val data = diary()
    if (editIndex >= 0) data.put(editIndex, JSONObject(foodDraft.toString())) else data.put(JSONObject(foodDraft.toString()))
    saveDiary(data); toast("Food saved"); completeFood()
}
