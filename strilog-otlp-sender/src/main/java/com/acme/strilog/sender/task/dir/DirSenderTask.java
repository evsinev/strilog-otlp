package com.acme.strilog.sender.task.dir;

import com.acme.strilog.sender.config.TSenderDir;
import com.acme.strilog.sender.task.file.IFileSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.List;

import static com.acme.strilog.sender.task.dir.DirList.getFilesToSend;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

public class DirSenderTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DirSenderTask.class);

    private final TSenderDir  senderDir;
    private final File        dir;
    private final IFileSender fileSender;
    private final Duration    sleepDuration;
    private final Duration    oldFileDetector;

    public DirSenderTask(TSenderDir senderDir, IFileSender aFileSender, Duration aSleep, Duration aOldFileDetector) {
        this.senderDir = senderDir;
        dir            = new File(senderDir.getPath());
        fileSender     = aFileSender;
        sleepDuration   = aSleep;
        oldFileDetector = aOldFileDetector;
    }

    @Override
    public void run() {
        currentThread().setName(senderDir.getPath());

        while (!currentThread().isInterrupted()) {

            try {
                List<File> files = getFilesToSend(dir, oldFileDetector);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Files to send {}", files);
                }

                for (File file : files) {
                    fileSender.sendFile(file);
                }

                //noinspection BusyWait
                sleep(sleepDuration.toMillis());
            } catch (InterruptedException e) {
                LOG.warn("Exiting ...");
                break;
            } catch (Exception e) {
                LOG.error("Cannot process dir " + dir.getAbsolutePath(), e);
            }

        }

        LOG.info("Exited");
    }

}
