package com.acme.strilog.sender.offset.impl;

import com.acme.strilog.sender.offset.IOffsetStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Reader;
import java.io.Writer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.newBufferedWriter;

public class OffsetStoreImpl implements IOffsetStore {

    private static final Logger LOG = LoggerFactory.getLogger(OffsetStoreImpl.class);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final File configFile;

    public OffsetStoreImpl(File dir) {
        configFile = new File(dir, "_offset.config");
    }

    @Override
    public void saveOffset(String aFilename, long aOffset) {
        LOG.debug("Saving offset {} / {}", aFilename, aOffset);

        TOffsetConfig offsetConfig = TOffsetConfig.builder()
                .filename(aFilename)
                .offset(aOffset)
                .build();

        try (Writer out = newBufferedWriter(configFile.toPath(), UTF_8)) {
            gson.toJson(offsetConfig, out);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot write config to file " + configFile.getAbsolutePath(), e);
        }

    }

    @Override
    public long getOffset(String aFilename) {
        if (!configFile.exists()) {
            return 0;
        }

        try (Reader in = newBufferedReader(configFile.toPath(), UTF_8)) {
            TOffsetConfig offsetConfig = gson.fromJson(in, TOffsetConfig.class);
            return offsetConfig != null && aFilename.equals(offsetConfig.getFilename()) ? offsetConfig.getOffset() : 0;
        } catch (Exception e) {
            LOG.warn("Cannot read offset from {}, sending {} from the start", configFile.getAbsolutePath(), aFilename, e);
            return 0;
        }
    }
}
