package com.zmn.pinbotserver.model.strategy;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;


import java.util.List;


public class StrategyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;
    @Getter
    private String coinName;
    @Getter
    private String timeframe = "15";
    @Getter
    private long testStartTime;
    @Getter
    private long testEndTime;
    @Getter
    private int tradeCount;
    @Getter
    private double profitableTradePercentage;
    @Getter
    private double profitInDollars;
    @Getter
    private double profitPercentage;
    @Getter
    private double maxDrawdown;
    @Getter
    private long testDate;
    @Getter
    private List<Position> positions;
    @Getter
    private List<Order> orders;

    // Конструктор по умолчанию
    public StrategyStats() {
    }

    // Конструктор со всеми параметрами
    public StrategyStats(List<Position> positions, List<Order> orders, List<Candle> candles) {
        this.positions = positions;
        this.orders = orders;
        if (!positions.isEmpty()) {
            this.coinName = positions.get(0).getTradingPair();
        }
        if (!candles.isEmpty()) {
            this.testStartTime = candles.get(0).getTime();
            this.testEndTime = candles.get(candles.size() - 1).getTime();
        }
        this.tradeCount = positions.size();
        this.profitableTradePercentage = calcProfitable();
        this.profitInDollars = calcProfitableInDollars();
        this.profitPercentage = calcProfitPercentage();
        this.maxDrawdown = calcMaxDrown();
        this.testDate = System.currentTimeMillis();
    }

    private double calcMaxDrown() {
        double peak = 0.0;
        double maxDrawdown = 0.0;
        double cumulativeProfit = 0.0;

        for (Position position : positions) {
            cumulativeProfit += position.getProfit();
            if (cumulativeProfit > peak) {
                peak = cumulativeProfit;
            }
            double drawdown = peak - cumulativeProfit;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown;
    }

    private double calcProfitPercentage() {
        double initialInvestment = orders.stream()
                .filter(order -> order.getDirection().equals("buy"))
                .mapToDouble(order -> order.getExecutionPrice() * order.getVolume())
                .sum();

        if (initialInvestment == 0) {
            return 0.0;
        }

        return (calcProfitableInDollars() / initialInvestment) * 100;
    }

    private double calcProfitableInDollars() {
        return positions.stream()
                .mapToDouble(Position::getProfit)
                .sum();
    }

    private double calcProfitable() {
        if (positions.isEmpty()) {
            return 0.0;
        }
        long profitableTrades = positions.stream()
                .filter(position -> position.getProfit() > 0)
                .count();
        return ((double) profitableTrades / positions.size()) * 100;
    }

    @Override
    public String toString() {
        return String.format("Статистика стратегии:\nМонета: %s\nТаймфрейм: %s\nДиапазон тестирования: %d - %d\nКоличество сделок: %d\nПроцент прибыльных сделок: %.2f%%\nПрофит в долларах: %.2f\nПрофит в проценте от изначального депозита: %.2f%%\nМаксимальная просадка: %.2f\nДата тестирования: %d",
                coinName, timeframe, testStartTime, testEndTime, tradeCount, profitableTradePercentage, profitInDollars, profitPercentage, maxDrawdown, testDate);
    }
}