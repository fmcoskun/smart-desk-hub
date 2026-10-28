package com.iot.widget.domain.devices

import org.junit.Assert.assertEquals
import org.junit.Test

/** The simulated "Add device" flow as a state machine: each event moves to the next step. */
class PairingTest {

    @Test
    fun happyPath() {
        var step: PairingStep = PairingStep.ChooseType
        step = nextPairing(step, PairingEvent.TypeChosen(DeviceType.LIGHT))
        assertEquals(PairingStep.Searching(DeviceType.LIGHT), step)
        step = nextPairing(step, PairingEvent.SearchFinished)
        assertEquals(PairingStep.Found(DeviceType.LIGHT, "Desk Lamp"), step)
        step = nextPairing(step, PairingEvent.Continue)
        assertEquals(PairingStep.Naming(DeviceType.LIGHT, "Desk Lamp", Room.DESK), step)
        step = nextPairing(step, PairingEvent.Named("Shelf Light", Room.HALLWAY))
        assertEquals(PairingStep.Naming(DeviceType.LIGHT, "Shelf Light", Room.HALLWAY), step)
        step = nextPairing(step, PairingEvent.Added)
        assertEquals(PairingStep.Done, step)
    }

    @Test
    fun cancelReturnsToStart() {
        val searching = PairingStep.Searching(DeviceType.PLUG)
        assertEquals(PairingStep.ChooseType, nextPairing(searching, PairingEvent.Cancel))
    }

    @Test
    fun invalidEventIgnored() {
        assertEquals(PairingStep.ChooseType, nextPairing(PairingStep.ChooseType, PairingEvent.SearchFinished))
        assertEquals(PairingStep.Done, nextPairing(PairingStep.Done, PairingEvent.Continue))
    }

    @Test
    fun camerasNotPairable() {
        assertEquals(PairingStep.ChooseType, nextPairing(PairingStep.ChooseType, PairingEvent.TypeChosen(DeviceType.CAMERA)))
        assertEquals(listOf(DeviceType.LIGHT, DeviceType.PLUG, DeviceType.SENSOR), PAIRABLE_TYPES)
    }


    @Test
    fun suggestedNameSkipsTakenNames() {
        fun found(type: DeviceType, taken: List<String>) =
            nextPairing(PairingStep.Searching(type), PairingEvent.SearchFinished, taken) as PairingStep.Found
        assertEquals("Desk Lamp", found(DeviceType.LIGHT, listOf("Smart Plug")).suggestedName)
        assertEquals("Desk Lamp 2", found(DeviceType.LIGHT, listOf("desk lamp")).suggestedName)
        assertEquals("Smart Plug 3", found(DeviceType.PLUG, listOf("Smart Plug", "Smart Plug 2")).suggestedName)
    }
}
