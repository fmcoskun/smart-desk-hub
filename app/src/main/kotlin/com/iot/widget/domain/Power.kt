package com.iot.widget.domain

import kotlin.math.roundToInt

/**
 * Domain layer · simulated desk power draw (reference `recalcPower`):
 * soundbar 14 W, plug 115 W, LED up to 18 W scaled by brightness.
 */
fun powerDrawWatts(state: DeskState): Int {
    var watts = 0
    if (state.isOn(DeviceId.Soundbar)) watts += 14
    if (state.isOn(DeviceId.SmartPlug)) watts += 115
    if (state.isOn(DeviceId.DeskLight)) watts += (18 * (state.settings.brightness / 100.0)).roundToInt()
    return watts
}
