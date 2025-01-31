/*
 * Copyright 2016-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.reactive

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop
/* ktlint-disable no-wildcard-imports */
import kotlinx.coroutines.channels.*
/* ktlint-enable no-wildcard-imports */
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.internal.LockFreeLinkedListNode
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * Subscribes to this [Publisher] and performs the specified action for each received element.
 *
 * If [action] throws an exception at some point, the subscription is cancelled, and the exception is rethrown from
 * [collect]. Also, if the publisher signals an error, that error is rethrown from [collect].
 */
suspend inline fun <T> Publisher<T>.collect(action: (T) -> Unit): Unit =
    toChannel().consumeEach(action)

@PublishedApi
internal fun <T> Publisher<T>.toChannel(request: Int = 1): ReceiveChannel<T> {
    val channel = SubscriptionChannel<T>(request)
    subscribe(channel)
    return channel
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "SubscriberImplementation")
private class SubscriptionChannel<T>(
    private val request: Int
) : LinkedListChannel<T>(null), Subscriber<T> {
    init {
        require(request >= 0) { "Invalid request size: $request" }
    }

    private val _subscription = atomic<Subscription?>(null)

    // requested from subscription minus number of received minus number of enqueued receivers,
    // can be negative if we have receivers, but no subscription yet
    private val _requested = atomic(0)

    // --------------------- AbstractChannel overrides -------------------------------
    @Suppress("CANNOT_OVERRIDE_INVISIBLE_MEMBER")
    override fun onReceiveEnqueued() {
        _requested.loop { wasRequested ->
            val subscription = _subscription.value
            val needRequested = wasRequested - 1
            if (subscription != null && needRequested < 0) { // need to request more from subscription
                // try to fixup by making request
                if (wasRequested != request && !_requested.compareAndSet(wasRequested, request)) {
                    return@loop // continue looping if failed
                }
                subscription.request((request - needRequested).toLong())
                return
            }
            // just do book-keeping
            if (_requested.compareAndSet(wasRequested, needRequested)) return
        }
    }

    @Suppress("CANNOT_OVERRIDE_INVISIBLE_MEMBER")
    override fun onReceiveDequeued() {
        _requested.incrementAndGet()
    }

    @Suppress("CANNOT_OVERRIDE_INVISIBLE_MEMBER")
    override fun onClosedIdempotent(closed: LockFreeLinkedListNode) {
        _subscription.getAndSet(null)?.cancel() // cancel exactly once
    }

    // --------------------- Subscriber overrides -------------------------------
    override fun onSubscribe(s: Subscription) {
        _subscription.value = s
        while (true) { // lock-free loop on _requested
            if (isClosedForSend) {
                s.cancel()
                return
            }
            val wasRequested = _requested.value
            if (wasRequested >= request) return // ok -- normal story
            // otherwise, receivers came before we had subscription or need to make initial request
            // try to fixup by making request
            if (!_requested.compareAndSet(wasRequested, request)) continue
            s.request((request - wasRequested).toLong())
            return
        }
    }

    override fun onNext(t: T) {
        _requested.decrementAndGet()
        trySend(t) // Safe to ignore return value here, expectedly racing with cancellation
    }

    override fun onComplete() {
        close(cause = null)
    }

    override fun onError(t: Throwable) {
        close(cause = t)
    }
}
