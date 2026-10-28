package com.iot.widget.widget

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.iot.widget.MainActivity
import com.iot.widget.R
import com.iot.widget.domain.Scene
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/**
 * Widget layer · the reference palette (design spec §4); Glance cannot draw the neumorphic
 * shadows. Backgrounds are shape drawables (res/drawable/widget_*.xml), so their rounded corners
 * also show below Android 12, where GlanceModifier.cornerRadius does nothing.
 */
private val TextDark = Color(0xFF2C3036)
private val TextMuted = Color(0xFF757D8A)
private val Green = Color(0xFF00B33C)
private val GreenText = Color(0xFF007F2B)
private val KnobOff = Color(0xFFEEEEEE)
private val KnobOffText = Color(0xFF616161)

/**
 * Widget layer · the home-screen widget: the Work / Meet / Leave scenes and the four desk
 * switches. Glance turns Compose-style code into RemoteViews, the only UI a launcher can show
 * for another app, so it has its own (smaller) set of components and no custom drawing.
 *
 * The widget is not a screen of the app: Hilt cannot inject into it, so it asks the app's
 * dependency graph through an @EntryPoint. Inside `provideContent` it collects the same
 * Room-backed flows as the Dashboard, but only while Glance keeps its session open (it closes
 * it after a while, even if the widget stays on screen). What keeps a placed widget current is
 * SmartDeskApp: it calls `updateAll` after every desk or sign-in change.
 */
class SmartDeskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        // load first, then draw: starting from "nothing" would briefly show an empty widget, or
        // "Open the app to sign in" to someone who is signed in
        val firstDesk = deps.desk().state.first()
        val firstUser = deps.session().currentUser.first()
        provideContent {
            val desk by deps.desk().state.collectAsState(initial = firstDesk)
            val user by deps.session().currentUser.collectAsState(initial = firstUser)
            WidgetContent(widgetUi(desk, user))
        }
    }
}

/** Android finds the widget through this receiver (declared in the manifest). */
class SmartDeskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmartDeskWidget()
}

@Composable
fun WidgetContent(ui: WidgetUi) {
    val context = LocalContext.current
    val canControl = ui.mode == WidgetMode.CONTROL
    // without permission every tap simply opens the app (to sign in, or to look around)
    val openApp: Action = actionStartActivity(Intent(context, MainActivity::class.java))
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(12.dp),
    ) {
        Text(context.getString(R.string.widget_title), style = TextStyle(color = ColorProvider(TextMuted), fontSize = 11.sp, fontWeight = FontWeight.Bold))
        when (ui.mode) {
            WidgetMode.SIGNED_OUT -> Note(context.getString(R.string.widget_sign_in))
            WidgetMode.GUEST -> Note(context.getString(R.string.widget_guest))
            WidgetMode.CONTROL -> Unit
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Scene.entries.forEachIndexed { i, scene ->
                if (i > 0) Spacer(GlanceModifier.width(6.dp))
                val active = ui.activeScene == scene
                Box(
                    GlanceModifier
                        .defaultWeight()
                        .height(36.dp)
                        .background(ImageProvider(if (active) R.drawable.widget_scene_active else R.drawable.widget_scene_idle))
                        .clickable(if (canControl) actionRunCallback<SceneAction>(actionParametersOf(SceneAction.SCENE to scene.name)) else openApp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        context.getString(sceneLabel(scene)),
                        style = TextStyle(color = ColorProvider(if (active) GreenText else TextDark), fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        // a LazyColumn scrolls: on a widget resized to its smallest height the last rows are
        // reachable instead of cut off (a plain Column cannot scroll in a widget)
        LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
            items(ui.rows, itemId = { it.id.ordinal.toLong() }) { row -> SwitchRow(row, canControl, openApp) }
        }
    }
}

@Composable
private fun SwitchRow(row: WidgetRow, canControl: Boolean, openApp: Action) {
    val context = LocalContext.current
    val toggle = actionRunCallback<ToggleAction>(actionParametersOf(ToggleAction.DEVICE to row.id.name, ToggleAction.ON to !row.isOn))
    Row(
        GlanceModifier
            .fillMaxWidth()
            .height(36.dp)
            // an offline device cannot be switched: the tap does nothing
            .then(if (!row.online) GlanceModifier else GlanceModifier.clickable(if (canControl) toggle else openApp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.name, style = TextStyle(color = ColorProvider(TextDark), fontSize = 13.sp), modifier = GlanceModifier.defaultWeight())
        when {
            !row.online -> Pill(context.getString(R.string.widget_offline), R.drawable.widget_pill_off, TextMuted)
            row.isOn -> Pill(context.getString(R.string.switch_on), R.drawable.widget_pill_on, Color.White)
            else -> Pill(context.getString(R.string.switch_off), R.drawable.widget_pill_off, KnobOffText)
        }
    }
}

@Composable
private fun Note(text: String) =
    Text(text, style = TextStyle(color = ColorProvider(TextMuted), fontSize = 11.sp), modifier = GlanceModifier.padding(top = 2.dp))

@Composable
private fun Pill(text: String, @DrawableRes bg: Int, fg: Color) {
    Box(GlanceModifier.width(56.dp).height(26.dp).background(ImageProvider(bg)), contentAlignment = Alignment.Center) {
        Text(text, style = TextStyle(color = ColorProvider(fg), fontSize = 11.sp, fontWeight = FontWeight.Bold))
    }
}

private fun sceneLabel(scene: Scene): Int = when (scene) {
    Scene.Work -> R.string.scene_work
    Scene.Meet -> R.string.scene_meet
    Scene.Leave -> R.string.scene_leave
}
