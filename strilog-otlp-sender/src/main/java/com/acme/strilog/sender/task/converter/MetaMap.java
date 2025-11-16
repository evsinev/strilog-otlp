package com.acme.strilog.sender.task.converter;

import java.util.HashMap;
import java.util.Map;

public class MetaMap {
    private final Map<String, String> map = new HashMap<>();

    public MetaMap put(String aKey, String aValue) {
        if (aValue == null || aValue.isEmpty()) {
            return this;
        }

        map.put(aKey, aValue);
        return this;
    }

    public Map<String, String> toMap() {
        return map;
    }
}
