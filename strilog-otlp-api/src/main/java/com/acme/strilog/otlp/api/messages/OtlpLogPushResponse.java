package com.acme.strilog.otlp.api.messages;

import com.acme.strilog.otlp.api.model.response.OtlpError;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class OtlpLogPushResponse {
    OtlpError error;
    int       statusCode;
}
