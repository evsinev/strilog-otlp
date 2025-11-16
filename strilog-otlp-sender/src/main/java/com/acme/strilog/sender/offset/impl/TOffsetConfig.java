package com.acme.strilog.sender.offset.impl;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class TOffsetConfig {
    String filename;
    long   offset;
}
