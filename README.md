# Retriable

## Usage

#### Retries

Define a [retryStrategy] with provided DSL that expresses when retries should be performed:

```kotlin
var iteration = 1

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
    if (iteration++ < 4) {
        println("Failure :(")
        throw IllegalArgumentException()
    } 
    println("Success!")
}
```