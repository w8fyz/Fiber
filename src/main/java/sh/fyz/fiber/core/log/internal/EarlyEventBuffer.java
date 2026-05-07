package sh.fyz.fiber.core.log.internal;

import sh.fyz.fiber.core.log.LogEvent;

public final class EarlyEventBuffer {

    private final LogEvent[] ring;
    private final int capacity;
    private int head;
    private int size;

    public EarlyEventBuffer(int capacity) {
        this.capacity = capacity;
        this.ring = new LogEvent[capacity];
    }

    public synchronized void offer(LogEvent event) {
        ring[head] = event;
        head = (head + 1) % capacity;
        if (size < capacity) size++;
    }

    public synchronized void drainTo(LogDispatcher dispatcher) {
        if (size == 0) return;
        int start = (head - size + capacity) % capacity;
        for (int i = 0; i < size; i++) {
            LogEvent ev = ring[(start + i) % capacity];
            ring[(start + i) % capacity] = null;
            if (ev != null) dispatcher.dispatch(ev);
        }
        head = 0;
        size = 0;
    }
}
