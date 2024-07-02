-- Создание таблицы USER
CREATE TABLE IF NOT EXISTS USER (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    email VARCHAR(255) NOT NULL UNIQUE,
                                    login VARCHAR(255) NOT NULL UNIQUE,
                                    password_hash VARCHAR(255) NOT NULL,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
                                    is_active BOOLEAN DEFAULT TRUE NOT NULL
);


-- Создание таблицы COINS
CREATE TABLE IF NOT EXISTS COINS (
                                     id INTEGER PRIMARY KEY AUTO_INCREMENT,
                                     coin_name VARCHAR(255) NOT NULL,
                                     timeframe VARCHAR(255) NOT NULL,
                                     date_of_addition TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                     min_trading_qty DOUBLE,
                                     max_trading_qty DOUBLE,
                                     min_leverage INTEGER,
                                     max_leverage INTEGER,
                                     data_check BOOLEAN DEFAULT FALSE NOT NULL,
                                     is_counted BOOLEAN DEFAULT FALSE NOT NULL,
                                     start_date_time_counted TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                     end_date_time_counted TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
