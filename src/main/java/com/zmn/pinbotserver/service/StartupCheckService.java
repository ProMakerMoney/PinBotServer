package com.zmn.pinbotserver.service;

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

    private String backupDiskPath;
    private String exchangeApiUrl = "https://api.bybit.com/v5/market/time"; // URL для проверки соединения с биржей
    private String databaseUrl = "jdbc:postgresql://localhost:5432/pinbotDB"; // URL для подключения к базе данных
    private String databaseUsername = "postgres"; // Имя пользователя базы данных
    private String databasePassword = "9278"; // Пароль базы данных

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
        checkDatabaseConnection(); // Проверка соединения с базой данных
        checkBackupSystem(); // Проверка системы бэкапов
        checkAndCreateCoinsTable(); // Проверка и создание таблицы coins
        checkAndCreateUsersTable(); // Проверка и создание таблицы users
    }

    /**
     * Метод для проверки и создания таблицы users
     */
    private void checkAndCreateUsersTable() {
        String checkTableExistsQuery = "SELECT EXISTS (" +
                "SELECT FROM information_schema.tables " +
                "WHERE table_name = 'users'" +
                ")";
        Boolean tableExists = jdbcTemplate.queryForObject(checkTableExistsQuery, Boolean.class);

        if (tableExists == null || !tableExists) {
            String createTableQuery = "CREATE TABLE USERS (" +
                    "id SERIAL PRIMARY KEY, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "username VARCHAR(255) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "is_active BOOLEAN NOT NULL DEFAULT TRUE" +
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
        String checkTableExistsQuery = "SELECT EXISTS (" +
                "SELECT FROM information_schema.tables " +
                "WHERE table_name = 'coins'" +
                ")";
        Boolean tableExists = jdbcTemplate.queryForObject(checkTableExistsQuery, Boolean.class);

        if (tableExists == null || !tableExists) {
            String createTableQuery = "CREATE TABLE COINS (" +
                    "id SERIAL PRIMARY KEY, " +
                    "coin_name VARCHAR(255) NOT NULL, " +
                    "timeframe VARCHAR(255) NOT NULL, " +
                    "date_of_addition BIGINT DEFAULT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) NOT NULL, " +
                    "min_trading_qty DOUBLE PRECISION, " +
                    "max_trading_qty DOUBLE PRECISION, " +
                    "min_leverage INT, " +
                    "max_leverage INT, " +
                    "data_check BOOLEAN DEFAULT false NOT NULL, " +
                    "is_counted BOOLEAN DEFAULT false NOT NULL, " +
                    "start_date_time_counted BIGINT DEFAULT 0 NOT NULL," +
                    "end_date_time_counted BIGINT DEFAULT 0 NOT NULL"+
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

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
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