package com.zmn.pinbotserver.exception.storage.user;


import com.zmn.pinbotserver.exception.storage.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public static final String USER_NOT_FOUND = "Пользователь ID_%d не найден";

    public UserNotFoundException(String message) {
        super(message);
    }
}
