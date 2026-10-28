package com.iot.widget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes

/**
 * UI layer · a full-screen page in the style of the reference `.detail-panel`: a card with a
 * Back button, a green icon and an uppercase title, and a scrolling body. Used by the device
 * details, About, Users and the user editor, so every sub-page looks and behaves the same.
 *
 * [title] must already be uppercase (from strings.xml, or uppercased with Locale.ROOT).
 */
@Composable
fun SubPage(
    title: String,
    icon: Bi,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(DeskColors.Bg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Column(
            Modifier
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp)
                .fillMaxSize()
                .windowCard(DeskShapes.Window)
                .padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.padding(horizontal = 10.dp)) {
                Row(
                    modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BackButton(onBack)
                    BiIcon(icon, tint = DeskColors.Green, size = 15.sp)
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        color = DeskColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                HorizontalDivider(thickness = 1.dp, color = Color(0x0D000000))
            }
            // The body scrolls; its inner padding leaves room so control shadows are not clipped
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .neuRaised(RoundedCornerShape(22.dp), distance = 3.dp, blur = 6.dp)
            .clickable(role = Role.Button, onClick = onBack)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BiIcon(Bi.ChevronLeft, tint = DeskColors.TextMuted, size = 14.sp)
        Text(stringResource(R.string.detail_back), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeskColors.TextMuted)
    }
}
