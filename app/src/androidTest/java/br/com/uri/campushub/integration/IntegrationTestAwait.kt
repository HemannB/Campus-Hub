package br.com.uri.campushub.integration

import com.google.android.gms.tasks.Task
import org.junit.Assert.fail
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

fun awaitTask(task: Task<*>) {
    val callback = CountDownLatch(1)
    task.addOnCompleteListener { callback.countDown() }

    assertTrue(
        "O Firebase Emulator não respondeu a tempo.",
        callback.await(15, TimeUnit.SECONDS)
    )

    if (!task.isSuccessful) {
        fail(task.exception?.message ?: "A operação no Firebase Emulator falhou.")
    }
}

fun <T> awaitTaskResult(task: Task<T>): T {
    awaitTask(task)
    return task.result
}
