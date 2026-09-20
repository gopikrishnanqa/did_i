package com.druanlabs.didicheck.widget.checklist

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.druanlabs.didicheck.DidIApplication
import com.druanlabs.didicheck.MainActivity
import com.druanlabs.didicheck.data.model.ChecklistRun
import com.druanlabs.didicheck.ui.theme.BrandBlue
import com.druanlabs.didicheck.ui.theme.BrandMuted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val ChecklistWidgetRoutineIdKey = stringPreferencesKey("routine_id")

private const val MAX_VISIBLE_ITEMS = 5

private val WidgetBg = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1A1D23))
private val WidgetOnSurface = ColorProvider(day = Color(0xFF1A202C), night = Color(0xFFF1F5F9))
private val WidgetMuted = ColorProvider(day = BrandMuted, night = Color(0xFF94A3B8))
private val WidgetAccent = ColorProvider(day = BrandBlue, night = Color(0xFF7BA3F0))

class ChecklistGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val routineId = prefs[ChecklistWidgetRoutineIdKey]
        val snapshot = withContext(Dispatchers.IO) { loadSnapshot(context, routineId) }

        provideContent {
            GlanceTheme {
                when (snapshot) {
                    WidgetSnapshot.MissingTemplate -> MissingTemplateContent()
                    is WidgetSnapshot.Ready -> ReadyContent(snapshot.run)
                }
            }
        }
    }

    private suspend fun loadSnapshot(context: Context, routineId: String?): WidgetSnapshot {
        if (routineId.isNullOrBlank()) return WidgetSnapshot.MissingTemplate
        val app = context.applicationContext as? DidIApplication
            ?: return WidgetSnapshot.MissingTemplate
        val run = runCatching { app.repository.getOrCreateTodayRun(routineId) }.getOrNull()
        return if (run == null) WidgetSnapshot.MissingTemplate else WidgetSnapshot.Ready(run)
    }
}

private sealed class WidgetSnapshot {
    data object MissingTemplate : WidgetSnapshot()
    data class Ready(val run: ChecklistRun) : WidgetSnapshot()
}

@Composable
private fun MissingTemplateContent() {
    val context = LocalContext.current
    val glanceId = LocalGlanceId.current
    val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBg)
            .padding(14.dp)
            .clickable(actionStartActivity(configIntent(context, appWidgetId))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "List not found, tap to choose",
            style = TextStyle(
                color = WidgetOnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun ReadyContent(run: ChecklistRun) {
    val context = LocalContext.current
    val visible = run.items.take(MAX_VISIBLE_ITEMS)
    val overflow = (run.items.size - visible.size).coerceAtLeast(0)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBg)
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(openChecklistIntent(context, run.routineId))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = run.routineName,
                style = TextStyle(
                    color = WidgetOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            Text(
                text = run.progressLabel,
                style = TextStyle(
                    color = WidgetAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        visible.forEach { item ->
            val mark = if (item.isChecked) "☑" else "☐"
            Text(
                text = "$mark  ${item.label}",
                style = TextStyle(
                    color = if (item.isChecked) WidgetMuted else WidgetOnSurface,
                    fontSize = 14.sp,
                ),
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                maxLines = 1,
            )
        }
        if (overflow > 0) {
            Text(
                text = "+$overflow more",
                style = TextStyle(
                    color = WidgetMuted,
                    fontSize = 12.sp,
                ),
                modifier = GlanceModifier.padding(top = 4.dp),
            )
        }
    }
}

private fun configIntent(context: Context, appWidgetId: Int): Intent {
    return Intent(context, ChecklistWidgetConfigActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
}

private fun openChecklistIntent(context: Context, routineId: String): Intent {
    return Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("open", "checklist")
        putExtra("routineId", routineId)
    }
}

class ChecklistGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChecklistGlanceWidget()
}

object ChecklistWidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val widget = ChecklistGlanceWidget()
        manager.getGlanceIds(ChecklistGlanceWidget::class.java).forEach { id ->
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
            val routineId = prefs[ChecklistWidgetRoutineIdKey]
            if (!routineId.isNullOrBlank()) {
                val app = context.applicationContext as? DidIApplication
                runCatching { app?.repository?.getOrCreateTodayRun(routineId) }
            }
            widget.update(context, id)
        }
    }

    fun requestUpdate(context: Context) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { updateAll(context.applicationContext) }
        }
    }
}
