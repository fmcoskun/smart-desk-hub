package com.iot.widget.ui.more

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.detail.DetailSection
import com.iot.widget.ui.detail.InsetBox
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors

/** UI layer · About: app name and version, what it is built with, third-party licences. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // versionName comes from app/build.gradle.kts; read it once from the installed package
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    SubPage(title = stringResource(R.string.about_title_upper), icon = Bi.InfoCircle, onBack = onBack) {
        DetailSection(null) {
            Text(stringResource(R.string.app_name), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.about_version, version), fontSize = 12.sp, color = DeskColors.TextMuted)
            Text(stringResource(R.string.about_description), fontSize = 13.sp)
        }
        DetailSection(stringResource(R.string.about_licenses_upper)) {
            InsetBox {
                Text(stringResource(R.string.about_licenses), fontSize = 12.sp, color = DeskColors.LogText)
            }
        }
    }
}
