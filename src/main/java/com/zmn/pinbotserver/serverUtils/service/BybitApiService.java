package com.zmn.pinbotserver.serverUtils.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service // Аннотация, обозначающая, что данный класс является сервисом в Spring
public class BybitApiService {
    private static final String BASE_URL = "https://api.bybit.com/v5/market/instruments-info"; // Базовый URL для API Bybit

    private final RestTemplate restTemplate; // Экземпляр RestTemplate для выполнения HTTP-запросов

    @Autowired // Аннотация для автоматической инъекции зависимостей
    public BybitApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate; // Инициализация RestTemplate через конструктор
    }

    /**
     * Метод для получения информации о инструменте
     * @param category категория инструмента (например, "linear")
     * @param symbol символ инструмента (например, "BTCUSD")
     * @return Map<String, Object> ответ API в виде карты
     */
    public Map<String, Object> getInstrumentInfo(String category, String symbol) {
        String url = String.format("%s?category=%s&symbol=%s", BASE_URL, category, symbol); // Формирование URL с параметрами
        System.out.println("Запрос URL: " + url);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class); // Выполнение GET-запроса и получение ответа в виде карты
        System.out.println("Ответ API: " + response);
        return response; // Возврат ответа
    }

    /**
     * Метод для получения минимального торгового количества из API
     * @param symbol символ инструмента
     * @return double минимальное торговое количество
     */
    public double getMinTradingQtyFromAPI(String symbol) {
        Map<String, Object> response = getInstrumentInfo("linear", symbol); // Получение информации о инструменте
        if (response == null || !response.containsKey("result")) {
            throw new RuntimeException("Invalid response from API");
        }

        Map<String, Object> result = (Map<String, Object>) response.get("result"); // Извлечение результата из ответа
        if (result == null || !result.containsKey("list")) {
            throw new RuntimeException("Invalid result data");
        }

        List<Map<String, Object>> instruments = (List<Map<String, Object>>) result.get("list"); // Получение списка инструментов
        if (instruments.isEmpty()) {
            throw new RuntimeException("No instruments found in API response");
        }

        Map<String, Object> instrument = instruments.get(0); // Получение первого элемента списка инструментов
        if (instrument == null || !instrument.containsKey("lotSizeFilter")) {
            throw new RuntimeException("Invalid instrument data");
        }

        Map<String, Object> lotSizeFilter = (Map<String, Object>) instrument.get("lotSizeFilter"); // Получение фильтра размера лота
        return Double.parseDouble(lotSizeFilter.get("minOrderQty").toString()); // Преобразование и возврат минимального торгового количества
    }

    /**
     * Метод для получения максимального торгового количества из API
     * @param symbol символ инструмента
     * @return double максимальное торговое количество
     */
    public double getMaxTradingQtyFromAPI(String symbol) {
        Map<String, Object> response = getInstrumentInfo("linear", symbol); // Получение информации о инструменте
        if (response == null || !response.containsKey("result")) {
            throw new RuntimeException("Invalid response from API");
        }

        Map<String, Object> result = (Map<String, Object>) response.get("result"); // Извлечение результата из ответа
        if (result == null || !result.containsKey("list")) {
            throw new RuntimeException("Invalid result data");
        }

        List<Map<String, Object>> instruments = (List<Map<String, Object>>) result.get("list"); // Получение списка инструментов
        if (instruments.isEmpty()) {
            throw new RuntimeException("No instruments found in API response");
        }

        Map<String, Object> instrument = instruments.get(0); // Получение первого элемента списка инструментов
        if (instrument == null || !instrument.containsKey("lotSizeFilter")) {
            throw new RuntimeException("Invalid instrument data");
        }

        Map<String, Object> lotSizeFilter = (Map<String, Object>) instrument.get("lotSizeFilter"); // Получение фильтра размера лота
        return Double.parseDouble(lotSizeFilter.get("maxOrderQty").toString()); // Преобразование и возврат максимального торгового количества
    }

    /**
     * Метод для получения минимального плеча из API
     * @param symbol символ инструмента
     * @return int минимальное плечо
     */
    public int getMinLeverageFromAPI(String symbol) {
        Map<String, Object> response = getInstrumentInfo("linear", symbol); // Получение информации о инструменте
        if (response == null || !response.containsKey("result")) {
            throw new RuntimeException("Invalid response from API");
        }

        Map<String, Object> result = (Map<String, Object>) response.get("result"); // Извлечение результата из ответа
        if (result == null || !result.containsKey("list")) {
            throw new RuntimeException("Invalid result data");
        }

        List<Map<String, Object>> instruments = (List<Map<String, Object>>) result.get("list"); // Получение списка инструментов
        if (instruments.isEmpty()) {
            throw new RuntimeException("No instruments found in API response");
        }

        Map<String, Object> instrument = instruments.get(0); // Получение первого элемента списка инструментов
        if (instrument == null || !instrument.containsKey("leverageFilter")) {
            throw new RuntimeException("Invalid instrument data");
        }

        Map<String, Object> leverageFilter = (Map<String, Object>) instrument.get("leverageFilter"); // Получение фильтра плеча
        return (int) Double.parseDouble(leverageFilter.get("minLeverage").toString()); // Преобразование и возврат минимального плеча
    }

    /**
     * Метод для получения максимального плеча из API
     * @param symbol символ инструмента
     * @return int максимальное плечо
     */
    public int getMaxLeverageFromAPI(String symbol) {
        Map<String, Object> response = getInstrumentInfo("linear", symbol); // Получение информации о инструменте
        if (response == null || !response.containsKey("result")) {
            throw new RuntimeException("Invalid response from API");
        }

        Map<String, Object> result = (Map<String, Object>) response.get("result"); // Извлечение результата из ответа
        if (result == null || !result.containsKey("list")) {
            throw new RuntimeException("Invalid result data");
        }

        List<Map<String, Object>> instruments = (List<Map<String, Object>>) result.get("list"); // Получение списка инструментов
        if (instruments.isEmpty()) {
            throw new RuntimeException("No instruments found in API response");
        }

        Map<String, Object> instrument = instruments.get(0); // Получение первого элемента списка инструментов
        if (instrument == null || !instrument.containsKey("leverageFilter")) {
            throw new RuntimeException("Invalid instrument data");
        }

        Map<String, Object> leverageFilter = (Map<String, Object>) instrument.get("leverageFilter"); // Получение фильтра плеча
        return (int) Double.parseDouble(leverageFilter.get("maxLeverage").toString()); // Преобразование и возврат максимального плеча
    }

    /**
     * Метод для получения данных свечей (K-line) из API
     * @param symbol символ инструмента
     * @param interval интервал времени для свечей
     * @param start начальное время
     * @param end конечное время
     * @return List<List<String>> список данных свечей
     */
    public List<List<String>> getKlineData(String symbol, String interval, long start, long end) {
        // Формирование URL для запроса данных свечей с параметрами
        String url = String.format("https://api.bybit.com/v5/market/kline?category=inverse&symbol=%s&interval=%s&start=%d&end=%d&limit=1000",
                symbol, interval, start, end);
        System.out.println("ЗАПРОС: " + url); // Вывод URL запроса для отладки
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class); // Выполнение GET-запроса и получение ответа в виде ResponseEntity

        //System.out.println("ОТВЕТ: " + response.getBody()); // Вывод тела ответа для отладки (закомментировано)

        // Проверка статуса ответа
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> result = (Map<String, Object>) response.getBody().get("result"); // Извлечение результата из ответа
            return (List<List<String>>) result.get("list"); // Возврат списка данных свечей
        } else {
            throw new RuntimeException("Failed to fetch data from Bybit API"); // Исключение в случае неудачи запроса
        }
    }
}