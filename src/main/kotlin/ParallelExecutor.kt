import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.consumeAsFlow
import java.lang.Runnable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory


@FlowPreview
class ParallelExecutor(private val capacity: Int) {
    private val dispatcher = Executors.newFixedThreadPool(capacity, SenderThreadFactory()).asCoroutineDispatcher()

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    fun <I, O> run(
        inputSeq: Sequence<I>,
        callFunction: suspend (I) -> O
    ): ReceiveChannel<Result<O>> {
        val semaphore = Channel<Unit>(capacity = capacity)
        val resultCh = Channel<Result<O>>()
        val channel = Channel<Result<O>>()
        var job: Job? = null

        if (inputSeq.firstOrNull() == null) {
            channel.close()
            return channel
        }

        val handler = CoroutineExceptionHandler { _, exception ->
            exception.printStackTrace()
            job?.cancel()
            resultCh.close(exception)
            channel.close()
        }
        job = GlobalScope.launch(handler) {
            // errorの場合は例外をthrowしてチャネルを閉じる
            inputSeq.forEach { input ->
                launch {
                    // jobがキャンセルされない限り実行する
                    if (isActive) {
                        semaphore.send(Unit)
                        withContext(dispatcher) {
                            runCatching {
                                callFunction(input)
                            }
                        }.let { result ->
                            // error内容も含めてresultで返す
                            resultCh.send(result)
                            semaphore.receive()
                            // errorの場合は例外をthrowしてチャネルを閉じる
                            result.onFailure {
                                throw it
                            }
                        }
                    }
                }
            }
        }

        GlobalScope.launch {
            val size = inputSeq.toList().size
            resultCh.consumeAsFlow().collectIndexed { index, item ->
                channel.send(item)
                if ((index + 1) == size) {
                    resultCh.close()
                    channel.close()
                }
            }
        }
        return channel
    }
}

class SenderThreadFactory : ThreadFactory {
    private var count = 0
    override fun newThread(r: Runnable): Thread {
        return Thread(r, "sender-thread-" + ++count)
    }
}