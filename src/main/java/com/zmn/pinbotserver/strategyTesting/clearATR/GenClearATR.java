package com.zmn.pinbotserver.strategyTesting.clearATR;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.model.strategy.StrategyParamsClearATR;
import com.zmn.pinbotserver.model.strategy.StrategyStats;
import com.zmn.pinbotserver.service.strategyTesting.StrategyTestingService;
import lombok.Getter;


import java.util.*;
import java.util.concurrent.*;

public class GenClearATR {

    private final List<Candle> candles;
    private final StrategyTestingService strategyTestingService;
    private final Coin coin;

    static final int POPULATION_SIZE = 1000;
    static final int GENERATIONS = 100;
    static double MUTATION_RATE = 0.2;
    static final double CROSSOVER_RATE = 0.8;

    public GenClearATR(List<Candle> candles, StrategyTestingService strategyTestingService, Coin coin) {
        this.candles = candles;
        this.strategyTestingService = strategyTestingService;
        this.coin = coin;
    }

    public StrategyParamsClearATR run() throws InterruptedException {
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
            System.out.printf("Поколение %d: лучшая пригодность = %.2f, параметры: LEV=%d, ATR_Length=%d, Coeff=%.2f%n",
                    generation, bestIndividual.fitness, bestIndividual.leverage, bestIndividual.atrLength, bestIndividual.coeff);

            if (generation % 50 == 0 && generation != 0) {
                MUTATION_RATE = Math.max(0.01, MUTATION_RATE - 0.01);
            }
        }

        executor.shutdown();
        Individual best = getBestIndividual(population);
        System.out.println("Лучшее решение:");
        System.out.println("LEV: " + best.leverage);
        System.out.println("ATR_LENGTH: " + best.atrLength);
        System.out.println("Coeff: " + best.coeff);
        System.out.println("Общая прибыль: " + best.totalProfit);
        System.out.println("Количество сделок: " + best.totalTrades);
        System.out.println("Процент прибыльных сделок: " + best.percentageProfitTrades);

        return new StrategyParamsClearATR(coin.getCoinName(), coin.getTimeframe(), best.leverage, best.atrLength, best.coeff);
    }

    static class Individual {
        int leverage;
        int atrLength;
        double coeff;
        @Getter
        double fitness;
        double percentageProfitTrades;
        int totalTrades;
        double totalProfit;

        public Individual(int leverage, int atrLength, double coeff) {
            this.leverage = leverage;
            this.atrLength = atrLength;
            this.coeff = coeff;
        }

        public boolean isValid() {
            return percentageProfitTrades > 0 && totalTrades > 0 && totalProfit > 0;
        }
    }

    List<Individual> initializePopulation() {
        List<Individual> population = new ArrayList<>();

        double minCoeff = 0.1;
        double maxCoeff = 10.0;
        double step = 0.1;
        int range = (int) ((maxCoeff - minCoeff) / step + 1);


        for (int i = 0; i < POPULATION_SIZE; i++) {
            double coeff = minCoeff + ThreadLocalRandom.current().nextInt(range) * step;
            population.add(new Individual(
                    ThreadLocalRandom.current().nextInt(1, 16),
                    ThreadLocalRandom.current().nextInt(1, 200),
                    coeff
            ));
        }
        return population;
    }

    void evaluatePopulation(List<Individual> population, List<Candle> candles, ExecutorService executor) throws InterruptedException {
        List<Future<?>> futures = new ArrayList<>();

        for (Individual individual : population) {
            Future<?> future = executor.submit(() -> {
                StrategyParamsClearATR params = new StrategyParamsClearATR(
                        coin.getCoinName(),
                        coin.getTimeframe(),
                        individual.leverage,
                        individual.atrLength,
                        individual.coeff
                );
                StrategyStats stats = strategyTestingService.testStrategyClearATR(coin, params, candles);

                individual.totalProfit = stats.getProfitInDollars();
                individual.totalTrades = stats.getTradeCount();
                individual.percentageProfitTrades = stats.getProfitableTradePercentage();

                if (individual.isValid()) {
                    individual.fitness = individual.totalProfit;
                } else {
                    individual.fitness = 0;
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
        int atrLength = ThreadLocalRandom.current().nextBoolean() ? parent1.atrLength : parent2.atrLength;
        double coeff = ThreadLocalRandom.current().nextBoolean() ? parent1.coeff : parent2.coeff;

        return Arrays.asList(
                new Individual(leverage, atrLength, coeff),
                new Individual(
                        parent1.leverage == leverage ? parent2.leverage : parent1.leverage,
                        parent1.atrLength == atrLength ? parent2.atrLength : parent1.atrLength,
                        parent1.coeff == coeff ? parent2.coeff : parent1.coeff)
        );
    }

    Individual mutate(Individual individual, int generation) {

        double minCoeff = 0.1;
        double maxCoeff = 10.0;
        double step = 0.1;
        int range = (int) ((maxCoeff - minCoeff) / step + 1);

        if (ThreadLocalRandom.current().nextDouble() > MUTATION_RATE) {
            return individual;
        }


        int leverage = ThreadLocalRandom.current().nextDouble() < MUTATION_RATE ?
                ThreadLocalRandom.current().nextInt(1, 16) : individual.leverage;

        int atrLength = ThreadLocalRandom.current().nextDouble() < MUTATION_RATE ?
                ThreadLocalRandom.current().nextInt(1, 200) : individual.atrLength;

        double coeff = ThreadLocalRandom.current().nextDouble() < MUTATION_RATE ?
                minCoeff + ThreadLocalRandom.current().nextInt(range) * step : individual.coeff;

        return new Individual(leverage, atrLength, coeff);
    }

    Individual getBestIndividual(List<Individual> population) {
        return population.stream().max(Comparator.comparingDouble(Individual::getFitness)).orElseThrow(NoSuchElementException::new);
    }

}
