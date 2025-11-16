package com.acme.strilog.otlp.api.model.request;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class OtlpAttributeValue {
    String stringValue;

    public static OtlpAttributeValue ofString(String aValue) {
        return OtlpAttributeValue.builder()
                .stringValue(aValue)
                .build();
    }
}
