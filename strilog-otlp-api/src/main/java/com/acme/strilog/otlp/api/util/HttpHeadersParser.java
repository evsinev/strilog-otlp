package com.acme.strilog.otlp.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class HttpHeadersParser {

    public static String[] parseHeadersToArray(String aHeaders) {
        List<String>    headers = new ArrayList<>();
        StringTokenizer st      = new StringTokenizer(aHeaders, "=:;,");
        while (st.hasMoreTokens()) {
            headers.add(st.nextToken().trim());
        }

        if (headers.isEmpty() || headers.size() % 2 != 0) {
            throw new IllegalStateException("Wrong number, " + headers.size() + " , of parameters for OTLP_PUSH_HEADERS. Example: Authorization: Basic token1, Custom-Header: Hello");
        }

        return headers.toArray(new String[0]);
    }

}
