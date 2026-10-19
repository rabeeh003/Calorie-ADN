package com.adn.calorieadn

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class Page(val title: String) {
    TODAY("Today"), DIARY("Food diary"), PLAN("Meal plan"), SETTINGS("Settings"),
    FOOD("Add food"), SCAN("Scan food"), AI("Photo AI"), GOALS("Daily goals"), REVIEW("Review estimate")
}

class MainActivity : Activity() {
    internal lateinit var prefs: android.content.SharedPreferences
    internal lateinit var body: LinearLayout
    internal var page = Page.TODAY
    internal val history = mutableListOf<Page>()
    internal var diaryDate = date()
    internal var foodDraft = JSONObject()
    internal var editIndex = -1
    internal var selectedPhoto: Uri? = null
    internal var photoContext = ""
    internal var scanError = ""
    internal var scanning = false
    internal var requestId = 0
    internal var activeCall: GeminiCall? = null
    internal var apiDraft = ""
    internal var modelDraft = ""
    internal val mealNames = arrayOf("Breakfast", "Lunch", "Dinner", "Snack")
    internal val dayNames = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        prefs = getSharedPreferences("calorie_adn", Context.MODE_PRIVATE)
        KeyVault.migrate(this, prefs)
        if (state != null) {
            page = runCatching { Page.valueOf(state.getString("page") ?: "TODAY") }.getOrDefault(Page.TODAY)
            state.getStringArrayList("history")?.forEach { name -> runCatching { history.add(Page.valueOf(name)) } }
            diaryDate = state.getString("date") ?: date()
            foodDraft = runCatching { JSONObject(state.getString("draft") ?: "{}") }.getOrDefault(JSONObject())
            editIndex = state.getInt("edit", -1)
            selectedPhoto = state.getString("photo")?.let { Uri.parse(it) }
            photoContext = state.getString("context") ?: ""
        }
        render()
    }

    override fun onSaveInstanceState(out: Bundle) {
        out.putString("page", page.name)
        out.putStringArrayList("history", ArrayList(history.map { it.name }))
        out.putString("date", diaryDate)
        out.putString("draft", foodDraft.toString())
        out.putInt("edit", editIndex)
        out.putString("photo", selectedPhoto?.toString())
        out.putString("context", photoContext)
        super.onSaveInstanceState(out)
    }

    internal fun navigate(next: Page, tab: Boolean = false) {
        if (next == page) return
        cancelScan()
        if (tab) { history.clear(); if (next != Page.TODAY) history.add(Page.TODAY) }
        else history.add(page)
        page = next
        render()
    }

    internal fun back() {
        if (page == Page.TODAY) { finish(); return }
        cancelScan()
        page = if (history.isNotEmpty()) history.removeAt(history.lastIndex) else Page.TODAY
        render()
    }

    @Deprecated("Platform back compatibility")
    override fun onBackPressed() { back() }

    internal fun render() {
        shell()
        when (page) {
            Page.TODAY -> todayScreen()
            Page.DIARY -> diaryScreen()
            Page.PLAN -> planScreen()
            Page.SETTINGS -> settingsScreen()
            Page.FOOD, Page.REVIEW -> foodScreen()
            Page.SCAN -> scanScreen()
            Page.AI -> aiScreen()
            Page.GOALS -> goalsScreen()
        }
    }

    internal fun add(view: View) { body.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }) }
    internal fun diary() = runCatching { JSONArray(prefs.getString("diary", "[]") ?: "[]") }.getOrDefault(JSONArray())
    internal fun saveDiary(value: JSONArray) { prefs.edit().putString("diary", value.toString()).apply() }
    internal fun plan() = runCatching { JSONObject(prefs.getString("plan", "{}") ?: "{}") }.getOrDefault(JSONObject())
    internal fun entries(day: String): List<Pair<Int, JSONObject>> {
        val data = diary()
        return (0 until data.length()).mapNotNull { i -> data.optJSONObject(i)?.takeIf { it.optString("date") == day }?.let { i to it } }
    }
    internal fun newFood(meal: String = "Lunch") {
        editIndex = -1
        foodDraft = JSONObject().put("meal", meal).put("date", if (page == Page.DIARY) diaryDate else date()).put("portion", "1 serving")
        navigate(Page.FOOD)
    }
    internal fun editFood(index: Int, food: JSONObject) {
        editIndex = index; foodDraft = JSONObject(food.toString()); navigate(Page.FOOD)
    }
    internal fun confirm(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ -> action() }.show()
    }
    internal fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    internal fun completeFood() {
        history.clear(); history.add(Page.TODAY); page = Page.DIARY
        diaryDate = foodDraft.optString("date", date())
        foodDraft = JSONObject(); editIndex = -1; render()
    }
    internal fun cancelScan() { requestId++; scanning = false; activeCall?.cancel(); activeCall = null }
    override fun onDestroy() { cancelScan(); super.onDestroy() }

    @Deprecated("Platform photo picker compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK && data?.data != null) {
            selectedPhoto = data.data
            runCatching { contentResolver.takePersistableUriPermission(data.data!!, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            scanError = ""
            if (page == Page.SCAN) render()
        }
    }
}

internal fun date(offset: Int = 0): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }.time)
internal fun prettyDate(value: String): String = runCatching {
    SimpleDateFormat("EEE, d MMM", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)!!)
}.getOrDefault(value)
internal fun number(value: Double): String = if (value == value.toLong().toDouble()) value.toLong().toString() else String.format(Locale.US, "%.1f", value)
