package com.acme.strilog.otlp.api.impl;


import com.acme.strilog.otlp.api.messages.OtlpLogPushRequest;
import com.acme.strilog.otlp.api.messages.OtlpLogPushResponse;
import com.acme.strilog.otlp.api.model.request.*;
import com.payneteasy.startup.parameters.AStartupParameter;
import com.payneteasy.startup.parameters.StartupParametersFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.acme.strilog.otlp.api.util.HttpHeadersParser.parseHeadersToArray;

public class OtlpRemoteClientImplTest {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static final Logger LOG = LoggerFactory.getLogger( OtlpRemoteClientImplTest.class );

    interface IConfig {
        @AStartupParameter(name = "OTLP_PUSH_URL", value = "")
        String otlpPushUrl();

        @AStartupParameter(name = "OTLP_PUTH_HEADERS", value = "", maskVariable = true)
        String otlpPushHeaders();
    }

    @Test
    public void test() {
        IConfig config = StartupParametersFactory.getStartupParameters(IConfig.class);
        if(config.otlpPushUrl() == null || config.otlpPushUrl().isEmpty()) {
            LOG.warn("OTLP_PUSH_URL is empty. Skipping test.");
            return;
        }

        OtlplLogRemoteClientImpl client = new OtlplLogRemoteClientImpl(
                config.otlpPushUrl()
                , parseHeadersToArray(config.otlpPushHeaders())
                , Duration.ofSeconds(10)
                , Duration.ofSeconds(10)
        );

        OtlpLogPushRequest request = OtlpLogPushRequest.builder()
                .resourceLogs(List.of(
                        OtlpResourceLog.builder()
                                .resource(
                                        OtlpResource.builder()
                                                  .attributes(
                                                          List.of(
                                                                  OtlpAttribute.builder()
                                                                          .key("service_name")
                                                                          .value(OtlpAttributeValue.builder()
                                                                                         .stringValue("example")
                                                                                         .build())
                                                                          .build()
                                                          )
                                                  )
                                                  .build()
                                )
                                .scopeLogs(List.of(
                                        OtlpScopeLog.builder()
                                                .logRecords(
                                                        List.of(
                                                                OtlpLogRecord.builder()
                                                                        .timeUnixNano(toNanoEpoch(System.currentTimeMillis()))
                                                                        .body(OtlpAttributeValue.builder()
                                                                                      .stringValue("Example message " + System.currentTimeMillis())
                                                                                      .build())
                                                                        .severityNumber(50)
                                                                        .severityText("INFO")
                                                                        .traceId(UUID.randomUUID().toString())
                                                                        .spanId(UUID.randomUUID().toString())
                                                                        .attributes(
                                                                                List.of(
                                                                                        OtlpAttribute.builder()
                                                                                                .key("order_id")
                                                                                                .value(
                                                                                                        OtlpAttributeValue.builder()
                                                                                                                .stringValue("1234")
                                                                                                                .build()
                                                                                                )
                                                                                                .build()
                                                                                        , OtlpAttribute.builder()
                                                                                                .key("trace_id")
                                                                                                .value(
                                                                                                        OtlpAttributeValue.builder()
                                                                                                                .stringValue(UUID.randomUUID().toString())
                                                                                                                .build()
                                                                                                )
                                                                                                .build()
                                                                                        , OtlpAttribute.builder()
                                                                                                .key("span_id")
                                                                                                .value(
                                                                                                        OtlpAttributeValue.builder()
                                                                                                                .stringValue(UUID.randomUUID().toString())
                                                                                                                .build()
                                                                                                )
                                                                                                .build()
                                                                                )
                                                                        )
                                                                        .build()
                                                        )
                                                )
                                                .build()
                                ))
                                .build()
                ))
                .build();

        OtlpLogPushResponse response = client.push(request);

        System.out.println("response = " + response);
    }

    private long toNanoEpoch(long epoch) {
        long nanos = System.nanoTime() % 1_000_000;
        return epoch * 1_000_000L + nanos ;
    }

}
