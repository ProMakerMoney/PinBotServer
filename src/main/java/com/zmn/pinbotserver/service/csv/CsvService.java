package com.zmn.pinbotserver.service.csv;

import com.zmn.pinbotserver.model.candle.Candle;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service // Аннотация, обозначающая, что данный класс является сервисом в Spring
public class CsvService {

    private static final String BASE_PATH = "historical_data/"; // Базовый путь для хранения CSV файлов

    /**
     * Метод для создания CSV файла для указанной торговой пары и таймфрейма
     * @param tradingPair торговая пара
     * @param timeframe таймфрейм
     * @throws IOException возможное исключение ввода-вывода
     */
    public void createCsvForCoin(String tradingPair, String timeframe) throws IOException {
        String filePath = getFilePath(tradingPair, timeframe); // Получаем путь к файлу
        if (!Files.exists(Paths.get(BASE_PATH))) {
            Files.createDirectories(Paths.get(BASE_PATH)); // Создаем директории, если они не существуют
        }
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile(); // Создаем новый файл, если он не существует
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                // Записываем заголовок CSV файла
                writer.write("timestamp,open,high,low,close,volume,quote_volume");
                writer.newLine();
            }
        } else {
            System.out.println("File for " + tradingPair + " with timeframe " + timeframe + " already exists."); // Сообщение, если файл уже существует
        }
    }

    /**
     * Метод для сохранения данных свечей в CSV файл
     * @param tradingPair торговая пара
     * @param timeframe таймфрейм
     * @param candles список свечей
     * @throws IOException возможное исключение ввода-вывода
     */
    public void saveCandleData(String tradingPair, String timeframe, List<Candle> candles) throws IOException {
        String filePath = getFilePath(tradingPair, timeframe); // Получаем путь к файлу
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) { // Открываем файл для записи
            for (Candle candle : candles) {
                writer.write(String.format("%d,%f,%f,%f,%f,%f,%f\n",
                        candle.getTime(),
                        candle.getOpen(),
                        candle.getHigh(),
                        candle.getLow(),
                        candle.getClose(),
                        candle.getVolume(),
                        candle.getQuoteVolume())); // Записываем данные свечи в файл
            }
        }
    }

    /**
     * Метод для чтения данных свечей из CSV файла
     * @param tradingPair торговая пара
     * @param timeframe таймфрейм
     * @return список свечей
     * @throws IOException возможное исключение ввода-вывода
     */
    public List<Candle> readCandleData(String tradingPair, String timeframe) throws IOException {
        String filePath = getFilePath(tradingPair, timeframe); // Получаем путь к файлу
        List<Candle> candles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) { // Открываем файл для чтения
            String line;
            reader.readLine(); // Пропускаем заголовок
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                Candle candle = new Candle();
                candle.setTimeFromLocalDateTime(LocalDateTime.ofEpochSecond(Long.parseLong(parts[0]) / 1000, 0, ZoneOffset.UTC)); // Парсим время из строки
                candle.setOpen(Double.parseDouble(parts[1])); // Парсим цену открытия
                candle.setHigh(Double.parseDouble(parts[2])); // Парсим максимальную цену
                candle.setLow(Double.parseDouble(parts[3])); // Парсим минимальную цену
                candle.setClose(Double.parseDouble(parts[4])); // Парсим цену закрытия
                candle.setVolume(Double.parseDouble(parts[5])); // Парсим объем
                candle.setQuoteVolume(Double.parseDouble(parts[6])); // Парсим квотируемый объем
                candles.add(candle); // Добавляем свечу в список
            }
        }
        return candles;
    }

    /**
     * Метод для получения пути к CSV файлу для указанной торговой пары и таймфрейма
     * @param tradingPair торговая пара
     * @param timeframe таймфрейм
     * @return путь к файлу
     */
    public String getFilePath(String tradingPair, String timeframe) {
        return BASE_PATH + tradingPair.toLowerCase() + "_" + timeframe + "_history.csv"; // Формируем путь к файлу
    }
}