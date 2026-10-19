package com.adn.calorieadn

import android.graphics.Color
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun MainActivity.planScreen() {
    section("A week with a little intention.", "Plan meals you enjoy. Changes save as you type.")
    val todayName = SimpleDateFormat("EEEE", Locale.US).format(Date())
    val selected = prefs.getString("plan_day", todayName) ?: todayName
    val days = row()
    dayNames.forEach { day ->
        days.addView(button(day.take(3), day == selected) {
            prefs.edit().putString("plan_day", day).apply(); render()
        }, LinearLayout.LayoutParams(dp(68), dp(52)).apply { marginEnd = dp(8) })
    }
    add(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(days) })
    val plan = plan()
    val parts = plan.optString(selected).split("|").map { it.trim() }
    val values = MutableList(4) { parts.getOrElse(it) { "" } }
    add(card {
        put(label(selected, 24f, Palette.green, true), 6)
        put(label("YOUR MEAL PLAN", 11f, Palette.muted, true), 20)
        mealNames.forEachIndexed { index, meal ->
            input(this, if (meal == "Snack") "Snacks" else meal, values[index], multiline = true) { value ->
                values[index] = value.replace("|", "/")
                if (values.all { it.isBlank() }) plan.remove(selected) else plan.put(selected, values.joinToString(" | "))
                prefs.edit().putString("plan", plan.toString()).apply()
            }
        }
        put(label("Saved on this device", 12f, Palette.green))
    })
    add(linkCard("✦", "Need a little inspiration?", "Add a sample Kerala meal week") {
        confirm("Use the sample meal week?", "This replaces your current seven-day plan. These are meal ideas; portions and nutrition are not personalized.") {
            val samples = arrayOf(
                "Puttu + kadala curry | Rice, fish curry, vegetables | Chapati + egg curry | Banana",
                "Dosa + sambar | Rice, chicken curry, thoran | Idiyappam + vegetable stew | Yogurt",
                "Appam + egg curry | Rice, dal, vegetables | Chapati + fish curry | Fruit",
                "Idli + sambar | Rice, fish, thoran | Dosa + sambar | Nuts",
                "Oats + milk | Rice, chicken, vegetables | Puttu + green gram | Fruit",
                "Upma + egg | Rice, sambar, thoran | Chapati + paneer curry | Yogurt",
                "Appam + stew | Rice, fish curry, vegetables | Idli + sambar | Fruit"
            )
            dayNames.forEachIndexed { i, day -> plan.put(day, samples[i]) }
            prefs.edit().putString("plan", plan.toString()).apply(); render(); toast("Meal ideas added")
        }
    })
    add(notice("Your plan, your portions", "This planner organizes meals. It does not calculate a personalized diet or diagnose nutrition needs."))
}

internal fun MainActivity.settingsScreen() {
    section("Make it yours.", "A simple space for your goals and preferences.")
    add(linkCard("◎", "Daily goals", "${prefs.getInt("goal", 2000)} kcal · adjust your target") { navigate(Page.GOALS) })
    val keySaved = KeyVault.read(this).isNotBlank()
    add(linkCard("✧", "Photo AI", if (keySaved) "Key saved · test connection in setup" else "Connect Gemini to estimate meals") { navigate(Page.AI) })
    add(card {
        put(label("Made for your everyday", 19f, bold = true), 10)
        put(label("Your food diary, water log and meal plan stay on this device. A food photo and your optional description are sent to Google only when you tap Estimate nutrition.", 14f, Palette.muted), 12)
        put(label("AI estimates need your review. The diary also works without an API key or internet.", 14f, Palette.muted), 16)
        put(label("CALORIE ADN  /  1.1", 11f, Palette.green, true))
    })
}

internal fun MainActivity.goalsScreen() {
    section("A goal that fits your life.", "Set your own daily calorie target.")
    var draft = prefs.getInt("goal", 2000).toString()
    add(card {
        val value = input(this, "Daily calories · kcal", draft, numeric = true) { draft = it }
        put(label("The initial 2,000 kcal value is a placeholder, not a personal recommendation. Your needs depend on your circumstances.", 13f, Palette.muted), 16)
        put(button("Save daily goal") {
            val goal = draft.toIntOrNull()
            if (goal == null || goal !in 500..8000) {
                value.error = "Enter a whole number between 500 and 8,000"; return@button
            }
            prefs.edit().putInt("goal", goal).apply(); toast("Daily goal updated"); back()
        })
    })
}
