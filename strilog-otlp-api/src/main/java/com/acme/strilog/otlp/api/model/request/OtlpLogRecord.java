package com.acme.strilog.otlp.api.model.request;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class OtlpLogRecord {
    long                timeUnixNano;
    int                 severityNumber;
    String              severityText;
    OtlpAttributeValue  body;
    List<OtlpAttribute> attributes;
    String              traceId;
    String              spanId;
}
