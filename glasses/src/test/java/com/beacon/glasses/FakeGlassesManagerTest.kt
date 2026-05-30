package com.beacon.glasses

import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.model.GlassesDevice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeGlassesManagerTest {

    private lateinit var manager: FakeGlassesManager

    private val device = GlassesDevice("O_Beacon-X01", "AA:BB:CC:DD:EE:01", -48)

    @Before
    fun setUp() {
        manager = FakeGlassesManager().apply {
            scanStepDelayMs = 0
            connectDelayMs = 0
            readyDelayMs = 0
        }
    }

    @Test
    fun `scan emits a growing list of devices`() = runTest {
        val emissions = manager.scan().toList()
        assertTrue(emissions.isNotEmpty())
        // Final emission should contain all simulated devices.
        assertEquals(2, emissions.last().size)
    }

    @Test
    fun `connect transitions to connected ready and reports battery`() = runTest {
        val result = manager.connect(device)
        assertTrue(result is OperationResult.Success)

        val state = manager.connectionState.value
        assertTrue(state is ConnectionState.Connected)
        assertTrue((state as ConnectionState.Connected).ready)
        assertTrue(manager.isConnected())
        assertNotNull(manager.battery.value)
    }

    @Test
    fun `failed connect surfaces a failure and stays disconnected`() = runTest {
        manager.failNextConnect = true
        val result = manager.connect(device)

        assertTrue(result is OperationResult.Failure)
        assertFalse(manager.isConnected())
        assertTrue(manager.connectionState.value is ConnectionState.Failed)
    }

    @Test
    fun `disconnect clears state and battery`() = runTest {
        manager.connect(device)
        manager.disconnect()

        assertEquals(ConnectionState.Disconnected, manager.connectionState.value)
        assertFalse(manager.isConnected())
        assertEquals(null, manager.battery.value)
    }

    @Test
    fun `device info available only when connected`() = runTest {
        assertTrue(manager.fetchDeviceInfo() is OperationResult.Failure)
        manager.connect(device)
        assertTrue(manager.fetchDeviceInfo() is OperationResult.Success)
    }
}
