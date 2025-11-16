package com.acme.strilog.otlp.api;

import com.acme.strilog.otlp.api.messages.OtlpLogPushRequest;
import com.acme.strilog.otlp.api.messages.OtlpLogPushResponse;

public interface IOtlplLogRemoteService {

    OtlpLogPushResponse push(OtlpLogPushRequest aLogs);

}
