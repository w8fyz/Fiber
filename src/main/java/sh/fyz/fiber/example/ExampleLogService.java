package sh.fyz.fiber.example;

import sh.fyz.fiber.core.security.logging.AuditLog;
import sh.fyz.fiber.core.security.logging.AuditLogService;

public class ExampleLogService extends AuditLogService {

    @Override
    public void onAuditLog(AuditLog log) {
        if(true) return;
        //System.out.println("BEGIN LOG TRANSACTION");
        //System.out.println("Action : "+log.getAction());
        //System.out.println("Method : "+log.getMethod());
        //System.out.println("IP : "+log.getIp());
        //System.out.println("URI : "+log.getUri());
        //System.out.println("UserAgent : "+log.getUserAgent());
        //System.out.println("Parameters : "+log.getParameters());
        //System.out.println("PathVariables : "+log.getPathVariables());
        //System.out.println("QueryParams : "+log.getQueryParams());
        //System.out.println("RequestBody : "+log.getRequestBody());
        //System.out.println("Response : "+log.getResponse());
        //System.out.println("Status : "+log.getStatus());
        //System.out.println("Timestamp : "+log.getTimestamp());
        //System.out.println("END LOG TRANSACTION");
    }
}
