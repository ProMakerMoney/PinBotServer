package com.zmn.pinbotserver.strategyTesting;

import com.zmn.pinbotserver.historicalData.model.candle.Candle;
import com.zmn.pinbotserver.historicalData.model.coin.Coin;
import com.zmn.pinbotserver.strategyTesting.model.strategy.StrategyParams;
import com.zmn.pinbotserver.strategyTesting.model.strategy.StrategyStats;
import com.zmn.pinbotserver.strategyTesting.service.StrategyTestingService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.*;

public class GeneticAlgorithmStrategyTester {

    private final List<Candle> candles;
    private final StrategyTestingService strategyTestingService;
    private final Coin coin;

    static final int POPULATION_SIZE = 5000;
    static final int GENERATIONS = 100;
    static double MUTATION_RATE = 0.1;
    static final double CROSSOVER_RATE = 0.9;
    static final double INITIAL_DEPOSIT = 10.0;

    public GeneticAlgorithmStrategyTester(List<Candle> candles, StrategyTestingService strategyTestingService, Coin coin) {
        this.candles = candles;
        this.strategyTestingService = strategyTestingService;
        this.coin = coin;
    }

    public StrategyParams run() throws InterruptedException {
        System.out.println("Начало - " + candles.get(0).getTimeAsLocalDateTime());
        System.out.println("Конец - " + candles.get(candles.size() - 1).getTimeAsLocalDateTime());

        List<Individual> population = initializePopulation(POPULATION_SIZE);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int generation = 0; generation < GENERATIONS; generation++) {
            evaluatePopulation(population, candles, executor);

            List<Individual> newPopulation = new ArrayList<>();
            int eliteCount = (int) (POPULATION_SIZE * 0.05);
            newPopulation.addAll(population.subList(0, eliteCount));

            for (int i = eliteCount; i < POPULATION_SIZE; i += 2) {
                Individual parent1 = selectParent(population);
                Individual parent2 = selectParent(population);

                List<Individual> offspring = crossover(parent1, parent2);
                newPopulation.add(mutate(offspring.get(0), generation));
                newPopulation.add(mutate(offspring.get(1), generation));
            }

            population = newPopulation;
            Individual bestIndividual = getBestIndividual(population);
            System.out.printf("Поколение %d: лучшая пригодность = %.2f, параметры: CCI=%d, EMA=%d, LEV=%d, RATIO=%.2f, MAX_ORDERS=%d%n",
                    generation, bestIndividual.fitness, bestIndividual.cci, bestIndividual.ema, bestIndividual.leverage, bestIndividual.ratio, bestIndividual.maxOrders);

            if (generation % 50 == 0 && generation != 0) {
                MUTATION_RATE = Math.max(0.01, MUTATION_RATE - 0.01);
            }
        }

        executor.shutdown();
        Individual best = getBestIndividual(population);
        System.out.println("Лучшее решение:");
        System.out.println("CCI: " + best.cci);
        System.out.println("EMA: " + best.ema);
        System.out.println("LEV: " + best.leverage);
        System.out.println("RATIO: " + best.ratio);
        System.out.println("MAX_ORDERS: " + best.maxOrders);
        System.out.println("Общая прибыль: " + best.totalProfit);
        System.out.println("Количество сделок: " + best.totalTrades);
        System.out.println("Процент прибыльных сделок: " + best.percentageProfitTrades);

