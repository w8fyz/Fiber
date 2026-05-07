package sh.fyz.fiber.core.log.internal;

import sh.fyz.fiber.core.log.LogLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class LevelTable {

    private volatile LogLevel root;
    private final ConcurrentMap<String, LogLevel> categories = new ConcurrentHashMap<>();

    public LevelTable(LogLevel root) {
        this.root = root;
    }

    public void setRoot(LogLevel level) {
        this.root = level;
    }

    public LogLevel getRoot() {
        return root;
    }

    public void set(String category, LogLevel level) {
        if (level == null) {
            categories.remove(category);
        } else {
            categories.put(category, level);
        }
    }

    public LogLevel resolve(String name) {
        if (categories.isEmpty() || name == null) return root;
        String best = null;
        for (Map.Entry<String, LogLevel> e : categories.entrySet()) {
            String key = e.getKey();
            if (name.equals(key) || name.startsWith(key + ".")) {
                if (best == null || key.length() > best.length()) {
                    best = key;
                }
            }
        }
        return best != null ? categories.get(best) : root;
    }
}
