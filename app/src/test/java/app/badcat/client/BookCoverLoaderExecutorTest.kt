package app.badcat.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor

class BookCoverLoaderExecutorTest {
    @Test
    fun coverRequestsUseFixedWorkersAndBoundedPendingQueue() {
        val executor = createBookCoverExecutor()
        try {
            assertEquals(3, executor.corePoolSize)
            assertEquals(3, executor.maximumPoolSize)
            assertIs<ArrayBlockingQueue<Runnable>>(executor.queue)
            assertEquals(48, executor.queue.remainingCapacity())
            assertIs<ThreadPoolExecutor.DiscardOldestPolicy>(executor.rejectedExecutionHandler)
        } finally {
            executor.shutdownNow()
        }
    }
}
