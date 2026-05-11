package net.sleepy.io.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseUtil {

    private static final String url = "jdbc:postgresql://localhost:5432/appdb";
    private static final String user = "appuser";
    private static final String password = "apppassword";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
