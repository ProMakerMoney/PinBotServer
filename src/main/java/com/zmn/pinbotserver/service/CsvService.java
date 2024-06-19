package com.zmn.pinbotserver.service;

import com.zmn.pinbotserver.model.candle.Candle;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvService {

    private static final String BASE_PATH = "historical_data/";

    public void createCsvForCoin(String tradingPair, String timeframe) throws IOException {
        String filePath = getFilePath(tradingPair, timeframe);
        if (!Files.exists(Paths.get(BASE_PATH))) {
            Files.createDirectories(Paths.get(BASE_PATH));
        }
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        } else {
            System.out.println("File for " + tradingPair + " with timeframe " + timeframe + " already exists.");
        }
    }

    public void saveCandleData(String tradingPair, String timeframe, List<Candle> candles) throws IOException {
        String filePath = getFilePath(tradingPair, timeframe);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            for (Candle candle : candles) {
                writer.write(String.format("%s,%f,%f,%f,%f,%f\n",
                        candle.getTime(),
                        candle.getOpen(),
                        candle.getHigh(),
                        candle.getLow(),
                        candle.getClose(),
                        candle.getVolume()));
            }
        }
    }

    public List<Candle> readCandleData(String tradingPair, String timeframe) throws IOException {
        String filePath = getFilePath(tradingPair, timeframe);
        List<Candle> candles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                Candle candle = new Candle();
                candle.setTime(LocalDateTime.parse(parts[0]));
                candle.setOpen(Double.parseDouble(parts[1]));
                candle.setHigh(Double.parseDouble(parts[2]));
                candle.setLow(Double.parseDouble(parts[3]));
                candle.setClose(Double.parseDouble(parts[4]));
                candle.setVolume(Double.parseDouble(parts[5]));
                candles.add(candle);
            }
        }
        return candles;
    }

    public String getFilePath(String tradingPair, String timeframe) {
        return BASE_PATH + tradingPair.toLowerCase() + "_" + timeframe + "_history.csv";
    }
}
