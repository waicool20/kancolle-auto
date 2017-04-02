package com.waicool20.kcsubswitcher

import kotlinx.coroutines.experimental.*

inline fun <T> Iterable<T>.parallelForEach(crossinline action: (T) -> Unit, pool: CoroutineDispatcher = CommonPool): Unit {
    runBlocking {
        val jobs = mutableListOf<Job>()
        for (element in this@parallelForEach) {
            launch(pool) {
                action(element)
            }.let(jobs::add)
        }
        jobs.forEach { it.join() }
    }
}

inline fun <T, R> Iterable<T>.parallelMap(crossinline transform: (T) -> (R), pool: CoroutineDispatcher = CommonPool): List<R> {
    return parallelMapTo(mutableListOf<R>(), transform, pool)
}

inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.parallelMapTo(destination: C, crossinline transform: (T) -> R, pool: CoroutineDispatcher = CommonPool): C {
    runBlocking {
        val jobs = mutableListOf<Deferred<R>>()
        for (item in this@parallelMapTo) {
            async(pool) {
                transform(item)
            }.let(jobs::add)
        }
        jobs.mapTo(destination, { it.await() })
    }
    return destination
}
