package com.acme.strilog.sender.task.batch;

import com.acme.strilog.otlp.api.IOtlplLogRemoteService;
import com.acme.strilog.otlp.api.messages.OtlpLogPushRequest;
import com.acme.strilog.otlp.api.messages.OtlpLogPushResponse;
import com.acme.strilog.otlp.api.model.request.OtlpResource;
import com.acme.strilog.otlp.api.model.request.OtlpLogRecord;
import com.acme.strilog.otlp.api.model.request.OtlpResourceLog;
import com.acme.strilog.otlp.api.model.request.OtlpScopeLog;
import com.acme.strilog.sender.task.mapper.OtlpResourceMapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BatchSenderClientImpl implements IBatchSenderClient<OtlpLogRecord> {

    private final IOtlplLogRemoteService otlpRemoteClient;
    private final OtlpResource           otlpResource;

    public BatchSenderClientImpl(IOtlplLogRemoteService aOtlpClient, Map<String, String> resourceAttributes) {
        otlpRemoteClient = aOtlpClient;
        otlpResource     = OtlpResourceMapper.of(resourceAttributes);
    }

    @Override
    public BatchSendResult sendItems(List<OtlpLogRecord> aItems) {
        OtlpLogPushResponse response;
        try {
            response = otlpRemoteClient.push(OtlpLogPushRequest.builder()
                     .resourceLogs(
                             List.of(
                                     OtlpResourceLog.builder()
                                             .resource  (otlpResource)
                                             .scopeLogs (
                                                     List.of(
                                                             OtlpScopeLog.builder()
                                                                     .logRecords(aItems)
                                                                     .build()
                                                     )
                                             )
                                             .build()
                             )
                     )
                    .build());
        } catch (Exception e) {
            return BatchSendResult.retry("Cannot send batch for resource " + otlpResource + ": " + e
                    + (e.getCause() != null ? ", caused by " + e.getCause() : ""));
        }

        return toResult(response);
    }

    private static BatchSendResult toResult(OtlpLogPushResponse aResponse) {
        int statusCode = aResponse.getStatusCode();

        if (statusCode == 200) {
            return BatchSendResult.delivered();
        }

        if (isRequestTooLarge(aResponse)) {
            return BatchSendResult.tooLarge("Cannot save logs " + aResponse);
        }

        if (statusCode == 429 || statusCode >= 500) {
            return BatchSendResult.retry("Cannot save logs " + aResponse);
        }

        return BatchSendResult.rejected("Cannot save logs " + aResponse);
    }

    /**
     * otelcol does not answer 413: confighttp cuts the body at max_request_body_size, and the OTLP
     * receiver turns any error of reading the body into 400 "http: request body too large".
     * 413 comes from a proxy in front of the collector.
     */
    private static boolean isRequestTooLarge(OtlpLogPushResponse aResponse) {
        if (aResponse.getStatusCode() == 413) {
            return true;
        }

        if (aResponse.getStatusCode() != 400) {
            return false;
        }

        String message = aResponse.getError() != null ? aResponse.getError().getMessage() : aResponse.getBody();
        return message != null && message.toLowerCase(Locale.ROOT).contains("too large");
    }
}
