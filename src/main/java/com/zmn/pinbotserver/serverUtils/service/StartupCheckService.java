package com.zmn.pinbotserver.serverUtils.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service // Аннотация, обозначающая, что данный класс является сервисом в Spring
public class StartupCheckService {

    private final String exchangeApiUrl = "https://api.bybit.com/v5/market/time"; // URL для проверки соединения с биржей
    private final String databaseUrl = "jdbc:h2:file:./db/pinbot"; // URL для подключения к базе данных

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Autowired // Автоматическая инъекция зависимостей
    public StartupCheckService(RestTemplate restTemplate, JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct // Аннотация, обозначающая, что метод должен быть выполнен после инициализации бина
    public void performStartupChecks() {
        checkInternetConnection(); // Проверка интернет-соединения
        checkExchangeConnection(); // Проверка соединения с биржей
        //checkDatabaseConnection(); // Проверка соединения с базой данных
        //checkBackupSystem(); // Проверка системы бэкапов
        //checkAndCreateCoinsTable(); // Проверка и создание таблицы coins
        //checkAndCreateUsersTable(); // Проверка и создание таблицы users
    }

    /**
     * Метод для проверки и создания таблицы users
     */
    private void checkAndCreateUsersTable() {
        String checkTableExistsQuery = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'USERS'";
        Integer tableExists = jdbcTemplate.queryForObject(checkTableExistsQuery, Integer.class);

        if (tableExists == null || tableExists == 0) {
            String createTableQuery = "CREATE TABLE USERS (" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "username VARCHAR(255) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                    "is_active BOOLEAN DEFAULT TRUE NOT NULL" +
                    ")";
            jdbcTemplate.execute(createTableQuery);
            System.out.println("Таблица USERS: создана!");
        } else {
            System.out.println("Таблица USERS: определена!");
        }
    }

    /**
     * Метод для проверки и создания таблицы coins
     */
    private void checkAndCreateCoinsTable() {
        String checkTableExistsQuery = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'COINS'";
        Integer tableExists = jdbcTemplate.queryForObject(checkTableExistsQuery, Integer.class);

        if (tableExists == null || tableExists == 0) {
            String createTableQuery = "CREATE TABLE COINS (" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                    "coin_name VARCHAR(255) NOT NULL, " +
                    "timeframe VARCHAR(255) NOT NULL, " +
                    "date_of_addition TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                    "min_trading_qty DOUBLE, " +
                    "max_trading_qty DOUBLE, " +
                    "min_leverage INTEGER, " +
                    "max_leverage INTEGER, " +
                    "data_check BOOLEAN DEFAULT FALSE NOT NULL, " +
                    "is_counted BOOLEAN DEFAULT FALSE NOT NULL, " +
                    "start_date_time_counted TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                    "end_date_time_counted TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL" +
                    ")";
            jdbcTemplate.execute(createTableQuery);
            System.out.println("Таблица COINS: создана!");
        } else {
            System.out.println("Таблица COINS: определена!");
        }
    }

    /**
     * Метод для проверки интернет-соединения
     */
    private void checkInternetConnection() {
        final String GREEN_TEXT = "\u001B[32m";
        final String RESET_TEXT = "\u001B[0m";

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 3000);
            System.out.println(GREEN_TEXT + "Интернет-соединение: ОК" + RESET_TEXT);
        } catch (IOException e) {
            System.err.println("Интернет-соединение: НЕ УДАЛОСЬ");
        }
    }

    /**
     * Метод для проверки соединения с биржей
     */
    private void checkExchangeConnection() {
        final String GREEN_TEXT = "\u001B[32m";
        final String RED_TEXT = "\u001B[31m";
        final String RESET_TEXT = "\u001B[0m";

        try {
            String response = restTemplate.getForObject(exchangeApiUrl, String.class);
            if (response != null && response.contains("\"retCode\":0")) {
                System.out.println(GREEN_TEXT + "Соединение с биржей: ОК" + RESET_TEXT);
            } else {
                System.err.println(RED_TEXT + "Соединение с биржей: НЕ УДАЛОСЬ" + RESET_TEXT);
            }
        } catch (Exception e) {
            System.err.println(RED_TEXT + "Соединение с биржей: НЕ УДАЛОСЬ" + RESET_TEXT);
        }
    }

    /**
     * Метод для проверки соединения с базой данных
     */
    public void checkDatabaseConnection() {
        String ANSI_GREEN = "\u001B[32m";
        String ANSI_RED = "\u001B[31m";
        String ANSI_RESET = "\u001B[0m";

        try (Connection connection = DriverManager.getConnection(databaseUrl)) {
            if (connection != null && !connection.isClosed()) {
                System.out.println(ANSI_GREEN + "Соединение с базой данных: ОК" + ANSI_RESET);
            } else {
                System.err.println(ANSI_RED + "Соединение с базой данных: НЕ УДАЛОСЬ" + ANSI_RESET);
            }
        } catch (SQLException e) {
            System.err.println(ANSI_RED + "Соединение с базой данных: НЕ УДАЛОСЬ" + ANSI_RESET);
        }
    }

    /**
     * Метод для проверки системы бэкапов
     */
    private void checkBackupSystem() {
        final String ORANGE_TEXT = "\u001B[33m"; // Код оранжевого цвета (yellow в ANSI)
        final String RESET_TEXT = "\u001B[0m"; // Сброс цвета

        // Если реализация не завершена, выводим сообщение "Находится в разработке"
        System.out.println(ORANGE_TEXT + "Система бэкапов: Находится в разработке" + RESET_TEXT);

        // Пример будущей проверки (сейчас закомментирована)
        /*
        try {
            if (Files.exists(Paths.get(backupDiskPath)) && Files.isWritable(Paths.get(backupDiskPath))) {
                System.out.println("Система бэкапов: ОК");
            } else {
                System.err.println("Система бэкапов: НЕ УДАЛОСЬ");
            }
        } catch (Exception e) {
            System.err.println("Система бэкапов: НЕ УДАЛОСЬ");
        }
        */
    }
}