package com.acme.strilog.sender.config;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BasicAuths {

    public static String createBasicAuthValue(String aUsername, String aPassword) {
        String usernameAndPassword = aUsername + ":" + aPassword;
        String base64Encoded       = Base64.getEncoder().encodeToString(usernameAndPassword.getBytes(UTF_8));
        return "Basic " + base64Encoded;
    }
}
