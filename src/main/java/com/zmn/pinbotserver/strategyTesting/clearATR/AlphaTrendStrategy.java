package com.zmn.pinbotserver.strategyTesting.clearATR;

import com.zmn.pinbotserver.model.candle.Candle;

import java.util.LinkedList;
import java.util.Queue;

public class AlphaTrendStrategy {

    private final int AP;
    private final double coeff;
    private final boolean novolumedata;
    private final int maxQueueSize;

    private double alphaTrend;
    private Queue<Double> atrValues;
    private Queue<Candle> candleQueue;

    public AlphaTrendStrategy(int ap, double coeff, boolean novolumedata) {
        this.AP = ap;
        this.coeff = coeff;
        this.novolumedata = novolumedata;
        this.alphaTrend = 0.0;
        this.maxQueueSize = ap;
        this.atrValues = new LinkedList<>();
        this.candleQueue = new LinkedList<>();
    }

    public void onPriceUpdate(Candle candle) {
        if (candleQueue.size() == maxQueueSize) {
            candleQueue.poll();
        }
        candleQueue.add(candle);

        double tr = calculateTrueRange(candle);
        if (atrValues.size() == maxQueueSize) {
            atrValues.poll();
        }
        atrValues.add(tr);

        double atr = TechnicalIndicators.sma(atrValues);

        double upT = candle.getLow() - atr * coeff;
        double downT = candle.getHigh() + atr * coeff;

        double prevAlphaTrend = alphaTrend;
        alphaTrend = (novolumedata ? calculateRsi(candleQueue, AP) >= 50 : TechnicalIndicators.mfi(candleQueue, AP) >= 50)
                ? Math.max(prevAlphaTrend, upT) : Math.min(prevAlphaTrend, downT);

        generateSignals(prevAlphaTrend, alphaTrend);
    }

    private double calculateTrueRange(Candle candle) {
        if (candleQueue.size() < 2) return 0.0;

        Candle prevCandle = ((LinkedList<Candle>) candleQueue).get(candleQueue.size() - 2);
        double highLow = candle.getHigh() - candle.getLow();
        double highClose = Math.abs(candle.getHigh() - prevCandle.getClose());
        double lowClose = Math.abs(candle.getLow() - prevCandle.getClose());

        return Math.max(highLow, Math.max(highClose, lowClose));
    }

    private double calculateRsi(Queue<Candle> candles, int period) {
        if (candles.size() < period) return 0.0;

        double gain = 0.0;
        double loss = 0.0;
        Candle prevCandle = null;

        for (Candle candle : candles) {
            if (prevCandle != null) {
                double change = candle.getClose() - prevCandle.getClose();
                if (change > 0) {
                    gain += change;
                } else {
                    loss -= change;
                }
            }
            prevCandle = candle;
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    private void generateSignals(double prevAlphaTrend, double alphaTrend) {
        if (alphaTrend > prevAlphaTrend && alphaTrend > 0) {
            System.out.println("Сигнал на ПОКУПКУ");
        } else if (alphaTrend < prevAlphaTrend && alphaTrend > 0) {
            System.out.println("Сигнал на ПРОДАЖУ");
        }
    }
}