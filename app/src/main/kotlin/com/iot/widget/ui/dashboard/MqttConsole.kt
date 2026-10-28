package com.iot.widget.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.data.mqtt.MqttMessage
import com.iot.widget.ui.components.neuInset
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes
import com.iot.widget.ui.theme.JetBrainsMonoFamily

/** UI layer · "MQTT Bridge Console" (`.telemetry-drawer`): the last published message and a ping. */
@Composable
fun MqttConsole(message: MqttMessage?, pingMs: Int, modifier: Modifier = Modifier) {
    val topic = message?.topic ?: stringResource(R.string.console_idle_topic)
    val json = message?.json ?: stringResource(R.string.console_idle_json)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .neuInset(
                DeskShapes.Drawer,
                distance = 4.dp,
                blur = 8.dp,
                background = DeskColors.InsetBg,
                dark = Color(0x2E000000),
                light = Color(0xB3FFFFFF),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BiIcon(Bi.Activity, tint = DeskColors.TextMuted, size = 11.sp)
            Text(
                text = " " + stringResource(R.string.console_title_upper),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DeskColors.TextMuted,
                letterSpacing = 0.4.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                // written in caps like the reference's text-transform: uppercase
                text = stringResource(R.string.console_ping, pingMs),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DeskColors.TextMuted,
                letterSpacing = 0.4.sp,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x66FFFFFF), DeskShapes.Log)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.console_tag),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(DeskColors.IconDefault, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
            Text(
                // buildAnnotatedString = one Text with mixed styles (bold topic, normal JSON)
                text = buildAnnotatedString {
                    append(" ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(topic) }
                    append(" -> ")
                    append(json)
                },
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = DeskColors.LogText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
