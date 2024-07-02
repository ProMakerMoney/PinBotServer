package com.zmn.pinbotserver.storage.dao.candle;

import com.zmn.pinbotserver.model.candle.Candle;
import com.zmn.pinbotserver.storage.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

@RequiredArgsConstructor
public class CandleDBStorage implements CandleRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<Candle> findAll() {
        return List.of();
    }

    @Override
    public void addCandle(Candle candle){

    }

    @Override
    public void addAllCandle(List<Candle> candles) {

    }

    public void createTableForCoin(String coin) {
        String tableName = coin.toLowerCase() + "_candles";
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "trading_pair VARCHAR(50), " +
                "timestamp TIMESTAMP, " +
                "open DOUBLE, " +
                "high DOUBLE, " +
                "low DOUBLE, " +
                "close DOUBLE, " +
                "volume DOUBLE" +
                ")";
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }
}
