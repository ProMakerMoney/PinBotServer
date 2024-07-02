package com.zmn.pinbotserver.storage.dao.user;

import com.zmn.pinbotserver.model.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {
    private final String url = "jdbc:h2:file:./data/mydb"; // Путь к базе данных
    private final String user = "sa";
    private final String password = "password";

    @Override
    public User add(User element) {
        String sql = "INSERT INTO USER (email, login, password_hash, created_at, updated_at, is_active) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE)";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, element.getEmail());
            statement.setString(2, element.getLogin());
            statement.setString(3, element.getPasswordHash());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    element.setId(generatedKeys.getLong(1));
                }
            }
            return element;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public User update(User element) {
        String sql = "UPDATE USER SET email = ?, login = ?, password_hash = ?, updated_at = CURRENT_TIMESTAMP, is_active = ? WHERE id = ?";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, element.getEmail());
            statement.setString(2, element.getLogin());
            statement.setString(3, element.getPasswordHash());
            statement.setBoolean(4, element.getIsActive());
            statement.setLong(5, element.getId());

            int rowsUpdated = statement.executeUpdate();
            return rowsUpdated > 0 ? element : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public User get(long elementID) {
        String sql = "SELECT * FROM USER WHERE id = ?";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, elementID);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToUser(resultSet);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Collection<User> getAll() {
        String sql = "SELECT * FROM USER";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(mapRowToUser(resultSet));
            }
            return users;
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @Override
    public boolean contains(long elementID) {
        String sql = "SELECT 1 FROM USER WHERE id = ?";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, elementID);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private User mapRowToUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setEmail(resultSet.getString("email"));
        user.setLogin(resultSet.getString("login"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        user.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        user.setActive(resultSet.getBoolean("is_active"));
        return user;
    }
}
