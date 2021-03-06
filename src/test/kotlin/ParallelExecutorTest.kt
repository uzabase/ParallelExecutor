import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.specs.StringSpec
import io.kotlintest.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi

@FlowPreview
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class ParallelExecutorTest : StringSpec() {
    init {
        "Operation check"{
            val target = ParallelExecutor(1)
            fun doTest(input: TestInput) = TestOutput(input.key, "${input.value} out")
            val key1 = "key1"
            val key2 = "key2"
            val value1 = "value1"
            val value2 = "value2"

            val inputSeq = sequenceOf(TestInput(key1, value1), TestInput(key2, value2))
            val ch = target.run(inputSeq) { s -> doTest(s) }
            val items = mutableListOf<Result<TestOutput>>()
            for (item in ch) {
                items.add(item)
            }

            items shouldContainAll  mutableListOf(
                Result.success(TestOutput(key1, "$value1 out")),
                Result.success(TestOutput(key2, "$value2 out"))
            )
        }

        "if sequens size = 0 , return closed channel"{
            fun doTest(s: String) = "out $s"
            val target = ParallelExecutor(1)
            val inputSeq = emptySequence<String>()
            val ch = target.run(inputSeq) { s -> doTest(s) }
            ch.isClosedForReceive shouldBe true
        }
    }
}

data class TestInput(val key: String, val value: String)
data class TestOutput(val key: String, val value: String)
