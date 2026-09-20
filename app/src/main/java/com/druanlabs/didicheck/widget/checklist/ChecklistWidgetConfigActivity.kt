package com.druanlabs.didicheck.widget.checklist

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.druanlabs.didicheck.DidIApplication
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.ui.components.QuietCard
import com.druanlabs.didicheck.ui.components.ScreenHeader
import com.druanlabs.didicheck.ui.theme.DidITheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Step 1: pick which routine template this widget instance shows.
 */
class ChecklistWidgetConfigActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        // Default cancel until a template is chosen.
        setResult(Activity.RESULT_CANCELED)

        setContent {
            DidITheme {
                val scope = rememberCoroutineScope()
                var routines by remember { mutableStateOf<List<Routine>>(emptyList()) }
                LaunchedEffect(Unit) {
                    val app = application as DidIApplication
                    routines = app.repository.routines.first()
                }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = { TopAppBar(title = { Text("Choose a checklist") }) },
                ) { padding ->
                    ConfigScreen(
                        modifier = Modifier.padding(padding),
                        routines = routines,
                        onPick = { routine ->
                            scope.launch {
                                bindWidget(appWidgetId, routine.id)
                                val result = Intent().putExtra(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                                    appWidgetId,
                                )
                                setResult(Activity.RESULT_OK, result)
                                finish()
                            }
                        },
                    )
                }
            }
        }
    }

    private suspend fun bindWidget(appWidgetId: Int, routineId: String) {
        val manager = GlanceAppWidgetManager(this)
        val glanceId = manager.getGlanceIdBy(appWidgetId)
        updateAppWidgetState(this, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[ChecklistWidgetRoutineIdKey] = routineId
            }
        }
        val app = application as DidIApplication
        app.repository.getOrCreateTodayRun(routineId)
        ChecklistGlanceWidget().update(this, glanceId)
    }
}

@Composable
private fun ConfigScreen(
    modifier: Modifier,
    routines: List<Routine>,
    onPick: (Routine) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "Which checklist?",
                subtitle = "This widget will show today's items for that routine.",
            )
        }
        if (routines.isEmpty()) {
            item {
                QuietCard {
                    Text(
                        "No routines yet. Create one in the app first.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(routines, key = { it.id }) { routine ->
            QuietCard(onClick = { onPick(routine) }) {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${routine.items.size} items",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
