package com.zmn.pinbotserver.strategyTesting;

import com.zmn.pinbotserver.model.candle.Candle;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс AlphaTrendIndicator реализует индикатор AlphaTrend.
 */
public class AlphaTrendIndicator {
    private final double coeff; // Множитель для расчета уровней AlphaTrend
    private final int ap; // Период для расчета скользящей средней истинного диапазона (ATR)
    private final boolean noVolumeData; // Флаг для изменения расчета при отсутствии данных о объеме

    private final List<Double> closePrices = new ArrayList<>(); // Список закрытий цен
    private final List<Double> highPrices = new ArrayList<>(); // Список максимумов цен
    private final List<Double> lowPrices = new ArrayList<>(); // Список минимумов цен
    private final List<Double> hlc3Prices = new ArrayList<>(); // Список средних значений цен (H+L+C)/3
    @Getter
    private final List<Double> alphaTrend = new ArrayList<>(); // Список значений AlphaTrend

    /**
     * Конструктор класса AlphaTrendIndicator.
     *
     * @param coeff        Множитель для расчета уровней AlphaTrend
     * @param ap           Период для расчета скользящей средней истинного диапазона (ATR)
     * @param noVolumeData Флаг для изменения расчета при отсутствии данных о объеме
     */
    public AlphaTrendIndicator(double coeff, int ap, boolean noVolumeData) {
        this.coeff = coeff;
        this.ap = ap;
        this.noVolumeData = noVolumeData;
    }

    /**
     * Добавляет новые данные о ценах.
     *
     * @param candle Объект Candle, содержащий данные о цене
     */
    public void addPrice(Candle candle) {
        closePrices.add(candle.getClose());
        highPrices.add(candle.getHigh());
        lowPrices.add(candle.getLow());
        hlc3Prices.add(candle.getTypicalPrice());

        if (closePrices.size() >= ap) {
            calculateAlphaTrend(); // Вызываем расчет AlphaTrend, если данных достаточно
        }
    }

    /**
     * Вычисляет значение AlphaTrend.
     */
    private void calculateAlphaTrend() {
        double atr = calculateATR(); // Рассчитываем ATR
        double upT = lowPrices.get(lowPrices.size() - 1) - atr * coeff; // Вычисляем верхний уровень тренда
        double downT = highPrices.get(highPrices.size() - 1) + atr * coeff; // Вычисляем нижний уровень тренда

        double previousAlphaTrend = alphaTrend.size() > 0 ? alphaTrend.get(alphaTrend.size() - 1) : 0.0;
        double newAlphaTrend;

        // Рассчитываем новое значение AlphaTrend в зависимости от флага noVolumeData
        if (noVolumeData) {
            double rsi = calculateRSI(closePrices, ap);
            newAlphaTrend = rsi >= 50 ? Math.max(previousAlphaTrend, upT) : Math.min(previousAlphaTrend, downT);
        } else {
            double mfi = calculateMFI(hlc3Prices, ap);
            newAlphaTrend = mfi >= 50 ? Math.max(previousAlphaTrend, upT) : Math.min(previousAlphaTrend, downT);
        }

        alphaTrend.add(newAlphaTrend); // Добавляем новое значение AlphaTrend в список
    }

    /**
     * Рассчитывает средний истинный диапазон (ATR).
     *
     * @return Значение ATR
     */
    private double calculateATR() {
        double sum = 0.0;
        for (int i = closePrices.size() - ap; i < closePrices.size(); i++) {
            double highLow = highPrices.get(i) - lowPrices.get(i);
            double highClose = i > 0 ? Math.abs(highPrices.get(i) - closePrices.get(i - 1)) : highLow;
            double lowClose = i > 0 ? Math.abs(lowPrices.get(i) - closePrices.get(i - 1)) : highLow;
            sum += Math.max(highLow, Math.max(highClose, lowClose));
        }
        return sum / ap; // Возвращаем среднее значение истинного диапазона
    }

    /**
     * Рассчитывает индекс относительной силы (RSI).
     *
     * @param prices Список цен
     * @param period Период для расчета
     * @return Значение RSI
     */
    private double calculateRSI(List<Double> prices, int period) {
        double gain = 0.0;
        double loss = 0.0;

        for (int i = prices.size() - period; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gain += change;
            } else {
                loss -= change;
            }
        }

        double rs = gain / loss;
        return 100 - (100 / (1 + rs)); // Возвращаем значение RSI
    }

    /**
     * Рассчитывает индекс денежного потока (MFI).
     *
     * @param hlc3 Список средних цен (H+L+C)/3
     * @param period Период для расчета
     * @return Значение MFI
     */
    private double calculateMFI(List<Double> hlc3, int period) {
        double positiveFlow = 0.0;
        double negativeFlow = 0.0;

        for (int i = hlc3.size() - period; i < hlc3.size(); i++) {
            double change = hlc3.get(i) - hlc3.get(i - 1);
            if (change > 0) {
                positiveFlow += hlc3.get(i);
            } else {
                negativeFlow += hlc3.get(i);
            }
        }

        double moneyFlowRatio = positiveFlow / negativeFlow;
        return 100 - (100 / (1 + moneyFlowRatio)); // Возвращаем значение MFI
    }

    /**
     * Определяет, есть ли сигнал на покупку.
     *
     * @return true, если сигнал на покупку, иначе false
     */
    public boolean isBuySignal() {
        return alphaTrend.size() > 2 && alphaTrend.get(alphaTrend.size() - 1) > alphaTrend.get(alphaTrend.size() - 3);
    }

    /**
     * Определяет, есть ли сигнал на продажу.
     *
     * @return true, если сигнал на продажу, иначе false
     */
    public boolean isSellSignal() {
        return alphaTrend.size() > 2 && alphaTrend.get(alphaTrend.size() - 1) < alphaTrend.get(alphaTrend.size() - 3);
    }
}