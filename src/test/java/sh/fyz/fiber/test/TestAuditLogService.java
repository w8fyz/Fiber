package sh.fyz.fiber.test;

import sh.fyz.fiber.core.security.logging.AuditLog;
import sh.fyz.fiber.core.security.logging.AuditLogService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestAuditLogService extends AuditLogService {

    private final List<AuditLog> logs = new CopyOnWriteArrayList<>();

    @Override
    public void onAuditLog(AuditLog log) {
        logs.add(log);
    }

    public List<AuditLog> getLogs() {
        return logs;
    }

    public AuditLog getLastLog() {
        return logs.isEmpty() ? null : logs.get(logs.size() - 1);
    }

    public void clear() {
        logs.clear();
    }
}
