package com.zmn.pinbotserver.model.order;


import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Position {
    private static int idCounter = 1; // Счетчик для генерации уникальных ID
    int id; // Уникальный идентификатор позиции
    @Getter
    String tradingPair; // Торговая пара
    long startPosition; // Время открытия позиции
    long endPosition; // Время закрытия позиции
    TYPE side; // Направление позиции (LONG или SHORT)
    int leverage; // Плечо
    List<Order> orders; // Список ордеров
    double commissionLEVEL = 0.1; // Комиссия в процентах
    double commission; // Общая комиссия
    @Getter
    double profit; // Прибыль
    double profitPer; // Прибыль в процентах
    STATUS status; // Статус позиции (Открыта или Закрыта)

    // Конструктор для создания позиции
    public Position(String tradingPair, TYPE side, int leverage) {
        this.id = idCounter++; // Присвоение уникального ID и инкремент счетчика
        this.tradingPair = tradingPair;
        this.side = side;
        this.leverage = leverage;
        this.orders = new ArrayList<>();
        this.commission = 0.0;
    }

    // Метод для добавления ордера к позиции
    public void addOrder(Order order) {
        orders.add(order); // Добавление ордера в список
        if (orders.size() == 1) {
            startPosition = order.executionTime; // Установка времени открытия позиции при первом ордере
        }
        commission -= calcCommission(order); // Вычисление и вычитание комиссии
        status = STATUS.OPEN; // Установка статуса позиции как открытой
    }

    // Метод для закрытия позиции
    public void closePosition(Order order) {
        orders.add(order); // Добавление ордера в список
        endPosition = order.executionTime; // Установка времени закрытия позиции
        commission += calcCommission(order); // Вычисление и вычитание комиссии
        profit = calcProfit() - commission; // Вычисление прибыли с учетом комиссии
        profitPer = calcProfitPer(); // Вычисление прибыли в процентах
        status = STATUS.CLOSE; // Установка статуса позиции как закрытой
    }

    // Метод для расчета комиссии за ордер
    private double calcCommission(Order order) {
        return order.executionPrice * order.volume * (commissionLEVEL / 100.0); // Вычисление комиссии
    }

    // Метод для расчета прибыли
    private double calcProfit() {
        // Если ордеров меньше двух, возврат 0, так как нет данных для расчета
        if (orders.size() < 2) return 0;

        // Инициализация переменных для хранения суммарного объема и стоимости покупок и продаж
        double totalBuyVolume = 0;
        double totalSellVolume = 0;
        double totalBuyCost = 0;
        double totalSellCost = 0;

        // Проход по всем ордерам в позиции
        for (Order order : orders) {
            if (side == TYPE.LONG) { // Если позиция длинная (LONG)
                if (order.direction.equals("buy")) {
                    // Увеличиваем суммарный объем и стоимость покупок
                    totalBuyVolume += order.volume;
                    totalBuyCost += order.executionPrice * order.volume;
                } else if (order.direction.equals("sell")) {
                    // Увеличиваем суммарный объем и стоимость продаж
                    totalSellVolume += order.volume;
                    totalSellCost += order.executionPrice * order.volume;
                }
            } else if (side == TYPE.SHORT) { // Если позиция короткая (SHORT)
                if (order.direction.equals("sell")) {
                    // Увеличиваем суммарный объем и стоимость продаж
                    totalSellVolume += order.volume;
                    totalSellCost += order.executionPrice * order.volume;
                } else if (order.direction.equals("buy")) {
                    // Увеличиваем суммарный объем и стоимость покупок
                    totalBuyVolume += order.volume;
                    totalBuyCost += order.executionPrice * order.volume;
                }
            }
        }

        // Расчет средней цены покупки и продажи
        double averageBuyPrice = totalBuyCost / totalBuyVolume;
        double averageSellPrice = totalSellCost / totalSellVolume;

        // Рассчет прибыли в зависимости от типа позиции (LONG или SHORT)
        if (side == TYPE.LONG) {
            // Для длинной позиции прибыль = (средняя цена продажи - средняя цена покупки) * объем * плечо
            return (averageSellPrice - averageBuyPrice) * totalBuyVolume * leverage;
        } else {
            // Для короткой позиции прибыль = (средняя цена покупки - средняя цена продажи) * объем * плечо
            return (averageBuyPrice - averageSellPrice) * totalSellVolume * leverage;
        }
    }

    // Метод для расчета прибыли в процентах
    private double calcProfitPer() {
        // Инициализация переменной для хранения начальной инвестиции
        double initialInvestment = 0;

        // Проход по всем ордерам в позиции
        for (Order order : orders) {
            if (side == TYPE.LONG && order.direction.equals("buy")) {
                // Для длинной позиции начальная инвестиция увеличивается на сумму всех покупок
                initialInvestment += order.executionPrice * order.volume;
            } else if (side == TYPE.SHORT && order.direction.equals("sell")) {
                // Для короткой позиции начальная инвестиция увеличивается на сумму всех продаж
                initialInvestment += order.executionPrice * order.volume;
            }
        }

        // Рассчет прибыли в процентах = (прибыль / начальная инвестиция) * 100
        return (profit / initialInvestment) * 100;
    }

    @Override
    public String toString() {
        return String.format(
                "ID: %d%n" +
                        "Торговая пара: %s%n" +
                        "Время открытия позиции: %d%n" +
                        "Время закрытия позиции: %d%n" +
                        "Направление позиции: %s%n" +
                        "Плечо: %d%n" +
                        "Комиссия в процентах: %.2f%%%n" +
                        "Общая комиссия: %.5f%n" +
                        "Прибыль: %.2f%n" +
                        "Прибыль в процентах: %.2f%%%n" +
                        "Статус: %s%n" +
                        "Ордера: %s",
                id,
                tradingPair,
                startPosition,
                endPosition,
                side,
                leverage,
                commissionLEVEL,
                commission,
                profit,
                profitPer,
                status,
                orders
        );
    }
}