package sh.fyz.fiber.core.log.internal;

import sh.fyz.fiber.core.log.LogEvent;
import sh.fyz.fiber.core.log.LogHandler;
import sh.fyz.fiber.core.log.LogLevel;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public final class LogDispatcher {

    private static final int DEFAULT_CAPACITY = 8192;
    private static final long DROP_REPORT_INTERVAL_MS = 5_000L;
    private static final long ERROR_PUT_TIMEOUT_MS = 50L;

    private final ArrayBlockingQueue<LogEvent> queue = new ArrayBlockingQueue<>(DEFAULT_CAPACITY);
    private final CopyOnWriteArrayList<LogHandler> handlers = new CopyOnWriteArrayList<>();
    private final LongAdder dropped = new LongAdder();
    private final AtomicBoolean threadStarted = new AtomicBoolean(false);
    private volatile Thread worker;
    private volatile boolean running = true;
    private volatile long lastDropReport = 0L;

    public void addHandler(LogHandler handler) {
        if (handler == null) return;
        handlers.addIfAbsent(handler);
        ensureWorker();
    }

    public void removeHandler(LogHandler handler) {
        handlers.remove(handler);
    }

    public int handlerCount() {
        return handlers.size();
    }

    public void dispatch(LogEvent event) {
        if (handlers.isEmpty()) return;

        boolean preferSync = event.level() == LogLevel.ERROR;
        if (!preferSync) {
            for (LogHandler h : handlers) {
                if (h.prefersSync()) {
                    preferSync = true;
                    break;
                }
            }
        }

        if (event.level() == LogLevel.ERROR) {
            try {
                if (queue.offer(event, ERROR_PUT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) return;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            emitSync(event);
            return;
        }

        if (preferSync) {
            emitSync(event);
            return;
        }

        if (!queue.offer(event)) {
            dropped.increment();
            maybeReportDrops();
        }
    }

    private void emitSync(LogEvent event) {
        for (LogHandler h : handlers) {
            if (event.level().severity < h.minLevel().severity) continue;
            try {
                h.emit(event);
            } catch (Throwable t) {
                System.err.println("[Fiber] log handler " + h.getClass().getName() + " failed: " + t);
            }
        }
    }

    private void maybeReportDrops() {
        long now = System.currentTimeMillis();
        if (now - lastDropReport < DROP_REPORT_INTERVAL_MS) return;
        lastDropReport = now;
        long count = dropped.sumThenReset();
        if (count == 0) return;
        LogEvent synthetic = new LogEvent(
                now,
                LogLevel.WARN,
                "fiber.log.dispatcher",
                "dispatcher dropped " + count + " log events (queue full)",
                null,
                Thread.currentThread().getName(),
                null,
                null
        );
        queue.offer(synthetic);
    }

    private void ensureWorker() {
        if (threadStarted.compareAndSet(false, true)) {
            worker = new Thread(this::runLoop, "fiber-log-dispatcher");
            worker.setDaemon(true);
            worker.start();
        }
    }

    private void runLoop() {
        while (running || !queue.isEmpty()) {
            try {
                LogEvent event = queue.poll(250, TimeUnit.MILLISECONDS);
                if (event != null) emitSync(event);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                System.err.println("[Fiber] log dispatcher loop error: " + t);
            }
        }
    }

    public void shutdown() {
        running = false;
        Thread w = worker;
        if (w == null) {
            closeHandlers();
            return;
        }
        try {
            w.join(5_000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        closeHandlers();
    }

    private void closeHandlers() {
        List<LogHandler> snapshot = List.copyOf(handlers);
        for (LogHandler h : snapshot) {
            try {
                h.close();
            } catch (Throwable t) {
                System.err.println("[Fiber] log handler close failed: " + t);
            }
        }
    }
}
