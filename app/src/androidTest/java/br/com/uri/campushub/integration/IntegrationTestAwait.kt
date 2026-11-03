package br.com.uri.campushub.integration

import org.junit.Assert.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun <T> awaitResult(
    operation: ((Result<T>) -> Unit) -> Unit
): Result<T> {
    val callback = CountDownLatch(1)
    var operationResult: Result<T>? = null

    operation { result ->
        operationResult = result
        callback.countDown()
    }

    assertTrue(
        "O Firebase Emulator não respondeu a tempo.",
        callback.await(15, TimeUnit.SECONDS)
    )

    return requireNotNull(operationResult)
}
