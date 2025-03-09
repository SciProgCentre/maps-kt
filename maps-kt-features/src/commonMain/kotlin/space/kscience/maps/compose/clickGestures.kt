package space.kscience.maps.compose

import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/*
 * Clone of tap gestures for mouse
 */

private val NoPressGesture: suspend PressGestureScope.(event: PointerEvent) -> Unit = { }

internal fun PointerEvent.consume() = changes.forEach { it.consume() }

internal val PointerEvent.firstChange get() = changes.first()

public val PointerEvent.position: Offset get() = firstChange.position


/**
 * An alternative to [detectTapGestures] with reimplementation of internal logic
 */
internal suspend fun PointerInputScope.detectClicks(
    onDoubleClick: (Density.(PointerEvent) -> Unit)? = null,
    onLongClick: (Density.(PointerEvent) -> Unit)? = null,
    onPress: suspend PressGestureScope.(event: PointerEvent) -> Unit = NoPressGesture,
    onClick: (Density.(PointerEvent) -> Unit)? = null,
): Unit = coroutineScope {
    // special signal to indicate to the sending side that it shouldn't intercept and consume
    // cancel/up events as we're only require down events
    val pressScope = PressGestureScopeImpl(this@detectClicks)

    awaitEachGesture {
        val down = awaitFirstDownEvent()
        down.consume()

        pressScope.reset()
        if (onPress !== NoPressGesture) launch {
            pressScope.onPress(down)
        }
        val longPressTimeout = onLongClick?.let {
            viewConfiguration.longPressTimeoutMillis
        } ?: (Long.MAX_VALUE / 2)
        var upOrCancel: PointerEvent? = null
        try {
            // wait for first tap up or long press
            upOrCancel = withTimeout(longPressTimeout) {
                waitForUpOrCancellation()
            }
            if (upOrCancel == null) {
                pressScope.cancel() // tap-up was canceled
            } else {
                upOrCancel.consume()
                pressScope.release()
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            onLongClick?.invoke(this, down)
            consumeUntilUp()
            pressScope.release()
        }

        if (upOrCancel != null) {
            // tap was successful.
            if (onDoubleClick == null) {
                onClick?.invoke(this, down) // no need to check for double-tap.
            } else {
                // check for second tap
                val secondDown = awaitSecondDown(upOrCancel.firstChange)

                if (secondDown == null) {
                    onClick?.invoke(this, down) // no valid second tap started
                } else {
                    // Second tap down detected
                    pressScope.reset()
                    if (onPress !== NoPressGesture) {
                        launch { pressScope.onPress(secondDown) }
                    }

                    try {
                        // Might have a long second press as the second tap
                        withTimeout(longPressTimeout) {
                            val secondUp = waitForUpOrCancellation()
                            if (secondUp != null) {
                                secondUp.consume()
                                pressScope.release()
                                onDoubleClick(secondDown)
                            } else {
                                pressScope.cancel()
                                onClick?.invoke(this, down)
                            }
                        }
                    } catch (e: PointerEventTimeoutCancellationException) {
                        // The first tap was valid, but the second tap is a long press.
                        // notify for the first tap
                        onClick?.invoke(this, down)

                        // notify for the long press
                        onLongClick?.invoke(this, secondDown)
                        consumeUntilUp()
                        pressScope.release()
                    }
                }
            }
        }
    }
}

/**
 * Consumes all pointer events until nothing is pressed and then returns. This method assumes
 * that something is currently pressed.
 */
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.consume()
    } while (event.changes.any { it.pressed })
}

/**
 * Waits for [ViewConfiguration.doubleTapTimeoutMillis] for a second press event. If a
 * second press event is received before the time-out, it is returned or `null` is returned
 * if no second press is received.
 */
private suspend fun AwaitPointerEventScope.awaitSecondDown(
    firstUp: PointerInputChange,
): PointerEvent? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
    val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
    var event: PointerEvent
    // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
    do {
        event = awaitFirstDownEvent()
    } while (event.firstChange.uptimeMillis < minUptime)
    event
}

