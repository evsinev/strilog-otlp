package com.acme.strilog.otlp.api.model.request;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class OtlpAttribute {
    String             key;
    OtlpAttributeValue value;

    public static OtlpAttribute ofString(String aKey, String aValue) {
        return OtlpAttribute.builder()
                .key(aKey)
                .value(
                        OtlpAttributeValue.builder()
                                .stringValue(aValue)
                               .build()
                )
                .build();
    }
}
