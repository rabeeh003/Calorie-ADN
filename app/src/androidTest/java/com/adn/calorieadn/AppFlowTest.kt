package com.adn.calorieadn

import android.app.Activity
import android.content.Intent
import android.test.InstrumentationTestCase
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import org.json.JSONObject

@Suppress("DEPRECATION")
class AppFlowTest : InstrumentationTestCase() {
    private lateinit var activity: MainActivity
    private var original = "[]"
    override fun setUp() {
        super.setUp()
        val intent = Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity = instrumentation.startActivitySync(intent) as MainActivity
        original = activity.prefs.getString("diary", "[]") ?: "[]"
    }
    override fun tearDown() {
        instrumentation.runOnMainSync {
            activity.prefs.edit().putString("diary", original).commit()
            activity.finish()
        }
        super.tearDown()
    }
    private fun ui(action: () -> Unit) { instrumentation.runOnMainSync(action) }
    private fun views(view: View): List<View> = listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    private fun click(text: String) { views(activity.body).filterIsInstance<Button>().first { it.text.toString() == text }.performClick() }

    fun testSystemBackReturnsThroughParentPages() = ui {
        activity.navigate(Page.SCAN)
        activity.navigate(Page.AI)
        activity.onBackPressed()
        assertEquals(Page.SCAN, activity.page)
        activity.onBackPressed()
        assertEquals(Page.TODAY, activity.page)
        assertFalse(activity.isFinishing)
        activity.navigate(Page.SETTINGS, true)
        activity.navigate(Page.GOALS)
        activity.onBackPressed()
        assertEquals(Page.SETTINGS, activity.page)
        activity.onBackPressed()
        assertEquals(Page.TODAY, activity.page)
        assertFalse(activity.isFinishing)
    }

    fun testFoodValidationSaveEditAndPersistence() = ui {
        val before = activity.diary().length()
        activity.newFood("Dinner")
        click("Save to food diary")
        assertEquals(Page.FOOD, activity.page)
        assertEquals(before, activity.diary().length())
        val inputs = views(activity.body).filterIsInstance<EditText>()
        listOf("Test meal", "150 g", "325", "21.5", "38", "10").forEachIndexed { i, value -> inputs[i].setText(value) }
        click("Save to food diary")
        assertEquals(Page.DIARY, activity.page)
        assertEquals(before + 1, activity.diary().length())
        val saved = activity.diary().getJSONObject(before)
        assertEquals("Dinner", saved.getString("meal"))
        assertEquals("150 g", saved.getString("portion"))
        assertEquals(325, saved.getInt("calories"))
        activity.editFood(before, saved)
        views(activity.body).filterIsInstance<EditText>()[2].setText("450")
        click("Save changes")
        assertEquals(before + 1, activity.diary().length())
        assertEquals(450, activity.diary().getJSONObject(before).getInt("calories"))
        activity.onBackPressed()
        assertEquals(Page.TODAY, activity.page)
    }

    fun testAiResponsesRejectMissingInvalidAndNonFoodData() {
        val valid = JSONObject().put("is_food", true).put("name", "Appam and egg curry").put("portion", "2 appam and 1 egg")
            .put("calories", 420).put("protein", 15).put("carbs", 52).put("fat", 16).put("notes", "Oil is uncertain")
        assertEquals(420, parseEstimate(valid.toString()).getInt("calories"))
        val invalid = listOf("not json", "{}", JSONObject(valid.toString()).put("is_food", false).toString(),
            JSONObject(valid.toString()).put("protein", -5).toString(), JSONObject(valid.toString()).apply { remove("calories") }.toString())
        invalid.forEach { response ->
            var rejected = false
            try { parseEstimate(response) } catch (_: IllegalStateException) { rejected = true }
            assertTrue("Invalid AI result must not reach the food form", rejected)
        }
    }
}
