package com.acme.strilog.otlp.api.messages;

import com.acme.strilog.otlp.api.model.request.OtlpResourceLog;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class OtlpLogPushRequest {
    List<OtlpResourceLog> resourceLogs;
}
