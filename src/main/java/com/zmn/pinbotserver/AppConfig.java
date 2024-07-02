package com.zmn.pinbotserver;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class AppConfig {

    /**
     * Создание бина RestTemplate
     * @return новый экземпляр RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Создание бина DataSource для H2 базы данных
     * @return новый экземпляр DataSource
     */
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:h2:mem:testdb")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .build();
    }

    /**
     * Создание бина JdbcTemplate
     * @param dataSource источник данных, передаваемый в бин
     * @return новый экземпляр JdbcTemplate, настроенный с DataSource
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Создание бина LocalContainerEntityManagerFactoryBean для JPA
     * @param dataSource источник данных, передаваемый в бин
     * @return новый экземпляр LocalContainerEntityManagerFactoryBean
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.zmn.pinbotserver.model");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        emf.setJpaProperties(properties);

        return emf;
    }

    /**
     * Создание бина JpaTransactionManager для управления транзакциями JPA
     * @param entityManagerFactory фабрика менеджера сущностей, передаваемая в бин
     * @return новый экземпляр JpaTransactionManager
     */
    @Bean
    public JpaTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }
}