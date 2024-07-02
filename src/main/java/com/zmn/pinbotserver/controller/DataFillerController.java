package com.zmn.pinbotserver.controller;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.service.getData.DataFillerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@RestController // Аннотация, обозначающая, что данный класс является контроллером Spring RESTful веб-сервиса
@RequestMapping("/api/data") // Базовый URL для всех методов данного контроллера
@RequiredArgsConstructor // Аннотация Lombok, создающая конструктор с обязательными аргументами для final полей
public class DataFillerController {

    private final DataFillerService dataFillerService; // Сервис для работы с данными

    /**
     * Метод для получения свечных данных
     * @param tradingPair торговая пара
     * @param timeframe таймфрейм
     * @param start начальная дата в формате строки
     * @return список свечей
     * @throws IOException возможное исключение ввода-вывода
     */
    @GetMapping("/fetchCandles") // Обрабатывает HTTP GET запросы по URL /api/data/fetchCandles
    public List<Candle> fetchCandles(@RequestParam String tradingPair, @RequestParam String timeframe,
                                     @RequestParam String start) throws IOException {
        // Преобразование строки с начальной датой в объект LocalDateTime
        LocalDateTime startDate = LocalDateTime.parse(start);

        // Формирование имени файла на основе торговой пары и таймфрейма
        String fileName = tradingPair + "_" + timeframe + "_history.csv";

        // Указание пути к файлу CSV
        Path filePath = Paths.get("C:\\Users\\dev-n\\IdeaProjects\\PinBotServer\\historical_data", fileName);

        // Проверка, существует ли файл. Если не существует, создаем его.
        if (Files.notExists(filePath)) {
            Files.createFile(filePath);
        }

        // Чтение существующих свечей из файла CSV
        List<Candle> existingCandles = dataFillerService.readCandlesFromCsv(filePath);

        // Проверка целостности данных в файле
        boolean isValid = dataFillerService.validateCandles(existingCandles, timeframe);
        if (!isValid) {
            // Если данные невалидные, выбрасываем исключение
            throw new RuntimeException("Invalid candle sequence in CSV file.");
        }

        // Если файл пустой или данные валидны, загружаем и записываем новые свечи с указанной начальной даты
        if (existingCandles.isEmpty()) {
            return dataFillerService.fetchAndWriteCandles(tradingPair, timeframe, startDate, filePath);
        } else {
            // Получаем время последней свечи из существующих данных
            LocalDateTime lastCandleTime = existingCandles.getLast().getTimeAsLocalDateTime();
            // Загружаем и записываем новые свечи начиная с времени последней свечи
            return dataFillerService.fetchAndWriteCandles(tradingPair, timeframe, lastCandleTime, filePath);
        }
    }
}