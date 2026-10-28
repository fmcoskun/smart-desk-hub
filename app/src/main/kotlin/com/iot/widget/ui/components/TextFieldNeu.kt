package com.iot.widget.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskDimens
import com.iot.widget.ui.theme.DeskShapes
import com.iot.widget.ui.theme.RobotoFamily

/**
 * UI layer · a single-line text field in the neumorphic style: a recessed (inset) box, like the
 * reference's `.inset-box`. BasicTextField is Compose's bare editable text, so the look is ours.
 */
@Composable
fun TextFieldNeu(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    /** Words for names (the default); None for search boxes. */
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontFamily = RobotoFamily, fontSize = 15.sp, color = DeskColors.Text),
        cursorBrush = SolidColor(DeskColors.Green),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Done),
        modifier = modifier
            .fillMaxWidth()
            // An empty field is described by its label. With text in it, the text is read and the
            // label follows as the state ("hello, Search logs"); a content description would hide the text.
            .semantics { if (value.isEmpty()) contentDescription = label else stateDescription = label },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = DeskDimens.MinTouch)
                    .neuInset(DeskShapes.Segment, distance = 3.dp, blur = 6.dp, background = DeskColors.InsetBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text(label, fontSize = 15.sp, color = DeskColors.TextMuted)
                inner()
            }
        },
    )
}
