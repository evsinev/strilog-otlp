package com.acme.strilog.sender.config;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class TSenderDir {
    @NonNull
    String path;

    @NonNull
    Map<String, String> resourceAttributes;
}
