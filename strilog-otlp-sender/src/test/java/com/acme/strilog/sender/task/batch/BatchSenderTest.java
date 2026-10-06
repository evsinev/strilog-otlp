package com.acme.strilog.sender.task.batch;

import com.acme.strilog.otlp.api.impl.OtlplLogRemoteClientImpl;
import com.acme.strilog.otlp.api.model.request.OtlpLogRecord;
import com.acme.strilog.sender.task.batch.OtlpCollectorStub.Answer;
import com.acme.strilog.sender.task.converter.LineToSaveLogEventConverter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.acme.strilog.sender.task.batch.OtlpCollectorStub.otelcolWithLimit;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

public class BatchSenderTest {

    /** resourceLogs, resource and scopeLogs around the records of a request */
    private static final int ENVELOPE_BYTES = 512;

    @Test(timeout = 10_000)
    public void splitsBatchTheCollectorRefusesAsTooLarge() throws IOException {
        List<String> messages = messages("split", 20, 300);

        try (OtlpCollectorStub collector = new OtlpCollectorStub(otelcolWithLimit(4_000))) {
            send(batchSender(collector, 1_000_000), lines(messages));

            assertThat(collector.acceptedBodies()).containsExactlyElementsOf(messages);
            assertThat(collector.requestSizes()).anyMatch(size -> size > 4_000);
        }
    }

    @Test(timeout = 10_000)
    public void splitsBatchAProxyRefusesWith413() throws IOException {
        List<String> messages = messages("proxy", 20, 300);

        try (OtlpCollectorStub collector = new OtlpCollectorStub(request -> request.length > 4_000
                ? new Answer(413, "text/html", "<html><body>413 Request Entity Too Large</body></html>")
                : Answer.accepted())) {
            send(batchSender(collector, 1_000_000), lines(messages));

            assertThat(collector.acceptedBodies()).containsExactlyElementsOf(messages);
        }
    }

    @Test(timeout = 10_000)
    public void skipsRecordTheCollectorRefusesEvenAlone() throws IOException {
        List<String> before = messages("before", 5, 100);
        List<String> after  = messages("after", 5, 100);

        List<String> lines = new ArrayList<>(lines(before));
        lines.add(line("huge " + "y".repeat(20_000)));
        lines.addAll(lines(after));

        try (OtlpCollectorStub collector = new OtlpCollectorStub(otelcolWithLimit(4_000))) {
            send(batchSender(collector, 1_000_000), lines);

            List<String> expected = new ArrayList<>(before);
            expected.addAll(after);
            assertThat(collector.acceptedBodies()).containsExactlyElementsOf(expected);
        }
    }

    @Test(timeout = 10_000)
    public void truncatesRecordOverBatchLimit() throws IOException {
        int    maxBytes = 5_000;
        String argument = "z".repeat(50_000);

        try (OtlpCollectorStub collector = new OtlpCollectorStub(otelcolWithLimit(maxBytes + ENVELOPE_BYTES))) {
            send(batchSender(collector, maxBytes), List.of(
                      line("before")
                    , line("payload {}", List.of(argument), "java.lang.IllegalStateException: boom\n\tat " + "w".repeat(30_000), Map.of())
                    , line("after")
            ));

            assertThat(collector.acceptedBodies()).hasSize(3);
            assertThat(collector.acceptedBodies().get(0)).isEqualTo("before");
            assertThat(collector.acceptedBodies().get(2)).isEqualTo("after");

            String truncatedBody = collector.acceptedBodies().get(1);
            assertThat(truncatedBody).startsWith("payload zzz");
            assertThat("payload " + argument).startsWith(truncatedBody).isNotEqualTo(truncatedBody);
            assertThat(attribute(collector.acceptedRecords().get(1), OtlpLogRecordTruncator.TRUNCATED_ATTRIBUTE)).isEqualTo("true");
            assertThat(attribute(collector.acceptedRecords().get(0), OtlpLogRecordTruncator.TRUNCATED_ATTRIBUTE)).isNull();
        }
    }

    @Test(timeout = 10_000)
    public void requestBodyStaysWithinBatchLimit() throws IOException {
        int          maxBytes = 20_000;
        List<String> lines    = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            lines.add(line(
                      "order {} \"declined\" by Банк \\ {}"
                    , List.of(String.valueOf(i), "reason " + i)
                    , stacktrace(40)
                    , Map.of("order_id", "100" + i, "merchant", "Мерчант \"" + i + "\"", "endpoint", "1234", "session", "s-" + i)
            ));
        }

