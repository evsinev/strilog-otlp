package com.acme.strilog.sender.task.converter;

import com.acme.strilog.otlp.api.model.request.OtlpAttribute;

import java.util.ArrayList;
import java.util.List;

public class OtlpAttributesBuilder {

    private final List<OtlpAttribute> attributes = new ArrayList<>();

    public OtlpAttributesBuilder add(String aKey, String aValue) {
        if (aValue != null && !aValue.isBlank()) {
            attributes.add(OtlpAttribute.ofString(aKey, aValue));
        }
        return this;
    }

    public List<OtlpAttribute> toList() {
        return attributes;
    }
}
