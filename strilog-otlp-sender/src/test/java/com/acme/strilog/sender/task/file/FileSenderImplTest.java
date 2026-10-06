package com.acme.strilog.sender.task.file;

import com.acme.strilog.sender.offset.impl.OffsetStoreImpl;
import com.acme.strilog.sender.task.batch.BatchSendResult;
import com.acme.strilog.sender.task.batch.BatchSender;
import com.acme.strilog.sender.task.batch.IBatchSenderClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class FileSenderImplTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test(timeout = 10_000)
    public void resumesFromSavedOffsetAfterCrash() throws IOException {
        File dir  = folder.getRoot();
        File file = writeLines(new File(dir, "json-2026-10-06-12-00-000.json"), 10);

        List<String> firstRun = new ArrayList<>();
        IBatchSenderClient<String> dyingAfterFirstBatch = items -> {
            if (!firstRun.isEmpty()) {
                throw new IllegalStateException("The sender process dies");
            }
            firstRun.addAll(items);
            return BatchSendResult.delivered();
        };

        try {
            new FileSenderImpl(new OffsetStoreImpl(dir), batchSender(dyingAfterFirstBatch, ofMillis(1))).sendFile(file);
            fail("The second batch must fail");
        } catch (IllegalStateException expected) {
            // the process is restarted
        }

        List<String> secondRun = new ArrayList<>();
        IBatchSenderClient<String> collector = items -> {
            secondRun.addAll(items);
            return BatchSendResult.delivered();
        };
        new FileSenderImpl(new OffsetStoreImpl(dir), batchSender(collector, ofMillis(1))).sendFile(file);

        assertThat(firstRun).containsExactly("line-1", "line-2", "line-3", "line-4", "line-5");
        assertThat(secondRun).containsExactly("line-6", "line-7", "line-8", "line-9", "line-10");
        assertThat(file).doesNotExist();
    }

    @Test(timeout = 10_000)
    public void retryOfSameFileDoesNotResendPendingItems() throws IOException {
        File dir  = folder.getRoot();
        File file = writeLines(new File(dir, "json-2026-10-06-12-00-000.json"), 10);

        AtomicInteger calls = new AtomicInteger();
        List<String>  sent  = new ArrayList<>();
        IBatchSenderClient<String> failingOnce = items -> {
            if (calls.incrementAndGet() == 2) {
                throw new IllegalStateException("Connection reset");
            }
            sent.addAll(items);
            return BatchSendResult.delivered();
        };
        FileSenderImpl fileSender = new FileSenderImpl(new OffsetStoreImpl(dir), batchSender(failingOnce, ofMillis(1)));

        try {
            fileSender.sendFile(file);
            fail("The second batch must fail");
        } catch (IllegalStateException expected) {
            // DirSenderTask takes the same file again
        }
        fileSender.sendFile(file);

        assertThat(sent).containsExactly("line-1", "line-2", "line-3", "line-4", "line-5", "line-6", "line-7", "line-8", "line-9", "line-10");
    }

    @Test(timeout = 10_000)
    public void sendsWholeFileWhenOffsetIsOfAnotherFile() throws IOException {
        File dir = folder.getRoot();
        new OffsetStoreImpl(dir).saveOffset("json-2026-10-06-11-59-000.json", 3);
        File file = writeLines(new File(dir, "json-2026-10-06-12-00-000.json"), 4);

        List<String> sent = new ArrayList<>();
        IBatchSenderClient<String> collector = items -> {
            sent.addAll(items);
            return BatchSendResult.delivered();
        };
        new FileSenderImpl(new OffsetStoreImpl(dir), batchSender(collector, ofMillis(1))).sendFile(file);

        assertThat(sent).containsExactly("line-1", "line-2", "line-3", "line-4");
    }

    @Test(timeout = 10_000)
    public void keepsFileWhenInterruptedWhileRetrying() throws Exception {
        File dir  = folder.getRoot();
        File file = writeLines(new File(dir, "json-2026-10-06-12-01-000.json"), 3);

        CountDownLatch attempted = new CountDownLatch(1);
        IBatchSenderClient<String> unavailable = items -> {
            attempted.countDown();
            return BatchSendResult.retry("503 Service Unavailable");
        };
        FileSenderImpl fileSender = new FileSenderImpl(new OffsetStoreImpl(dir), batchSender(unavailable, ofMillis(50)));

        Thread thread = new Thread(() -> {
            try {
                fileSender.sendFile(file);
            } catch (IllegalStateException expected) {
                // DirSenderTask logs it and exits
            }
        });
        thread.start();
        attempted.await();
        thread.interrupt();
        thread.join(5_000);

        assertThat(thread.isAlive()).isFalse();
        assertThat(file).exists();
        assertThat(new OffsetStoreImpl(dir).getOffset(file.getName())).isZero();
    }

    private static BatchSender<String> batchSender(IBatchSenderClient<String> aClient, Duration aErrorSleep) {
        return new BatchSender<>(
                  line -> line
                , aClient
                , String::length
                , (item, maxBytes) -> item
                , 5
                , 1_000_000
                , aErrorSleep
                , aErrorSleep
        );
    }

    private static File writeLines(File aFile, int aCount) throws IOException {
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= aCount; i++) {
            lines.add("line-" + i);
        }
        Files.write(aFile.toPath(), lines, UTF_8);
        return aFile;
    }
}
