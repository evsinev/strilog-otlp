package com.acme.strilog.sender.task.batch;

import com.acme.strilog.sender.task.converter.ILineToItemConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BatchSender<I> {

    private static final Logger LOG = LoggerFactory.getLogger(BatchSender.class);

    private final ILineToItemConverter<I>     converter;
    private final int                         maxItemsCount;
    private final int                         maxBytes;
    private final List<I>                     list;
    private final IBatchSenderClient<I>       batchSenderClient;
    private final IBatchItemSizeCalculator<I> batchItemSizeCalculator;
    private final Duration                    errorSleepDuration;

    private long currentBytes;

    public BatchSender(
              ILineToItemConverter<I>       aConverter
            , IBatchSenderClient<I>         aClient
            , IBatchItemSizeCalculator<I>   aCalculator
            , int                           aMaxItemsCount
            , int                           aMaxBytes
            , Duration                      aErrorSleepDuration
    ) {
        converter               = aConverter;
        maxItemsCount           = aMaxItemsCount;
        list                    = new ArrayList<>(aMaxItemsCount);
        batchSenderClient       = aClient;
        batchItemSizeCalculator = aCalculator;
        maxBytes                = aMaxBytes;
        errorSleepDuration      = aErrorSleepDuration;
        currentBytes            = 0;
    }

    public void addLine(String aLine) {
        I item = converter.convertToItem(aLine);
        currentBytes += batchItemSizeCalculator.sizeOfItem(item);
        list.add(item);
    }

    public boolean shouldSend() {
        return currentBytes >= maxBytes || list.size() >= maxItemsCount;
    }

    public void sendBatch() {
        LOG.debug("Sending batch of {} items with ~{} bytes ...", list.size(), currentBytes);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                batchSenderClient.sendItems(list);
                clearItems();
                return;
            } catch (Exception e) {
                LOG.error("Cannot send batch of {} items with ~{} bytes. Sleeping {}", list.size(), currentBytes, errorSleepDuration, e);
                try {
                    //noinspection BusyWait
                    Thread.sleep(errorSleepDuration.toMillis());
                } catch (InterruptedException ex) {
                    LOG.warn("Batch sleep interrupted");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void clearItems() {
        list.clear();
        currentBytes = 0;
    }
}
