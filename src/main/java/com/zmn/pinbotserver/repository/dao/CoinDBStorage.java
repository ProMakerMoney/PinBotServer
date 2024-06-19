package com.zmn.pinbotserver.repository.dao;

import com.zmn.pinbotserver.model.coin.Coin;
import com.zmn.pinbotserver.repository.CoinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Класс для работы с монетами в базе данных.
 * Реализует интерфейс CoinRepository.
 */
@Repository
public class CoinDBStorage implements CoinRepository {

    private final JdbcTemplate jdbcTemplate;

    private SimpleJdbcInsert insertCoin;

    /**
     * Конструктор, инициализирующий jdbcTemplate.
     *
     * @param jdbcTemplate JdbcTemplate для выполнения SQL-запросов.
     */
    @Autowired
    public CoinDBStorage(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertCoin = new SimpleJdbcInsert(dataSource).withTableName("coins").usingGeneratedKeyColumns("id");
    }

    /**
     * Метод для получения всех монет из базы данных.
     *
     * @return список всех монет.
     */
    @Override
    public List<Coin> findAll() {
        String sql = "SELECT * FROM COINS";
        return jdbcTemplate.query(sql, new CoinRowMapper());
    }

    /**
     * Метод для поиска монеты по её идентификатору.
     *
     * @param id идентификатор монеты.
     * @return Optional с найденной монетой или пустой Optional, если монета не найдена.
     */
    @Override
    public Optional<Coin> findById(Long id) {
        String sql = "SELECT * FROM COINS WHERE id = ?";
        List<Coin> coins = jdbcTemplate.query(sql, new CoinRowMapper(), id);
        if (coins.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(coins.get(0));
        }
    }

    /**
     * Метод для сохранения монеты в базу данных.
     * Если монета с таким идентификатором уже существует, она будет обновлена.
     *
     * @param coin монета для сохранения.
     * @return сохраненная монета.
     */
    @Override
    public Coin save(Coin coin) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("coin_name", coin.getCoinName());
        parameters.put("timeframe", coin.getTimeframe());
        parameters.put("date_of_addition", coin.getDateOfAddition());
        parameters.put("min_trading_qty", coin.getMinTradingQty());
        parameters.put("max_trading_qty", coin.getMaxTradingQty());
        parameters.put("min_leverage", coin.getMinLeverage());
        parameters.put("max_leverage", coin.getMaxLeverage());
        parameters.put("data_check", coin.getDataCheck());
        parameters.put("is_counted", coin.getIsCounted());
        parameters.put("date_time_counted", coin.getDateTimeCounted());

        Number newId = insertCoin.executeAndReturnKey(new MapSqlParameterSource(parameters));
        coin.setId(newId.longValue());
        return coin;
    }

    /**
     * Метод для удаления монеты из базы данных по её идентификатору.
     *
     * @param id идентификатор монеты, которую нужно удалить.
     */
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM COINS WHERE COIN_ID = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * Метод для проверки существования монеты по её идентификатору.
     *
     * @param id идентификатор монеты.
     * @return true, если монета существует, иначе false.
     */
    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM COINS WHERE COIN_ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    /**
     * Внутренний класс для маппинга строк из базы данных в объекты Coin.
     */
    private static class CoinRowMapper implements RowMapper<Coin> {
        @Override
        public Coin mapRow(ResultSet rs, int rowNum) throws SQLException {
            Coin coin = new Coin();
            coin.setId(rs.getLong("id"));
            coin.setCoinName(rs.getString("coin_name"));
            coin.setMinTradingQty(rs.getDouble("min_trading_qty"));
            coin.setMaxTradingQty(rs.getDouble("max_trading_qty"));
            coin.setMinLeverage(rs.getInt("min_leverage"));
            coin.setMaxLeverage(rs.getInt("max_leverage"));
            coin.setTimeframe(rs.getString("timeframe"));
            coin.setDateOfAddition(rs.getLong("date_of_addition"));
            coin.setDateTimeCounted(rs.getLong("date_time_counted"));
            coin.setIsCounted(rs.getBoolean("is_counted"));
            coin.setDataCheck(rs.getBoolean("data_check"));
            return coin;
        }
    }
}