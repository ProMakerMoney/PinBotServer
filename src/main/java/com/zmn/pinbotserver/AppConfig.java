package com.zmn.pinbotserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@Configuration // Аннотация, обозначающая, что данный класс является конфигурационным для Spring
public class AppConfig {

    /**
     * Создание бина RestTemplate
     * @return новый экземпляр RestTemplate
     */
    @Bean // Аннотация, обозначающая, что данный метод создает бин
    public RestTemplate restTemplate() {
        return new RestTemplate(); // Создаем и возвращаем новый экземпляр RestTemplate
    }

    /**
     * Создание бина DataSource для подключения к базе данных PostgreSQL
     * @return новый экземпляр DriverManagerDataSource, настроенный для подключения к базе данных PostgreSQL
     */
    @Bean // Аннотация, обозначающая, что данный метод создает бин DataSource
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver"); // Указываем драйвер PostgreSQL
        dataSource.setUrl("jdbc:postgresql://localhost:5432/pinbotDB"); // Указываем URL базы данных PostgreSQL
        dataSource.setUsername("postgres"); // Указываем имя пользователя для базы данных PostgreSQL
        dataSource.setPassword("9278"); // Указываем пароль для базы данных PostgreSQL
        return dataSource;
    }

    /**
     * Создание бина JdbcTemplate
     * @param dataSource источник данных, передаваемый в бин
     * @return новый экземпляр JdbcTemplate, настроенный с DataSource
     */
    @Bean // Аннотация, обозначающая, что данный метод создает бин JdbcTemplate
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource); // Создаем и возвращаем новый экземпляр JdbcTemplate с DataSource
    }
}
