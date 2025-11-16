package com.acme.strilog.sender;

import com.payneteasy.startup.parameters.AStartupParameter;

import java.io.File;
import java.time.Duration;

public interface IStartupConfig {

    @AStartupParameter(name = "OTLP_PUSH_URL", value = "http://localhost:4318/v1/logs")
    String otlpPushUrl();

    @AStartupParameter(name = "OTLP_PUSH_HEADERS", value = "", maskVariable = true)
    String otlpPushHeaders();

    @AStartupParameter(name = "OTLP_PUSH_CONNECT_TIMEOUT", value = "PT10S")
    Duration otlpConnectTimeout();

    @AStartupParameter(name = "OTLP_PUSH_READ_TIMEOUT", value = "PT20S")
    Duration otlpReadTimeout();

    @AStartupParameter(name = "CONFIG_FILE", value = "./sender-config.yaml")
    File senderConfigFile();

    @AStartupParameter(name = "DIR_SLEEP_BETWEEN_LIST_FILES", value = "PT1S")
    Duration dirSleep();

    @AStartupParameter(name = "DIR_DETECT_OLD_FILES", value = "PT3M")
    Duration dirDetectOldFiles();

    @AStartupParameter(name = "BATCH_MAX_BYTES", value = "1000000")
    int maxBatchSize();

    @AStartupParameter(name = "BATCH_MAX_ITEMS", value = "1000")
    int maxBatchItems();

    @AStartupParameter(name = "BATCH_ERROR_SLEEP", value = "PT1S")
    Duration batchErrorSleep();

}
