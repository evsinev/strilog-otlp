package com.acme.strilog.otlp.api.util;


import org.junit.Test;

import static com.acme.strilog.otlp.api.util.HttpHeadersParser.parseHeadersToArray;
import static org.junit.Assert.assertEquals;

public class HttpHeadersParserTest {

    @Test
    public void test() {
        {
            String[] headers = parseHeadersToArray("Authorization: Basic dGVsbG8");
            assertEquals(2, headers.length);
            assertEquals("Authorization", headers[0]);
            assertEquals("Basic dGVsbG8", headers[1]);
        }

        {
            String[] headers = parseHeadersToArray("Authorization: Basic dGVsbG8, Custom-Header: value1");
            assertEquals(4, headers.length);
            assertEquals("Authorization", headers[0]);
            assertEquals("Basic dGVsbG8", headers[1]);
            assertEquals("Custom-Header", headers[2]);
            assertEquals("value1"       , headers[3]);
        }

    }
}
