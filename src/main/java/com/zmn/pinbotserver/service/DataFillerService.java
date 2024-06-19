package com.zmn.pinbotserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmn.pinbotserver.BybitResponse;
import com.zmn.pinbotserver.model.candle.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DataFillerService {

    private static final String BASE_URL = "https://api.bybit.com/v5/market/kline";
    private static final String CATEGORY = "linear";
    private final RestTemplate restTemplate;

    @Autowired
    public DataFillerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Candle> readCandlesFromCsv(Path filePath) throws IOException {
        List<Candle> candles = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                Candle candle = new Candle(
                        Long.parseLong(fields[0]),
                        Double.parseDouble(fields[1]),
                        Double.parseDouble(fields[2]),
                        Double.parseDouble(fields[3]),
                        Double.parseDouble(fields[4]),
                        Double.parseDouble(fields[5])
                );
                candles.add(candle);
            }
        }
        return candles;
    }

    public boolean validateCandles(List<Candle> candles, String timeframe) {
        Set<LocalDateTime> uniqueTimes = new HashSet<>();
        List<LocalDateTime> duplicateTimes = new ArrayList<>();
        List<LocalDateTime> missingTimes = new ArrayList<>();

        long intervalMinutes = getIntervalMinutes(timeframe);

        // Проходим по списку свечей, проверяя на дубли и "дырки"
        for (int i = 0; i < candles.size(); i++) {
            LocalDateTime time = candles.get(i).getTime();

            // Проверка на дублирование
            if (!uniqueTimes.add(time)) {
                duplicateTimes.add(time);
            }

            // Проверка на "дырки"
            if (i > 0) {
                LocalDateTime previousTime = candles.get(i - 1).getTime();
                long minutesBetween = ChronoUnit.MINUTES.between(previousTime, time);
                if (minutesBetween != intervalMinutes) {
                    for (long j = intervalMinutes; j < minutesBetween; j += intervalMinutes) {
                        missingTimes.add(previousTime.plusMinutes(j));
                    }
                }
            }
        }

        // Выводим результаты проверки
        if (!duplicateTimes.isEmpty()) {
            System.out.println("Найдены дублирующиеся времена: " + duplicateTimes);
            candles.removeIf(candle -> duplicateTimes.contains(candle.getTime())); // Удаляем дубли
        }

        if (!missingTimes.isEmpty()) {
            System.out.println("Найдены пропущенные времена: " + missingTimes);
            return false; // Неполные данные
        }

        return true; // Данные полные и корректные
    }

    private long getIntervalMinutes(String timeframe) {
        switch (timeframe) {
            case "1":
                return 1;
            case "5":
                return 5;
            case "15":
                return 15;
            case "60":
                return 60;
            case "4h":
                return 240;
            case "1d":
                return 1440;
            default:
                throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        }
    }

    public List<Candle> fetchAndWriteCandles(String tradingPair, String timeframe, LocalDateTime startDate, Path filePath) throws IOException {
        List<Candle> newCandles = fetchCandles(tradingPair, timeframe, startDate);
        Set<LocalDateTime> existingTimes = new HashSet<>();

        // Читаем существующие свечи из файла, чтобы избежать дублирования
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                LocalDateTime time = LocalDateTime.ofEpochSecond(Long.parseLong(fields[0]) / 1000, 0, ZoneOffset.UTC);
                existingTimes.add(time);
            }
        }

        // Записываем новые свечи в файл, избегая дублирования
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.APPEND)) {
            for (Candle candle : newCandles) {
                if (!existingTimes.contains(candle.getTime())) {
                    writer.write(candle.toCsvString());
                    writer.newLine();
                }
            }
        }

        return newCandles;
    }

    public List<Candle> fetchCandles(String tradingPair, String timeframe, LocalDateTime startDate) {
        List<Candle> allCandles = new ArrayList<>();
        LocalDateTime currentDate = startDate; // Начальная дата
        LocalDateTime endDate = LocalDateTime.now(); // Конечная дата - текущее время
        long maxIntervalMinutes = getMaxInterval(timeframe); // Получаем максимальный интервал в минутах для данного таймфрейма

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC")); // Форматтер для преобразования времени в строку

        System.out.println("Start Time: " + formatter.format(currentDate.atZone(ZoneId.of("UTC")).toInstant())); // Выводим стартовое время
        System.out.println("End Time: " + formatter.format(endDate.atZone(ZoneId.of("UTC")).toInstant())); // Выводим конечное время

        while (currentDate.isBefore(endDate)) {
            LocalDateTime adjustedEndDate = currentDate.plusMinutes(maxIntervalMinutes).isBefore(endDate)
                    ? currentDate.plusMinutes(maxIntervalMinutes)
                    : endDate; // Рассчитываем корректированное конечное время

            System.out.println("Current Start Time: " + formatter.format(currentDate.atZone(ZoneId.of("UTC")).toInstant())); // Выводим текущее стартовое время
            System.out.println("Current Adjusted End Time: " + formatter.format(adjustedEndDate.atZone(ZoneId.of("UTC")).toInstant())); // Выводим текущее корректированное конечное время

            String url = String.format("%s?category=%s&symbol=%s&interval=%s&start=%s&end=%s&limit=1000",
                    BASE_URL, CATEGORY, tradingPair, timeframe,
                    currentDate.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(),
                    adjustedEndDate.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()); // Формируем URL для запроса

            System.out.println("ВЫПОЛНЯЮ ЗАПРОС: " + url); // Выводим URL запроса
            try {
                String response = restTemplate.getForObject(url, String.class); // Выполняем запрос и получаем ответ в виде строки
                System.out.println("Ответ API: " + response);  // Выводим ответ API для отладки
                List<Candle> candles = parseCandles(response); // Парсим ответ API и получаем список свечей
                allCandles.addAll(candles); // Добавляем полученные свечи в общий список
                System.out.println("Получил " + candles.size() + " свечей");

                if (candles.size() < 1000) {
                    break; // Если получено меньше 1000 свечей, значит, достигли конца данных, прерываем цикл
                }

                currentDate = adjustedEndDate.plusMinutes(Long.parseLong(timeframe)); // Обновляем стартовое время на основе последней полученной свечи

            } catch (Exception e) {
                e.printStackTrace(); // Выводим стек вызовов в случае исключения
                break; // Прерываем цикл в случае ошибки
            }
            try {
                Thread.sleep(200); // Добавляем паузу между запросами, чтобы не перегружать сервер
            } catch (InterruptedException e) {
                e.printStackTrace(); // Выводим стек вызовов в случае исключения
            }
        }

        return allCandles; // Возвращаем полный список полученных свечей
    }

    private List<Candle> parseCandles(String response) {
        List<Candle> candles = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode resultNode = rootNode.path("result").path("list");

            if (resultNode.isArray()) {
                for (JsonNode node : resultNode) {
                    long time = node.get(0).asLong();
                    double open = node.get(1).asDouble();
                    double high = node.get(2).asDouble();
                    double low = node.get(3).asDouble();
                    double close = node.get(4).asDouble();
                    double volume = node.get(5).asDouble();
                    double quoteVolume = node.get(6).asDouble();

                    Candle candle = new Candle(time, open, high, low, close, volume);
                    candles.add(candle);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return candles;
    }

    private long getMaxInterval(String timeframe) {
        switch (timeframe) {
            case "1":
                return 1000; // 1 минутный интервал, 1000 минут
            case "5":
                return 5 * 1000; // 5 минутный интервал, 5000 минут
            case "15":
                return 15 * 1000; // 15 минутный интервал, 15 000 минут
            case "60":
                return 60 * 1000; // 1 часовой интервал, 60 000 минут
            case "4h":
                return 60 * 4 * 1000; // 4 часовой интервал, 240 000 минут
            case "1d":
                return 60 * 24 * 1000; // 1 дневной интервал, 1 440 000 минут
            default:
                throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        }
    }
}
