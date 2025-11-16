package com.acme.strilog.otlp.api.impl;

import com.acme.strilog.otlp.api.IOtlplLogRemoteService;
import com.acme.strilog.otlp.api.messages.OtlpLogPushRequest;
import com.acme.strilog.otlp.api.messages.OtlpLogPushResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.net.http.HttpClient.Redirect.NEVER;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;

public class OtlplLogRemoteClientImpl implements IOtlplLogRemoteService {

    private static final Logger LOG = LoggerFactory.getLogger(OtlplLogRemoteClientImpl.class);

    private final Gson gson = LOG.isTraceEnabled()
            ? new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
            : new GsonBuilder().disableHtmlEscaping().create();

    private final HttpClient httpClient;
    private final URI        uri;
    private final String[]   headers;
    private final Duration   readTimeout;

    public OtlplLogRemoteClientImpl(String aUrl, String[] aHeaders, Duration aConnectTimeout, Duration aReadTimeout) {
        uri         = URI.create(aUrl);
        headers     = aHeaders;
        readTimeout = aReadTimeout;

        httpClient = HttpClient.newBuilder()
                .version         ( HTTP_1_1        )
                .connectTimeout  ( aConnectTimeout )
                .followRedirects ( NEVER           )
                .build();
    }

    @Override
    public OtlpLogPushResponse push(OtlpLogPushRequest aLogs) {
        String json  = gson.toJson(aLogs);
        byte[] bytes = json.getBytes(UTF_8);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending json {} to {}", json, uri);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Sending {} bytes to {} ...", bytes.length, uri);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri     ( uri           )
                .timeout ( ofSeconds(30) )
                .header  ( "Content-Type", "application/json")
                .headers ( headers )
                .POST    ( ofByteArray(bytes) )
                .timeout ( readTimeout        )
                .build();

        HttpResponse<String> response     = sendHttpRequest(request);
        String               responseBody = response.body();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Received response {}", responseBody);
        }

        if (response.statusCode() == 200) {
            LOG.debug("Success response");
            return OtlpLogPushResponse.builder()
                    .statusCode(response.statusCode())
                    .build();
        }

        OtlpLogPushResponse otlpLogPushResponse = gson.fromJson(responseBody, OtlpLogPushResponse.class);

        LOG.warn("Received unexpected response {}", otlpLogPushResponse);

        return otlpLogPushResponse;
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request) {
        try {
            return httpClient.send(request, ofString());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot send request to " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending to " + uri, e);
        }
    }
}
