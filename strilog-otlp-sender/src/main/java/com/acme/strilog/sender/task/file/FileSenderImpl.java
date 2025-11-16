package com.acme.strilog.sender.task.file;

import com.acme.strilog.sender.offset.IOffsetStore;
import com.acme.strilog.sender.task.batch.BatchSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FileSenderImpl implements IFileSender{

    private static final Logger LOG = LoggerFactory.getLogger(FileSenderImpl.class);

    private final IOffsetStore   offsetStore;
    private final BatchSender<?> batchSender;

    public FileSenderImpl(IOffsetStore offsetStore, BatchSender<?> batchSender) {
        this.offsetStore = offsetStore;
        this.batchSender = batchSender;
    }

    public void sendFile(File file) {
        LOG.info("Sending file {}...", file.getAbsolutePath());

        try (LineNumberReader in = new LineNumberReader(new InputStreamReader(new FileInputStream(file), UTF_8))) {
            for (String line; (line = in.readLine()) != null; ) {
                batchSender.addLine(line);
                
                if(batchSender.shouldSend()) {
                    batchSender.sendBatch();
                    offsetStore.saveOffset(file.getName(), in.getLineNumber());
                }
            }

            // send the rest of the items
            batchSender.sendBatch();
            offsetStore.saveOffset(file.getName(), in.getLineNumber());

        } catch (Exception e) {
            throw new IllegalStateException("Cannot process file " + file.getAbsolutePath(), e);
        }

        try {
            LOG.info("Deleting file {} ...", file.getAbsolutePath());
            Files.delete(file.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete file " + file.getAbsolutePath(), e);
        }
    }
}
