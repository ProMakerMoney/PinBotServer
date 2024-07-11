package com.zmn.pinbotserver.strategyTesting.clearATR;

import com.zmn.pinbotserver.model.candle.Candle;

import java.util.Queue;

public class TechnicalIndicators {

    public static double sma(Queue<Double> values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    public static double typicalPrice(Candle candle) {
        return (candle.getHigh() + candle.getLow() + candle.getClose()) / 3;
    }

    public static double mfi(Queue<Candle> candles, int period) {
        if (candles.size() < period) return 0.0;

        double positiveFlow = 0.0;
        double negativeFlow = 0.0;

        Candle prevCandle = null;
        for (Candle candle : candles) {
            if (prevCandle != null) {
                double tp = typicalPrice(candle);
                double prevTp = typicalPrice(prevCandle);
                double rawMoneyFlow = tp * candle.getVolume();

                if (tp > prevTp) {
                    positiveFlow += rawMoneyFlow;
                } else if (tp < prevTp) {
                    negativeFlow += rawMoneyFlow;
                }
            }
            prevCandle = candle;
        }

        double moneyFlowRatio = positiveFlow / negativeFlow;
        return 100 - (100 / (1 + moneyFlowRatio));
    }
}