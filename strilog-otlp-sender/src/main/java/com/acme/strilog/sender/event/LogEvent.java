package com.acme.strilog.sender.event;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class LogEvent {
    String              date;
    long                epoch;
    String              level;
    String              thread;
    String              clazz;
    Map<String, String> mdc;
    String              template;
    List<String>        args;
    Map<String, String> kv;
    String              messageId;
    String              stacktrace;
    String              exceptionLine;
    String              exceptionMessage;
    String              appName;
    String              appInstance;
}
