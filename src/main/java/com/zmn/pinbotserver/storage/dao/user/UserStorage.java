package com.zmn.pinbotserver.storage.dao.user;

import com.zmn.pinbotserver.model.user.User;
import com.zmn.pinbotserver.storage.Storage;

import java.util.Collection;
import java.util.Optional;

public interface UserStorage {
    User add(User element);
    User update(User element);
    User get(long elementID);
    Collection<User> getAll();
    boolean contains(long elementID);
    Optional<User> findByUsername(String username);
}