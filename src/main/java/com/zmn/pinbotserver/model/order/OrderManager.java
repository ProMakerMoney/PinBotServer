package com.zmn.pinbotserver.model.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OrderManager {
    List<Order> orders = new ArrayList<Order>();

    public List<Order> getClosedOrders() {
        return orders.stream()
                .filter(Order::isClosed)
                .collect(Collectors.toList());
    }

    // Метод для создания и открытия нового ордера
    public void openOrder(TYPE type, double enterPrice, LocalDateTime enterTime, double margin, double commission, int leverage) {

        // Создаём новый ордер с заданными параметрами и добавляем его в список
        Order newOrder = new Order(type, enterPrice, enterTime, margin, leverage, commission);
        orders.add(newOrder);
    }

    // Метод для закрытия существующего ордера
    public double closeOrder(int orderId, double exitPrice, LocalDateTime exitTime) {
        for (Order order : orders) {
            if (order.getId() == orderId && !order.isClosed()) {
                order.closeOrder(exitPrice, exitTime);
                return order.calculateProfit(); // Возвращаем прибыль от закрытия ордера
            }
        }
        throw new IllegalArgumentException("Ордер не найден или уже закрыт.");
    }

    // Опционально: Метод для получения списка всех ордеров
    public List<Order> getOrders() {
        return orders;
    }

    // Метод для закрытия всех ордеров определенного типа
    public double closeAllOrdersOfType(TYPE type, double exitPrice, LocalDateTime exitTime) {
        double totalProfit = 0;
        for (Order order : orders) {
            if (order.getType() == type && !order.isClosed()) {
                order.closeOrder(exitPrice, exitTime);
                double profit = order.calculateProfit();
                totalProfit += profit;
                //System.out.println("Закрыт ордер ID: " + order.getId() + ", тип: " + type + ", прибыль: " + profit);
            }
        }
        return totalProfit;
    }

    // Метод для подсчета активных ордеров определенного типа
    public int countActiveOrdersOfType(TYPE type) {
        int count = 0;
        for (Order order : orders) {
            if (order.getType() == type && !order.isClosed()) {
                count++;
            }
        }
        return count;
    }

    // Метод для подсчета положительных сделок
    public int countPositiveTrades() {
        int count = 0;
        for (Order order : orders) {
            if (order.isClosed() && order.calculateProfit() > 0) {
                count++;
            }
        }
        return count;
    }

    // Метод для подсчета отрицательных сделок
    public int countNegativeTrades() {
        int count = 0;
        for (Order order : orders) {
            if (order.isClosed() && order.calculateProfit() < 0) {
                count++;
            }
        }
        return count;
    }

    // Метод для подсчета процента прибыльных сделок
    public double calculateProfitableTradesPercentage() {
        int positiveCount = countPositiveTrades();
        int totalClosed = countClosedTrades();
        if (totalClosed == 0) return 0; // Избегаем деления на ноль
        return (double) positiveCount / totalClosed * 100;
    }

    // Метод для подсчета общего количества сделок
    public int countTotalTrades() {
        return orders.size();
    }

    // Метод для подсчета общего количества закрытых сделок
    private int countClosedTrades() {
        int count = 0;
        for (Order order : orders) {
            if (order.isClosed()) {
                count++;
            }
        }
        return count;
    }

    public double calculateTotalProfit() {
        double totalProfit = 0;
        for (Order order : orders) {
            if (order.isClosed()) {
                totalProfit += order.calculateProfit();
            }
        }
        return totalProfit;
    }

    public double calculateAverageEntryPrice() {
        double totalEntryPrice = 0;
        int count = 0;
        for (Order order : orders) {
            if (!order.isClosed()) {
                totalEntryPrice += order.getEnterPrice();
                count++;
            }
        }
        if (count == 0) {
            return 0; // Избегаем деления на ноль
        }
        return totalEntryPrice / count;
    }
}
