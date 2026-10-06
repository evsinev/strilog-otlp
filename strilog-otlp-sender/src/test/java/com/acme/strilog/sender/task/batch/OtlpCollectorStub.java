package com.acme.strilog.sender.task.batch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * OTLP/HTTP logs endpoint that answers with a given function and keeps the records it accepted.
 */
class OtlpCollectorStub implements AutoCloseable {

    record Answer(int statusCode, String contentType, String body) {

        static Answer accepted() {
            return new Answer(200, "application/json", "{}");
        }
    }

    private final HttpServer       server;
    private final List<Integer>    requestSizes     = new CopyOnWriteArrayList<>();
    private final List<JsonObject> acceptedRecords  = new CopyOnWriteArrayList<>();

    OtlpCollectorStub(Function<byte[], Answer> aAnswers) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1/logs", exchange -> {
            byte[] request = exchange.getRequestBody().readAllBytes();
            requestSizes.add(request.length);

            Answer answer = aAnswers.apply(request);
            if (answer.statusCode() == 200) {
                acceptedRecords.addAll(logRecords(request));
            }

            byte[] body = answer.body().getBytes(UTF_8);
            exchange.getResponseHeaders().set("Content-Type", answer.contentType());
            exchange.sendResponseHeaders(answer.statusCode(), body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    /**
     * What otelcol answers on a body over max_request_body_size: 400 with google.rpc.Status, not 413.
     */
    static Function<byte[], Answer> otelcolWithLimit(int aMaxBodyBytes) {
        return request -> request.length > aMaxBodyBytes
                ? new Answer(400, "application/json", "{\"code\":3,\"message\":\"http: request body too large\"}")
                : Answer.accepted();
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/logs";
    }

    List<Integer> requestSizes() {
        return requestSizes;
    }

    List<JsonObject> acceptedRecords() {
        return acceptedRecords;
    }

    List<String> acceptedBodies() {
        return acceptedRecords.stream()
                .map(it -> it.getAsJsonObject("body").get("stringValue").getAsString())
                .toList();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static List<JsonObject> logRecords(byte[] aRequest) {
        List<JsonObject> records = new ArrayList<>();
        JsonObject       request = JsonParser.parseString(new String(aRequest, UTF_8)).getAsJsonObject();
        for (JsonElement resourceLog : request.getAsJsonArray("resourceLogs")) {
            for (JsonElement scopeLog : resourceLog.getAsJsonObject().getAsJsonArray("scopeLogs")) {
                for (JsonElement logRecord : scopeLog.getAsJsonObject().getAsJsonArray("logRecords")) {
                    records.add(logRecord.getAsJsonObject());
                }
            }
        }
        return records;
    }
}
