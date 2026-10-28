package com.iot.widget.domain.devices

import com.iot.widget.domain.Names

/** Types the simulated pairing can find. Cameras are fixed (the camera screen relies on its four). */
val PAIRABLE_TYPES = listOf(DeviceType.LIGHT, DeviceType.PLUG, DeviceType.SENSOR)

/** One screen of the "Add device" flow. */
sealed interface PairingStep {
    data object ChooseType : PairingStep
    data class Searching(val type: DeviceType) : PairingStep
    data class Found(val type: DeviceType, val suggestedName: String) : PairingStep
    data class Naming(val type: DeviceType, val name: String, val room: Room) : PairingStep
    data object Done : PairingStep
}

/** Something that happened in the flow (a tap, or the search timer finishing). */
sealed interface PairingEvent {
    data class TypeChosen(val type: DeviceType) : PairingEvent
    data object SearchFinished : PairingEvent
    data object Continue : PairingEvent
    data class Named(val name: String, val room: Room) : PairingEvent
    data object Added : PairingEvent
    data object Cancel : PairingEvent
}

/**
 * Domain layer · the pairing flow as a state machine: (current step, event) → next step.
 * Writing it as one pure function makes every allowed and every ignored transition testable,
 * and the screen simply draws whatever step it is given.
 */
fun nextPairing(step: PairingStep, event: PairingEvent, takenNames: Collection<String> = emptyList()): PairingStep = when {
    event is PairingEvent.Cancel -> PairingStep.ChooseType
    step is PairingStep.ChooseType && event is PairingEvent.TypeChosen && event.type in PAIRABLE_TYPES ->
        PairingStep.Searching(event.type)
    step is PairingStep.Searching && event is PairingEvent.SearchFinished ->
        PairingStep.Found(step.type, suggestedName(step.type, takenNames))
    step is PairingStep.Found && event is PairingEvent.Continue ->
        PairingStep.Naming(step.type, step.suggestedName, Room.DESK)
    step is PairingStep.Naming && event is PairingEvent.Named -> step.copy(name = event.name, room = event.room)
    step is PairingStep.Naming && event is PairingEvent.Added -> PairingStep.Done
    else -> step // anything else is ignored: the flow stays where it is
}

/** "Desk Lamp", or "Desk Lamp 2", "Desk Lamp 3"… — the first name nobody uses yet. */
private fun suggestedName(type: DeviceType, taken: Collection<String>): String {
    val base = when (type) {
        DeviceType.LIGHT -> "Desk Lamp"
        DeviceType.PLUG -> "Smart Plug"
        else -> "Climate Sensor"
    }
    fun free(name: String) = taken.none { Names.sameName(it, name) }
    return if (free(base)) base else generateSequence(2) { it + 1 }.map { "$base $it" }.first(::free)
}
