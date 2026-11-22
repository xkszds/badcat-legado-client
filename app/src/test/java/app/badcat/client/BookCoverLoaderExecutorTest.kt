package app.badcat.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor

class BookCoverLoaderExecutorTest {
    @Test
    fun coverRequestsUseFixedWorkersAndBoundedPendingQueue() {
        val executor = createBookCoverExecutor()
        try {
            assertEquals(3, executor.corePoolSize)
            assertEquals(3, executor.maximumPoolSize)
            assertTrue(executor.queue is ArrayBlockingQueue<*>)
            assertEquals(48, (executor.queue as ArrayBlockingQueue<*>).remainingCapacity())
            assertTrue(executor.rejectedExecutionHandler is ThreadPoolExecutor.DiscardOldestPolicy)
        } finally {
            executor.shutdownNow()
        }
    }
}
