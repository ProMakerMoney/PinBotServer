package com.zmn.pinbotserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@Configuration
public class AppConfig {
    @Bean // Объявляем создание бина
    public RestTemplate restTemplate() {
        return new RestTemplate(); // Создаем и возвращаем новый экземпляр RestTemplate
    }

    @Bean // Объявляем создание бина DataSource
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver"); // Укажите драйвер PostgreSQL
        dataSource.setUrl("jdbc:postgresql://localhost:5432/pinbotDB"); // Укажите URL вашей базы данных PostgreSQL
        dataSource.setUsername("postgres"); // Укажите имя пользователя для вашей базы данных PostgreSQL
        dataSource.setPassword("9278"); // Укажите пароль для вашей базы данных PostgreSQL
        return dataSource;
    }

    @Bean // Объявляем создание бина JdbcTemplate
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource); // Создаем и возвращаем новый экземпляр JdbcTemplate с DataSource
    }
}
