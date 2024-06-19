package com.zmn.pinbotserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public void generateHistoricalDataFile(String symbol, String interval, LocalDateTime startDate, LocalDateTime endDate) {
        File dir = new File(HISTORICAL_DATA_FOLDER);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String filename = String.format("%s/%s_%s_history.csv", HISTORICAL_DATA_FOLDER, symbol, interval);
        File file = new File(filename);

        if (file.exists()) {
            System.out.println("Файл найден: " + filename);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Записываем заголовок CSV файла
            writer.write("timestamp,open,high,low,close,volume,quote_volume");
            writer.newLine();

            LocalDateTime startTimePeriod = startDate;
            boolean flag = true;
            while (flag) {
                LocalDateTime endTimePeriod = startTimePeriod.plusMinutes(getMinutesPerCandle(interval));

                if (endTimePeriod.isAfter(LocalDateTime.now())) {
                    System.out.println("СРАБОТАЛО УСЛОВИЕ ПОСЛЕДНЕГО ЗАПРОСА");
                    startTimePeriod = startTimePeriod.plusMinutes(15);
                    endTimePeriod = LocalDateTime.now();
                    flag = false;
                }

                // Вывод всех интервалов
                System.out.printf("Интервал: %s - %s%n", startTimePeriod, endTimePeriod);

                long start = startTimePeriod.toInstant(ZoneOffset.UTC).toEpochMilli();
                long end = endTimePeriod.toInstant(ZoneOffset.UTC).toEpochMilli();

                // Технические выводы для запроса
                System.out.printf("Отправка запроса к бирже: symbol=%s, interval=%s, start=%d, end=%d%n", symbol, interval, start, end);

                List<List<String>> klineData = bybitApiService.getKlineData(symbol, interval, start, end);
                System.out.println("До поворота - " + klineData.get(0));
                klineData=klineData.reversed(); // Сортировка в обратном порядке
                System.out.println("После поворота - " + klineData.get(0));

                // Технические выводы для ответа
                if (!klineData.isEmpty()) {
                    System.out.println("Получен ответ от биржи:");
                    LocalDateTime firstCandleTime = LocalDateTime.ofEpochSecond(Long.parseLong(klineData.get(0).get(0)) / 1000, 0, ZoneOffset.UTC);
                    LocalDateTime lastCandleTime = LocalDateTime.ofEpochSecond(Long.parseLong(klineData.get(klineData.size() - 1).get(0)) / 1000, 0, ZoneOffset.UTC);
                    System.out.println("Первая свеча: " + firstCandleTime.format(formatter));
                    System.out.println("Последняя свеча: " + lastCandleTime.format(formatter));
                } else {
                    System.out.println("Получен пустой ответ от биржи для интервала: " + startTimePeriod + " - " + endTimePeriod);
                }

                for (List<String> kline : klineData) {
                    writer.write(String.join(",", kline));
                    writer.newLine();
                }

                startTimePeriod = endTimePeriod;
            }

            System.out.println("Данные успешно записаны в файл: " + filename);

        } catch (IOException e) {
            System.err.println("Ошибка при записи в файл: " + e.getMessage());
        }

        System.out.println("ПРОВЕРКА - " + checkDataFile(symbol, interval));
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
    public Object checkDataFile(String symbol, String interval) {
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

        long startTimeInFile;
        long endTimeInFile = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine();
            line = br.readLine();
            String[] split = line.split(",");
            long previousTimestamp = Long.parseLong(split[0]);
            startTimeInFile = previousTimestamp;

            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");

                try {
                    long timestamp = Long.parseLong(data[0]);

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
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка формата временной метки в строке: " + line);
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
