package com.acme.strilog.sender.task.dir;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.sort;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.List.of;

public class DirList {

    private static final Logger LOG = LoggerFactory.getLogger( DirList.class );

    @Nonnull
    public static List<File> getFilesToSend(File dir, Duration aOldFileDetector) {
        File[] files = dir.listFiles(it -> it.isFile() && it.getName().endsWith(".json"));

        if(files == null || files.length == 0) {
            return emptyList();
        }

        if(files.length == 1) {
            return checkFileTime(files[0], aOldFileDetector);
        }

        sort(files, comparing(File::getName));

        return Arrays.asList(files).subList(0, files.length - 1);
    }

    private static List<File> checkFileTime(File aFile, Duration aOldFileDetector) {
        if(currentTimeMillis() - aFile.lastModified() > aOldFileDetector.toMillis()) {
            LOG.debug("File is old {}. Sending it.", aFile.getAbsolutePath());
            return of(aFile);
        }

        return emptyList();
    }

}
