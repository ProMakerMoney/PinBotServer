package com.zmn.pinbotserver.bybit;


import org.apache.catalina.connector.Request;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;


public class BybitApi {
    private final String BASE_URL;
    private final String API_KEY;
    private final String API_SECRET;

    public BybitApi(String apiKey, String apiSecret, boolean isTestnet) {
        this.BASE_URL = isTestnet ? "https://api-testnet.bybit.com" : "https://api.bybit.com";
        this.API_KEY = apiKey;
        this.API_SECRET = apiSecret;
    }

    private String getTimestamp() {
        return Long.toString(ZonedDateTime.now().toInstant().toEpochMilli());
    }

    private String generateSignature(String timestamp, String queryString, String requestBody) throws NoSuchAlgorithmException, InvalidKeyException {
        String recvWindow = "5000";
        String payload = timestamp + API_KEY + recvWindow + requestBody;
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(API_SECRET.getBytes(), "HmacSHA256");
        sha256Hmac.init(secretKeySpec);
        return bytesToHex(sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String toJson(Map<String, Object> params) {
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":\"");
            json.append(entry.getValue()).append("\",");
        }
        json.deleteCharAt(json.length() - 1).append("}");
        return json.toString();
    }

    public void placeOrder(String symbol, String side, String orderType, String qty, String price) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        String timestamp = getTimestamp();
        String endpoint = "/v5/order/create";
        String url = BASE_URL + endpoint;

        Map<String, Object> params = new HashMap<>();
        params.put("category", "linear");
        params.put("symbol", symbol);
        params.put("side", side);
        params.put("orderType", orderType);
        params.put("qty", qty);
        params.put("price", price);
        params.put("timeInForce", "GTC");

        String jsonParams = toJson(params);
        String signature = generateSignature(timestamp, "", jsonParams);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-BAPI-API-KEY", API_KEY);
        connection.setRequestProperty("X-BAPI-SIGN", signature);
        connection.setRequestProperty("X-BAPI-SIGN-TYPE", "2");
        connection.setRequestProperty("X-BAPI-TIMESTAMP", timestamp);
        connection.setRequestProperty("X-BAPI-RECV-WINDOW", "5000");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonParams.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Ордер размещено успешно: " + new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } else {
            System.err.println("Ошибка размещения ордера: " + new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public void getOpenOrder(String symbol) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        String timestamp = getTimestamp();
        String endpoint = "/v5/order/realtime";
        String url = BASE_URL + endpoint;

        Map<String, Object> params = new HashMap<>();
        params.put("category", "linear");
        params.put("symbol", symbol);
        params.put("settleCoin", "USDT");

        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryString.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        queryString.deleteCharAt(queryString.length() - 1);

        String signature = generateSignature(timestamp, queryString.toString(), "");

        HttpURLConnection connection = (HttpURLConnection) new URL(url + "?" + queryString.toString()).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-BAPI-API-KEY", API_KEY);
        connection.setRequestProperty("X-BAPI-SIGN", signature);
        connection.setRequestProperty("X-BAPI-SIGN-TYPE", "2");
        connection.setRequestProperty("X-BAPI-TIMESTAMP", timestamp);
        connection.setRequestProperty("X-BAPI-RECV-WINDOW", "5000");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Открытие ордера: " + new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } else {
            System.err.println("Ошибка открытия ордера: " + new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}