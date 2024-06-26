package com.zmn.pinbotserver.historicalData.repository;


import com.zmn.pinbotserver.historicalData.model.coin.Coin;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс для работы с монетами в базе данных.
 * Определяет основные операции для CRUD-операций с сущностью Coin.
 */
public interface CoinRepository {

    /**
     * Метод для получения всех монет из базы данных.
     *
     * @return список всех монет.
     */
    List<Coin> findAll();

    /**
     * Метод для поиска монеты по её идентификатору.
     *
     * @param id идентификатор монеты.
     * @return Optional с найденной монетой или пустой Optional, если монета не найдена.
     */
    Optional<Coin> findById(Long id);

    /**
     * Метод для сохранения монеты в базу данных.
     * Если монета с таким идентификатором уже существует, она будет обновлена.
     *
     * @param coin монета для сохранения.
     * @return сохраненная монета.
     */
    Coin save(Coin coin);

    /**
     * Метод для удаления монеты из базы данных по её идентификатору.
     *
     * @param id идентификатор монеты, которую нужно удалить.
     */
    void deleteById(Long id);

    /**
     * Метод для проверки существования монеты в базе данных по её идентификатору.
     *
     * @param id идентификатор монеты.
     * @return true, если монета существует, иначе false.
     */
    boolean existsById(Long id);

    /**
     * Метод для обновления информации о монете в базе данных.
     * Принимает объект Coin и обновляет соответствующую запись в таблице coins.
     *
     * @param coin объект Coin, содержащий обновленные данные.
     * @return обновленный объект Coin.
     */
    Coin updateCoin(Coin coin);

    Optional<Coin> findByCoinName(String name);
}
