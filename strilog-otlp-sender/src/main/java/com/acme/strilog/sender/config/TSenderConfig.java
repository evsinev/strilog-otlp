package com.acme.strilog.sender.config;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class TSenderConfig {
    @NonNull
    List<TSenderDir> dirs;
}
