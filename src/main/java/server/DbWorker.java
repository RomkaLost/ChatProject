package server;

import util.CommandMessage;
import util.User;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;

public class DbWorker {
    private static final String DB_URL = "jdbc:postgresql://127.0.0.1:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASS = "kfqnth25";
    private Connection connection;


    public void makeNewUser(User user) {
        String login = user.getPrivateUserInfo().getLogin();
        String password = user.getPrivateUserInfo().getPassword();
        String realName = user.getPublicUserInfo().getRealName();
        String gender = user.getPublicUserInfo().getGender();
        String sql = "INSERT INTO users (login, password, realname, gender) VALUES (?,?,?,?)";
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, realName);
            preparedStatement.setString(4, gender);
            preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            generatedKeys.next();
            user.getPublicUserInfo().setId(generatedKeys.getInt("id"));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public HashMap<Integer, User> loadUsers() {
        String sql = "SELECT * FROM users";
        HashMap<Integer, User> userMap = new HashMap<>();
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                User user = new User();
                user.setPrivate(resultSet.getString("login"), resultSet.getString("password"));
                user.setPublic(resultSet.getString("realName"), resultSet.getString("gender"), resultSet.getInt("id"));
                userMap.put(user.getPublicUserInfo().getId(), user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return userMap;

    }
    public boolean checkLogin(String login) {
        String sql = ("SELECT * FROM users WHERE login = (?)");
        boolean isEmpty = false;

        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            preparedStatement.setString(1, login);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.first() == false) {
                isEmpty = true;
            } else isEmpty = false;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return isEmpty;
    }

    public boolean checkLogPass(User user) {
        String login = user.getPrivateUserInfo().getLogin();
        String password = user.getPrivateUserInfo().getPassword();
        String sql = ("SELECT * FROM users WHERE login = ? AND password = ?");
        boolean logPassChek = false;
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.first() == false) {
                logPassChek = false;
            } else {
                logPassChek = true;
                user.getPublicUserInfo().setId(resultSet.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return logPassChek;
    }

    public void createNewRoom(String roomName, Integer userId) {
        try {
            String sqlForRoom = "INSERT INTO rooms (name) VALUES (?)";
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement preparedStatement = connection.prepareStatement(sqlForRoom, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, roomName);
            preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            generatedKeys.next();
            int roomId = generatedKeys.getInt("id");
            insertUserInRoom(userId, roomId, connection);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void joinRoom(String roomName, Integer userId) {
        try {

            String sqlForRoom = "SELECT id FROM rooms WHERE name = ?";
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement preparedStatement = connection.prepareStatement(sqlForRoom);
            preparedStatement.setString(1, roomName);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            int roomId = resultSet.getInt("id");
            insertUserInRoom(userId, roomId, connection);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void insertUserInRoom(int userId, int roomId, Connection connection) throws SQLException {

        String sqlForRoomAndUsers = "INSERT INTO users_rooms (roomid, userid) VALUES (?,?)";
        PreparedStatement preparedStatementUR = connection.prepareStatement(sqlForRoomAndUsers);
        preparedStatementUR.setInt(1, roomId);
        preparedStatementUR.setInt(2, userId);
        preparedStatementUR.executeUpdate();
    }


    public HashMap<String, HashSet<Integer>> loadRoomMap() {
        HashMap<String, HashSet<Integer>> roomMap = new HashMap<>();
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            String sql = "SELECT name, userid FROM rooms JOIN users_rooms ON rooms.id = users_rooms.roomid";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String roomName = resultSet.getString("name");
                if (!roomMap.containsKey(roomName)) {
                    roomMap.put(roomName, new HashSet<Integer>());
                }
                roomMap.get(roomName).add(resultSet.getInt("userid"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return roomMap;
    }

    public void leaveRoom(Integer userId, String roomName) {
        String sql = "SELECT id FROM rooms WHERE name = ?";
        String sql1 = "DELETE roomId userId FROM users_rooms WHERE roomId = ? AND userId = ?";
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, roomName);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            Integer roomId = resultSet.getInt("id");
            PreparedStatement preparedStatement1 = connection.prepareStatement(sql1);
            preparedStatement1.setInt(1, roomId);
            preparedStatement1.setInt(2, userId);
            preparedStatement1.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

