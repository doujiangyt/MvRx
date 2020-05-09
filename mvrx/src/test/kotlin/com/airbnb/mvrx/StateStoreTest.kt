package com.airbnb.mvrx

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

data class MvRxStateStoreTestState(val count: Int = 1, val list: List<Int> = emptyList()) : MvRxState

@Suppress("EXPERIMENTAL_API_USAGE")
class StateStoreTest : BaseTest() {

    @Test
    fun testGetRunsSynchronouslyForTests() {
        val store = CoroutinesStateStore(MvRxStateStoreTestState())
        var callCount = 0
        store.get { callCount++ }
        assertEquals(1, callCount)
    }

    @Test
    fun testSetState() {
        val store = CoroutinesStateStore(MvRxStateStoreTestState())
        store.set {
            copy(count = 2)
        }
        var called = false
        store.get {
            assertEquals(2, it.count)
            called = true
        }
        assertTrue(called)
    }

    @Test
    fun testSubscribeNotCalledForNoop() {
        val scope = TestCoroutineScope()
        val store = CoroutinesStateStore(MvRxStateStoreTestState())
        var callCount = 0
        store.flow.onEach {
            callCount++
        }.launchIn(scope)
        assertEquals(1, callCount)
        store.set { this }
        assertEquals(1, callCount)
    }

    @Test
    fun testSubscribeNotCalledForSameValue() {
        val scope = TestCoroutineScope()
        val store = CoroutinesStateStore(MvRxStateStoreTestState())
        var callCount = 0
        store.flow.onEach {
            callCount++
        }.launchIn(scope)
        assertEquals(1, callCount)
        store.set { copy() }
        assertEquals(1, callCount)
    }

    @Test
    fun testStateFlowReceivesAllStates() = runBlockingTest {
        val store = CoroutinesStateStore(MvRxStateStoreTestState(), this)
        val receivedValues = mutableListOf<Int>()
        val subscribeJob = store.flow.onEach {
            println("received count=${it.count}")
            receivedValues += it.count
            delay(1000)
        }.launchIn(this)
        (2..6).forEach {
            println("send count=$it")
            store.set { copy(count = it) }
        }
        delay(6000)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), receivedValues)
        subscribeJob.cancel()
    }

    @Test
    fun testScope() = runBlockingTest {
        val store = CoroutinesStateStore(MvRxStateStoreTestState(), this)
        val job = store.flow.onEach {
            println("it")
        }.launchIn(this)
        job.cancel()
    }
}
