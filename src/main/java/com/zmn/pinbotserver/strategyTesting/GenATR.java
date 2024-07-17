package com.zmn.pinbotserver.strategyTesting;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.model.strategy.StrategyParamsATR;
import com.zmn.pinbotserver.model.strategy.StrategyStats;
import com.zmn.pinbotserver.service.strategyTesting.StrategyTestingService;
import lombok.Getter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class GenATR {

    private final List<Candle> candles;
    private final StrategyTestingService strategyTestingService;
    private final Coin coin;

    static final int POPULATION_SIZE = 10000;
    static final int GENERATIONS = 100;
    static double MUTATION_RATE = 0.5;
    static final double CROSSOVER_RATE = 0.9;

    public GenATR(List<Candle> candles, StrategyTestingService strategyTestingService, Coin coin) {
        this.candles = candles;
        this.strategyTestingService = strategyTestingService;
        this.coin = coin;
    }

    public StrategyParamsATR run() throws InterruptedException {
        System.out.println("Начало - " + candles.getFirst().getTimeAsLocalDateTime());
        System.out.println("Конец - " + candles.getLast().getTimeAsLocalDateTime());

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
            System.out.printf("Поколение %d: лучшая пригодность = %.2f, параметры: CCI=%d, EMA=%d, LEV=%d, RATIO=%.2f, MAX_ORDERS=%d, ATR_Length=%d, Coeff=%.2f%n",
                    generation, bestIndividual.fitness, bestIndividual.cci, bestIndividual.ema, bestIndividual.leverage, bestIndividual.ratio, bestIndividual.maxOrders, bestIndividual.atrLength, bestIndividual.coeff);

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
        System.out.println("ATR_LENGTH: " + best.atrLength);
        System.out.println("Coeff: " + best.coeff);
        System.out.println("Общая прибыль: " + best.totalProfit);
        System.out.println("Количество сделок: " + best.totalTrades);
        System.out.println("Процент прибыльных сделок: " + best.percentageProfitTrades);

        return new StrategyParamsATR(coin.getCoinName(), coin.getTimeframe(), best.leverage, best.maxOrders, best.cci, best.ema, best.ratio, best.atrLength, best.coeff);
    }

    static class Individual {
        int cci;
        int ema;
        int leverage;
        double ratio;
        int maxOrders;
        int atrLength;
        double coeff;
        @Getter
        double fitness;
        double percentageProfitTrades;
        int totalTrades;
        double totalProfit;

        public Individual(int cci, int ema, int leverage, double ratio, int maxOrders, int atrLength, double coeff) {
            this.cci = cci;
            this.ema = ema;
            this.leverage = leverage;
            this.ratio = ratio;
            this.maxOrders = maxOrders;
            this.atrLength = atrLength;
            this.coeff = coeff;
        }

        public boolean isValid() {
            return percentageProfitTrades > 65 && totalTrades > 50 && totalProfit > 0;
        }
    }

    List<Individual> initializePopulation() {
        List<Individual> population = new ArrayList<>();
        for (int i = 0; i < GenATR.POPULATION_SIZE; i++) {
            population.add(new Individual(
                    ThreadLocalRandom.current().nextInt(1, 301),
                    ThreadLocalRandom.current().nextInt(1, 301),
                    ThreadLocalRandom.current().nextInt(1, 16),
                    ThreadLocalRandom.current().nextDouble(0.7, 4.1),
                    ThreadLocalRandom.current().nextInt(1, 11),
                    ThreadLocalRandom.current().nextInt(1, 200),
                    ThreadLocalRandom.current().nextDouble(0.1, 10.0)
            ));
        }
        return population;
    }

    void evaluatePopulation(List<Individual> population, List<Candle> candles, ExecutorService executor) throws InterruptedException {
        List<Future<?>> futures = new ArrayList<>();

        for (Individual individual : population) {
            Future<?> future = executor.submit(() -> {
                try {
                    StrategyParamsATR params = new StrategyParamsATR(
                            coin.getCoinName(),
                            coin.getTimeframe(),
                            individual.leverage,
                            individual.maxOrders,
                            individual.cci,
                            individual.ema,
                            individual.ratio,
                            individual.atrLength,
                            individual.coeff
                    );
                    StrategyStats stats = strategyTestingService.testStrategyATR(coin, params, candles);

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
        int atrLength = ThreadLocalRandom.current().nextBoolean() ? parent1.atrLength : parent2.atrLength;
        double coeff = ThreadLocalRandom.current().nextBoolean() ? parent1.coeff : parent2.coeff;

        return Arrays.asList(
                new Individual(cci, ema, leverage, ratio, maxOrders, atrLength, coeff),
                new Individual(parent1.cci == cci ? parent2.cci : parent1.cci,
                        parent1.ema == ema ? parent2.ema : parent1.ema,
                        parent1.leverage == leverage ? parent2.leverage : parent1.leverage,
                        parent1.ratio == ratio ? parent2.ratio : parent1.ratio,
                        parent1.maxOrders == maxOrders ? parent2.maxOrders : parent1.maxOrders,
                        parent1.atrLength == atrLength ? parent2.atrLength : parent1.atrLength,
                        parent1.coeff == coeff ? parent2.coeff : parent1.coeff)
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
            leverage = ThreadLocalRandom.current().nextInt(1, 16);
        }

        double ratio = individual.ratio;
        if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
            ratio = ThreadLocalRandom.current().nextDouble(0.7, 4.1);
        }


        int maxOrders = individual.maxOrders;
        if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
            maxOrders = ThreadLocalRandom.current().nextInt(1, 11);
        }

        int atrLength = individual.atrLength;
        if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
            maxOrders = ThreadLocalRandom.current().nextInt(1, 200);
        }

        double coeff = individual.coeff;
        if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
            ratio = ThreadLocalRandom.current().nextDouble(0.1, 10.0);
        }

        return new Individual(cci, ema, leverage, ratio, maxOrders, atrLength, coeff);
    }

    Individual getBestIndividual(List<Individual> population) {
        return population.stream().max(Comparator.comparingDouble(Individual::getFitness)).orElseThrow(NoSuchElementException::new);
    }

}