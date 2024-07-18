package com.zmn.pinbotserver.bybit;


import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class TradeBotService {
    private static final String FILE_PATH = "tradeBots.dat";
    private List<TradeBot> tradeBots;

    public TradeBotService() {
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

    private void saveTradeBots() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(tradeBots);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<TradeBot> loadTradeBots() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            return (List<TradeBot>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new ArrayList<>();
        }
    }

    public List<TradeBot> getTradeBots() {
        return tradeBots;
    }
}
