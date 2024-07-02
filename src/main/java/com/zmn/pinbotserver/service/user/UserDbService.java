package com.zmn.pinbotserver.service.user;


import com.zmn.pinbotserver.exception.storage.user.UserAlreadyExistsException;
import com.zmn.pinbotserver.exception.storage.user.UserNotFoundException;
import com.zmn.pinbotserver.model.user.User;
import com.zmn.pinbotserver.storage.dao.user.UserStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

import static com.zmn.pinbotserver.exception.storage.user.UserAlreadyExistsException.USER_ALREADY_EXISTS;
import static com.zmn.pinbotserver.exception.storage.user.UserNotFoundException.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDbService implements UserService {
    private final UserStorage userStorage;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User add(User user) {
        checkUserToAdd(user);
        return userStorage.add(user);
    }

    @Override
    public User update(User user) {
        checkUserToUpdate(user);
        return userStorage.update(user);
    }

    @Override
    public User get(long userID) {
        if (!userStorage.contains(userID)) {
            log.warn("Не удалось вернуть пользователя: {}.", String.format(USER_NOT_FOUND, userID));
            throw new UserNotFoundException(String.format(USER_NOT_FOUND, userID));
        }
        return userStorage.get(userID);
    }

    @Override
    public Collection<User> getAll() {
        return userStorage.getAll();
    }

    @Override
    public Optional<User> register(String email, String username, String password) {
        if (userStorage.getAll().stream().anyMatch(user -> user.getEmail().equals(email) || user.getLogin().equals(username))) {
            return Optional.empty();
        }

        User user = new User();
        user.setEmail(email);
        user.setLogin(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        userStorage.add(user);
        return Optional.of(user);
    }

    @Override
    public Optional<User> authenticate(String email, String password) {
        return userStorage.getAll().stream()
                .filter(user -> user.getEmail().equals(email) && passwordEncoder.matches(password, user.getPasswordHash()))
                .findFirst();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userStorage.getAll().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("USER")
                .build();
    }

    private void checkUserToAdd(User user) {
        log.debug("checkUserToAdd({}).", user);
        String msg = "Не удалось добавить пользователя: {}.";
        if (user.getId() != null && user.getId() != 0) {
            if (userStorage.contains(user.getId())) {
                log.warn(msg, String.format(USER_ALREADY_EXISTS, user.getId()));
                throw new UserAlreadyExistsException(String.format(USER_ALREADY_EXISTS, user.getId()));
            } else {
                log.warn(msg, "Запрещено устанавливать ID вручную");
                throw new IllegalArgumentException("Запрещено устанавливать ID вручную");
            }
        }
    }

    private void checkUserToUpdate(User user) {
        if (!userStorage.contains(user.getId())) {
            log.warn("Не удалось обновить пользователя: {}.", String.format(USER_NOT_FOUND, user.getId()));
            throw new UserNotFoundException(String.format(USER_NOT_FOUND, user.getId()));
        }
    }
}