        return new StrategyParams(coin.getCoinName(), coin.getTimeframe(), best.leverage, best.maxOrders, best.cci, best.ema, best.ratio);
    }

    class Individual {
        int cci;
        int ema;
        int leverage;
        double ratio;
        int maxOrders;
        double fitness;
        double percentageProfitTrades;
        int totalTrades;
        double totalProfit;

        public Individual(int cci, int ema, int leverage, double ratio, int maxOrders) {
            this.cci = cci;
            this.ema = ema;
            this.leverage = leverage;
            this.ratio = ratio;
            this.maxOrders = maxOrders;
        }

        public double getFitness() {
            return fitness;
        }

        public boolean isValid() {
            return percentageProfitTrades > 56 && totalTrades > 10 && totalProfit > 0;
        }
    }

    List<Individual> initializePopulation(int size) {
        List<Individual> population = new ArrayList<>();
        double currentPrice = candles.get(candles.size() - 1).getClose();
        for (int i = 0; i < size; i++) {
            int maxOrders = ThreadLocalRandom.current().nextInt(1, 11);
            int leverageMin = calculateMinLeverage(currentPrice, coin.getMinTradingQty(), INITIAL_DEPOSIT, maxOrders);
            int leverage = ThreadLocalRandom.current().nextInt(leverageMin, 26);
            population.add(new Individual(
                    ThreadLocalRandom.current().nextInt(1, 301),
                    ThreadLocalRandom.current().nextInt(1, 301),
                    leverage,
                    ThreadLocalRandom.current().nextDouble(0.5, 4.1),
                    maxOrders
            ));
        }
        return population;
    }

    void evaluatePopulation(List<Individual> population, List<Candle> candles, ExecutorService executor) throws InterruptedException {
        List<Future<?>> futures = new ArrayList<>();

        for (Individual individual : population) {
            Future<?> future = executor.submit(() -> {
                try {
                    StrategyParams params = new StrategyParams(
                            coin.getCoinName(),
                            coin.getTimeframe(),
                            individual.leverage,
                            individual.maxOrders,
                            individual.cci,
                            individual.ema,
                            individual.ratio
                    );
                    StrategyStats stats = strategyTestingService.testStrategy(coin, params);

                    individual.totalProfit = stats.getProfitInDollars();
                    individual.totalTrades = stats.getTradeCount();
                    individual.percentageProfitTrades = stats.getProfitableTradePercentage();

                    if (individual.isValid()) {
                        individual.fitness = individual.totalProfit;
                    } else {
                        individual.fitness = 0;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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

        int cci = ThreadLocalRandom.current().nextBoolean() ? parent1.cci : parent2.cci;
        int ema = ThreadLocalRandom.current().nextBoolean() ? parent1.ema : parent2.ema;
        int leverage = ThreadLocalRandom.current().nextBoolean() ? parent1.leverage : parent2.leverage;
        double ratio = ThreadLocalRandom.current().nextBoolean() ? parent1.ratio : parent2.ratio;
        int maxOrders = ThreadLocalRandom.current().nextBoolean() ? parent1.maxOrders : parent2.maxOrders;

        return Arrays.asList(
                new Individual(cci, ema, leverage, ratio, maxOrders),
                new Individual(parent1.cci == cci ? parent2.cci : parent1.cci,
                        parent1.ema == ema ? parent2.ema : parent1.ema,
                        parent1.leverage == leverage ? parent2.leverage : parent1.leverage,
                        parent1.ratio == ratio ? parent2.ratio : parent1.ratio,
                        parent1.maxOrders == maxOrders ? parent2.maxOrders : parent1.maxOrders)
        );
    }

    Individual mutate(Individual individual, int generation) {
        if (ThreadLocalRandom.current().nextDouble() > MUTATION_RATE) {
            return individual;
        }

        int cci = individual.cci;
        if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
            cci = ThreadLocalRandom.current().nextInt(1, 301);
        }

        int ema = individual.ema;
        if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
            ema = ThreadLocalRandom.current().nextInt(1, 301);
        }

        int leverage = individual.leverage;
        if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
            leverage = Math.max(calculateMinLeverage(candles.get(candles.size() - 1).getClose(), coin.getMinTradingQty(), INITIAL_DEPOSIT, individual.maxOrders),
                    ThreadLocalRandom.current().nextInt(2, 26));
        }

        double ratio = individual.ratio;
        if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
            ratio = ThreadLocalRandom.current().nextDouble(0.5, 4.1);
        }

        int maxOrders = individual.maxOrders;
        if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
            maxOrders = ThreadLocalRandom.current().nextInt(1, 11);
        }

        return new Individual(cci, ema, leverage, ratio, maxOrders);
    }

    Individual getBestIndividual(List<Individual> population) {
        return population.stream().max(Comparator.comparingDouble(Individual::getFitness)).orElseThrow(NoSuchElementException::new);
    }

    private int calculateMinLeverage(double currentPrice, double minTradingQty, double initialDeposit, int maxOpenOrders) {
        double costPerTrade = currentPrice * minTradingQty;
        double requiredFunds = costPerTrade * maxOpenOrders;
        return (int) Math.ceil(requiredFunds / initialDeposit);
    }
}