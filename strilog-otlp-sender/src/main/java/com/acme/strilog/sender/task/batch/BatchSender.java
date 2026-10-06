package com.acme.strilog.sender.task.batch;

import com.acme.strilog.sender.task.converter.ILineToItemConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BatchSender<I> {

    private static final Logger LOG = LoggerFactory.getLogger(BatchSender.class);

    private static final int MAX_LOGGED_LINE_CHARS = 200;

    private final ILineToItemConverter<I>     converter;
    private final int                         maxItemsCount;
    private final int                         maxBytes;
    private final List<SizedItem<I>>          list;
    private final IBatchSenderClient<I>       batchSenderClient;
    private final IBatchItemSizeCalculator<I> batchItemSizeCalculator;
    private final IBatchItemTruncator<I>      batchItemTruncator;
    private final Duration                    errorSleepDuration;
    private final Duration                    maxErrorSleepDuration;

    private long currentBytes;

    public BatchSender(
              ILineToItemConverter<I>       aConverter
            , IBatchSenderClient<I>         aClient
            , IBatchItemSizeCalculator<I>   aCalculator
            , IBatchItemTruncator<I>        aTruncator
            , int                           aMaxItemsCount
            , int                           aMaxBytes
            , Duration                      aErrorSleepDuration
            , Duration                      aMaxErrorSleepDuration
    ) {
        converter               = aConverter;
        maxItemsCount           = aMaxItemsCount;
        list                    = new ArrayList<>(aMaxItemsCount);
        batchSenderClient       = aClient;
        batchItemSizeCalculator = aCalculator;
        batchItemTruncator      = aTruncator;
        maxBytes                = aMaxBytes;
        errorSleepDuration      = aErrorSleepDuration;
        maxErrorSleepDuration   = aMaxErrorSleepDuration;
        currentBytes            = 0;
    }

    public void addLine(String aLine) {
        I item;
        try {
            item = converter.convertToItem(aLine);
        } catch (RuntimeException e) {
            // a line cut by a crash of the app would stop the whole dir on every retry
            LOG.warn("Skipping a line that cannot be converted: {}", abbreviate(aLine), e);
            return;
        }

        long size = batchItemSizeCalculator.sizeOfItem(item);
        if (size > maxBytes) {
            I truncated = batchItemTruncator.truncate(item, maxBytes);
            if (truncated == null) {
                LOG.warn("Skipping an item of ~{} bytes, it does not fit {} bytes even truncated", size, maxBytes);
                return;
            }

            long truncatedSize = batchItemSizeCalculator.sizeOfItem(truncated);
            LOG.warn("Truncated an item of ~{} bytes to ~{} bytes to fit {} bytes", size, truncatedSize, maxBytes);
            item = truncated;
            size = truncatedSize;
        }

        list.add(new SizedItem<>(item, size));
        currentBytes += size;
    }

    public boolean shouldSend() {
        return currentBytes >= maxBytes || list.size() >= maxItemsCount;
    }

    public void sendBatch() {
        if (list.isEmpty()) {
            return;
        }

        LOG.debug("Sending batch of {} items with ~{} bytes ...", list.size(), currentBytes);

        for (List<I> chunk : splitByMaxBytes()) {
            sendChunk(chunk);
        }

        clearItems();
    }

    /**
     * Drops the items left by an attempt that failed halfway: the file sender reads them again from
     * the saved offset.
     */
    public void clearItems() {
        list.clear();
        currentBytes = 0;
    }

    /**
     * {@link #shouldSend()} is checked after an item is added, so the last item may cross the limit:
     * then it goes in a request of its own.
     */
    private List<List<I>> splitByMaxBytes() {
        List<List<I>> chunks     = new ArrayList<>();
        List<I>       chunk      = new ArrayList<>();
        long          chunkBytes = 0;

        for (SizedItem<I> sizedItem : list) {
            if (!chunk.isEmpty() && chunkBytes + sizedItem.size() > maxBytes) {
                chunks.add(chunk);
                chunk      = new ArrayList<>();
                chunkBytes = 0;
            }
            chunk.add(sizedItem.item());
            chunkBytes += sizedItem.size();
        }
        chunks.add(chunk);

        return chunks;
    }

    private void sendChunk(List<I> aChunk) {
        Duration sleep = errorSleepDuration;

        while (true) {
            BatchSendResult result = batchSenderClient.sendItems(aChunk);

            switch (result.status()) {
                case DELIVERED -> {
                    return;
                }
                case TOO_LARGE -> {
                    splitAndSend(aChunk, result);
                    return;
                }
                case REJECTED -> {
                    LOG.error("Dropping batch of {} items, the collector rejected it: {}", aChunk.size(), result.description());
                    return;
                }
                case RETRY -> {
                    LOG.error("Cannot send batch of {} items. Sleeping {}: {}", aChunk.size(), sleep, result.description());
                    sleepBeforeRetry(sleep);
                    sleep = nextSleep(sleep);
                }
            }
        }
    }

    private void splitAndSend(List<I> aChunk, BatchSendResult aResult) {
        if (aChunk.size() == 1) {
            LOG.warn("Skipping an item, the collector refuses it as too large even alone: {}", aResult.description());
            return;
        }

        LOG.warn("Splitting batch of {} items, the collector refused it as too large: {}", aChunk.size(), aResult.description());

        int half = aChunk.size() / 2;
        sendChunk(aChunk.subList(0, half));
        sendChunk(aChunk.subList(half, aChunk.size()));
    }

    private Duration nextSleep(Duration aSleep) {
        Duration doubled = aSleep.multipliedBy(2);
        return doubled.compareTo(maxErrorSleepDuration) > 0 ? maxErrorSleepDuration : doubled;
    }

    /**
     * Throws on interruption: returning as if the batch went out would let the file sender save
     * the offset and delete the file with the batch unsent.
     */
    private static void sleepBeforeRetry(Duration aSleep) {
        try {
            //noinspection BusyWait
            Thread.sleep(aSleep.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to resend a batch", e);
        }
    }

    private static String abbreviate(String aLine) {
        if (aLine.length() <= MAX_LOGGED_LINE_CHARS) {
            return aLine;
        }
        return aLine.substring(0, MAX_LOGGED_LINE_CHARS) + "... (" + aLine.length() + " chars)";
    }

    private record SizedItem<I>(I item, long size) {
    }
}
