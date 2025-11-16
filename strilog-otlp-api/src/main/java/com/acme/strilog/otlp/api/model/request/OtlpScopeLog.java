package com.acme.strilog.otlp.api.model.request;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class OtlpScopeLog {
    // Object scope;

    List<OtlpLogRecord> logRecords;
}
