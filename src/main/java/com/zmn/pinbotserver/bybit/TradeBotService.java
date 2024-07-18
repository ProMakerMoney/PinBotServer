package com.zmn.pinbotserver.bybit;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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

}