package com.mirego.trikot.foundation.concurrent.dispatchQueue

import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.native.concurrent.AtomicInt

actual class UIThreadDispatchQueue actual constructor() : TrikotDispatchQueue {
    private val count = AtomicInt(0)

    override fun isSerial() = true

    override fun dispatch(block: DispatchBlock) {
        val currentCount = count.addAndGet(1)
        if (currentCount == 1 && NSThread.isMainThread) {
            runQueueTask(block)
        } else {
            dispatch_async(
                dispatch_get_main_queue()
            ) {
                runQueueTask(block)
            }
        }
    }

    private fun runQueueTask(block: DispatchBlock) {
        block()
        count.decrement()
    }
}
