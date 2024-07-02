package com.zmn.pinbotserver.service.user;

import com.zmn.pinbotserver.model.user.User;
import com.zmn.pinbotserver.service.Service;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Optional;

public interface UserService extends Service<User> {
    User add(User user);
    User update(User user);
    User get(long userID);
    Collection<User> getAll();
    Optional<User> register(String email, String username, String password);
    Optional<User> authenticate(String email, String password);
    UserDetails loadUserByUsername(String email);
}
