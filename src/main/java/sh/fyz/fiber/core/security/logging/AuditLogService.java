package sh.fyz.fiber.core.security.logging;

public abstract class AuditLogService {

    public abstract void onAuditLog(AuditLog log);

}
