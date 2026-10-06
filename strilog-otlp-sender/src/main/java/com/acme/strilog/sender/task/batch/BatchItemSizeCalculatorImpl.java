package com.acme.strilog.sender.task.batch;

import com.acme.strilog.otlp.api.model.request.OtlpLogRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Measures a record the way it goes into the request body: serialized with the settings of
 * OtlplLogRemoteClientImpl and counted in UTF-8 bytes. A guess from the body length misses the
 * attributes (a stacktrace, MDC, the arguments repeating the body) and the JSON escaping.
 */
public class BatchItemSizeCalculatorImpl implements IBatchItemSizeCalculator<OtlpLogRecord> {

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    public long sizeOfItem(OtlpLogRecord aItem) {
        // plus the comma between records in the array
        return utf8Length(gson.toJson(aItem)) + 1;
    }

    private static long utf8Length(String aText) {
        long length = 0;
        for (int i = 0; i < aText.length(); i++) {
            char c = aText.charAt(i);
            if (c < 0x80) {
                length += 1;
            } else if (c < 0x800) {
                length += 2;
            } else if (Character.isHighSurrogate(c) && i + 1 < aText.length() && Character.isLowSurrogate(aText.charAt(i + 1))) {
                length += 4;
                i++;
            } else {
                length += 3;
            }
        }
        return length;
    }
}
