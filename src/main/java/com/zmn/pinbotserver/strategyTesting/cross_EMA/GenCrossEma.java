package com.zmn.pinbotserver.strategyTesting.cross_EMA;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.model.strategy.StrategyStats;
import com.zmn.pinbotserver.service.strategyTesting.StrategyTestingService;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

import java.util.*;


public class GenCrossEma {
    private final List<Candle> candles;
    private final StrategyTestingService strategyTestingService;
    private final Coin coin;

    static final int POPULATION_SIZE = 1000;
    static final int GENERATIONS = 100;
    static double MUTATION_RATE = 0.2;
    static final double CROSSOVER_RATE = 0.8;

    public GenCrossEma(List<Candle> candles, StrategyTestingService strategyTestingService, Coin coin) {
        this.candles = candles;
        this.strategyTestingService = strategyTestingService;
        this.coin = coin;
    }

    public CrossEmaParams run() throws InterruptedException {
        System.out.println("Начало - " + candles.get(0).getTimeAsLocalDateTime());
        System.out.println("Конец - " + candles.get(candles.size() - 1).getTimeAsLocalDateTime());

        List<Individual> population = initializePopulation();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int generation = 0; generation < GENERATIONS; generation++) {
            evaluatePopulation(population, candles, executor);

            int eliteCount = (int) (POPULATION_SIZE * 0.05);
            List<Individual> newPopulation = new ArrayList<>(population.subList(0, eliteCount));

            for (int i = eliteCount; i < POPULATION_SIZE; i += 2) {
                Individual parent1 = selectParent(population);
                Individual parent2 = selectParent(population);

                List<Individual> offspring = crossover(parent1, parent2);
                newPopulation.add(mutate(offspring.get(0), generation));
                newPopulation.add(mutate(offspring.get(1), generation));
            }

            population = newPopulation;
            Individual bestIndividual = getBestIndividual(population);
            System.out.printf("Поколение %d: лучшая пригодность = %.2f, параметры: LEV=%d, Fast_EMA=%d, Slow_EMA=%d, TP=%.2f, SL=%.2f%n",
                    generation, bestIndividual.fitness, bestIndividual.leverage, bestIndividual.fastEmaLength, bestIndividual.slowEmaLength,
                    bestIndividual.takeProfit, bestIndividual.stopLoss);

            if (generation % 50 == 0 && generation != 0) {
                MUTATION_RATE = Math.max(0.01, MUTATION_RATE - 0.01);
            }
        }

        executor.shutdown();
        Individual best = getBestIndividual(population);
        System.out.println("Лучшее решение:");
        System.out.println("LEV: " + best.leverage);
        System.out.println("Fast_EMA: " + best.fastEmaLength);
        System.out.println("Slow_EMA: " + best.slowEmaLength);
        System.out.println("TP: " + best.takeProfit);
        System.out.println("SL: " + best.stopLoss);
        System.out.println("Общая прибыль: " + best.totalProfit);
        System.out.println("Количество сделок: " + best.totalTrades);
        System.out.println("Процент прибыльных сделок: " + best.percentageProfitTrades);

