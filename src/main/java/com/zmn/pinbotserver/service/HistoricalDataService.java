package com.zmn.pinbotserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HistoricalDataService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String HISTORICAL_DATA_FOLDER = "historical_data";
    private static final int MAX_CANDLES = 1000;

    private final BybitApiService bybitApiService;

    @Autowired
    public HistoricalDataService(BybitApiService bybitApiService) {
        this.bybitApiService = bybitApiService;
    }

    public void generateHistoricalDataFile(String symbol, String interval, LocalDateTime startDate, LocalDateTime endDate) {
        File dir = new File("historical_data");
        if (!dir.exists()) {
            dir.mkdir();
        }

        String filename = String.format("historical_data/%s_%s_history.csv", symbol, interval); // Changed extension to .csv
        File file = new File(filename);

        if (file.exists()) {
            System.out.println("Файл найден: " + filename);
            //return;
        }

        //System.out.println("ПРОВЕРКА - " + checkDataFile(symbol, interval));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            //List<LocalDateTime[]> intervals = generateIntervals(startDate, endDate, interval);

            // Write CSV header
            writer.write("timestamp,open,high,low,close,volume,quote_volume");
            writer.newLine();
            LocalDateTime startTimePeriod = startDate;
            boolean flag = true;
            while (flag){
                LocalDateTime endTimePeriod = startTimePeriod.plusMinutes(getMinutesPerCandle(interval));

                if(endTimePeriod.isAfter(LocalDateTime.now())) {
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
                System.out.println("До поворота - " + klineData.getFirst());
                klineData=klineData.reversed();
                System.out.println("После поворота - " + klineData.getFirst());

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

                //System.out.println("ПРОВЕРКА - " + checkDataFile(symbol, interval));


                startTimePeriod = endTimePeriod;
            }



            System.out.println("Данные успешно записаны в файл: " + filename);

        } catch (IOException e) {
            System.err.println("Ошибка при записи в файл: " + e.getMessage());
        }

        System.out.println("ПРОВЕРКА - " + checkDataFile(symbol, interval));
    }



    private int getMinutesPerCandle(String timeframe) {
        switch (timeframe) {
            case "1":
                return 1*MAX_CANDLES;
            case "3":
                return 3*MAX_CANDLES;
            case "5":
                return 5*MAX_CANDLES;
            case "15":
                return 15*MAX_CANDLES;
            case "30":
                return 30*MAX_CANDLES;
            case "60":
                return 60*MAX_CANDLES;
            case "120":
                return 120*MAX_CANDLES;
            case "240":
                return 240*MAX_CANDLES;
            case "360":
                return 360*MAX_CANDLES;
            case "720":
                return 720*MAX_CANDLES;
            case "1D":
                return 1440*MAX_CANDLES;
            case "1W":
                return 10080*MAX_CANDLES;
            case "1M":
                return 43200*MAX_CANDLES;
            default:
                throw new IllegalArgumentException("Неподдерживаемый таймфрейм: " + timeframe);
        }
    }

    public Object checkDataFile(String symbol, String interval) {
        // Формируем путь к файлу с использованием переданных параметров symbol и interval
        String filename = String.format("%s/%s_%s_history.csv", HISTORICAL_DATA_FOLDER, symbol, interval);
        File file = new File(filename);

        // Проверяем, существует ли файл
        if (!file.exists()) {
            System.out.println("Файл не существует: " + filename);
            return false;
        }

        // Проверяем содержимое файла
        return checkFileContent(file, interval);
    }

    private boolean checkFileContent(File file, String interval) {
        // Создаем множество для хранения уникальных временных меток
        Set<Long> timestamps = new HashSet<>();


        // Преобразуем интервал из строки в число минут
        int minutes = Integer.parseInt(interval);
        // Преобразуем интервал из минут в миллисекунды
        long timeFrameMillis = minutes * 60 * 1000;

        // Открываем файл для чтения
        long startTimeInFile;
        long endTimeInFile = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            // Пропускаем первую строку с заголовком
            br.readLine();
            line = br.readLine();
            String[] split = line.split(",");
            long previousTimestamp = Long.parseLong(split[0]); // Переменная для хранения предыдущей временной метки
            startTimeInFile = previousTimestamp;

            // Читаем файл построчно
            while ((line = br.readLine()) != null) {
                // Разделяем строку на массив строк по запятой
                String[] data = line.split(",");

                try {
                    // Преобразуем первую строку массива (временную метку) в число
                    long timestamp = Long.parseLong(data[0]);

                    // Проверка на дубликат временной метки
                    if (!timestamps.add(timestamp)) {
                        // Если временная метка уже существует в множестве, выводим сообщение и продолжаем
                        System.out.println("Дублирующая запись найдена и удалена: " + line);
                        continue;
                    }

                    // Проверка временного интервала
                    if (previousTimestamp != -1 && (timestamp - previousTimestamp != timeFrameMillis)) {
                        System.out.println("Неправильный временной интервал между записями: "
                                + previousTimestamp + " и " + timestamp + ". Строка: " + line);
                        return false;
                    }

                    // Обновляем предыдущую временную метку
                    previousTimestamp = timestamp;
                    endTimeInFile = timestamp;
                } catch (NumberFormatException e) {
                    // Обработка ошибки преобразования строки в число
                    System.out.println("Ошибка формата временной метки в строке: " + line);
                }
            }

        } catch (IOException e) {
            // Обработка исключений при чтении файла
            e.printStackTrace();
            return false;
        }

        LocalDateTime sTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeInFile), ZoneOffset.UTC);
        LocalDateTime eTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeInFile), ZoneOffset.UTC);
        System.out.println("В прочитанном файле: ");
        System.out.println("Начальное время: " + sTime.format(formatter) + " , а именно: " + startTimeInFile);
        System.out.println("Конечное время: " + eTime.format(formatter) + " , а именно: " + endTimeInFile);
        // Если все проверки пройдены, возвращаем true
        return true;
    }
}
