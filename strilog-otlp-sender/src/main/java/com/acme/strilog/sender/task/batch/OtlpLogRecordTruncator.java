package com.acme.strilog.sender.task.batch;

import com.acme.strilog.otlp.api.model.request.OtlpAttribute;
import com.acme.strilog.otlp.api.model.request.OtlpAttributeValue;
import com.acme.strilog.otlp.api.model.request.OtlpLogRecord;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Cuts the body and every attribute value to one length, the longest that still fits. A huge
 * argument loses its tail while the short attributes (thread, class, trace_id) stay whole.
 */
public class OtlpLogRecordTruncator implements IBatchItemTruncator<OtlpLogRecord> {

    public static final String TRUNCATED_ATTRIBUTE = "strilog.truncated";

    private final IBatchItemSizeCalculator<OtlpLogRecord> sizeCalculator;

    public OtlpLogRecordTruncator(IBatchItemSizeCalculator<OtlpLogRecord> aSizeCalculator) {
        sizeCalculator = aSizeCalculator;
    }

    @Nullable
    @Override
    public OtlpLogRecord truncate(OtlpLogRecord aItem, long aMaxBytes) {
        if (sizeCalculator.sizeOfItem(aItem) <= aMaxBytes) {
            return aItem;
        }

        if (sizeCalculator.sizeOfItem(cut(aItem, 0)) > aMaxBytes) {
            return null;
        }

        // cut to the longest value the record only gains the marker, so that length does not fit
        int fits    = 0;
        int tooLong = longestValue(aItem);
        while (tooLong - fits > 1) {
            int length = (fits + tooLong) >>> 1;
            if (sizeCalculator.sizeOfItem(cut(aItem, length)) <= aMaxBytes) {
                fits = length;
            } else {
                tooLong = length;
            }
        }

        return cut(aItem, fits);
    }

    private static OtlpLogRecord cut(OtlpLogRecord aItem, int aLength) {
        List<OtlpAttribute> attributes = new ArrayList<>();
        if (aItem.getAttributes() != null) {
            for (OtlpAttribute attribute : aItem.getAttributes()) {
                attributes.add(OtlpAttribute.builder()
                        .key   ( attribute.getKey()                  )
                        .value ( cut(attribute.getValue(), aLength) )
                        .build());
            }
        }
        attributes.add(OtlpAttribute.ofString(TRUNCATED_ATTRIBUTE, "true"));

        return OtlpLogRecord.builder()
                .timeUnixNano   ( aItem.getTimeUnixNano()        )
                .severityNumber ( aItem.getSeverityNumber()      )
                .severityText   ( aItem.getSeverityText()        )
                .body           ( cut(aItem.getBody(), aLength)  )
                .attributes     ( attributes                     )
                .traceId        ( aItem.getTraceId()             )
                .spanId         ( aItem.getSpanId()              )
                .build();
    }

    private static OtlpAttributeValue cut(OtlpAttributeValue aValue, int aLength) {
        if (aValue == null || aValue.getStringValue() == null || aValue.getStringValue().length() <= aLength) {
            return aValue;
        }

        String text = aValue.getStringValue();
        int    end  = aLength;
        // do not leave half of a surrogate pair: it would go out as '?'
        if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return OtlpAttributeValue.ofString(text.substring(0, end));
    }

    private static int longestValue(OtlpLogRecord aItem) {
        int longest = lengthOf(aItem.getBody());
        if (aItem.getAttributes() != null) {
            for (OtlpAttribute attribute : aItem.getAttributes()) {
                longest = Math.max(longest, lengthOf(attribute.getValue()));
            }
        }
        return longest;
    }

    private static int lengthOf(OtlpAttributeValue aValue) {
        return aValue == null || aValue.getStringValue() == null ? 0 : aValue.getStringValue().length();
    }
}
