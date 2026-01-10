package com.deskit.deskit.common.util.verification.solapi;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class SolapiClient {

    public static String sendMessage(String apiKey, String apiSecret, String messageJson)
            throws Exception {
        String authHeader = SolapiAuth.createAuthHeader(apiKey, apiSecret);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.solapi.com/messages/v4/send-many/detail"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(messageJson))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
}