package com.zmn.pinbotserver.storage;

import java.util.Collection;

public interface Storage<T> {
    /**
     * Метод добавляет объект в хранилище.
     *
     * @param element добавляемый объект.
     * @return Добавленный объект.
     */
    T add(T element);

    /**
     * Метод обновляет объект в хранилище.
     *
     * @param element обновлённый объект.
     * @return Обновлённый объект.
     */
    T update(T element);

    /**
     * Метод возвращает объект из хранилища
     * по идентификатору.
     *
     * @param elementID идентификатор объекта.
     * @return Объект, принадлежащий идентификатору.
     */
    T get(long elementID);

    /**
     * Метод возвращает коллекцию из всех
     * элементов хранилища.
     *
     * @return Коллекция, состоящая из всех
     * элементов хранилища.
     */
    Collection<T> getAll();

    /**
     * Метод проверяет наличие в хранилище
     * элемента с указанным идентификатором.
     *
     * @param elementID идентификатор элемента.
     * @return Логическое значение, true - если
     * элемент с указанным идентификатором содержится
     * в хранилище, и false - если нет.
     */
    boolean contains(long elementID);
}
