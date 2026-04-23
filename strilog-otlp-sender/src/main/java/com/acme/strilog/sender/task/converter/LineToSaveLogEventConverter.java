package com.acme.strilog.sender.task.converter;

import com.acme.strilog.otlp.api.model.request.OtlpAttribute;
import com.acme.strilog.otlp.api.model.request.OtlpAttributeValue;
import com.acme.strilog.otlp.api.model.request.OtlpLogRecord;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.acme.strilog.sender.event.LogEvent;

import java.util.List;
import java.util.Map;

import static org.slf4j.helpers.MessageFormatter.arrayFormat;

public class LineToSaveLogEventConverter implements ILineToItemConverter<OtlpLogRecord> {

    private static final int INFO_LEVEL = 6;

    private static final String MDC_TRACE_ID_KEY = "trace_id";
    private static final String MDC_SPAN_ID_KEY  = "span_id";

    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    @Override
    public OtlpLogRecord convertToItem(String aLine) {
        LogEvent event = gson.fromJson(aLine, LogEvent.class);
        return mapEvent(event);
    }

    private OtlpLogRecord mapEvent(LogEvent aEvent) {
        Map<String, String> mdc = aEvent.getMdc();
        String traceId = mdc != null ? mdc.get(MDC_TRACE_ID_KEY) : null;
        String spanId  = mdc != null ? mdc.get(MDC_SPAN_ID_KEY)  : null;

        return OtlpLogRecord.builder()
                .body         ( OtlpAttributeValue.ofString(toMessage(aEvent)) )
                .severityText ( aEvent.getLevel()                              )
                .attributes   ( toAttributes(aEvent)                           )
                .timeUnixNano ( toNanoEpoch(aEvent.getEpoch())                 )
                .traceId      ( traceId                                        )
                .spanId       ( spanId                                         )
                .build();
    }

    private List<OtlpAttribute> toAttributes(LogEvent aEvent) {
        OtlpAttributesBuilder builder = new OtlpAttributesBuilder();

        builder.add("log.body.template"  , aEvent.getTemplate());
        builder.add("otel.library.name"  , aEvent.getClazz());
        builder.add("thread.name"        , aEvent.getThread());
        builder.add("service.version"    , aEvent.getVersion());

        builder.add("exception.message"   , aEvent.getExceptionMessage());
        builder.add("exception.stacktrace", aEvent.getStacktrace());
        builder.add("exception.type"      , aEvent.getExceptionType());
        builder.add("exception.line"      , aEvent.getExceptionLine());

        if (aEvent.getMdc() != null) {
            for (Map.Entry<String, String> entry : aEvent.getMdc().entrySet()) {
                if (MDC_TRACE_ID_KEY.equals(entry.getKey()) || MDC_SPAN_ID_KEY.equals(entry.getKey())) {
                    builder.add(entry.getKey(), entry.getValue());
                    continue;
                }
                builder.add("mdc." + entry.getKey(), entry.getValue());
            }
        }

        List<String> args = aEvent.getArgs();
        // log.body.parameters
        if (args != null) {
            for (int i = 0; i< args.size(); i++) {
                builder.add("arg." + i, args.get(i));
            }
        }

        if (aEvent.getKv() != null) {
            for (Map.Entry<String, String> entry : aEvent.getKv().entrySet()) {
                builder.add("kv." + entry.getKey(), entry.getValue());
            }
        }

        return builder.toList();
    }

//    private Map<String, String> createMeta(LogEvent aEvent) {
//        MetaMap map = new MetaMap()
//          .put("level"             , aEvent.getLevel())
//          .put("template"          , aEvent.getTemplate())
//          .put("class"             , aEvent.getClazz())
//          .put("thread"            , aEvent.getThread())
//          .put("app_instance"      , aEvent.getAppInstance())
//          .put("app_name"          , aEvent.getAppName())
//          .put("exception_line"    , aEvent.getExceptionLine())
//          .put("exception_message" , aEvent.getExceptionMessage());
//
//        if (aEvent.getMdc() != null) {
//            for (Map.Entry<String, String> entry : aEvent.getMdc().entrySet()) {
//                map.put("mdc_" + entry.getKey(), entry.getValue());
//            }
//        }
//
//        List<String> args = aEvent.getArgs();
//        if (args != null) {
//            for (int i = 0; i< args.size(); i++) {
//                map.put("arg_" + i, args.get(i));
//            }
//        }
//
//        if (aEvent.getKv() != null) {
//            for (Map.Entry<String, String> entry : aEvent.getKv().entrySet()) {
//                map.put("kv_" + entry.getKey(), entry.getValue());
//            }
//        }
//
//        return map.toMap();
//    }

    private long toNanoEpoch(long epoch) {
        long nanos = System.nanoTime() % 1_000_000;
        return epoch * 1_000_000L + nanos ;
    }

    private String toMessage(LogEvent aEvent) {
        StringBuilder sb = new StringBuilder();

//        sb.append(aEvent.getThread());
//        sb.append(' ');
//        sb.append(aEvent.getLevel());
//        sb.append(' ');
//        sb.append(aEvent.getClazz());
//        sb.append(' ');

        if (aEvent.getArgs() != null && !aEvent.getArgs().isEmpty()) {
            sb.append(arrayFormat(aEvent.getTemplate(), aEvent.getArgs().toArray(new String[0])).getMessage());
        } else {
            sb.append(aEvent.getTemplate());
        }

//        if (aEvent.getExceptionMessage() != null) {
//            sb.append(' ');
//            sb.append(aEvent.getExceptionMessage());
//        }
//
//        if (aEvent.getStacktrace() != null) {
//            sb.append('\n');
//            sb.append(aEvent.getStacktrace());
//        }

        return sb.toString();
    }

}
