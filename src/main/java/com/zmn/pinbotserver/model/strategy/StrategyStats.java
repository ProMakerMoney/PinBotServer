package com.zmn.pinbotserver.model.strategy;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.order.Order;
import com.zmn.pinbotserver.model.order.Position;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;


public class StrategyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Автоматическая генерация ID
    @Getter
    private Long id;

    @Getter
    private String coinName; // Название монеты

    @Getter
    private String timeframe; // Таймфрейм

    @Getter
    private long testStartTime; // Время начала тестирования

    @Getter
    private long testEndTime; // Время окончания тестирования

    @Getter
    private int tradeCount; // Количество сделок

    @Getter
    private double profitableTradePercentage; // Процент прибыльных сделок

    @Getter
    private double profitInDollars; // Профит в долларах

    @Getter
    private double profitPercentage; // Профит в процентах от начального депозита

    @Getter
    private double maxDrawdown; // Максимальная просадка

    @Getter
    private long testDate; // Дата тестирования

    @Getter
    private List<Position> positions; // Список позиций

    @Getter
    private List<Order> orders; // Список ордеров

    // Конструктор по умолчанию
    public StrategyStats() {
    }

    // Конструктор с параметрами для инициализации всех полей
    public StrategyStats(List<Position> positions, List<Order> orders, List<Candle> candles) {
        this.positions = positions;
        this.orders = orders;
        this.coinName = positions.getFirst().getTradingPair(); // Получение названия монеты из первой позиции
        this.testStartTime = candles.getFirst().getTime(); // Получение времени начала тестирования из первой свечи
        this.testEndTime = candles.getLast().getTime(); // Получение времени окончания тестирования из последней свечи
        this.tradeCount = positions.size(); // Установка количества сделок
        this.profitableTradePercentage = calcProfitable(); // Вычисление процента прибыльных сделок
        this.profitInDollars = calcProfitableInDollars(); // Вычисление профита в долларах
        this.profitPercentage = calcProfitPercentage(); // Вычисление профита в процентах от начального депозита
        this.maxDrawdown = calcMaxDrown(); // Вычисление максимальной просадки
        this.testDate = System.currentTimeMillis(); // Установка текущего времени как даты тестирования
    }

    // Метод для расчета процента прибыльных сделок
    private double calcProfitable() {
        if (positions.isEmpty()) {
            return 0.0; // Если нет позиций, возвращаем 0
        }
        long profitableTrades = positions.stream()
                .filter(position -> position.getProfit() > 0) // Фильтрация прибыльных позиций
                .count(); // Подсчет количества прибыльных позиций
        return ((double) profitableTrades / positions.size()) * 100; // Вычисление процента прибыльных сделок
    }

    // Метод для расчета прибыли в долларах
    private double calcProfitableInDollars() {
        return positions.stream()
                .mapToDouble(Position::getProfit) // Получение прибыли каждой позиции
                .sum(); // Суммирование всей прибыли
    }

    // Метод для расчета прибыли в процентах от начального депозита
    private double calcProfitPercentage() {
        double initialInvestment = orders.stream()
                .filter(order -> order.getDirection().equals("buy")) // Фильтрация ордеров на покупку
                .mapToDouble(order -> order.getExecutionPrice() * order.getVolume()) // Вычисление стоимости каждого ордера
                .sum(); // Суммирование всех начальных инвестиций

        if (initialInvestment == 0) {
            return 0.0; // Если нет начальных инвестиций, возвращаем 0
        }

        return (calcProfitableInDollars() / initialInvestment) * 100; // Вычисление процента прибыли
    }

    // Метод для расчета максимальной просадки
    private double calcMaxDrown() {
        double peak = 0.0; // Начальная пиковая стоимость
        double maxDrawdown = 0.0; // Начальная максимальная просадка
        double cumulativeProfit = 0.0; // Кумулятивная прибыль

        for (Position position : positions) {
            cumulativeProfit += position.getProfit(); // Добавление прибыли каждой позиции к кумулятивной прибыли
            if (cumulativeProfit > peak) {
                peak = cumulativeProfit; // Обновление пикового значения
            }
            double drawdown = peak - cumulativeProfit; // Вычисление текущей просадки
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown; // Обновление максимальной просадки, если текущая больше
            }
        }

        return maxDrawdown; // Возвращение максимальной просадки
    }
}