        return new CrossEmaParams(
                coin.getCoinName(),
                coin.getTimeframe(),
                best.leverage,
                best.fastEmaLength,
                best.slowEmaLength,
                best.takeProfit,
                best.stopLoss
        );
    }

    static class Individual {
        int leverage;
        int fastEmaLength;
        int slowEmaLength;
        double takeProfit;
        double stopLoss;
        @Getter
        double fitness;
        double percentageProfitTrades;
        int totalTrades;
        double totalProfit;

        public Individual(int leverage, int fastEmaLength, int slowEmaLength, double takeProfit, double stopLoss) {
            this.leverage = leverage;
            this.fastEmaLength = fastEmaLength;
            this.slowEmaLength = slowEmaLength;
            this.takeProfit = takeProfit;
            this.stopLoss = stopLoss;
        }

        public boolean isValid() {
            return percentageProfitTrades > 0 && totalTrades > 0 && totalProfit > 0;
        }
    }

    List<Individual> initializePopulation() {
        List<Individual> population = new ArrayList<>();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            int fastEmaLength = ThreadLocalRandom.current().nextInt(5, 301); // Изменен диапазон: 5 до 300 включительно
            int slowEmaLength = ThreadLocalRandom.current().nextInt(fastEmaLength + 1, 401); // Изменен диапазон: fastEmaLength + 1 до 400 включительно

            double takeProfit = 1.0 + ThreadLocalRandom.current().nextDouble(300.0 - 1.0); // Изменен диапазон: 1 до 300
            double stopLoss = 1.0 + ThreadLocalRandom.current().nextDouble(100.0 - 1.0); // Изменен диапазон: 1 до 100

            population.add(new Individual(
                    ThreadLocalRandom.current().nextInt(1, 16),
                    fastEmaLength,
                    slowEmaLength,
                    takeProfit,
                    stopLoss
            ));
        }
        return population;
    }

    void evaluatePopulation(List<Individual> population, List<Candle> candles, ExecutorService executor) throws InterruptedException {
        List<Future<?>> futures = new ArrayList<>();

        for (Individual individual : population) {
            Future<?> future = executor.submit(() -> {
                try {
                    if (coin == null || strategyTestingService == null) {
                        throw new IllegalStateException("Coin or StrategyTestingService is not initialized.");
                    }

                    CrossEmaParams params = new CrossEmaParams(
                            coin.getCoinName(),
                            coin.getTimeframe(),
                            individual.leverage,
                            individual.fastEmaLength,
                            individual.slowEmaLength,
                            individual.takeProfit,
                            individual.stopLoss
                    );

                    StrategyStats stats = strategyTestingService.testCrossEma(coin, params, candles);

                    if (stats == null) {
                        throw new IllegalStateException("StrategyStats returned null.");
                    }

                    individual.totalProfit = stats.getProfitInDollars();
                    individual.totalTrades = stats.getTradeCount();
                    individual.percentageProfitTrades = stats.getProfitableTradePercentage();

                    // Убедитесь, что все значения корректны
                    if (individual.isValid()) {
                        individual.fitness = individual.totalProfit;
                    } else {
                        individual.fitness = 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    individual.fitness = 0;  // Сбросьте fitness в случае ошибки
                }
            });
            futures.add(future);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        population.sort(Comparator.comparingDouble(Individual::getFitness).reversed());
    }

    Individual selectParent(List<Individual> population) {
        return population.get(ThreadLocalRandom.current().nextInt(0, POPULATION_SIZE / 2));
    }

    List<Individual> crossover(Individual parent1, Individual parent2) {
        if (ThreadLocalRandom.current().nextDouble() > CROSSOVER_RATE) {
            return Arrays.asList(parent1, parent2);
        }

        int leverage = ThreadLocalRandom.current().nextBoolean() ? parent1.leverage : parent2.leverage;
        int fastEmaLength = ThreadLocalRandom.current().nextBoolean() ? parent1.fastEmaLength : parent2.fastEmaLength;
        int slowEmaLength = ThreadLocalRandom.current().nextBoolean() ? parent1.slowEmaLength : parent2.slowEmaLength;
        double takeProfit = ThreadLocalRandom.current().nextBoolean() ? parent1.takeProfit : parent2.takeProfit;
        double stopLoss = ThreadLocalRandom.current().nextBoolean() ? parent1.stopLoss : parent2.stopLoss;

        return Arrays.asList(
                new Individual(leverage, fastEmaLength, slowEmaLength, takeProfit, stopLoss),
                new Individual(
                        parent1.leverage == leverage ? parent2.leverage : parent1.leverage,
                        parent1.fastEmaLength == fastEmaLength ? parent2.fastEmaLength : parent1.fastEmaLength,
                        parent1.slowEmaLength == slowEmaLength ? parent2.slowEmaLength : parent1.slowEmaLength,
                        parent1.takeProfit == takeProfit ? parent2.takeProfit : parent1.takeProfit,
                        parent1.stopLoss == stopLoss ? parent2.stopLoss : parent1.stopLoss)
        );
    }

    Individual mutate(Individual individual, int generation) {
        int fastEmaLength = ThreadLocalRandom.current().nextDouble() < MUTATION_RATE ?
                ThreadLocalRandom.current().nextInt(5, 301) : individual.fastEmaLength;

        int slowEmaLength = ThreadLocalRandom.current().nextDouble() < MUTATION_RATE ?
                ThreadLocalRandom.current().nextInt(fastEmaLength + 1, 401) : individual.slowEmaLength;

        double takeProfit = ThreadLocalRandom.current().nextDouble() < MUTATION_RATE ?
                1.0 + ThreadLocalRandom.current().nextDouble(300.0 - 1.0) : individual.takeProfit;

        double stopLoss = ThreadLocalRandom.current().nextDouble() < MUTATION_RATE ?
                1.0 + ThreadLocalRandom.current().nextDouble(100.0 - 1.0) : individual.stopLoss;

        return new Individual(
                ThreadLocalRandom.current().nextDouble() < MUTATION_RATE ? ThreadLocalRandom.current().nextInt(1, 16) : individual.leverage,
                fastEmaLength,
                slowEmaLength,
                takeProfit,
                stopLoss
        );
    }

    Individual getBestIndividual(List<Individual> population) {
        return population.stream().max(Comparator.comparingDouble(Individual::getFitness)).orElseThrow(NoSuchElementException::new);
    }
}
