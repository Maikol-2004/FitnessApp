package com.maikol.fitnessapp.feature.nutrition

import com.maikol.fitnessapp.feature.nutrition.ui.UndoSlot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** N1.1-3: deshacer conserva lo borrado hasta que restaurar termina bien y permite reintentar. */
@OptIn(ExperimentalCoroutinesApi::class)
class UndoSlotTest {

    @Test
    fun success_clearsReference() = runTest {
        val restored = mutableListOf<String>()
        val slot = UndoSlot<String>(this) { restored += it }
        slot.offer("Prueba")
        slot.undo()
        advanceUntilIdle()
        assertEquals(listOf("Prueba"), restored)
        assertNull(slot.state.value.deleted)
        assertFalse(slot.state.value.failed)
    }

    @Test
    fun failure_keepsReference_andRetryRestoresOnce() = runTest {
        var attempts = 0
        val restored = mutableListOf<String>()
        val slot = UndoSlot<String>(this) {
            attempts++
            if (attempts == 1) error("fallo simulado")
            restored += it
        }
        slot.offer("Prueba")
        slot.undo()
        advanceUntilIdle()
        assertEquals("Prueba", slot.state.value.deleted)
        assertTrue(slot.state.value.failed)
        assertFalse(slot.state.value.restoring)
        assertTrue(restored.isEmpty())

        slot.undo()
        advanceUntilIdle()
        assertEquals(listOf("Prueba"), restored)
        assertEquals(2, attempts)
        assertNull(slot.state.value.deleted)
    }

    @Test
    fun doubleTapWhileRestoring_restoresOnce_andDismissDoesNotDropIt() = runTest {
        val gate = CompletableDeferred<Unit>()
        var attempts = 0
        val slot = UndoSlot<String>(this) {
            attempts++
            gate.await()
        }
        slot.offer("Prueba")
        slot.undo()
        slot.undo()
        advanceUntilIdle()
        assertTrue(slot.state.value.restoring)
        slot.dismiss()
        assertEquals("se conserva mientras restaura", "Prueba", slot.state.value.deleted)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, attempts)
        assertNull(slot.state.value.deleted)
    }

    @Test
    fun dismiss_withoutRestoring_dropsOffer() = runTest {
        val slot = UndoSlot<String>(this) { }
        slot.offer("Prueba")
        slot.dismiss()
        assertNull(slot.state.value.deleted)
        slot.undo()
        advanceUntilIdle()
        assertNull(slot.state.value.deleted)
    }
}
