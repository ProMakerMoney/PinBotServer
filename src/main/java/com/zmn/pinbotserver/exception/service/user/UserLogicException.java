package com.zmn.pinbotserver.exception.service.user;

public class UserLogicException extends RuntimeException {
    public static final String UNABLE_TO_ADD_YOURSELF =
            "Пользователь ID_%d не может добавить сам себя в друзья";
    public static final String UNABLE_TO_DELETE_YOURSELF =
            "Пользователь ID_%d не может удалить себя из друзей";
    public static final String UNABLE_FRIENDS_AMONG_THEMSELVES =
            "Пользователь ID_%d не может запросить общих друзей между собой";

    public UserLogicException(String message) {
        super(message);
    }
}
