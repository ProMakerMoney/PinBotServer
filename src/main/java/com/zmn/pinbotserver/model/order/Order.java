package com.zmn.pinbotserver.model.order;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class Order {
    private static int nextId = 1;
    @Getter
    @Setter
    int id;
    @Setter
    @Getter
    TYPE type;
    boolean isClosed = false;
    @Setter
    @Getter
    double enterPrice;
    @Setter
    @Getter
    double exitPrice;
    @Setter
    @Getter
    LocalDateTime enterTime;
    @Getter
    @Setter
    LocalDateTime exitTime;
    @Getter
    @Setter
    double margin;
    @Getter
    @Setter
    int leverage;
    @Getter
    @Setter
    double commission;
    

    public Order(TYPE type, double enterPrice, LocalDateTime enterTime, double margin, int leverage, double commission) {
        this.id = nextId++;
        this.type = type;
        this.enterPrice = enterPrice;
        this.enterTime = enterTime;
        this.margin = margin;
        this.leverage = leverage;
        this.commission = commission;
    }

    //Закрытие сделки
    public void closeOrder(double exitPrice, LocalDateTime exitTime) {
        this.exitPrice = exitPrice;
        this.exitTime = exitTime;
        this.isClosed = true;
    }


    // Method to calculate profit
    public double calculateProfit() {
        if (!isClosed) {
            throw new IllegalStateException("Ордер должен быть закрыт для расчета профита!");
        }

        // Расчёт размера позиции в единицах актива
        double positionSize = (margin * leverage) / enterPrice;

        double profit = 0;
        if (type == TYPE.LONG) {
            profit = (exitPrice - enterPrice) * positionSize;
        } else if (type == TYPE.SHORT) {
            profit = (enterPrice - exitPrice) * positionSize;
        }

        // Учет комиссии
        double entryCommission = enterPrice * positionSize * (commission / 100);
        double exitCommission = exitPrice * positionSize * (commission / 100);
        double totalCommission = entryCommission + exitCommission;

        profit -= totalCommission; // Вычитаем общую комиссию из прибыли

        return profit;
    }

    // Метод для расчета процентного чистого движения
    public double calculateNetPercentageMovement() {
        if (!isClosed) {
            throw new IllegalStateException("Ордер должен быть закрыт для расчета процентного движения!");
        }

        double percentageMovement = 0;
        if (type == TYPE.LONG) {
            percentageMovement = ((exitPrice - enterPrice) / enterPrice) * 100;
        } else if (type == TYPE.SHORT) {
            percentageMovement = ((enterPrice - exitPrice) / enterPrice) * 100;
        }

        return percentageMovement;
    }


    @Override
    public String toString() {
        String profitStr = isClosed ? String.format("Profit: %s", calculateProfit()) : "Order is still open";
        return String.format("Order{id=%d, ePrice=%.2f, xPrice=%.2f, enter=%s, exit=%s, type=%s, margin=%.2f, %s}",
                id, enterPrice, exitPrice, enterTime, exitTime, type, margin, profitStr);
    }


    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

}
