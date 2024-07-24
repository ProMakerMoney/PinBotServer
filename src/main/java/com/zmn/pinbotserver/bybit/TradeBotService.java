package com.zmn.pinbotserver.bybit;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmn.pinbotserver.controller.HistoricalDataController;
import com.zmn.pinbotserver.service.coin.CoinService;
import com.zmn.pinbotserver.storage.CoinRepository;
import com.zmn.pinbotserver.storage.dao.coin.CoinDBStorage;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class TradeBotService {
    private static final String FILE_PATH = "tradeBots.json";
    @Getter
    private List<TradeBot> tradeBots;
    private ObjectMapper objectMapper;
    @Getter
    private List<StrategyATRBybit> strategies = new ArrayList<>();

    @Autowired
    private HistoricalDataController historicalDataController; // Добавляем контроллер как зависимость

    @Autowired
    CoinRepository coinDBStorage;

    public TradeBotService() {
        this.objectMapper = new ObjectMapper();
        this.tradeBots = loadTradeBots();
    }

    public TradeBot addBot(double initialDeposit, double risk) {
        TradeBot bot = new TradeBot(initialDeposit, risk);
        tradeBots.add(bot);
        saveTradeBots();
        return bot;
    }

    public StrategyParamsBybit addStrategyToBot(int botIndex, String coinName, String timeFrame, int CCI, int EMA, int leverage, double ratio, int maxOrders, int ATR, double coeff) {
        if (botIndex < 0 || botIndex >= tradeBots.size()) {
            throw new IllegalArgumentException("Invalid bot index");
        }
        TradeBot bot = tradeBots.get(botIndex);
        StrategyParamsBybit strategy = new StrategyParamsBybit(coinName, timeFrame, bot.getInitialDeposit(), bot.getRisk(), CCI, EMA, leverage, ratio, maxOrders, ATR, coeff);
        bot.addStrategy(strategy);
        saveTradeBots();
        return strategy;
    }

    public void deleteStrategy(int botIndex, String coinName, String timeFrame) {
        if (botIndex < 0 || botIndex >= tradeBots.size()) {
            throw new IllegalArgumentException("Invalid bot index");
        }
        TradeBot bot = tradeBots.get(botIndex);
        bot.getStrategies().removeIf(strategy -> strategy.getCoinName().equals(coinName) && strategy.getTimeFrame().equals(timeFrame));
        saveTradeBots();
    }

    public void deleteBot(int botIndex) {
        if (botIndex < 0 || botIndex >= tradeBots.size()) {
            throw new IllegalArgumentException("Invalid bot index");
        }
        tradeBots.remove(botIndex);
        saveTradeBots();
    }

    private void saveTradeBots() {
        try {
            objectMapper.writeValue(new File(FILE_PATH), tradeBots);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<TradeBot> loadTradeBots() {
        try {
            return objectMapper.readValue(new File(FILE_PATH), new TypeReference<List<TradeBot>>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public ResponseEntity<String> initializeBotsFromFile() {


        // Сначала обновляем все исторические данные
        ResponseEntity<String> updateResponse = historicalDataController.updateAllHistoricalData();
        if (updateResponse.getStatusCode() != HttpStatus.OK) {
            return updateResponse;
        }

        try {
            List<TradeBot> bots = objectMapper.readValue(new File(FILE_PATH), new TypeReference<List<TradeBot>>() {});

            for (TradeBot bot : bots) {
                for (StrategyParamsBybit strategyParams : bot.getStrategies()) {
                    double qty = coinDBStorage.findMinQTYByCoinName(strategyParams.getCoinName());
                    StrategyATRBybit strategy = new StrategyATRBybit(
                            strategyParams,
                            bot.getInitialDeposit(),
                            qty,
                            bot.getRisk()
                    );
                    strategies.add(strategy);
                    // Загрузка свечек для каждой стратегии
                    strategy.getCurrentCandles(); // метод загрузки свечей
                }
            }
            return ResponseEntity.ok("Боты и стратегии успешно инициализированы из файла.");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при чтении файла: " + e.getMessage());
        }
    }


    public void updateStrategiesMode(String newMode) {
        for (TradeBot bot : tradeBots) {
            for (StrategyATRBybit strategy : strategies) {
                strategy.setMode(newMode);
            }
        }
        saveTradeBots();
    }

    public void closeAllPairs() {
        for (StrategyATRBybit strategy : strategies) {
            strategy.closeNow();
        }
    }

    public void closePair(String pairName) {
        for (StrategyATRBybit strategy : strategies) {
            if (strategy.getTradingPair().equals(pairName)) {
                strategy.closeNow();
            }
        }
    }
}