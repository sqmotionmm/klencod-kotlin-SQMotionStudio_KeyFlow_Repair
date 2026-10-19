package com.sq.create

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ScrollView
import org.json.JSONObject
import org.json.JSONArray
import android.widget.Toast
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** SQ Motion Studio - lightweight native motion graphics starter editor. */
class MainActivity : Activity() {
    private val bg = Color.rgb(13, 17, 23)
    private val surface = Color.rgb(22, 27, 34)
    private val surface2 = Color.rgb(33, 38, 45)
    private val blue = Color.rgb(31, 111, 235)
    private val text = Color.rgb(230, 237, 243)
    private val muted = Color.rgb(139, 148, 158)
    private lateinit var stage: StageView
    private lateinit var timeLabel: TextView
    private lateinit var seek: SeekBar
    private lateinit var layerLabel: TextView
    private var playhead = 0f
    private var durationSeconds = 5
    private var animator: ValueAnimator? = null
    private var importedUri: Uri? = null
    private val pickImage = 201
    private var selectedEffectName = "بدون تأثير"
    private var selectedEffectCategory = "none"
    private var selectedEffectIndex = -1
    private var effectNames: List<String> = emptyList()
    private var effectCategories: List<String> = emptyList()
    private var currentPage = "home"
    private val projectNames = mutableListOf("مشروعي الأول", "تصميم تجريبي")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        loadEffectCatalog()
        buildUi()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun buildUi() = showHome()

