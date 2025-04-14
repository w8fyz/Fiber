package sh.fyz.fiber.example;

import nonapi.io.github.classgraph.json.JSONUtils;
import sh.fyz.fiber.core.security.logging.AuditLog;
import sh.fyz.fiber.core.security.logging.AuditLogService;
import sh.fyz.fiber.util.JsonUtil;

public class LogService implements AuditLogService {

    @Override
    public void onAuditLog(AuditLog log) {
        System.out.println(log.getFormated());
        try {
            System.out.println(log.getResult().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
