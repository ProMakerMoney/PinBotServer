package com.zmn.pinbotserver.mexc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.MapUtils;

public class MexcApiClient {

    private static final Logger logger = LoggerFactory.getLogger(MexcApiClient.class);

    private static final String API_URL = "https://contract.mexc.com/api/v1/private/order/submit";

    public MexcApiClient() {
        String apiKey = "mx0vgl0APdNFMpN6Nd";
        String secretKey = "ee6be98509d8489fa0c9cfc409bf5e79";
    }

    /**
     * Gets the get request parameter string
     *
     * @param param get/delete Request parameters map
     * @return
     */
    public static String getRequestParamString(Map<String, String> param) {
        if (MapUtils.isEmpty(param)) {
            return "";
        }
        StringBuilder sb = new StringBuilder(1024);
        SortedMap<String, String> map = new TreeMap<>(param);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = StringUtils.isBlank(entry.getValue()) ? "" : entry.getValue();
            sb.append(key).append('=').append(urlEncode(value)).append('&');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("UTF-8 encoding not supported!");
        }
    }

    /**
     * signature
     */
    public static String sign(SignVo signVo) {
        if (signVo.getRequestParam() == null) {
            signVo.setRequestParam("");
        }
        String str = signVo.getAccessKey() + signVo.getReqTime() + signVo.getRequestParam();
        return actualSignature(str, signVo.getSecretKey());
    }

    public static String actualSignature(String inputStr, String key) {
        Mac hmacSha256;
        try {
            hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secKey =
                    new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm: " + e.getMessage());
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key: " + e.getMessage());
        }
        byte[] hash = hmacSha256.doFinal(inputStr.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(hash);
    }

    @Getter
    @Setter
    public static class SignVo {
        private String reqTime;
        private String accessKey;
        private String secretKey;
        private String requestParam; ////get параметры запроса сортируются в порядке словаря, при этом строки & объединяются, POST должен быть строкой JSON
    }
}
