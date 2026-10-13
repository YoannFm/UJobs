package me.usainsrht.ujobs.storage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Runs database writes one at a time on their own thread, so the server thread never waits for the database.
 * Closing waits for the writes still queued, nothing is lost when the plugin shuts down.
 */
class BackgroundWriter implements AutoCloseable {

    private static final long CLOSE_TIMEOUT_SECONDS = 30;

    private final ExecutorService executor;

    BackgroundWriter(String threadName) {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Queues the task. Once closed, the task runs right away on the calling thread instead of being dropped. */
    void execute(Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
