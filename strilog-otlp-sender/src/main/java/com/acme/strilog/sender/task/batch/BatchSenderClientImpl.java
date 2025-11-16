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
import java.util.Map;

public class BatchSenderClientImpl implements IBatchSenderClient<OtlpLogRecord> {

    private final IOtlplLogRemoteService otlpRemoteClient;
    private final OtlpResource           otlpResource;

    public BatchSenderClientImpl(IOtlplLogRemoteService aOtlpClient, Map<String, String> resourceAttributes) {
        otlpRemoteClient = aOtlpClient;
        otlpResource     = OtlpResourceMapper.of(resourceAttributes);
    }

    @Override
    public void sendItems(List<OtlpLogRecord> aItems) {
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
            throw new IllegalStateException("Cannot send batch for resource " + otlpResource, e);
        }

        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("Cannot save logs " + response);
        }

    }
}
