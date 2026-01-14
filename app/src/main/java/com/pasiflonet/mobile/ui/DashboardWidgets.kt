package com.pasiflonet.mobile.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.rss.RssEngine
import com.pasiflonet.mobile.rss.RssSources
import com.pasiflonet.mobile.telegram.TelegramTextSender
import java.lang.ref.WeakReference

object DashboardWidgets {

    private val main = Handler(Looper.getMainLooper())
    private var activityRef: WeakReference<Activity>? = null
    private var rssEngine: RssEngine? = null
    private var overlayInstalled = false

    fun ensureInstalled(activity: Activity) {
        activityRef = WeakReference(activity)

        // 1) סידור Layout (2 עמודות)
        applyTwoColumnLayout(activity)

        // 2) כפתורים תמיד נראים
        installOverlayControls(activity)

        // 3) RSS+Radar engine
        val rvX = activity.findViewById<androidx.recyclerview.widget.RecyclerView?>(R.id.rvX)
        val radar = activity.findViewById<RadarView?>(R.id.radarView)

        if (rvX != null) {
            rssEngine?.stop()
            rssEngine = RssEngine(activity.applicationContext, rvX, radar).also { it.start() }
        }

        // 4) stop on destroy
        if (activity is LifecycleOwner) {
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    rssEngine?.stop()
                    rssEngine = null
                    overlayInstalled = false
                }
            })
        }
    }

    private fun applyTwoColumnLayout(activity: Activity) {
        // מנסה למצוא root ConstraintLayout במסך
        val root = findRootConstraint(activity) ?: return
        val dashboard = activity.findViewById<View?>(R.id.dashboardWidgets) ?: return
        val telegram = activity.findViewById<View?>(R.id.rvMessages) ?: return

        val set = ConstraintSet()
        set.clone(root)

        // dashboard (שמאל)
        set.constrainWidth(dashboard.id, 0)
        set.constrainHeight(dashboard.id, 0)
        set.connect(dashboard.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 12)
        set.connect(dashboard.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 12)
        set.connect(dashboard.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 12)
        set.connect(dashboard.id, ConstraintSet.END, telegram.id, ConstraintSet.START, 12)

        // telegram list (ימין)
        set.constrainWidth(telegram.id, 0)
        set.constrainHeight(telegram.id, 0)
        set.connect(telegram.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 12)
        set.connect(telegram.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 12)
        set.connect(telegram.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 12)
        set.connect(telegram.id, ConstraintSet.START, dashboard.id, ConstraintSet.END, 12)

        set.applyTo(root)

        // בתוך dashboard: RSS למעלה + Radar למטה (אם זה LinearLayout)
        if (dashboard is LinearLayout) {
            dashboard.orientation = LinearLayout.VERTICAL

            val rvX = activity.findViewById<View?>(R.id.rvX)
            val radar = activity.findViewById<View?>(R.id.radarView)

            rvX?.let {
                val lp = it.layoutParams
                if (lp is LinearLayout.LayoutParams) {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                    lp.height = 0
                    lp.weight = 1.15f
                    it.layoutParams = lp
                }
            }
            radar?.let {
                val lp = it.layoutParams
                if (lp is LinearLayout.LayoutParams) {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                    lp.height = 0
                    lp.weight = 0.85f
                    it.layoutParams = lp
                }
            }
        }
    }

    private fun findRootConstraint(activity: Activity): ConstraintLayout? {
        // try common ids
        val direct = activity.findViewById<ConstraintLayout?>(R.id.root)
        if (direct != null) return direct

        // fallback: first child of content
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val child0 = content.getChildAt(0)
        return child0 as? ConstraintLayout
    }

    private fun installOverlayControls(activity: Activity) {
        if (overlayInstalled) return
        overlayInstalled = true

        val root = activity.findViewById<ViewGroup>(android.R.id.content)

        val bar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(18, 14, 18, 14)
        }

        fun mkBtn(text: String, onClick: () -> Unit): Button =
            Button(activity).apply {
                this.text = text
                isAllCaps = false
                setOnClickListener { onClick() }
            }

        val btnSettings = mkBtn("הגדרות") {
            // אם יש כפתור קיים במסך - נלחץ עליו, אחרת לא נשבור כלום
            activity.findViewById<View?>(R.id.btnSettings)?.performClick()
                ?: run {
                    // fallback: לנסות לפתוח SettingsActivity אם קיימת
                    runCatching {
                        val cls = Class.forName("com.pasiflonet.mobile.ui.SettingsActivity")
                        activity.startActivity(android.content.Intent(activity, cls))
                    }
                }
        }

        val btnClear = mkBtn("נקה קאש") {
            // ניקוי קאש בטוח (לא מוחק DB)
            runCatching { activity.cacheDir?.deleteRecursively() }
            runCatching { activity.externalCacheDir?.deleteRecursively() }
        }

        val btnRss = mkBtn("RSS") {
            RssSources.openEditor(activity) {
                rssEngine?.reschedule()
            }
        }

        val btnNew = mkBtn("הודעה חדשה") {
            val input = EditText(activity).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 4
                hint = "כתוב הודעה לשליחה לערוץ…"
            }
            AlertDialog.Builder(activity)
                .setTitle("שליחת הודעה חדשה לערוץ")
                .setView(input)
                .setPositiveButton("שלח") { _, _ ->
                    TelegramTextSender.sendNewText(activity, input.text?.toString().orEmpty())
                }
                .setNegativeButton("ביטול", null)
                .show()
        }

        bar.addView(btnSettings)
        bar.addView(btnClear)
        bar.addView(btnRss)
        bar.addView(btnNew)

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 18
            rightMargin = 18
        }

        root.addView(bar, lp)

        // להעלות גם את הכפתורים הישנים אם קיימים (שלא "ייעלמו")
        activity.findViewById<View?>(R.id.btnSettings)?.apply { visibility = View.VISIBLE; bringToFront() }
        activity.findViewById<View?>(R.id.btnClearCache)?.apply { visibility = View.VISIBLE; bringToFront() }
    }
}