    private fun buildEditorUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10)); setBackgroundColor(surface)
        }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(label("SQ MOTION", 17f, text, true))
        titleBox.addView(label("Motion Graphics Studio", 11f, muted, false))
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("تأثيرات 520", blue) { showEffects() })
        header.addView(button("تصدير PNG", blue) { exportFrame() })
        root.addView(header)

        val previewTitle = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        previewTitle.addView(label("معاينة التصميم", 13f, text, true), LinearLayout.LayoutParams(0, -2, 1f))
        previewTitle.addView(label("1080 × 1920", 11f, muted, false))
        root.addView(previewTitle)

        val previewWrap = FrameLayout(this).apply { setPadding(dp(18), dp(2), dp(18), dp(2)) }
        stage = StageView()
        previewWrap.addView(stage, FrameLayout.LayoutParams(-1, -1))
        root.addView(previewWrap, LinearLayout.LayoutParams(-1, 0, 1f))

        val timeline = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(10)); setBackgroundColor(surface)
        }
        val timeRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        timeLabel = label("00:00.0 / 00:05.0", 12f, text, true)
        timeRow.addView(timeLabel, LinearLayout.LayoutParams(0, -2, 1f))
        timeRow.addView(button("▶ تشغيل", blue) { togglePlayback() })
        timeline.addView(timeRow)
        seek = SeekBar(this).apply {
            max = durationSeconds * 10; progress = 0
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) { playhead = p / 10f; updateTime(); stage.invalidate() }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) { stopPlayback() }
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        timeline.addView(seek, LinearLayout.LayoutParams(-1, dp(38)))
        layerLabel = label("الطبقات: عنوان النص", 12f, muted, false)
        timeline.addView(layerLabel)
        root.addView(timeline)

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(8), dp(10), dp(10)); setBackgroundColor(bg)
        }
        val row1 = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        row1.addView(button("＋ نص", surface2) { addText() }, weightParams())
        row1.addView(button("▧ صورة", surface2) { chooseImage() }, weightParams())
        row1.addView(button("لون النص", surface2) { chooseTextColor() }, weightParams())
        tools.addView(row1)
        val row2 = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        row2.addView(button("مدة المشروع", surface2) { chooseDuration() }, weightParams())
        row2.addView(button("حركة دخول", surface2) { stage.animationStyle = (stage.animationStyle + 1) % 3; stage.invalidate(); toast("تم تغيير نمط الحركة") }, weightParams())
        row2.addView(button("التأثيرات", surface2) { showEffects() }, weightParams())
        row2.addView(button("إعادة ضبط", surface2) { resetProject() }, weightParams())
        tools.addView(row2, LinearLayout.LayoutParams(-1, dp(48)))
        val row3 = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        row3.addView(button("الطبقات", surface2) { showLayersPage() }, weightParams())
        row3.addView(button("الإطارات المفتاحية", surface2) { showKeyframesPage() }, weightParams())
        row3.addView(button("التصدير", surface2) { showExportPage() }, weightParams())
        tools.addView(row3, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(tools)
        root.addView(navBar("editor"))
        setContentView(root)
        updateTime()
    }

    private fun navBar(active: String): LinearLayout {
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(4), dp(5), dp(4), dp(5)); setBackgroundColor(surface) }
        val items = listOf("home" to "الرئيسية", "editor" to "المحرر", "effects" to "التأثيرات", "projects" to "المشاريع", "settings" to "الإعدادات")
        items.forEach { (id, title) ->
            nav.addView(button(if (id == active) "● $title" else title, if (id == active) blue else surface2) {
                when (id) { "home" -> showHome(); "editor" -> showEditor(); "effects" -> showEffectsPage(); "projects" -> showProjectsPage(); "settings" -> showSettingsPage() }
            }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        return nav
    }

    private fun pageRoot(title: String, subtitle: String, active: String): LinearLayout {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(14)); setBackgroundColor(surface) }
        header.addView(label(title, 23f, text, true))
        header.addView(label(subtitle, 12f, muted, false))
        root.addView(header)
        return root
    }

    private fun addPageCard(parent: LinearLayout, title: String, desc: String, actionLabel: String, action: () -> Unit) {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)); background = GradientDrawable().apply { setColor(surface); cornerRadius = dp(14).toFloat(); setStroke(dp(1), 0xFF30363D.toInt()) } }
        card.addView(label(title, 16f, text, true))
        val description = label(desc, 12f, muted, false); card.addView(description)
        val button = button(actionLabel, blue, action)
        val lp = LinearLayout.LayoutParams(-1, dp(44)); lp.topMargin = dp(10); card.addView(button, lp)
        val wrap = LinearLayout(this); wrap.setPadding(dp(12), dp(6), dp(12), dp(6)); wrap.addView(card, LinearLayout.LayoutParams(-1, -2)); parent.addView(wrap)
    }

    private fun showHome() {
        currentPage = "home"
        val root = pageRoot("KeyFlow · SQ Motion", "استوديو موشن جرافيك على أندرويد", "home")
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(14)) }
        addPageCard(body, "ابدأ مشروعًا جديدًا", "إنشاء تصميم عمودي وتجربة النص والصورة والحركة", "＋ مشروع جديد") { showEditor(); toast("تم فتح محرر المشروع") }
        addPageCard(body, "مكتبة التأثيرات البصرية", "استعراض كتالوج التأثيرات المضمّن وعددها ${effectNames.size}", "استعراض التأثيرات") { showEffectsPage() }
        addPageCard(body, "القوالب الجاهزة", "اختيار فكرة بداية لتصميمك", "عرض القوالب") { showTemplatesPage() }
        addPageCard(body, "مشاريعي", "فتح المشاريع التجريبية أو بدء مشروع جديد", "فتح المكتبة") { showProjectsPage() }
        addPageCard(body, "إعدادات الاستوديو", "تفضيلات مساحة العمل ومعلومات التطبيق", "الإعدادات") { showSettingsPage() }
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(navBar("home")); setContentView(root)
    }

    private fun showEditor() {
        currentPage = "editor"
        buildEditorUi()
    }

    private fun showEffectsPage() {
        currentPage = "effects"
        val root = pageRoot("التأثيرات البصرية", "${effectNames.size} تأثيرًا في الكتالوج · اختر تأثيرًا للمعاينة", "effects")
        val search = android.widget.EditText(this).apply { hint = "ابحث عن تأثير…"; setTextColor(this@MainActivity.text); setHintTextColor(muted); setSingleLine(true); setPadding(dp(14), dp(8), dp(14), dp(8)); background = GradientDrawable().apply { setColor(surface2); cornerRadius = dp(10).toFloat() } }
        val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)) }
        holder.addView(search, LinearLayout.LayoutParams(-1, dp(48)))
        val scroll = ScrollView(this); val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun populate(filter: String) {
            list.removeAllViews()
            val indexes = effectNames.indices.filter { effectNames[it].contains(filter, true) || effectCategories[it].contains(filter, true) }.take(150)
            indexes.forEach { i ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(9), dp(12), dp(9)); background = GradientDrawable().apply { setColor(surface); cornerRadius = dp(10).toFloat() } }
                val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                info.addView(label("✦  ${effectNames[i]}", 14f, text, true)); info.addView(label("${effectCategories[i]} · #${i + 1}", 11f, muted, false))
                row.addView(info, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(button("تطبيق", blue) { applyEffect(i); showEditor() }, LinearLayout.LayoutParams(dp(84), dp(40)))
                val wrap = LinearLayout(this); wrap.setPadding(0, dp(3), 0, dp(3)); wrap.addView(row); list.addView(wrap)
            }
            if (indexes.isEmpty()) list.addView(label("لا توجد نتائج", 13f, muted, false))
        }
        populate(""); search.addTextChangedListener(object : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { populate(s?.toString().orEmpty()) }; override fun afterTextChanged(s: android.text.Editable?) {} })
        scroll.addView(list); holder.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(holder, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(navBar("effects")); setContentView(root)
    }

    private fun applyEffect(index: Int) {
        if (index !in effectNames.indices) return
        selectedEffectIndex = index; selectedEffectName = effectNames[index]; selectedEffectCategory = effectCategories[index]
        if (::stage.isInitialized) { stage.effectCategory = selectedEffectCategory; stage.effectSeed = index; stage.invalidate(); if (::layerLabel.isInitialized) layerLabel.text = "التأثير: $selectedEffectName" }
        toast("تم اختيار: $selectedEffectName")
    }

    private fun showProjectsPage() {
        currentPage = "projects"
        val root = pageRoot("المشاريع", "المشاريع المحلية التجريبية", "projects")
        val scroll = ScrollView(this); val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(12)) }
        addPageCard(body, "＋ مشروع جديد", "بدء تصميم فارغ في المحرر", "إنشاء") { showEditor() }
        projectNames.forEachIndexed { index, name -> addPageCard(body, name, "مشروع ${index + 1} · قابل للتعديل", "فتح المشروع") { showEditor(); toast("تم فتح $name") } }
        addPageCard(body, "استيراد صورة", "استخدم صورة من جهازك كبداية للتصميم", "اختيار صورة") { showEditor(); chooseImage() }
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(navBar("projects")); setContentView(root)
    }

    private fun showTemplatesPage() {
        currentPage = "templates"
        val root = pageRoot("القوالب", "قوالب بداية قابلة للتعديل", "home")
        val scroll = ScrollView(this); val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(12)) }
        listOf("عنوان نيون", "ملصق متحرك", "صورة مع عنوان", "بطاقة اجتماعية", "مشهد تأثيرات").forEachIndexed { i, name -> addPageCard(body, name, "قالب ${i + 1} · نص وحركة وتأثير لبدء التصميم", "استخدام القالب") { showEditor(); stage.caption = name; stage.animationStyle = i % 3; stage.invalidate() } }
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(navBar("home")); setContentView(root)
    }

    private fun showSettingsPage() {
        currentPage = "settings"
        val root = pageRoot("الإعدادات", "خيارات مساحة العمل", "settings")
        val scroll = ScrollView(this); val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(12)) }
        addPageCard(body, "جودة المعاينة", "أبعاد المشروع الحالية 1080 × 1920", "تغيير مدة المشروع") { if (::stage.isInitialized) chooseDuration() else showEditor() }
        addPageCard(body, "التصدير", "حفظ إطار التصميم بصيغة PNG في Pictures/SQMotion", "خيارات التصدير") { showExportPage() }
        addPageCard(body, "كتالوج التأثيرات", "تم تضمين أسماء وتصنيفات التأثيرات من ملف KeyFlow", "إعادة تحميل الكتالوج") { loadEffectCatalog(); toast("تم تحميل ${effectNames.size} تأثيرًا"); showSettingsPage() }
        addPageCard(body, "حول التطبيق", "SQ Motion Studio · واجهات Kotlin أصلية", "معلومات التطبيق") { AlertDialog.Builder(this).setTitle("حول SQ Motion").setMessage("محرر موشن جرافيك تجريبي بواجهات Kotlin. كتالوج التأثيرات مضمّن، لكن محرك KeyFlow الأصلي ليس جزءًا من هذا المشروع.").setPositiveButton("حسنًا", null).show() }
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(navBar("settings")); setContentView(root)
    }


    private fun showLayersPage() {
        val root = pageRoot("الطبقات", "إدارة عناصر المشهد", "editor")
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)) }
        addPageCard(body, "طبقة النص · ${if (::stage.isInitialized) stage.caption else "SQ MOTION"}", "العنوان الأساسي في المشهد", "تعديل النص") { showEditor(); addText() }
        addPageCard(body, "طبقة الصورة", if (::stage.isInitialized && stage.image != null) "تمت إضافة صورة إلى المشهد" else "لا توجد صورة مضافة", "استيراد / استبدال الصورة") { showEditor(); chooseImage() }
        addPageCard(body, "طبقة التأثير", if (selectedEffectIndex >= 0) selectedEffectName else "بدون تأثير محدد", "اختيار تأثير") { showEffectsPage() }
        addPageCard(body, "ترتيب الطبقات", "النص والصورة والتأثيرات عناصر مستقلة في واجهة التحرير", "العودة للمحرر") { showEditor() }
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(navBar("editor")); setContentView(root)
    }

    private fun showKeyframesPage() {
        val root = pageRoot("الإطارات المفتاحية", "تحكم مبسط بتوقيت الحركة", "editor")
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)) }
        body.addView(label("الوقت الحالي: ${String.format(Locale.US, "%.1f", playhead)} ثانية", 15f, text, true))
        val slider = SeekBar(this).apply { max = durationSeconds * 10; progress = (playhead * 10).toInt() }
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) { playhead = progress / 10f; if (::seek.isInitialized) seek.progress = progress; updateTime(); if (::stage.isInitialized) stage.invalidate() } }
            override fun onStartTrackingTouch(sb: SeekBar?) { stopPlayback() }
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        body.addView(slider, LinearLayout.LayoutParams(-1, dp(48)))
        addPageCard(body, "إطار البداية · 00:00", "تثبيت بداية حركة الدخول", "الانتقال للبداية") { playhead = 0f; if (::seek.isInitialized) seek.progress = 0; updateTime(); if (::stage.isInitialized) stage.invalidate(); showKeyframesPage() }
        addPageCard(body, "إطار النهاية · ${durationSeconds} ثانية", "الانتقال إلى نهاية مدة المشروع", "الانتقال للنهاية") { playhead = durationSeconds.toFloat(); if (::seek.isInitialized) seek.progress = durationSeconds * 10; updateTime(); if (::stage.isInitialized) stage.invalidate(); showKeyframesPage() }
        addPageCard(body, "نمط الحركة", "التبديل بين أنماط حركة الدخول المتاحة", "تغيير الحركة") { if (::stage.isInitialized) { stage.animationStyle = (stage.animationStyle + 1) % 3; stage.invalidate() }; toast("تم تغيير نمط الحركة") }
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(navBar("editor")); setContentView(root)
    }

    private fun showExportPage() {
        val root = pageRoot("التصدير", "إعدادات إخراج التصميم", "editor")
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)) }
        addPageCard(body, "صورة PNG", "تصدير إطار ثابت بدقة 1080 × 1920", "تصدير PNG") { if (::stage.isInitialized) exportFrame() else { showEditor(); toast("افتح المحرر ثم اختر التصدير") } }
        addPageCard(body, "فيديو MP4", "تصدير الفيديو الكامل غير مفعّل في هذا الإصدار", "غير متاح حاليًا") { toast("تصدير MP4 يحتاج محرك ترميز فيديو") }
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(navBar("editor")); setContentView(root)
    }

    private fun weightParams() = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }

    private fun label(value: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER_VERTICAL
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun button(value: String, color: Int, action: () -> Unit): Button = Button(this).apply {
        text = value; textSize = 11f; isAllCaps = false; setTextColor(this@MainActivity.text); setPadding(dp(5), 0, dp(5), 0)
        background = GradientDrawable().apply { setColor(color); cornerRadius = dp(9).toFloat(); setStroke(dp(1), if (color == blue) 0xFF58A6FF.toInt() else 0xFF30363D.toInt()) }
        setOnClickListener { action() }
    }

    private fun loadEffectCatalog() {
        try {
            val json = assets.open("custom_effects/effects_catalog.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(json).getJSONArray("effects")
            val names = ArrayList<String>(arr.length())
            val categories = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                names.add(item.optString("name", "Effect ${i + 1}"))
                categories.add(item.optString("category", "other"))
            }
            effectNames = names
            effectCategories = categories
        } catch (_: Exception) {
            effectNames = listOf("Neon Pulse", "Solar Flare", "Crystal Drift", "Electric Bloom")
            effectCategories = listOf("glow", "flare", "crystal", "electric")
        }
    }

    private fun showEffects() {
        if (effectNames.isEmpty()) { toast("تعذر تحميل كتالوج التأثيرات"); return }
        val categories = listOf("الكل") + effectCategories.distinct().sorted()
        AlertDialog.Builder(this)
            .setTitle("التأثيرات البصرية · ${effectNames.size}")
            .setItems(categories.toTypedArray()) { _, categoryIndex ->
                val indexes = effectNames.indices.filter { categoryIndex == 0 || effectCategories[it] == categories[categoryIndex] }
                showEffectList(indexes, categories[categoryIndex])
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showEffectList(indexes: List<Int>, category: String) {
        val names = indexes.map { "✦  ${effectNames[it]}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("${category} · ${indexes.size} تأثير")
            .setItems(names) { _, position ->
                val index = indexes[position]
                applyEffect(index)
                if (::stage.isInitialized && ::layerLabel.isInitialized) layerLabel.text = "التأثير: $selectedEffectName"
            }
            .setPositiveButton("رجوع") { _, _ -> showEffects() }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun addText() {
        val input = android.widget.EditText(this).apply { setText(stage.caption); setTextColor(this@MainActivity.text); setHintTextColor(muted); hint = "اكتب النص هنا"; setSingleLine(false) }
        AlertDialog.Builder(this).setTitle("إضافة / تعديل النص").setView(input)
            .setPositiveButton("تطبيق") { _, _ -> stage.caption = input.text.toString().ifBlank { "عنوان جديد" }; layerLabel.text = "الطبقات: ${stage.caption}"; stage.invalidate() }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        startActivityForResult(intent, pickImage)
    }

    @Deprecated("Activity result API kept for compatibility with lightweight IDE templates")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImage && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    if (bitmap != null) { stage.image?.recycle(); stage.image = bitmap; importedUri = uri; stage.invalidate(); toast("تمت إضافة الصورة") }
                }
            } catch (e: Exception) { toast("تعذر فتح الصورة") }
        }
    }

    private fun chooseTextColor() {
        val colors = intArrayOf(Color.WHITE, Color.YELLOW, Color.CYAN, 0xFFFF7B72.toInt(), 0xFFD2A8FF.toInt(), Color.GREEN)
        val names = arrayOf("أبيض", "أصفر", "سماوي", "أحمر", "بنفسجي", "أخضر")
        AlertDialog.Builder(this).setTitle("لون النص").setItems(names) { _, which -> stage.captionColor = colors[which]; stage.invalidate() }.show()
    }

    private fun chooseDuration() {
        val options = arrayOf("5 ثوانٍ", "10 ثوانٍ", "15 ثانية", "30 ثانية")
        val values = intArrayOf(5, 10, 15, 30)
        AlertDialog.Builder(this).setTitle("مدة المشروع").setItems(options) { _, which ->
            stopPlayback(); durationSeconds = values[which]; seek.max = durationSeconds * 10
            playhead = playhead.coerceAtMost(durationSeconds.toFloat()); seek.progress = (playhead * 10).toInt(); updateTime()
        }.show()
    }

    private fun togglePlayback() {
        if (animator?.isRunning == true) { stopPlayback(); return }
        if (playhead >= durationSeconds) playhead = 0f
        animator = ValueAnimator.ofFloat(playhead, durationSeconds.toFloat()).apply {
            duration = ((durationSeconds - playhead) * 1000).toLong().coerceAtLeast(1)
            interpolator = LinearInterpolator()
            addUpdateListener { value -> playhead = value.animatedValue as Float; seek.progress = (playhead * 10).toInt(); updateTime(); stage.invalidate() }
            start()
        }
    }

    private fun stopPlayback() { animator?.cancel(); animator = null }
    private fun updateTime() { if (::timeLabel.isInitialized) timeLabel.text = String.format(Locale.US, "%02d:%04.1f / 00:%04.1f", (playhead / 60).toInt(), playhead % 60, durationSeconds.toFloat()) }

    private fun resetProject() {
        stopPlayback(); stage.caption = "SQ MOTION"; stage.captionColor = Color.WHITE
        stage.image?.recycle(); stage.image = null; stage.animationStyle = 0; playhead = 0f; seek.progress = 0
        layerLabel.text = "الطبقات: عنوان النص"; updateTime(); stage.invalidate(); toast("تمت إعادة ضبط المشروع")
    }

    private fun exportFrame() {
        try {
            val w = 1080; val h = 1920
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            stage.drawFrame(canvas, w.toFloat(), h.toFloat(), playhead)
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "SQMotion_$stamp.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SQMotion")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) { bitmap.recycle(); toast("تعذر حفظ الصورة"); return }
            contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle(); toast("تم تصدير إطار PNG إلى Pictures/SQMotion")
        } catch (e: Exception) { toast("فشل التصدير: ${e.message}") }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() { stopPlayback(); stage.image?.recycle(); super.onDestroy() }

    private inner class StageView : View(this@MainActivity) {
        var caption = "SQ MOTION"
        var captionColor = Color.WHITE
        var image: Bitmap? = null
        var animationStyle = 0
        var effectCategory = "none"
        var effectSeed = -1
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val rect = RectF()
        override fun onDraw(canvas: Canvas) { super.onDraw(canvas); drawFrame(canvas, width.toFloat(), height.toFloat(), playhead) }

        fun drawFrame(canvas: Canvas, width: Float, height: Float, time: Float) {
            canvas.drawColor(Color.rgb(7, 10, 15))
            val pad = width * 0.045f
            rect.set(pad, pad, width - pad, height - pad)
            paint.color = Color.rgb(18, 24, 33); paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, 22f, 22f, paint)
            // Subtle grid for composition alignment
            paint.color = 0x223F5268; paint.strokeWidth = 1f
            for (i in 1..4) { val x = width * i / 5f; canvas.drawLine(x, pad, x, height-pad, paint); val y = height * i / 5f; canvas.drawLine(pad, y, width-pad, y, paint) }
            val progress = (time / durationSeconds.toFloat()).coerceIn(0f, 1f)
            val enter = when (animationStyle) { 1 -> progress; 2 -> (progress * 1.7f).coerceIn(0f,1f); else -> (progress * 2.2f).coerceIn(0f,1f) }
            val alpha = (enter * 255).toInt().coerceIn(0,255)
            image?.let { bmp ->
                val maxW = width * 0.72f; val maxH = height * 0.42f
                val scale = minOf(maxW / bmp.width, maxH / bmp.height)
                val bw = bmp.width * scale; val bh = bmp.height * scale
                paint.alpha = alpha; canvas.drawBitmap(bmp, null, RectF((width-bw)/2, height*0.27f + (1-enter)*height*0.08f, (width+bw)/2, height*0.27f + (1-enter)*height*0.08f+bh), paint); paint.alpha = 255
            }
            paint.style = Paint.Style.FILL; paint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER; paint.textSize = width * 0.075f; paint.color = captionColor; paint.alpha = alpha
            val y = height * (if (image == null) 0.53f else 0.77f) + (1-enter)*height*0.08f
            canvas.drawText(caption, width/2f, y, paint)
            paint.alpha = 255
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL); paint.textSize = width*0.026f; paint.color = 0xFF58A6FF.toInt()
            canvas.drawText("MOTION DESIGN  •  ${String.format(Locale.US, "%.1f", time)}s", width/2f, height*0.88f, paint)
            // Lightweight native preview renderer for the selected preset category.
            // Full particle3D renderers from the source app are not included in this starter engine.
            if (effectCategory != "none") {
                val cx = width / 2f
                val cy = height * 0.49f
                val phase = (time * 2.4f + (effectSeed.coerceAtLeast(0) % 9) * 0.31f)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = maxOf(2f, width * 0.006f)
                paint.alpha = (100 + 100 * kotlin.math.abs(kotlin.math.sin(phase.toDouble()))).toInt().coerceIn(0, 200)
                paint.color = when (effectCategory.lowercase(Locale.US)) {
                    "glow", "electric", "warp" -> 0xFF42D9FF.toInt()
                    "flare", "fire" -> 0xFFFF9345.toInt()
                    "crystal", "shards" -> 0xFFD2A8FF.toInt()
                    "ribbon", "liquid" -> 0xFF3DDBB2.toInt()
                    else -> 0xFF58A6FF.toInt()
                }
                val radius = width * (0.07f + 0.025f * kotlin.math.abs(kotlin.math.sin(phase.toDouble())))
                canvas.drawCircle(cx, cy, radius, paint)
                canvas.drawCircle(cx, cy, radius * 1.55f, paint)
                paint.style = Paint.Style.FILL
                paint.alpha = 255
            }
            // small accent bar
            paint.color = 0xFF1F6FEB.toInt(); canvas.drawRoundRect(width*0.38f, height*0.57f, width*0.62f, height*0.578f, 8f, 8f, paint)
        }
    }
}