package retriable

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay as sleep


data class RetryableExceptions(val exceptions: List<Class<out Throwable>>)

data class BackoffStrategy(
    var delay: Long,
    val timeUnit: TimeUnit,
    val delayFactor: Double
)

data class Retry(
    val retryOn: RetryableExceptions?,
    val backoff: BackoffStrategy?,
    val retries: Int

) {
    suspend operator fun <T> invoke(block: suspend () -> T): T? {
        for (i in 0..retries) {
            try {
                return block()
            } catch (e: Throwable) {
                val none = retryOn?.exceptions?.none { e::class.java.isAssignableFrom(it) } ?: true
                if (none || i == retries) throw e
            }
            backoff?.let {
                with(backoff) {
                    if (i == 0) sleep(timeUnit.toMillis(delay))
                    delay = (delay * delayFactor).toLong()
                    sleep(timeUnit.toMillis(delay))
                }
            }
        }
        return null
    }

    operator fun <T> invoke(block: () -> T): T? {
        for (i in 0..retries) {
            try {
                return block()
            } catch (e: Throwable) {
                val none = retryOn?.exceptions?.none { e::class.java.isAssignableFrom(it) } ?: true
                if (none || i == retries) throw e
            }
            backoff?.let {
                with(backoff) {
                    if (i == 0) Thread.sleep(timeUnit.toMillis(delay))
                    delay = (delay * delayFactor).toLong()
                    Thread.sleep(timeUnit.toMillis(delay))
                }
            }
        }
        return null
    }

    suspend fun <T> retryAsync(block: suspend () -> T): T? = this(block)

    fun <T> retry(block: () -> T): T? = this(block)
}

fun retryStrategy(block: RetryBuilder.() -> Unit): Retry = RetryBuilder().apply(block).build()

class RetryBuilder {
    private var retryOn: RetryableExceptions? = null
    fun retryOn(block: RetryableExceptionsBuilder.() -> Unit) {
        retryOn = RetryableExceptionsBuilder().apply(block).build()
    }

    private var backoff: BackoffStrategy? = null
    fun backoff(block: BackoffStrategyBuilder.() -> Unit) {
        backoff = BackoffStrategyBuilder().apply(block).build()
    }

    var retries: Int = 1

    fun build(): Retry = Retry(retryOn, backoff, retries)

}

class RetryableExceptionsBuilder {
    private var exs: MutableList<Class<out Throwable>> = mutableListOf()

    var exception: Class<out Throwable>? = null
        set(value) {
            value?.let { exs.add(value) }
        }

    var exceptions: List<Class<out Throwable>> = emptyList()
        set(value) {
            value.let { exs.addAll(value) }
        }

    fun build(): RetryableExceptions = RetryableExceptions(exs)
}

class BackoffStrategyBuilder {
    var delay: Long = 1
    var timeUnit: TimeUnit = TimeUnit.SECONDS
    var delayFactor: Double = 2.0

    fun build(): BackoffStrategy = BackoffStrategy(delay, timeUnit, delayFactor)
}