///**
// * Shortcut for cases when we only need to get press/click logic, as for cases without long press
// * and double click we don't require channelling or any other complications.
// *
// * Each function parameter receives an [Offset] representing the position relative to the containing
// * element. The [Offset] can be outside the actual bounds of the element itself meaning the numbers
// * can be negative or larger than the element bounds if the touch target is smaller than the
// * [ViewConfiguration.minimumTouchTargetSize].
// */
//internal suspend fun PointerInputScope.detectTapAndPress(
//    onPress: suspend PressGestureScope.(event: PointerEvent, Offset) -> Unit = NoPressGesture,
//    onTap: ((Offset) -> Unit)? = null,
//) {
//    val pressScope = PressGestureScopeImpl(this)
//    forEachGesture {
//        coroutineScope {
//            pressScope.reset()
//            awaitPointerEventScope {
//
//                val down = awaitFirstDownEvent()
//
//                down.consume()
//
//                if (onPress !== NoPressGesture) {
//                    launch { pressScope.onPress(down, down.position) }
//                }
//
//                val up = waitForUpOrCancellation()
//                if (up == null) {
//                    pressScope.cancel() // tap-up was canceled
//                } else {
//                    up.consume()
//                    pressScope.release()
//                    onTap?.invoke(up.position)
//                }
//            }
//        }
//    }
//}

/**
 * Reads events until the first down is received. If [requireUnconsumed] is `true` and the first
 * down is consumed in the [PointerEventPass.Main] pass, that gesture is ignored.
 */
internal suspend fun AwaitPointerEventScope.awaitFirstDownEvent(
    requireUnconsumed: Boolean = true,
): PointerEvent = awaitFirstDownEventOnPass(pass = PointerEventPass.Main, requireUnconsumed = requireUnconsumed)

internal suspend fun AwaitPointerEventScope.awaitFirstDownEventOnPass(
    pass: PointerEventPass,
    requireUnconsumed: Boolean,
): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(pass)
    } while (!event.changes.all {
            if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed()
        })
    return event
}

/**
 * Reads events until all pointers are up or the gesture was canceled. The gesture
 * is considered canceled when a pointer leaves the event region, a position change
 * has been consumed or a pointer down change event was consumed in the [PointerEventPass.Main]
 * pass. If the gesture was not canceled, the final up change is returned or `null` if the
 * event was canceled.
 */
internal suspend fun AwaitPointerEventScope.waitForUpOrCancellation(): PointerEvent? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        if (event.changes.all { it.changedToUp() }) {
            // All pointers are up
            return event
        }

        if (event.changes.any {
                it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding)
            }
        ) {
            return null // Canceled
        }

        // Check for cancel by position consumption. We can look on the Final pass of the
        // existing pointer event because it comes after the Main pass we checked above.
        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.any { it.isConsumed }) {
            return null
        }
    }
}

/**
 * [detectTapGestures]'s implementation of [PressGestureScope].
 */
internal class PressGestureScopeImpl(
    density: Density,
) : PressGestureScope, Density by density {
    private var isReleased = false
    private var isCanceled = false
    private val mutex = Mutex(locked = false)

    /**
     * Called when a gesture has been canceled.
     */
    fun cancel() {
        isCanceled = true
        mutex.unlock()
    }

    /**
     * Called when all pointers are up.
     */
    fun release() {
        isReleased = true
        mutex.unlock()
    }

    /**
     * Called when a new gesture has started.
     */
    fun reset() {
        mutex.tryLock() // If tryAwaitRelease wasn't called, this will be unlocked.
        isReleased = false
        isCanceled = false
    }

    override suspend fun awaitRelease() {
        if (!tryAwaitRelease()) {
            throw GestureCancellationException("The press gesture was canceled.")
        }
    }

    override suspend fun tryAwaitRelease(): Boolean {
        if (!isReleased && !isCanceled) {
            mutex.lock()
        }
        return isReleased
    }
}