        try (OtlpCollectorStub collector = new OtlpCollectorStub(request -> Answer.accepted())) {
            send(batchSender(collector, maxBytes), lines);

            assertThat(collector.acceptedRecords()).hasSize(200);
            assertThat(collector.requestSizes().size()).isGreaterThan(1);
            assertThat(collector.requestSizes()).allMatch(size -> size <= maxBytes + ENVELOPE_BYTES);
        }
    }

    @Test(timeout = 10_000)
    public void dropsBatchTheCollectorRejects() throws IOException {
        try (OtlpCollectorStub collector = new OtlpCollectorStub(request ->
                new Answer(400, "application/json", "{\"code\":3,\"message\":\"ReadObject: invalid length for ID\"}"))) {
            send(batchSender(collector, 1_000_000), lines(messages("rejected", 3, 10)));

            assertThat(collector.requestSizes()).hasSize(1);
            assertThat(collector.acceptedRecords()).isEmpty();
        }
    }

    @Test(timeout = 10_000)
    public void retriesServerErrorWithGrowingSleep() throws IOException {
        AtomicInteger requests = new AtomicInteger();

        try (OtlpCollectorStub collector = new OtlpCollectorStub(request -> requests.incrementAndGet() <= 3
                ? new Answer(503, "text/plain", "Service Unavailable")
                : Answer.accepted())) {
            long started = System.nanoTime();
            send(batchSender(collector, 1_000_000, ofMillis(50), ofSeconds(1)), lines(messages("retried", 3, 10)));
            long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

            assertThat(collector.acceptedRecords()).hasSize(3);
            assertThat(collector.requestSizes()).hasSize(4);
            // 50 + 100 + 200, a fixed sleep would give 150
            assertThat(elapsedMillis).isGreaterThanOrEqualTo(350);
        }
    }

    @Test(timeout = 10_000)
    public void skipsLineThatCannotBeConverted() throws IOException {
        try (OtlpCollectorStub collector = new OtlpCollectorStub(request -> Answer.accepted())) {
            send(batchSender(collector, 1_000_000), List.of(
                      line("first")
                    , "{\"epoch\":1759740000000,\"level\":\"IN"
                    , ""
                    , line("second")
            ));

            assertThat(collector.acceptedBodies()).containsExactly("first", "second");
        }
    }

    /** The loop of FileSenderImpl without a file. */
    private static void send(BatchSender<?> aSender, List<String> aLines) {
        for (String line : aLines) {
            aSender.addLine(line);
            if (aSender.shouldSend()) {
                aSender.sendBatch();
            }
        }
        aSender.sendBatch();
    }

    private static BatchSender<OtlpLogRecord> batchSender(OtlpCollectorStub aCollector, int aMaxBytes) {
        return batchSender(aCollector, aMaxBytes, ofMillis(1), ofMillis(1));
    }

    private static BatchSender<OtlpLogRecord> batchSender(OtlpCollectorStub aCollector, int aMaxBytes, Duration aErrorSleep, Duration aMaxErrorSleep) {
        BatchItemSizeCalculatorImpl calculator = new BatchItemSizeCalculatorImpl();
        return new BatchSender<>(
                  new LineToSaveLogEventConverter()
                , new BatchSenderClientImpl(
                        new OtlplLogRemoteClientImpl(aCollector.url(), new String[] {"X-Test", "1"}, ofSeconds(5), ofSeconds(5))
                        , Map.of("service.name", "test")
                )
                , calculator
                , new OtlpLogRecordTruncator(calculator)
                , 1000
                , aMaxBytes
                , aErrorSleep
                , aMaxErrorSleep
        );
    }

    private static List<String> messages(String aPrefix, int aCount, int aPadding) {
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < aCount; i++) {
            messages.add(aPrefix + " " + i + " " + "x".repeat(aPadding));
        }
        return messages;
    }

    private static List<String> lines(List<String> aMessages) {
        return aMessages.stream().map(BatchSenderTest::line).toList();
    }

    private static String line(String aMessage) {
        return line(aMessage, List.of(), null, Map.of());
    }

    /** A line of strilog-encoder JsonEncoder. */
    private static String line(String aTemplate, List<String> aArgs, String aStacktrace, Map<String, String> aMdc) {
        JsonObject json = new JsonObject();
        json.addProperty("epoch"   , 1_759_740_000_000L);
        json.addProperty("level"   , "INFO");
        json.addProperty("thread"  , "qtp-1");
        json.addProperty("clazz"   , "com.example.Service");
        json.addProperty("template", aTemplate);

        if (!aArgs.isEmpty()) {
            JsonArray args = new JsonArray();
            aArgs.forEach(args::add);
            json.add("args", args);
        }

        if (aStacktrace != null) {
            json.addProperty("stacktrace", aStacktrace);
        }

        if (!aMdc.isEmpty()) {
            JsonObject mdc = new JsonObject();
            aMdc.forEach(mdc::addProperty);
            json.add("mdc", mdc);
        }

        return json.toString();
    }

    private static String stacktrace(int aFrames) {
        StringBuilder sb = new StringBuilder("java.lang.IllegalStateException: Cannot process \"order\"\n");
        for (int i = 0; i < aFrames; i++) {
            sb.append("\tat com.example.Service.method").append(i).append("(Service.java:").append(i).append(")\n");
        }
        return sb.toString();
    }

    private static String attribute(JsonObject aRecord, String aKey) {
        for (JsonElement attribute : aRecord.getAsJsonArray("attributes")) {
            if (aKey.equals(attribute.getAsJsonObject().get("key").getAsString())) {
                return attribute.getAsJsonObject().getAsJsonObject("value").get("stringValue").getAsString();
            }
        }
        return null;
    }
}
