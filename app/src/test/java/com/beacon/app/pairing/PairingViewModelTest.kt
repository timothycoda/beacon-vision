package com.beacon.app.pairing

import com.beacon.core.bluetooth.BluetoothStateProvider
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.usecase.ConnectGlassesUseCase
import com.beacon.domain.glasses.usecase.ObserveConnectionStateUseCase
import com.beacon.domain.glasses.usecase.ScanForGlassesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeGlassesRepository
    private lateinit var viewModel: com.beacon.app.ui.pairing.PairingViewModel

    private class FakeBluetooth(private val on: Boolean) : BluetoothStateProvider {
        override fun isEnabled(): Boolean = on
        override fun enabledState(): Flow<Boolean> = MutableStateFlow(on)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeGlassesRepository()
        viewModel = com.beacon.app.ui.pairing.PairingViewModel(
            scanForGlasses = ScanForGlassesUseCase(repository),
            connectGlasses = ConnectGlassesUseCase(repository),
            observeConnectionState = ObserveConnectionStateUseCase(repository),
            bluetoothStateMonitor = FakeBluetooth(on = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `scanning populates discovered devices`() = runTest(dispatcher) {
        val collector = backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.startScan()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.devices.size)
        collector.cancel()
    }

    @Test
    fun `connecting reaches connected ready state`() = runTest(dispatcher) {
        val collector = backgroundScope.launch { viewModel.uiState.collect {} }

        val device = com.beacon.domain.glasses.model.GlassesDevice(
            name = "O_Beacon-X01",
            address = "AA:BB:CC:DD:EE:01",
            rssi = -48,
        )
        viewModel.connect(device)
        advanceUntilIdle()

        val state = viewModel.uiState.value.connectionState
        assertTrue(state is ConnectionState.Connected)
        assertTrue((state as ConnectionState.Connected).ready)
        collector.cancel()
    }

    @Test
    fun `bluetooth off blocks scanning with a message`() = runTest(dispatcher) {
        val offViewModel = com.beacon.app.ui.pairing.PairingViewModel(
            scanForGlasses = ScanForGlassesUseCase(repository),
            connectGlasses = ConnectGlassesUseCase(repository),
            observeConnectionState = ObserveConnectionStateUseCase(repository),
            bluetoothStateMonitor = FakeBluetooth(on = false),
        )
        val collector = backgroundScope.launch { offViewModel.uiState.collect {} }

        offViewModel.startScan()
        advanceUntilIdle()

        assertEquals(0, offViewModel.uiState.value.devices.size)
        assertTrue(offViewModel.uiState.value.errorMessage != null)
        collector.cancel()
    }
}
