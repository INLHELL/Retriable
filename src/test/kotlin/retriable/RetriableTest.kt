package retriable

import io.mockk.coVerify
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.TimeUnit.SECONDS

class RetriableTest {
    class TestStub {
        private var iteration: Int = 1
        fun call() = if (iteration++ < 4) throw IllegalArgumentException() else iteration
        suspend fun asyncCall() = if (iteration++ < 4) throw IllegalArgumentException() else iteration
    }

    @Test
    fun `basic synchronous retry usage`() {
        val testStub = spyk(TestStub())

        retryStrategy {
            retryOn {
                exception = IllegalArgumentException::class.java
            }
            backoff {
                delay = 1
                timeUnit = SECONDS
                delayFactor = 1.0
            }
            retries = 3
        }.retry {
            testStub.call()
        }

        verify (exactly = 4) { testStub.call() }
    }

    @Test
    fun `basic asynchronous retry usage`() {
        val testStub = spyk(TestStub())

        runBlocking {
            retryStrategy {
                retryOn {
                    exception = IllegalArgumentException::class.java
                }
                backoff {
                    delay = 1
                    timeUnit = SECONDS
                    delayFactor = 1.0
                }
                retries = 3
            }.retryAsync {
                testStub.asyncCall()
            }

            coVerify (exactly = 4) { testStub.asyncCall() }
        }
    }
}

