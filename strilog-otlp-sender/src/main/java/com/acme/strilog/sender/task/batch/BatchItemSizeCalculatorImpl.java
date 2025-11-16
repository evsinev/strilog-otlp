package com.acme.strilog.sender.task.batch;

import com.acme.strilog.otlp.api.model.request.OtlpLogRecord;

public class BatchItemSizeCalculatorImpl implements IBatchItemSizeCalculator<OtlpLogRecord> {

    @Override
    public long sizeOfItem(OtlpLogRecord aItem) {
        return aItem.getBody().getStringValue().length() * 2L;
    }
}
