package com.payflow.exporter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;

/**
 * Scheduled job (EventBridge cron): the Lambda candidate of PayFlow is a periodic task, not the API.
 * Only the handler contract is shown here; the real export reads the reporting replica (P9.08).
 */
public class StatementExporter implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        return "exported at " + java.time.Instant.now() + " remaining=" + context.getRemainingTimeInMillis();
    }
}
