package com.zmn.pinbotserver.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class BybitApiService {
    private static final String BASE_URL = "https://api-testnet.bybit.com/v5/market/instruments-info";

    private final RestTemplate restTemplate;

    @Autowired
    public BybitApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getInstrumentInfo(String category, String symbol) {
        String url = String.format("%s?category=%s&symbol=%s", BASE_URL, category, symbol);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return response;
    }

    public double getMinTradingQtyFromAPI(String symbol) {
        Map<String, Object> response = getInstrumentInfo("linear", symbol);
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        Map<String, Object> instrument = ((List<Map<String, Object>>) result.get("list")).get(0);
        Map<String, Object> lotSizeFilter = (Map<String, Object>) instrument.get("lotSizeFilter");
        return Double.parseDouble((String) lotSizeFilter.get("minOrderQty"));
    }

    public double getMaxTradingQtyFromAPI(String symbol) {
        Map<String, Object> response = getInstrumentInfo("linear", symbol);
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        Map<String, Object> instrument = ((List<Map<String, Object>>) result.get("list")).get(0);
        Map<String, Object> lotSizeFilter = (Map<String, Object>) instrument.get("lotSizeFilter");
        return Double.parseDouble((String) lotSizeFilter.get("maxOrderQty"));
    }

    public int getMinLeverageFromAPI(String symbol) {
        Map<String, Object> response = getInstrumentInfo("linear", symbol);
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        Map<String, Object> instrument = ((List<Map<String, Object>>) result.get("list")).get(0);
        Map<String, Object> leverageFilter = (Map<String, Object>) instrument.get("leverageFilter");
        return (int) Double.parseDouble((String) leverageFilter.get("minLeverage"));
    }

    public int getMaxLeverageFromAPI(String symbol) {
        Map<String, Object> response = getInstrumentInfo("linear", symbol);
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        Map<String, Object> instrument = ((List<Map<String, Object>>) result.get("list")).get(0);
        Map<String, Object> leverageFilter = (Map<String, Object>) instrument.get("leverageFilter");
        return (int) Double.parseDouble((String) leverageFilter.get("maxLeverage"));
    }

    public List<List<String>> getKlineData(String symbol, String interval, long start, long end) {
        String url = String.format("https://api.bybit.com/v5/market/kline?category=inverse&symbol=%s&interval=%s&start=%d&end=%d&limit=1000",
                symbol, interval, start, end);
        System.out.println("ЗАПРОС: " + url);
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        //System.out.println("ОТВЕТ: " + response.getBody());

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
            return (List<List<String>>) result.get("list");
        } else {
            throw new RuntimeException("Failed to fetch data from Bybit API");
        }
    }
}
