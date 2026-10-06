package com.acme.strilog.otlp.api.impl;

import com.acme.strilog.otlp.api.messages.OtlpLogPushRequest;
import com.acme.strilog.otlp.api.messages.OtlpLogPushResponse;
import com.acme.strilog.otlp.api.model.request.OtlpAttributeValue;
import com.acme.strilog.otlp.api.model.request.OtlpLogRecord;
import com.acme.strilog.otlp.api.model.request.OtlpResourceLog;
import com.acme.strilog.otlp.api.model.request.OtlpScopeLog;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OtlplLogRemoteClientImplErrorResponseTest {

    private HttpServer server;

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void keepsStatusCodeAndPlainTextBody() throws IOException {
        OtlpLogPushResponse response = push(400, "text/plain", "bad request from the proxy");

        assertEquals(400, response.getStatusCode());
        assertEquals("bad request from the proxy", response.getBody());
        assertNull(response.getError());
    }

    @Test
    public void parsesOtlpStatusOfJsonBody() throws IOException {
        OtlpLogPushResponse response = push(400, "application/json", "{\"code\":3,\"message\":\"http: request body too large\"}");

        assertEquals(400, response.getStatusCode());
        assertEquals("http: request body too large", response.getError().getMessage());
        assertEquals("3", response.getError().getCode());
        assertNull(response.getBody());
    }

    @Test
    public void cutsLongBody() throws IOException {
        String page = "<html>" + "x".repeat(5000) + "</html>";

        OtlpLogPushResponse response = push(502, "text/html", page);

        assertEquals(502, response.getStatusCode());
        assertTrue(response.getBody(), response.getBody().startsWith("<html>xxx"));
        assertTrue(response.getBody(), response.getBody().length() < 1100);
    }

    private OtlpLogPushResponse push(int aStatusCode, String aContentType, String aBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1/logs", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = aBody.getBytes(UTF_8);
            exchange.getResponseHeaders().set("Content-Type", aContentType);
            exchange.sendResponseHeaders(aStatusCode, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        OtlplLogRemoteClientImpl client = new OtlplLogRemoteClientImpl(
                  "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/logs"
                , new String[] {"X-Test", "1"}
                , ofSeconds(5)
                , ofSeconds(5)
        );

        return client.push(OtlpLogPushRequest.builder()
                .resourceLogs(List.of(OtlpResourceLog.builder()
                        .scopeLogs(List.of(OtlpScopeLog.builder()
                                .logRecords(List.of(OtlpLogRecord.builder()
                                        .body(OtlpAttributeValue.ofString("hello"))
                                        .build()))
                                .build()))
                        .build()))
                .build());
    }
}
