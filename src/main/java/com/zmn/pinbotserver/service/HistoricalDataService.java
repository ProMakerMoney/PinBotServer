package com.zmn.pinbotserver.service;

import com.zmn.pinbotserver.model.candle.Candle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service // Аннотация, обозначающая, что данный класс является сервисом в Spring
public class HistoricalDataService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String HISTORICAL_DATA_FOLDER = "historical_data";
    private static final int MAX_CANDLES = 1000;

    private final BybitApiService bybitApiService;

    @Autowired // Автоматическая инъекция зависимости от BybitApiService
    public HistoricalDataService(BybitApiService bybitApiService) {
        this.bybitApiService = bybitApiService;
    }

    /**
     * Метод для генерации файла с историческими данными
     * @param symbol символ торговой пары
     * @param interval таймфрейм
     * @param startDate начальная дата
     * @param endDate конечная дата
     */
    public List<Candle> generateHistoricalDataFile(String symbol, String interval, LocalDateTime startDate, LocalDateTime endDate) {
        System.out.println("Зашло в метод с параметрами: " + symbol + ", " + interval + ", " + startDate + ", " + endDate);
        File dir = new File(HISTORICAL_DATA_FOLDER);
        if (!dir.exists()) {
            dir.mkdir(); // Создаем директорию, если она не существует
        }

        String filename = String.format("%s/%s_%s_history.csv", HISTORICAL_DATA_FOLDER, symbol, interval);
        File file = new File(filename);

        List<Candle> allCandles = new ArrayList<>(); // Список для хранения всех свечей
        Set<Long> existingTimestamps = new HashSet<>(); // Множество для хранения существующих временных меток

        // Чтение существующего файла CSV для сбора временных меток
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("timestamp")) continue; // Пропускаем заголовок
                String[] fields = line.split(",");
                long timestamp = Long.parseLong(fields[0]);
                existingTimestamps.add(timestamp); // Добавляем временную метку в множество
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла: " + e.getMessage());
        }

        // Открываем файл для записи данных
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // Записываем заголовок CSV файла, если файл пуст
                writer.write("timestamp,open,high,low,close,volume,quote_volume");
                writer.newLine();
            }

            LocalDateTime startTimePeriod = startDate;
            boolean flag = true;

            while (flag) {
                LocalDateTime endTimePeriod = startTimePeriod.plusMinutes(getMinutesPerCandle(interval));

                // Проверка, чтобы конечное время не превышало заданную конечную дату
                if (endTimePeriod.isAfter(endDate)) {
                    endTimePeriod = endDate;
                    flag = false;
                }

                long start = startTimePeriod.toInstant(ZoneOffset.UTC).toEpochMilli();
                long end = endTimePeriod.toInstant(ZoneOffset.UTC).toEpochMilli();

                // Получение данных свечей (K-line) из API Bybit
                List<List<String>> klineData = bybitApiService.getKlineData(symbol, interval, start, end);
                Collections.reverse(klineData); // Переворачиваем данные, чтобы они шли в хронологическом порядке

                // Запись данных в файл и в список свечей, избегая дублирования
                for (List<String> kline : klineData) {
                    long timestamp = Long.parseLong(kline.get(0));
                    if (!existingTimestamps.contains(timestamp)) {
                        writer.write(String.join(",", kline)); // Запись строки в файл
                        writer.newLine();
                        existingTimestamps.add(timestamp); // Добавляем временную метку в множество
                        allCandles.add(new Candle(
                                timestamp,
                                Double.parseDouble(kline.get(1)),
                                Double.parseDouble(kline.get(2)),
                                Double.parseDouble(kline.get(3)),
                                Double.parseDouble(kline.get(4)),
                                Double.parseDouble(kline.get(5)),
                                Double.parseDouble(kline.get(6))
                        )); // Добавляем свечу в список
                    }
                }

                startTimePeriod = endTimePeriod; // Обновляем стартовое время для следующего запроса
            }

            System.out.println("Данные успешно записаны в файл: " + filename);

        } catch (IOException e) {
            System.err.println("Ошибка при записи в файл: " + e.getMessage());
        }

        checkDataFile(symbol, interval); // Проверка целостности данных в файле
        return allCandles; // Возвращаем список всех свечей
    }

    /**
     * Метод для получения минут на свечу на основе таймфрейма
     * @param timeframe таймфрейм
     * @return количество минут на свечу
     */
    private int getMinutesPerCandle(String timeframe) {
        switch (timeframe) {
            case "1":
                return 1 * MAX_CANDLES;
            case "3":
                return 3 * MAX_CANDLES;
            case "5":
                return 5 * MAX_CANDLES;
            case "15":
                return 15 * MAX_CANDLES;
            case "30":
                return 30 * MAX_CANDLES;
            case "60":
                return 60 * MAX_CANDLES;
            case "120":
                return 120 * MAX_CANDLES;
            case "240":
                return 240 * MAX_CANDLES;
            case "360":
                return 360 * MAX_CANDLES;
            case "720":
                return 720 * MAX_CANDLES;
            case "1D":
                return 1440 * MAX_CANDLES;
            case "1W":
                return 10080 * MAX_CANDLES;
            case "1M":
                return 43200 * MAX_CANDLES;
            default:
                throw new IllegalArgumentException("Неподдерживаемый таймфрейм: " + timeframe);
        }
    }

    /**
     * Метод для проверки файла данных
     * @param symbol символ торговой пары
     * @param interval таймфрейм
     * @return результат проверки
     */
    public boolean checkDataFile(String symbol, String interval) {
        String filename = String.format("%s/%s_%s_history.csv", HISTORICAL_DATA_FOLDER, symbol, interval);
        File file = new File(filename);

        if (!file.exists()) {
            System.out.println("Файл не существует: " + filename);
            return false;
        }

        return checkFileContent(file, interval);
    }

    /**
     * Метод для проверки содержимого файла данных
     * @param file файл данных
     * @param interval таймфрейм
     * @return результат проверки
     */
    private boolean checkFileContent(File file, String interval) {
        Set<Long> timestamps = new HashSet<>();
        int minutes = getMinutesPerCandle(interval) / MAX_CANDLES;
        long timeFrameMillis = minutes * 60 * 1000;

        long startTimeInFile = 0;
        long endTimeInFile = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine(); // Пропускаем заголовок
            String[] split;
            long previousTimestamp = -1;

            while ((line = br.readLine()) != null) {
                split = line.split(",");
                long timestamp = Long.parseLong(split[0]);

                if (!timestamps.add(timestamp)) {
                    System.out.println("Дублирующая запись найдена и удалена: " + line);
                    continue;
                }

                if (previousTimestamp != -1 && (timestamp - previousTimestamp != timeFrameMillis)) {
                    System.out.println("Неправильный временной интервал между записями: "
                            + previousTimestamp + " и " + timestamp + ". Строка: " + line);
                    return false;
                }

                previousTimestamp = timestamp;
                endTimeInFile = timestamp;
                if (startTimeInFile == 0) {
                    startTimeInFile = timestamp;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        LocalDateTime sTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeInFile), ZoneOffset.UTC);
        LocalDateTime eTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeInFile), ZoneOffset.UTC);
        System.out.println("В прочитанном файле: ");
        System.out.println("Начальное время: " + sTime.format(formatter) + " , а именно: " + startTimeInFile);
        System.out.println("Конечное время: " + eTime.format(formatter) + " , а именно: " + endTimeInFile);
        return true;
    }
}