## Parallel Executor
+ A parallel execution support library using kotlin coroutine.
+ You can run a suspend function in parallel limiting the parallelism by a `capacity` parameter.
+ Share an instance created with a `capacity` through the context where you want to limit parallelism.
+ You can pass a sequence as an input, and then receive a result channel. all items in a sequence will be passed to a suspend function you supplied to a ParallelExecutor and processed in parallel. All results can be retrieved from a result channel.
+ If an exception occurred in an execution, a result channel will be closed automatically and the last item of the channel hold the exception. 
### Using it
```
dependencies {
    implementation "com.uzabase:pararell-executor:1.0"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:kotlinx_version"
}
```

### Example
I'd like to call an external API in parallel by limiting the capacity to 10. Sample code is below.
```
import kotlinx.coroutines.runBlocking

suspend fun callApi(word: String) = "call-$word"

fun main() {
    val parallelExecutor = ParallelExecutor(capacity = 10)    
    val seq = sequence {
        for (word in 0 until 100) {
            yield("$word")
        }
    }
   // return ReceiveChannel<Result<O>>
    val resultCh = parallelExecutor.run(
        inputSeq: Sequence<I> = seq,
        callFunction: suspend (I) -> O = { word -> callApi(word) }
    )
    // get values
    runBlocking {
        for (item in resultCh) {
            item.onSuccess { value ->
                println(value)
            }.onFailure { ex ->
                //When an error occurred, resultCh will be closed automatically and the last item in the channel will hold an exception.
                throw ex
            }
        }
    }
}
```

⚠︎You should share a ParallelExecutor instance while you want to keep the parallelism. 

