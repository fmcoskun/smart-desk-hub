package com.iot.widget.ui.icons

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.iot.widget.ui.theme.BootstrapIconsFamily
import com.iot.widget.ui.theme.DeskColors

/**
 * UI layer · icons. Bootstrap Icons is an icon *font*: each icon is one character. The
 * codepoints are copied from reference/web-ui/vendor/bootstrap-icons/bootstrap-icons.min.css
 * (`.bi-bluetooth::before { content: "\f682" }` → '\uF682'), so the icons are the exact same
 * glyphs as in the reference design.
 */
enum class Bi(val codepoint: Char) {
    Grid1x2Fill('\uF3F3'),
    BriefcaseFill('\uF1CB'),
    CameraVideoFill('\uF21C'),
    MoonStarsFill('\uF495'),
    Bluetooth('\uF682'),
    LightbulbFill('\uF468'),
    Outlet('\uF4AE'),
    ShieldLockFill('\uF537'),
    Activity('\uF66B'),
    ThermometerHalf('\uF5CD'),
    Droplet('\uF30D'),
    LightningCharge('\uF46D'),
    Sun('\uF5A2'),
    ChevronLeft('\uF284'),
    SpeakerFill('\uF57D'),
    Headphones('\uF413'),
    Mouse2Fill('\uF49A'),
    VolumeDown('\uF60B'),
    Sunrise('\uF5A5'),
    Sunset('\uF5A7'),
    Display('\uF302'),
    UsbPlugFill('\uF6DB'),
    MicFill('\uF48D'),
    WebcamFill('\uF67C'),
    Clock('\uF293'),
    PersonFill('\uF4DA'),
    PeopleFill('\uF4CF'),
    ThreeDots('\uF5D4'),
    BoxArrowRight('\uF1C3'),
    InfoCircle('\uF431'),
    PlusLg('\uF64D'),
    Trash('\uF5DE'),
    ChevronRight('\uF285'),
    PersonBadge('\uF4D3'),
    ShieldFillCheck('\uF531'),
    Eye('\uF341'),
    Cpu('\uF2D6'),
    CameraVideo('\uF21F'),
    Thermometer('\uF5D2'),
    Wifi('\uF61C'),
    BatteryHalf('\uF187'),
    ArrowRepeat('\uF130'),
    Pencil('\uF4CB'),
    DoorOpen('\uF308'),
    Search('\uF52A'),
    CheckCircleFill('\uF26A'),
    ExclamationTriangleFill('\uF33A'),
    JournalText('\uF444'),
    Funnel('\uF3E1'),
    Share('\uF52E'),
    CalendarRange('\uF1F1'),
    XLg('\uF659'),
    Braces('\uF1C9'),
    CameraVideoOffFill('\uF21D'),
    CameraFill('\uF219'),
    RecordCircleFill('\uF517'),
    PlayFill('\uF4F4'),
    PauseFill('\uF4C3'),
    ArrowCounterclockwise('\uF117'),
    ArrowClockwise('\uF116'),
    Film('\uF3C3'),
    CalendarEvent('\uF1E8'),
    QuestionCircle('\uF505'),
}

/** Draws one Bootstrap icon. Icons are decorative here; the surrounding control carries the label. */
@Composable
fun BiIcon(
    icon: Bi,
    modifier: Modifier = Modifier,
    tint: Color = DeskColors.IconDefault,
    size: TextUnit = 16.sp,
) {
    Text(
        text = icon.codepoint.toString(),
        // decorative: the glyph is a private-use character TalkBack cannot read; the label or
        // contentDescription next to it says what it means
        modifier = modifier.clearAndSetSemantics {},
        style = TextStyle(
            fontFamily = BootstrapIconsFamily,
            fontSize = size,
            lineHeight = size,
            color = tint,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        ),
    )
}
