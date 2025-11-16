package com.acme.strilog.sender.task.converter;

import com.acme.strilog.otlp.api.model.request.OtlpAttribute;
import com.acme.strilog.otlp.api.model.request.OtlpAttributeValue;
import com.acme.strilog.otlp.api.model.request.OtlpLogRecord;
import com.google.gson.Gson;
import com.acme.strilog.sender.event.LogEvent;

import java.util.List;
import java.util.Map;

import static org.slf4j.helpers.MessageFormatter.arrayFormat;

public class LineToSaveLogEventConverter implements ILineToItemConverter<OtlpLogRecord> {

    private static final int INFO_LEVEL = 6;

    private final Gson gson = new Gson();

    @Override
    public OtlpLogRecord convertToItem(String aLine) {
        LogEvent event = gson.fromJson(aLine, LogEvent.class);
        return mapEvent(event);
    }

    private OtlpLogRecord mapEvent(LogEvent aEvent) {
        return OtlpLogRecord.builder()
                .body         ( OtlpAttributeValue.ofString(toMessage(aEvent)) )
                .severityText ( aEvent.getLevel()                              )
                .attributes   ( toAttributes(aEvent)                           )
                .timeUnixNano ( toNanoEpoch(aEvent.getEpoch())                 )
                .build();
    }

    private List<OtlpAttribute> toAttributes(LogEvent aEvent) {
        OtlpAttributesBuilder builder = new OtlpAttributesBuilder();

        builder.add("log.body.template"  , aEvent.getTemplate());
        builder.add("code.namespace"     , aEvent.getClazz());
        builder.add("thread.name"        , aEvent.getThread());

        builder.add("exception.message"   , aEvent.getExceptionMessage());
        builder.add("exception.stacktrace", aEvent.getStacktrace());
        builder.add("exception.type"      , aEvent.getExceptionLine());

        if (aEvent.getMdc() != null) {
            for (Map.Entry<String, String> entry : aEvent.getMdc().entrySet()) {
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
