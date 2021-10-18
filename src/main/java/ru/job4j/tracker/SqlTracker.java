package ru.job4j.tracker;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SqlTracker implements Store {

    private Connection cn;

    public SqlTracker() {
    }

    public SqlTracker(Connection cn) {
        this.cn = cn;
    }

    public void init() {
        try (InputStream in = SqlTracker.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            Properties config = new Properties();
            config.load(in);
            Class.forName(config.getProperty("driver-class-name"));
            cn = DriverManager.getConnection(
                    config.getProperty("url"),
                    config.getProperty("username"),
                    config.getProperty("password")
            );
        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        if (cn != null) {
            cn.close();
        }
    }

    @Override
    public Item add(Item item) {
        try (PreparedStatement statement = cn.prepareStatement(
                "insert into items(name, created) values (?, ?);",
                Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setString(1, item.getName());
            statement.setTimestamp(2, Timestamp.valueOf(item.getCreated()));
            statement.execute();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return item;
    }

    @Override
    public boolean replace(int id, Item item) {
        boolean result = false;
        try (PreparedStatement statement = cn.prepareStatement(
                "update items "
                        + "set name = ?,"
                        + "created = ?;")
        ) {
            statement.setString(1, item.getName());
            statement.setTimestamp(2, Timestamp.valueOf(item.getCreated()));
            result = statement.executeUpdate() > 0;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return result;
    }

    @Override
    public boolean delete(int id) {
        boolean result = false;
        try (PreparedStatement statement = cn.prepareStatement(
                "delete from items "
                        + "where id = ?;")
        ) {
            statement.setInt(1, id);
            result = statement.executeUpdate() > 0;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return result;
    }

    @Override
    public List<Item> findAll() {
        List<Item> list = new ArrayList<>();
        try (PreparedStatement statement = cn.prepareStatement("select * from items;")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(new Item(
                            resultSet.getString("name"),
                            resultSet.getInt("id"),
                            resultSet.getTimestamp("created").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Item> findByName(String key) {
        List<Item> list = new ArrayList<>();
        try (PreparedStatement statement = cn.prepareStatement(
                "select * from items"
                        + " where name = ?;"
        )) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(new Item(
                            resultSet.getString("name"),
                            resultSet.getInt("id"),
                            resultSet.getTimestamp("created").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return list;
    }

    @Override
    public Item findById(int id) {
        Item item = null;
        try (PreparedStatement statement = cn.prepareStatement(
                "select * from items "
                        + "where id = ?;")
        ) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                item = new Item(
                        resultSet.getString("name"),
                        resultSet.getInt("id"),
                        resultSet.getTimestamp("created").toLocalDateTime()
                );
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return item;
    }
}