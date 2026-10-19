package com.adn.calorieadn

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.InputType
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import kotlin.concurrent.thread
import org.json.JSONObject

internal fun MainActivity.aiScreen() {
    val connected = KeyVault.read(this).isNotBlank()
    section("A smarter way to log meals.", "Connect your own Gemini API key.")
    add(notice(if (connected) "Key saved on this device" else "Photo AI is not connected", "Saving a key does not verify access. Use Test saved connection to check the key and model. API usage may count toward Google's quota or billing."))
    add(card {
        val keyInput = input(this, if (connected) "Replace API key · leave blank to keep" else "Gemini API key", apiDraft) { apiDraft = it }
        keyInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        keyInput.setSelection(keyInput.text.length)
        put(CheckBox(this@aiScreen).apply {
            text = "Show key"; setTextColor(Palette.muted)
            setOnCheckedChangeListener { _, checked ->
                keyInput.inputType = InputType.TYPE_CLASS_TEXT or if (checked) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD
                keyInput.setSelection(keyInput.text.length)
            }
        }, 16)
        input(this, "Gemini model", modelDraft.ifBlank { prefs.getString("ai_model", DEFAULT_MODEL) ?: DEFAULT_MODEL }) { modelDraft = it }
        put(label("Default: $DEFAULT_MODEL. Use an image-capable model available to your Google project.", 12f, Palette.muted), 16)
        put(button("Save AI settings") {
            val model = modelDraft.trim().ifBlank { prefs.getString("ai_model", DEFAULT_MODEL) ?: DEFAULT_MODEL }
            if (!model.matches(Regex("gemini-[a-zA-Z0-9.-]+"))) { toast("Enter a valid Gemini model name"); return@button }
            if (apiDraft.isBlank() && !connected) { keyInput.error = "Paste your API key"; return@button }
            try {
                if (apiDraft.isNotBlank()) KeyVault.save(this@aiScreen, apiDraft.trim())
                prefs.edit().putString("ai_model", model).apply(); apiDraft = ""; modelDraft = ""
                toast("AI settings saved"); render()
            } catch (_: Exception) { toast("The key could not be saved. Please try again.") }
        }, 12)
        val result = label("No connection test run yet.", 13f, Palette.muted)
        val test = button("Test saved connection", false) {}
        test.isEnabled = connected
        test.alpha = if (connected) 1f else 0.45f
        test.setOnClickListener {
            val call = GeminiCall(); activeCall?.cancel(); activeCall = call
            val id = ++requestId
            val key = KeyVault.read(this@aiScreen)
            val model = prefs.getString("ai_model", DEFAULT_MODEL) ?: DEFAULT_MODEL
            test.isEnabled = false; result.text = "Checking saved key and model…"
            thread {
                val message = try { call.test(key, model); "Connected. Your saved key and model responded successfully." }
                    catch (error: Exception) { aiError(error) }
                runOnUiThread {
                    if (!isDestroyed && page == Page.AI && requestId == id) {
                        test.isEnabled = true; result.text = message
                    }
                }
            }
        }
        put(test, 10); put(result)
    })
    add(button("Get a key in Google AI Studio", false) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey")))
    })
    if (connected) add(button("Remove saved API key", false) {
        confirm("Remove API key?", "Photo estimates will stop until you connect a key again. Your diary and plan will remain saved.") {
            activeCall?.cancel(); requestId++; KeyVault.remove(this); apiDraft = ""; render()
        }
    }.apply { setTextColor(Palette.red) })
    add(notice("You stay in control", "The key is encrypted on this device with Android Keystore. When you request an estimate, the selected photo and meal details are sent to Google. Never share your key or put it in a public APK."))
}

internal fun MainActivity.scanScreen() {
    if (scanning) {
        add(card {
            put(label("Reading your plate…", 24f, Palette.green, true), 12)
            put(label("Looking for foods, portions and nutrition. You will review everything before it goes in your diary.", 14f, Palette.muted), 20)
            put(ProgressBar(this@scanScreen), 20)
            put(button("Cancel estimate", false) { cancelScan(); render() })
        })
        return
    }
    section("Snap. Check. Enjoy.", "An easier start to your food diary.")
    if (scanError.isNotBlank()) add(notice("Couldn't finish the estimate", scanError, Color.rgb(255, 237, 220)))
    add(card {
        val uri = selectedPhoto
        if (uri != null) {
            val preview = ImageView(this@scanScreen).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP; contentDescription = "Selected meal photo"
                background = shape(Palette.mint, 18); clipToOutline = true
            }
            try { preview.setImageBitmap(photoBitmap(this@scanScreen, uri, 640)) }
            catch (_: Exception) { scanError = "This photo is no longer accessible. Choose it again." }
            addView(preview, LinearLayout.LayoutParams(-1, dp(220)).apply { bottomMargin = dp(16) })
        } else {
            put(label("✧", 56f, Palette.green).apply { gravity = android.view.Gravity.CENTER }, 8)
            put(label("Start with a clear meal photo", 19f, bold = true), 6)
            put(label("Show the whole plate in good light. A known portion or weight helps improve the estimate.", 14f, Palette.muted), 20)
        }
        put(button(if (uri == null) "Choose food photo" else "Change photo", false) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }, 101)
        }, 16)
        input(this, "Meal details · optional", photoContext, multiline = true) { photoContext = it }
            .hint = "e.g. 2 appam, 1 egg, curry with coconut milk"
        put(label("Tapping Estimate sends this photo and the details above to Google's Gemini API. Nutrition is approximate.", 12f, Palette.muted), 16)
        put(button("Estimate nutrition") { beginEstimate() })
    })
    if (KeyVault.read(this).isBlank()) add(linkCard("⚙", "Connect Photo AI", "An API key is required for estimates") { navigate(Page.AI) })
    add(button("Enter food manually", false) { newFood() })
    add(notice("A photo can't tell the whole story", "Gemini can suggest food names and nutrition, but hidden ingredients, oil and portion weight are uncertain. Check the result before saving."))
}

private fun MainActivity.beginEstimate() {
    val uri = selectedPhoto ?: run { toast("Choose a food photo first"); return }
    val key = KeyVault.read(this)
    if (key.isBlank()) { navigate(Page.AI); return }
    val model = prefs.getString("ai_model", DEFAULT_MODEL) ?: DEFAULT_MODEL
    val details = photoContext
    val call = GeminiCall(); activeCall?.cancel(); activeCall = call
    val id = ++requestId; scanning = true; scanError = ""; render()
    thread {
        try {
            val estimate = call.estimate(this, uri, details, key, model)
            runOnUiThread {
                if (!isDestroyed && page == Page.SCAN && requestId == id) {
                    scanning = false; editIndex = -1
                    foodDraft = JSONObject(estimate.toString()).put("date", date()).put("meal", "Lunch").put("source", "Photo AI")
                    navigate(Page.REVIEW)
                }
            }
        } catch (error: Exception) {
            runOnUiThread {
                if (!isDestroyed && page == Page.SCAN && requestId == id) {
                    scanning = false; scanError = aiError(error); render()
                }
            }
        }
    }
}
