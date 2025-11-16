package com.acme.strilog.sender.task.mapper;

import com.acme.strilog.otlp.api.model.request.OtlpResource;
import com.acme.strilog.otlp.api.model.request.OtlpAttribute;
import com.acme.strilog.otlp.api.model.request.OtlpAttributeValue;

import java.util.List;
import java.util.Map;

public class OtlpResourceMapper {

    public static OtlpResource of(Map<String, String> resourceAttributes) {
        return OtlpResource.builder()
                .attributes(toAttributes(resourceAttributes))
                .build();
    }

    private static List<OtlpAttribute> toAttributes(Map<String, String> resourceAttributes) {
        return resourceAttributes.entrySet()
                .stream()
                .map(it -> OtlpAttribute.builder()
                        .key(it.getKey())
                        .value(
                                OtlpAttributeValue.builder()
                                        .stringValue(it.getValue())
                                       .build()
                        )
                        .build()
                )
                .toList();
    }
}
