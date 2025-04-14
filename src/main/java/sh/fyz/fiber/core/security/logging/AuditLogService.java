package sh.fyz.fiber.core.security.logging;

public interface AuditLogService {

    void onAuditLog(AuditLog log);

}
