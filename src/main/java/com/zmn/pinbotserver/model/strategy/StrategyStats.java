package com.zmn.pinbotserver.model.strategy;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import jakarta.persistence.*;
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
    private String timeframe;
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
        this.coinName = positions.getFirst().getTradingPair();
        this.testStartTime = candles.getFirst().getTime();
        this.testEndTime = candles.getLast().getTime();
        this.tradeCount = positions.size();
        this.profitableTradePercentage = calcProfitable();
        this.profitInDollars = calcProfitableInDollars();
        this.profitPercentage = calcProfitPercentage();
        this.maxDrawdown = calcMaxDown();
        this.testDate = System.currentTimeMillis();;
    }

    private double calcMaxDown() {
        return 0.0;
    }

    private double calcProfitPercentage() {
        return 0.0;
    }

    private double calcProfitableInDollars() {
        return 0.0;
    }

    private double calcProfitable() {
        return 0.0;
    }
}